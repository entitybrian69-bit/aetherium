package com.aetherium.client;

import com.aetherium.render.CompactTerrain;
import com.aetherium.util.AetheriumLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * Aetherium's experimental terrain renderer (opt-in; Minecraft 1.16.5 only for now).
 *
 * <p>Vanilla 1.16.5 draws every chunk section of a layer with the fixed-function pipeline:
 * client-state vertex pointers, a push/load/mult/pop of the GL modelview matrix and a
 * {@code glDrawArrays(GL_QUADS)}. Desktop drivers cope; GL4ES (Android) has to emulate the
 * matrix stack, rebuild a fixed-function shader for the state, and convert every quad list into
 * triangles on the CPU, per section, per frame. This class draws the same vertex buffers vanilla
 * built with one small GLSL 1.20 program, a single shared quad-to-triangle index buffer, and one
 * uniform per section for its offset.</p>
 *
 * <p>Version-neutral on purpose: it knows LWJGL and nothing about Minecraft classes. The version
 * mixin hands it the section VBO ids, vertex counts and camera-relative origins; vanilla keeps
 * meshing, uploading, culling, sorting and the render-state setup (textures, lightmap, blend,
 * depth) exactly as before.</p>
 *
 * <p><b>Aetherium pipeline (1.3.0).</b> Sections uploaded through {@code VertexBufferUploadMixin}
 * are in {@link CompactTerrain}'s 16-byte format, sorted into face-direction groups. They are drawn
 * by a second program that decodes the packed attributes, and only the groups that can face the
 * camera are drawn (in one or two index ranges per section). Buffers that did not fit the format
 * keep vanilla's layout and are drawn by the original program.</p>
 *
 * <p>Failure policy: if a shader does not compile or link, or anything throws, the renderer
 * turns itself off for the session, logs once, asks for one chunk rebuild (vanilla cannot read
 * compact buffers) and vanilla draws from then on.</p>
 *
 * <p>Render thread only.</p>
 */
public final class ChunkRenderer {

    // GL constants (local so nothing depends on stub values).
    static final int GL_TRIANGLES = 0x0004;
    static final int GL_UNSIGNED_BYTE = 0x1401;
    static final int GL_SHORT = 0x1402;
    static final int GL_UNSIGNED_SHORT = 0x1403;
    static final int GL_FLOAT = 0x1406;
    static final int GL_FOG = 0x0B60;
    static final int GL_FOG_DENSITY = 0x0B62;
    static final int GL_FOG_START = 0x0B63;
    static final int GL_FOG_END = 0x0B64;
    static final int GL_FOG_MODE = 0x0B65;
    static final int GL_FOG_COLOR = 0x0B66;
    static final int GL_PROJECTION_MATRIX = 0x0BA7;
    static final int GL_LINEAR = 0x2601;
    static final int GL_EXP = 0x0800;
    static final int GL_EXP2 = 0x0801;
    static final int GL_ARRAY_BUFFER = 0x8892;
    static final int GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    static final int GL_STATIC_DRAW = 0x88E4;
    static final int GL_FRAGMENT_SHADER = 0x8B30;
    static final int GL_VERTEX_SHADER = 0x8B31;
    static final int GL_COMPILE_STATUS = 0x8B81;
    static final int GL_LINK_STATUS = 0x8B82;

    /** Vanilla's BLOCK vertex format: position 3f, color 4ub, uv0 2f, uv2 2s, normal 3b + pad. */
    static final int STRIDE = 32;
    static final int OFFSET_POS = 0;
    static final int OFFSET_COLOR = 12;
    static final int OFFSET_UV0 = 16;
    static final int OFFSET_UV2 = 24;

    static final int ATTR_POS = 0;
    static final int ATTR_COLOR = 1;
    static final int ATTR_UV0 = 2;
    static final int ATTR_UV2 = 3;

    /** 16-bit indices address 65 536 vertices = 16 384 quads per draw; bigger sections are split. */
    static final int MAX_QUADS_PER_DRAW = 16384;
    static final int MAX_VERTICES_PER_DRAW = MAX_QUADS_PER_DRAW * 4;

    static final int ATTR_COMPACT_POS = 0;
    static final int ATTR_COMPACT_COLOR = 1;
    static final int ATTR_COMPACT_UV = 2;

