package com.aetherium.client;

import com.aetherium.util.AetheriumLog;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * The far terrain renderer for Minecraft 1.17+ ({@link FarTerrain}): plain OpenGL 3.0 calls and a
 * GLSL 150 program of its own (the same language vanilla's core shaders use since 1.17, so it runs
 * wherever the game runs, gl4es included).
 *
 * <p>Vanilla's GPU API changed shape five times between 1.17 and 26.x; this class does not touch
 * it. It owns a framebuffer around the main colour and depth textures, one vertex array per page
 * of at most {@link #QUADS_PER_PAGE} quads (32000 vertices, 16-bit indices, under gl4es's draw
 * limit) and a shared index buffer, and puts every piece of GL state it changes back exactly as it
 * found it, so vanilla's state caches stay right. The fog (vanilla's fog colour, linear, the same
 * cylindrical distance as the terrain) and the daylight dimming happen in the shader, so the mesh
 * only has to be rebuilt when the camera moves to another chunk or new chunks arrive.</p>
 *
 * <p>Render thread only.</p>
 */
public final class FarTerrainGl {
    private FarTerrainGl() {
    }

    private static final AetheriumLog LOGGER = AetheriumLog.of(FarTerrainGl.class);

    /** 32000 vertices: gl4es splits bigger draws, and 16-bit indices reach 65535. */
    public static final int QUADS_PER_PAGE = 8000;
    /** Position (3 floats) + colour (4 unsigned bytes). */
    public static final int BYTES_PER_VERTEX = 16;
    public static final int BYTES_PER_PAGE = QUADS_PER_PAGE * 4 * BYTES_PER_VERTEX;

    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_FLOAT = 0x1406;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_UNSIGNED_SHORT = 0x1403;
    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_ARRAY_BUFFER = 0x8892;
    private static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    private static final int GL_ARRAY_BUFFER_BINDING = 0x8894;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;
    private static final int GL_READ_FRAMEBUFFER = 0x8CA8;
    private static final int GL_DRAW_FRAMEBUFFER_BINDING = 0x8CA6;
    private static final int GL_READ_FRAMEBUFFER_BINDING = 0x8CAA;
    private static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    private static final int GL_DEPTH_ATTACHMENT = 0x8D00;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_CURRENT_PROGRAM = 0x8B8D;
    private static final int GL_VERTEX_ARRAY_BINDING = 0x85B5;
    private static final int GL_VIEWPORT = 0x0BA2;
    private static final int GL_BLEND = 0x0BE2;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_DEPTH_FUNC = 0x0B74;
    private static final int GL_DEPTH_WRITEMASK = 0x0B72;
    private static final int GL_LEQUAL = 0x0203;
    private static final int GL_GEQUAL = 0x0206;
    private static final int GL_CULL_FACE = 0x0B44;
    private static final int GL_SCISSOR_TEST = 0x0C11;
    private static final int GL_STENCIL_TEST = 0x0B90;
    private static final int GL_COLOR_WRITEMASK = 0x0C23;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_PIXEL_PACK_BUFFER = 0x88EB;
    private static final int GL_PIXEL_PACK_BUFFER_BINDING = 0x88ED;
    private static final int GL_VERTEX_SHADER = 0x8B31;
    private static final int GL_FRAGMENT_SHADER = 0x8B30;
    private static final int GL_COMPILE_STATUS = 0x8B81;
    private static final int GL_LINK_STATUS = 0x8B82;

    private static final String VERTEX = "#version 150\n"
            + "in vec3 Position;\n"
            + "in vec4 Color;\n"
            + "uniform mat4 Mvp;\n"
            + "uniform vec3 Offset;\n"
            + "out vec4 vColor;\n"
            + "out float vDistance;\n"
            + "void main() {\n"
            + "    vec3 p = Position + Offset;\n"
            + "    gl_Position = Mvp * vec4(p, 1.0);\n"
            + "    vColor = Color;\n"
            + "    vDistance = max(length(p.xz), abs(p.y));\n"
            + "}\n";
    private static final String FRAGMENT = "#version 150\n"
            + "in vec4 vColor;\n"
            + "in float vDistance;\n"
            + "uniform vec4 FogColor;\n"
            + "uniform vec2 Fog;\n"
            + "out vec4 fragColor;\n"
            + "void main() {\n"
            + "    float f = clamp((vDistance - Fog.x) / max(Fog.y - Fog.x, 1.0), 0.0, 1.0);\n"
            + "    fragColor = vec4(mix(vColor.rgb * FogColor.a, FogColor.rgb, f), 1.0);\n"
            + "}\n";

