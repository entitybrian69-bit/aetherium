package com.aetherium.client;

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
 * <p>Failure policy: if the shader does not compile or link, or anything throws, the renderer
 * turns itself off for the session, logs once, and vanilla draws the frame.</p>
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

    private static int program;
    private static int indexBuffer;
    private static int uProjection = -1;
    private static int uModelView = -1;
    private static int uOffset = -1;
    private static int uAlphaCutoff = -1;
    private static int uFogMode = -1;
    private static int uFog = -1;
    private static int uFogColor = -1;
    private static int uBlocks = -1;
    private static int uLightmap = -1;

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
            return "Aetherium shader (" + sectionsLastLayer + " sections in the last layer)";
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
            if (program == 0 && !createProgram()) {
                return false;
            }
            GL20.glUseProgram(program);
            MATRIX.clear();
            GL11.glGetFloatv(GL_PROJECTION_MATRIX, MATRIX);
            MATRIX.rewind();
            GL20.glUniformMatrix4fv(uProjection, false, MATRIX);
            modelView.rewind();
            GL20.glUniformMatrix4fv(uModelView, false, modelView);
            GL20.glUniform1f(uAlphaCutoff, cutout ? 0.5f : -1.0f);
            uploadFog();
            GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            GL20.glEnableVertexAttribArray(ATTR_POS);
            GL20.glEnableVertexAttribArray(ATTR_COLOR);
            GL20.glEnableVertexAttribArray(ATTR_UV0);
            GL20.glEnableVertexAttribArray(ATTR_UV2);
            sectionsLastLayer = 0;
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
        GL20.glUniform3f(uOffset, dx, dy, dz);
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
        drawingThisFrame = !failed;
        lastDrawFrame = frame;
    }

    /** Called by the mixin when something outside this class threw mid-layer. */
    public static void fail(final String reason) {
        if (!failed) {
            failed = true;
            failure = reason.length() > 80 ? reason.substring(0, 80) : reason;
            LOG.warn("Experimental chunk renderer disabled, vanilla draws terrain: {}", reason);
        }
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

    private static void uploadFog() {
        final int mode = fogModeFor(GL11.glIsEnabled(GL_FOG), GL11.glGetInteger(GL_FOG_MODE));
        GL20.glUniform1i(uFogMode, mode);
        if (mode == FOG_NONE) {
            return;
        }
        GL20.glUniform3f(uFog, GL11.glGetFloat(GL_FOG_START), GL11.glGetFloat(GL_FOG_END), GL11.glGetFloat(GL_FOG_DENSITY));
        FOG_COLOR.clear();
        GL11.glGetFloatv(GL_FOG_COLOR, FOG_COLOR);
        GL20.glUniform4f(uFogColor, FOG_COLOR.get(0), FOG_COLOR.get(1), FOG_COLOR.get(2), FOG_COLOR.get(3));
    }

    private static boolean createProgram() {
        final int vertex = compile(GL_VERTEX_SHADER, VERTEX_SHADER);
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
        GL20.glBindAttribLocation(linked, ATTR_POS, "a_pos");
        GL20.glBindAttribLocation(linked, ATTR_COLOR, "a_color");
        GL20.glBindAttribLocation(linked, ATTR_UV0, "a_uv0");
        GL20.glBindAttribLocation(linked, ATTR_UV2, "a_uv2");
        GL20.glLinkProgram(linked);
        GL20.glDeleteShader(vertex);
        GL20.glDeleteShader(fragment);
        if (GL20.glGetProgrami(linked, GL_LINK_STATUS) == 0) {
            fail("link: " + GL20.glGetProgramInfoLog(linked).trim());
            GL20.glDeleteProgram(linked);
            return false;
        }
        uProjection = GL20.glGetUniformLocation(linked, "u_projection");
        uModelView = GL20.glGetUniformLocation(linked, "u_modelView");
        uOffset = GL20.glGetUniformLocation(linked, "u_offset");
        uAlphaCutoff = GL20.glGetUniformLocation(linked, "u_alphaCutoff");
        uFogMode = GL20.glGetUniformLocation(linked, "u_fogMode");
        uFog = GL20.glGetUniformLocation(linked, "u_fog");
        uFogColor = GL20.glGetUniformLocation(linked, "u_fogColor");
        uBlocks = GL20.glGetUniformLocation(linked, "u_blocks");
        uLightmap = GL20.glGetUniformLocation(linked, "u_lightmap");
        if (uProjection < 0 || uModelView < 0 || uOffset < 0) {
            fail("program is missing its matrix uniforms");
            GL20.glDeleteProgram(linked);
            return false;
        }
        GL20.glUseProgram(linked);
        GL20.glUniform1i(uBlocks, 0);     // block atlas: texture unit 0
        GL20.glUniform1i(uLightmap, 2);   // lightmap: texture unit 2 (vanilla's LightTexture)
        GL20.glUseProgram(0);

        final short[] indices = quadIndices(MAX_QUADS_PER_DRAW);
        final ShortBuffer data = ByteBuffer.allocateDirect(indices.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        data.put(indices).flip();
        indexBuffer = GL15.glGenBuffers();
        GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        GL15.glBufferData(GL_ELEMENT_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        GL15.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        program = linked;
        LOG.info("Experimental chunk renderer ready (GLSL 1.20, shared index buffer of {} quads)", MAX_QUADS_PER_DRAW);
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