    /** Hidden quads drawn anyway to save a draw call (see {@link CompactTerrain#mergeRuns}). */
    static final int MERGE_GAP_QUADS = 256;

    /** Fog mode uniform values. */
    static final int FOG_NONE = -1;
    static final int FOG_LINEAR = 0;
    static final int FOG_EXP = 1;
    static final int FOG_EXP2 = 2;

    static final String VERTEX_SHADER = String.join("\n",
            "#version 120",
            "attribute vec3 a_pos;",
            "attribute vec4 a_color;",
            "attribute vec2 a_uv0;",
            "attribute vec2 a_uv2;",
            "uniform mat4 u_projection;",
            "uniform mat4 u_modelView;",
            "uniform vec3 u_offset;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_distance;",
            "void main() {",
            "    vec4 view = u_modelView * vec4(a_pos + u_offset, 1.0);",
            "    gl_Position = u_projection * view;",
            "    v_color = a_color;",
            "    v_uv0 = a_uv0;",
            // Vanilla's lightmap texture matrix: scale 1/256, translate 8 texels.
            "    v_uv2 = (a_uv2 + vec2(8.0)) / 256.0;",
            "    v_distance = length(view.xyz);",
            "}",
            "");

    /**
     * Compact vertices: x, y, z, light as four unsigned shorts (not normalised, so exact), colour
     * as normalised bytes, uv as normalised unsigned shorts. Light packs block | sky << 8.
     */
    static final String COMPACT_VERTEX_SHADER = String.join("\n",
            "#version 120",
            "attribute vec4 a_pos;",
            "attribute vec4 a_color;",
            "attribute vec2 a_uv0;",
            "uniform mat4 u_projection;",
            "uniform mat4 u_modelView;",
            "uniform vec3 u_offset;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_distance;",
            "void main() {",
            "    vec3 local = a_pos.xyz / " + glslFloat(CompactTerrain.POSITION_SCALE) + " - vec3("
                    + glslFloat(CompactTerrain.POSITION_BIAS) + ");",
            "    vec4 view = u_modelView * vec4(local + u_offset, 1.0);",
            "    gl_Position = u_projection * view;",
            "    v_color = a_color;",
            "    v_uv0 = a_uv0;",
            "    float sky = floor(a_pos.w / 256.0);",
            "    v_uv2 = (vec2(a_pos.w - sky * 256.0, sky) + vec2(8.0)) / 256.0;",
            "    v_distance = length(view.xyz);",
            "}",
            "");

    static final String FRAGMENT_SHADER = String.join("\n",
            "#version 120",
            "uniform sampler2D u_blocks;",
            "uniform sampler2D u_lightmap;",
            "uniform float u_alphaCutoff;",
            "uniform int u_fogMode;",
            "uniform vec3 u_fog;",
            "uniform vec4 u_fogColor;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_distance;",
            "void main() {",
            "    vec4 color = texture2D(u_blocks, v_uv0) * v_color;",
            "    if (color.a < u_alphaCutoff) {",
            "        discard;",
            "    }",
            "    color *= texture2D(u_lightmap, v_uv2);",
            "    float visibility = 1.0;",
            "    if (u_fogMode == 0) {",
            "        visibility = clamp((u_fog.y - v_distance) / max(u_fog.y - u_fog.x, 0.0001), 0.0, 1.0);",
            "    } else if (u_fogMode == 1) {",
            "        visibility = clamp(exp(-u_fog.z * v_distance), 0.0, 1.0);",
            "    } else if (u_fogMode == 2) {",
            "        float k = u_fog.z * v_distance;",
            "        visibility = clamp(exp(-k * k), 0.0, 1.0);",
            "    }",
            "    gl_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, visibility), color.a);",
            "}",
            "");

    private static final AetheriumLog LOG = AetheriumLog.of(ChunkRenderer.class);

    private static final FloatBuffer MATRIX = directFloats(16);
    private static final FloatBuffer FOG_COLOR = directFloats(16);
    private static final FloatBuffer POSE = directFloats(16);

    private static boolean failed;
    private static String failure = "";
    private static boolean drawingThisFrame;
    private static long lastDrawFrame = -1L;
    private static long frame;
    private static int sectionsLastLayer;