    private static final class Page {
        int vao;
        int vbo;
        int quads;
    }

    private static final List<Page> PAGES = new ArrayList<Page>();
    private static int pageCount;
    private static int program;
    private static boolean programBroken;
    private static int mvpLocation;
    private static int offsetLocation;
    private static int fogLocation;
    private static int fogColorLocation;
    private static int indexBuffer;
    private static int framebuffer;
    private static int fbColor;
    private static int fbDepth;
    private static int fbWidth;
    private static int fbHeight;
    private static final int[] VIEWPORT = new int[4];
    private static final ByteBuffer BOOLS = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());

    /** A buffer the mesh worker can fill with one page of vertices. */
    public static ByteBuffer newPageBuffer() {
        return ByteBuffer.allocateDirect(BYTES_PER_PAGE).order(ByteOrder.nativeOrder());
    }

    /** Pages ready to draw. */
    public static int pages() {
        return pageCount;
    }

    /**
     * Uploads the worker's pages ({@code data[i]} flipped, {@code quads[i]} quads); pages beyond
     * {@code count} are freed. False when the driver rejected the program.
     */
    public static boolean upload(final List<ByteBuffer> data, final int[] quads, final int count) {
        if (!ensureProgram()) {
            return false;
        }
        final int oldVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldArray = GL11.glGetInteger(GL_ARRAY_BUFFER_BINDING);
        try {
            for (int i = 0; i < count; i++) {
                if (i >= PAGES.size()) {
                    PAGES.add(create());
                }
                final Page page = PAGES.get(i);
                GL15.glBindBuffer(GL_ARRAY_BUFFER, page.vbo);
                GL15.glBufferData(GL_ARRAY_BUFFER, data.get(i), GL_STATIC_DRAW);
                page.quads = quads[i];
            }
        } finally {
            GL30.glBindVertexArray(oldVao);
            GL15.glBindBuffer(GL_ARRAY_BUFFER, oldArray);
        }
        while (PAGES.size() > count) {
            delete(PAGES.remove(PAGES.size() - 1));
        }
        pageCount = count;
        return true;
    }

    /** A page's vertex array: attribute 0 position, 1 colour, the shared quad index buffer. */
    private static Page create() {
        final Page page = new Page();
        page.vao = GL30.glGenVertexArrays();
        page.vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(page.vao);
        GL15.glBindBuffer(GL_ARRAY_BUFFER, page.vbo);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, BYTES_PER_VERTEX, 0L);
        GL20.glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, BYTES_PER_VERTEX, 12L);
        GL20.glEnableVertexAttribArray(0);
        GL20.glEnableVertexAttribArray(1);
        // Element buffer binding is vertex array state: it stays with this page's array only.
        GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        return page;
    }

    private static void delete(final Page page) {
        GL30.glDeleteVertexArrays(page.vao);
        GL15.glDeleteBuffers(page.vbo);
    }

    /** Frees every GL object (the program and index buffer stay for the next use). */
    public static void release() {
        for (final Page page : PAGES) {
            delete(page);
        }
        PAGES.clear();
        pageCount = 0;
        if (framebuffer != 0) {
            GL30.glDeleteFramebuffers(framebuffer);
            framebuffer = 0;
            fbColor = 0;
            fbDepth = 0;
        }
    }

    /**
     * Draws every page into the main colour and depth textures.
     *
     * @param mvp     projection x model-view, column-major
     * @param fog       start, end (blocks), fog colour r, g, b, daylight factor (0..1)
     * @param reversedZ the depth buffer holds 1 near and 0 far (26.2+), so nearer means greater
     * @return false when a texture cannot be reached or the framebuffer is incomplete
     */
    public static boolean draw(final int color, final int depth, final int width, final int height, final float[] mvp,
                               final float ox, final float oy, final float oz, final float[] fog,
                               final boolean reversedZ) {
        if (pageCount == 0) {
            return true;
        }
        if (color <= 0 || depth <= 0 || width <= 0 || height <= 0 || !ensureProgram()
                || !attach(color, depth, width, height)) {
            return false;
        }
        final int drawFb = GL11.glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        final int oldProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
        final int oldVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        GL11.glGetIntegerv(GL_VIEWPORT, VIEWPORT);
        final boolean blend = GL11.glIsEnabled(GL_BLEND);
        final boolean depthTest = GL11.glIsEnabled(GL_DEPTH_TEST);
        final boolean cull = GL11.glIsEnabled(GL_CULL_FACE);
        final boolean scissor = GL11.glIsEnabled(GL_SCISSOR_TEST);
        final boolean stencil = GL11.glIsEnabled(GL_STENCIL_TEST);
        final int depthFunc = GL11.glGetInteger(GL_DEPTH_FUNC);
        BOOLS.clear();
        GL11.glGetBooleanv(GL_DEPTH_WRITEMASK, BOOLS);
        final boolean depthMask = BOOLS.get(0) != 0;
        BOOLS.clear();
        GL11.glGetBooleanv(GL_COLOR_WRITEMASK, BOOLS);
        final boolean r = BOOLS.get(0) != 0;
        final boolean g = BOOLS.get(1) != 0;
        final boolean b = BOOLS.get(2) != 0;
        final boolean a = BOOLS.get(3) != 0;
        try {
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
            GL11.glViewport(0, 0, width, height);
            GL11.glDisable(GL_BLEND);
            GL11.glDisable(GL_CULL_FACE);
            GL11.glDisable(GL_SCISSOR_TEST);
            GL11.glDisable(GL_STENCIL_TEST);
            GL11.glEnable(GL_DEPTH_TEST);
            GL11.glDepthFunc(reversedZ ? GL_GEQUAL : GL_LEQUAL);
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            GL20.glUseProgram(program);
            GL20.glUniformMatrix4fv(mvpLocation, false, mvp);
            GL20.glUniform3f(offsetLocation, ox, oy, oz);
            GL20.glUniform2f(fogLocation, fog[0], fog[1]);
            GL20.glUniform4f(fogColorLocation, fog[2], fog[3], fog[4], fog[5]);
            for (int i = 0; i < pageCount; i++) {
                final Page page = PAGES.get(i);
                GL30.glBindVertexArray(page.vao);
                GL11.glDrawElements(GL_TRIANGLES, page.quads * 6, GL_UNSIGNED_SHORT, 0L);
            }
        } finally {
            GL30.glBindVertexArray(oldVao);
            GL20.glUseProgram(oldProgram);
            GL11.glColorMask(r, g, b, a);
            GL11.glDepthMask(depthMask);
            GL11.glDepthFunc(depthFunc);
            enable(GL_BLEND, blend);
            enable(GL_DEPTH_TEST, depthTest);
            enable(GL_CULL_FACE, cull);
            enable(GL_SCISSOR_TEST, scissor);
            enable(GL_STENCIL_TEST, stencil);
            GL11.glViewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
            GL30.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, drawFb);
        }
        return true;
    }

    /** Points our framebuffer at the main colour and depth textures (re-attached when they change). */
    private static boolean attach(final int color, final int depth, final int width, final int height) {
        if (framebuffer != 0 && fbColor == color && fbDepth == depth && fbWidth == width && fbHeight == height) {
            return true;
        }
        final int read = GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        try {
            if (framebuffer == 0) {
                framebuffer = GL30.glGenFramebuffers();
            }
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
            GL30.glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
            GL30.glFramebufferTexture2D(GL_READ_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
            if (GL30.glCheckFramebufferStatus(GL_READ_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                fbColor = 0;
                LOGGER.warn("Far terrain: the framebuffer around the main target is incomplete");
                return false;
            }
            fbColor = color;
            fbDepth = depth;
            fbWidth = width;
            fbHeight = height;
            return true;
        } finally {
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
        }
    }

    private static void enable(final int cap, final boolean on) {
        if (on) {
            GL11.glEnable(cap);
        } else {
            GL11.glDisable(cap);
        }
    }

    private static boolean ensureProgram() {
        if (program != 0) {
            return true;
        }
        if (programBroken) {
            return false;
        }
        final int oldProgram = GL11.glGetInteger(GL_CURRENT_PROGRAM);
        final int oldVao = GL11.glGetInteger(GL_VERTEX_ARRAY_BINDING);
        final int oldArray = GL11.glGetInteger(GL_ARRAY_BUFFER_BINDING);
        try {
            final int vs = shader(GL_VERTEX_SHADER, VERTEX);
            final int fs = vs == 0 ? 0 : shader(GL_FRAGMENT_SHADER, FRAGMENT);
            if (fs == 0) {
                if (vs != 0) {
                    GL20.glDeleteShader(vs);
                }
                programBroken = true;
                return false;
            }
            final int p = GL20.glCreateProgram();
            GL20.glAttachShader(p, vs);
            GL20.glAttachShader(p, fs);
            GL20.glBindAttribLocation(p, 0, "Position");
            GL20.glBindAttribLocation(p, 1, "Color");
            GL30.glBindFragDataLocation(p, 0, "fragColor");
            GL20.glLinkProgram(p);
            GL20.glDeleteShader(vs);
            GL20.glDeleteShader(fs);
            if (GL20.glGetProgrami(p, GL_LINK_STATUS) == 0) {
                LOGGER.warn("Far terrain: the driver could not link the program: " + GL20.glGetProgramInfoLog(p));
                GL20.glDeleteProgram(p);
                programBroken = true;
                return false;
            }
            mvpLocation = GL20.glGetUniformLocation(p, "Mvp");
            offsetLocation = GL20.glGetUniformLocation(p, "Offset");
            fogLocation = GL20.glGetUniformLocation(p, "Fog");
            fogColorLocation = GL20.glGetUniformLocation(p, "FogColor");
            // Quad i = vertices 4i..4i+3 as two triangles; shared by every page. Bound with a
            // temporary vertex array so no vanilla array's element binding changes.
            final int tmp = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(tmp);
            final ByteBuffer indices = ByteBuffer.allocateDirect(QUADS_PER_PAGE * 6 * 2).order(ByteOrder.nativeOrder());
            for (int q = 0; q < QUADS_PER_PAGE; q++) {
                final int v = q * 4;
                indices.putShort((short) v).putShort((short) (v + 1)).putShort((short) (v + 2));
                indices.putShort((short) (v + 2)).putShort((short) (v + 3)).putShort((short) v);
            }
            indices.flip();
            indexBuffer = GL15.glGenBuffers();
            GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
            GL30.glBindVertexArray(oldVao);
            GL30.glDeleteVertexArrays(tmp);
            program = p;
            return true;
        } finally {
            GL20.glUseProgram(oldProgram);
            GL30.glBindVertexArray(oldVao);
            GL15.glBindBuffer(GL_ARRAY_BUFFER, oldArray);
        }
    }

    private static int shader(final int type, final String text) {
        final int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, text);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, GL_COMPILE_STATUS) == 0) {
            LOGGER.warn("Far terrain: the driver rejected a shader: " + GL20.glGetShaderInfoLog(s));
            GL20.glDeleteShader(s);
            return 0;
        }
        return s;
    }

    /**
     * CI smoke test: one page with a quad covering the screen (identity matrix, on the near plane) drawn into
     * the main target, read back at the centre, then everything freed again.
     */
    public static String selfTest(final int color, final int depth, final int width, final int height,
                                  final boolean reversedZ) {
        if (color <= 0 || depth <= 0) {
            return "unavailable (main target is not an OpenGL texture)";
        }
        for (int guard = 0; guard < 16 && GL11.glGetError() != 0; guard++) {
            // drop errors left by earlier code, so only this test's calls are reported
        }
        final ByteBuffer page = newPageBuffer();
        final float[][] corners = {{-1f, -1f}, {1f, -1f}, {1f, 1f}, {-1f, 1f}};
        for (final float[] c : corners) {
            // On the near plane in either depth convention, so it is in front of everything drawn.
            page.putFloat(c[0]).putFloat(c[1]).putFloat(reversedZ ? 0.999f : -0.999f);
            page.put((byte) 255).put((byte) 0).put((byte) 0).put((byte) 255);
        }
        page.flip();
        final List<ByteBuffer> data = new ArrayList<ByteBuffer>();
        data.add(page);
        if (!upload(data, new int[] {1}, 1)) {
            return "FAILED: the driver rejected the program";
        }
        final float[] identity = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        // Fog starts far beyond the quad and daylight is full: the pixel must come out pure red.
        final float[] fog = {1.0E6f, 2.0E6f, 0f, 0f, 1f, 1f};
        final boolean drawn = draw(color, depth, width, height, identity, 0f, 0f, 0f, fog, reversedZ);
        String result;
        if (!drawn) {
            result = "FAILED: framebuffer around the main target incomplete";
        } else {
            final int read = GL11.glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
            final int pack = GL11.glGetInteger(GL_PIXEL_PACK_BUFFER_BINDING);
            final ByteBuffer pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
            GL15.glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);
            GL11.glReadPixels(width / 2, height / 2, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
            GL30.glBindFramebuffer(GL_READ_FRAMEBUFFER, read);
            GL15.glBindBuffer(GL_PIXEL_PACK_BUFFER, pack);
            final int pr = pixel.get(0) & 0xFF;
            final int pg = pixel.get(1) & 0xFF;
            final int pb = pixel.get(2) & 0xFF;
            final String rgb = String.format("%02x%02x%02x", pr, pg, pb);
            result = pr > 200 && pg < 60 && pb < 60 ? "ok (pixel " + rgb + ")" : "FAILED: pixel " + rgb;
        }
        release();
        final int error = GL11.glGetError();
        return error == 0 ? result : result + " (GL error " + error + ")";
    }
}