    private static long compactQuadsTotal;
    private static long compactQuadsDrawn;
    private static int compactSectionsLastLayer;
    private static String lastStats = "";

    /** One linked program and its uniform locations. */
    private static final class Program {
        final String name;
        int id;
        int uProjection = -1;
        int uModelView = -1;
        int uOffset = -1;
        int uAlphaCutoff = -1;
        int uFogMode = -1;
        int uFog = -1;
        int uFogColor = -1;

        Program(final String name) {
            this.name = name;
        }
    }

    private static final Program LEGACY = new Program("legacy");
    private static final Program COMPACT = new Program("compact");
    private static boolean programsReady;
    private static int indexBuffer;

    // Per-layer state captured in begin(), uploaded to whichever program is switched to.
    private static Program current;
    private static final FloatBuffer MODEL_VIEW = directFloats(16);
    private static boolean layerCutout;
    private static int fogMode = FOG_NONE;
    private static float fogStart;
    private static float fogEnd;
    private static float fogDensity;

    private static final int[] RUN_FIRST = new int[4];
    private static final int[] RUN_COUNT = new int[4];

    // Pipeline switch tracking (render thread).
    private static boolean compactWanted;
    private static boolean compactUploaded;

    private ChunkRenderer() {
    }

    // ------------------------------------------------------------------ status (GUI)

    /** Whether the custom path drew terrain during the last couple of frames. */
    public static boolean isDrawing() {
        return drawingThisFrame && frame - lastDrawFrame <= 2L;
    }

    public static boolean hasFailed() {
        return failed;
    }

    public static String statusText() {
        if (failed) {
            return "Vanilla (fell back: " + failure + ")";
        }
        if (isDrawing()) {
            return "Aetherium pipeline (" + sectionsLastLayer + " sections in the last layer" + lastStats + ")";
        }
        return "Vanilla";
    }

    /** Scratch buffer the mixin stores the camera pose into (render thread only). */
    public static FloatBuffer poseBuffer() {
        POSE.clear();
        return POSE;
    }

    /** GameRenderer frame start: ages the "drawing" status. */
    public static void onFrame() {
        frame++;
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Prepares the program for one terrain layer. Vanilla's render state for the layer
     * (textures, lightmap, blend, depth, cull) must already be set up.
     *
     * @param modelView the camera pose, column-major (Matrix4f.store order)
     * @param cutout    discard texels under alpha 0.5 (cutout layers); solid draws everything
     * @return false when the renderer is unavailable: the caller lets vanilla draw the layer
     */
    public static boolean begin(final FloatBuffer modelView, final boolean cutout) {
        if (failed) {
            return false;
        }
        try {
            if (!programsReady && !createPrograms()) {
                return false;
            }
            MATRIX.clear();
            GL11.glGetFloatv(GL_PROJECTION_MATRIX, MATRIX);
            MATRIX.rewind();
            MODEL_VIEW.clear();
            modelView.rewind();
            MODEL_VIEW.put(modelView).flip();
            layerCutout = cutout;
            readFog();
            current = null;
            GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL20.glEnableVertexAttribArray(ATTR_POS);
            GL20.glEnableVertexAttribArray(ATTR_COLOR);
            GL20.glEnableVertexAttribArray(ATTR_UV0);
            sectionsLastLayer = 0;
            compactSectionsLastLayer = 0;
            return true;
        } catch (final RuntimeException | LinkageError error) {
            fail("setup failed: " + error);
            end();
            return false;
        }
    }

    /**
     * Draws one section's buffer. Positions in the buffer are relative to the section origin;
     * {@code dx, dy, dz} is that origin minus the camera position.
     */
    public static void draw(final int vertexBuffer, final int vertexCount, final float dx, final float dy, final float dz) {
        if (vertexBuffer <= 0 || vertexCount < 4) {
            return;
        }
        use(LEGACY);
        GL20.glUniform3f(LEGACY.uOffset, dx, dy, dz);
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        int first = 0;
        while (first < vertexCount) {
            final int count = Math.min(vertexCount - first, MAX_VERTICES_PER_DRAW);
            pointAttributes((long) first * STRIDE);
            GL11.glDrawElements(GL_TRIANGLES, indexCountFor(count), GL_UNSIGNED_SHORT, 0L);
            first += count;
        }
        sectionsLastLayer++;
    }

    /**
     * Draws one compact section: only the face groups that can face the camera, merged into as
     * few index ranges as possible.
     */
    public static void drawCompact(final int vertexBuffer, final CompactTerrain.Layout layout, final float dx, final float dy,
                                   final float dz) {
        final int quads = layout.quads();
        if (vertexBuffer <= 0 || quads == 0) {
            return;
        }
        final int mask = CompactTerrain.visibleGroups(layout.plane, -dx, -dy, -dz);
        int runs = CompactTerrain.runs(layout, mask, RUN_FIRST, RUN_COUNT);
        if (runs == 0) {
            return;
        }
        runs = CompactTerrain.mergeRuns(RUN_FIRST, RUN_COUNT, runs, MERGE_GAP_QUADS);
        use(COMPACT);
        GL20.glUniform3f(COMPACT.uOffset, dx, dy, dz);
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        if (quads <= MAX_QUADS_PER_DRAW) {
            pointCompactAttributes(0L);
            for (int r = 0; r < runs; r++) {
                // Quad q's six indices start at index 6q (12q bytes) of the shared buffer.
                GL11.glDrawElements(GL_TRIANGLES, RUN_COUNT[r] * 6, GL_UNSIGNED_SHORT, RUN_FIRST[r] * 12L);
                compactQuadsDrawn += RUN_COUNT[r];
            }
        } else {
            for (int r = 0; r < runs; r++) {
                int at = RUN_FIRST[r];
                final int end = at + RUN_COUNT[r];
                while (at < end) {
                    final int count = Math.min(end - at, MAX_QUADS_PER_DRAW);
                    pointCompactAttributes((long) at * CompactTerrain.QUAD_BYTES);
                    GL11.glDrawElements(GL_TRIANGLES, count * 6, GL_UNSIGNED_SHORT, 0L);
                    compactQuadsDrawn += count;
                    at += count;
                }
            }
        }
        compactQuadsTotal += quads;
        compactSectionsLastLayer++;
        sectionsLastLayer++;
    }

    /** Restores the state vanilla expects after a layer: no program, no attribute arrays, no buffers. */
    public static void end() {
        try {
            GL20.glDisableVertexAttribArray(ATTR_POS);
            GL20.glDisableVertexAttribArray(ATTR_COLOR);
            GL20.glDisableVertexAttribArray(ATTR_UV0);
            GL20.glDisableVertexAttribArray(ATTR_UV2);
            GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL_ARRAY_BUFFER, 0);
            GL20.glUseProgram(0);
        } catch (final RuntimeException | LinkageError error) {
            fail("cleanup failed: " + error);
        }
        current = null;
        drawingThisFrame = !failed;
        lastDrawFrame = frame;
        if (compactQuadsTotal > 2_000_000L) {
            compactQuadsTotal >>= 1;
            compactQuadsDrawn >>= 1;
        }
        lastStats = compactSectionsLastLayer == 0 || compactQuadsTotal == 0 ? ""
                : ", " + (100 - (int) (compactQuadsDrawn * 100 / compactQuadsTotal)) + "% of faces skipped";
    }

    /** Called by the mixin when something outside this class threw mid-layer. */
    public static void fail(final String reason) {
        if (!failed) {
            failed = true;
            failure = reason.length() > 80 ? reason.substring(0, 80) : reason;
            LOG.warn("Experimental chunk renderer disabled, vanilla draws terrain: {}", reason);
        }
    }

    // ------------------------------------------------------------------ pipeline switching

    /**
     * Whether chunk uploads should use the compact format right now. Called by the upload mixin
     * on the render thread; remembers that compact buffers exist.
     */
    public static boolean compactUploadsWanted() {
        return compactWanted;
    }

    /** The upload mixin stored a compact buffer: from now on, turning the pipeline off needs a rebuild. */
    public static void noteCompactUpload() {
        compactUploaded = true;
    }

    /**
     * Once per frame before the world renders. Returns true when every chunk must be rebuilt
     * ({@code LevelRenderer.allChanged()}): the pipeline was switched on (so sections convert now
     * instead of trickling in) or off/failed while compact buffers exist (vanilla cannot draw them).
     */
    public static boolean updatePipeline(final boolean enabled) {
        final boolean wanted = enabled && !failed;
        if (wanted == compactWanted) {
            return false;
        }
        compactWanted = wanted;
        if (wanted) {
            return true;
        }
        final boolean rebuild = compactUploaded;
        compactUploaded = false;
        return rebuild;
    }

    /** Whether any compact buffers may still be live (then vanilla must not draw these layers). */
    public static boolean hasCompactBuffers() {
        return compactUploaded;
    }

    // ------------------------------------------------------------------ helpers

    /** Triangle indices needed for {@code vertexCount} quad vertices (incomplete quads are dropped). */
    static int indexCountFor(final int vertexCount) {
        return (vertexCount >> 2) * 6;
    }

    /** Quad i -> triangles (4i, 4i+1, 4i+2) and (4i+2, 4i+3, 4i): vanilla's quad winding, split. */
    static short[] quadIndices(final int quads) {
        final short[] out = new short[quads * 6];
        for (int q = 0, i = 0; q < quads; q++) {
            final int v = q * 4;
            out[i++] = (short) v;
            out[i++] = (short) (v + 1);
            out[i++] = (short) (v + 2);
            out[i++] = (short) (v + 2);
            out[i++] = (short) (v + 3);
            out[i++] = (short) v;
        }
        return out;
    }

    /** Maps the GL fog state to the shader's mode uniform. */
    static int fogModeFor(final boolean fogEnabled, final int glMode) {
        if (!fogEnabled) {
            return FOG_NONE;
        }
        if (glMode == GL_EXP) {
            return FOG_EXP;
        }
        if (glMode == GL_EXP2) {
            return FOG_EXP2;
        }
        return FOG_LINEAR;
    }

    private static void pointAttributes(final long base) {
        GL20.glVertexAttribPointer(ATTR_POS, 3, GL_FLOAT, false, STRIDE, base + OFFSET_POS);
        GL20.glVertexAttribPointer(ATTR_COLOR, 4, GL_UNSIGNED_BYTE, true, STRIDE, base + OFFSET_COLOR);
        GL20.glVertexAttribPointer(ATTR_UV0, 2, GL_FLOAT, false, STRIDE, base + OFFSET_UV0);
        GL20.glVertexAttribPointer(ATTR_UV2, 2, GL_SHORT, false, STRIDE, base + OFFSET_UV2);
    }

    private static void pointCompactAttributes(final long base) {
        GL20.glVertexAttribPointer(ATTR_COMPACT_POS, 4, GL_UNSIGNED_SHORT, false, CompactTerrain.STRIDE,
                base + CompactTerrain.OFFSET_POS);
        GL20.glVertexAttribPointer(ATTR_COMPACT_COLOR, 4, GL_UNSIGNED_BYTE, true, CompactTerrain.STRIDE,
                base + CompactTerrain.OFFSET_COLOR);
        GL20.glVertexAttribPointer(ATTR_COMPACT_UV, 2, GL_UNSIGNED_SHORT, true, CompactTerrain.STRIDE,
                base + CompactTerrain.OFFSET_UV);
    }

    /** Switches program within a layer, uploading the layer's uniforms to it. */
    private static void use(final Program program) {
        if (current == program) {
            return;
        }
        current = program;
        GL20.glUseProgram(program.id);
        GL20.glUniformMatrix4fv(program.uProjection, false, MATRIX);
        MATRIX.rewind();
        GL20.glUniformMatrix4fv(program.uModelView, false, MODEL_VIEW);
        MODEL_VIEW.rewind();
        GL20.glUniform1f(program.uAlphaCutoff, layerCutout ? 0.5f : -1.0f);
        GL20.glUniform1i(program.uFogMode, fogMode);
        if (fogMode != FOG_NONE) {
            GL20.glUniform3f(program.uFog, fogStart, fogEnd, fogDensity);
            GL20.glUniform4f(program.uFogColor, FOG_COLOR.get(0), FOG_COLOR.get(1), FOG_COLOR.get(2), FOG_COLOR.get(3));
        }
        // Attribute 3 (lightmap uv) exists only in vanilla's layout.
        if (program == LEGACY) {
            GL20.glEnableVertexAttribArray(ATTR_UV2);
        } else {
            GL20.glDisableVertexAttribArray(ATTR_UV2);
        }
    }

    private static void readFog() {
        fogMode = fogModeFor(GL11.glIsEnabled(GL_FOG), GL11.glGetInteger(GL_FOG_MODE));
        if (fogMode == FOG_NONE) {
            return;
        }
        fogStart = GL11.glGetFloat(GL_FOG_START);
        fogEnd = GL11.glGetFloat(GL_FOG_END);
        fogDensity = GL11.glGetFloat(GL_FOG_DENSITY);
        FOG_COLOR.clear();
        GL11.glGetFloatv(GL_FOG_COLOR, FOG_COLOR);
    }

    /** GLSL float literal (always has a decimal point). */
    static String glslFloat(final float value) {
        final String text = Float.toString(value);
        return text.indexOf('.') >= 0 || text.indexOf('E') >= 0 ? text : text + ".0";
    }

    private static boolean createPrograms() {
        if (!link(LEGACY, VERTEX_SHADER, "a_pos", "a_color", "a_uv0", "a_uv2")
                || !link(COMPACT, COMPACT_VERTEX_SHADER, "a_pos", "a_color", "a_uv0", null)) {
            if (LEGACY.id != 0) {
                GL20.glDeleteProgram(LEGACY.id);
                LEGACY.id = 0;
            }
            return false;
        }
        final short[] indices = quadIndices(MAX_QUADS_PER_DRAW);
        final ShortBuffer data = ByteBuffer.allocateDirect(indices.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        data.put(indices).flip();
        indexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        programsReady = true;
        LOG.info("Aetherium pipeline ready (GLSL 1.20, 16-byte compact vertices, shared index buffer of {} quads)",
                MAX_QUADS_PER_DRAW);
        return true;
    }

    private static boolean link(final Program program, final String vertexSource, final String pos, final String color,
                                final String uv0, final String uv2) {
        final int vertex = compile(GL_VERTEX_SHADER, vertexSource);
        final int fragment = vertex == 0 ? 0 : compile(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        if (vertex == 0 || fragment == 0) {
            if (vertex != 0) {
                GL20.glDeleteShader(vertex);
            }
            return false;
        }
        final int linked = GL20.glCreateProgram();
        GL20.glAttachShader(linked, vertex);
        GL20.glAttachShader(linked, fragment);
        GL20.glBindAttribLocation(linked, ATTR_POS, pos);
        GL20.glBindAttribLocation(linked, ATTR_COLOR, color);
        GL20.glBindAttribLocation(linked, ATTR_UV0, uv0);
        if (uv2 != null) {
            GL20.glBindAttribLocation(linked, ATTR_UV2, uv2);
        }
        GL20.glLinkProgram(linked);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(linked, GL_LINK_STATUS) == 0) {
            fail(program.name + " link: " + GL20.glGetProgramInfoLog(linked).trim());
            GL20.glDeleteProgram(linked);
            return false;
        }
        program.uProjection = GL20.glGetUniformLocation(linked, "u_projection");
        program.uModelView = GL20.glGetUniformLocation(linked, "u_modelView");
        program.uOffset = GL20.glGetUniformLocation(linked, "u_offset");
        program.uAlphaCutoff = GL20.glGetUniformLocation(linked, "u_alphaCutoff");
        program.uFogMode = GL20.glGetUniformLocation(linked, "u_fogMode");
        program.uFog = GL20.glGetUniformLocation(linked, "u_fog");
        program.uFogColor = GL20.glGetUniformLocation(linked, "u_fogColor");
        if (program.uProjection < 0 || program.uModelView < 0 || program.uOffset < 0) {
            fail(program.name + " program is missing its matrix uniforms");
            GL20.glDeleteProgram(linked);
            return false;
        }
        GL20.glUseProgram(linked);
        GL20.glUniform1i(GL20.glGetUniformLocation(linked, "u_blocks"), 0);     // block atlas: texture unit 0
        GL20.glUniform1i(GL20.glGetUniformLocation(linked, "u_lightmap"), 2);   // lightmap: unit 2 (LightTexture)
        GL20.glUseProgram(0);
        program.id = linked;
        return true;
    }

    private static int compile(final int type, final String source) {
        final int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            fail((type == GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader: " + GL20.glGetShaderInfoLog(shader).trim());
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private static FloatBuffer directFloats(final int count) {
        return ByteBuffer.allocateDirect(count * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }
}
