package com.aetherium.client;

import com.aetherium.perf.PipelineCrashGuard;
import com.aetherium.render.CompactTerrain;
import com.aetherium.util.AetheriumLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

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
 * built with one small GLSL 1.20 program and one uniform per section for its offset.</p>
 *
 * <p><b>Only the most basic GL (1.3.2).</b> Draws use {@code glDrawArrays} (no element buffer:
 * an Android GL translator crashed natively on the first indexed draw in 1.3.0/1.3.1). The
 * projection comes from vanilla's {@code renderLevel} argument and fog from vanilla's own
 * {@code GlStateManager} calls ({@link FogTracker}), so no fixed-function state is queried from
 * GL, which translators may not implement.</p>
 *
 * <p>Version-neutral on purpose: it knows LWJGL and nothing about Minecraft classes. The version
 * mixin hands it the section VBO ids, vertex counts and camera-relative origins; vanilla keeps
 * meshing, uploading, culling, sorting and the render-state setup (textures, lightmap, blend,
 * depth) exactly as before.</p>
 *
 * <p><b>Aetherium pipeline (1.3.0).</b> Sections uploaded through {@code VertexBufferMixin}
 * are in {@link CompactTerrain}'s 16-byte format, sorted into face-direction groups. They are drawn
 * by a second program that decodes the packed attributes, and only the groups that can face the
 * camera are drawn (in one or two index ranges per section). Buffers that did not fit the format
 * keep vanilla's layout and are drawn by the original program.</p>
 *
 * <p><b>Two modes (1.3.3).</b> Shader mode is the above. Compatible mode keeps vanilla's 32-byte
 * vertices and vanilla's own GL calls (the version mixin replays vanilla's {@code renderChunkLayer}
 * through {@code RenderSystem}); the quads are only reordered into the same face groups
 * ({@link CompactTerrain#sortQuads}) so the groups facing away are skipped. No shader, no generic
 * attributes: it runs wherever vanilla runs. A Java-side failure in shader mode, or a native crash
 * caught by {@link PipelineCrashGuard} at the next start, switches to compatible mode.</p>
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
    static final int GL_QUADS = 0x0007;
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


    static final int ATTR_COMPACT_POS = 0;
    /** Type of attribute 0 in the compact layout; must be one GL4ES accepts as a vertex position. */
    static final int COMPACT_POS_TYPE = GL_SHORT;
    static final int ATTR_COMPACT_COLOR = 1;
    static final int ATTR_COMPACT_UV = 2;

    /** Hidden quads drawn anyway to save a draw call (see {@link CompactTerrain#mergeRuns}). */
    static final int MERGE_GAP_QUADS = 256;

    /** Fog mode uniform values. */
    static final int FOG_NONE = -1;
    static final int FOG_LINEAR = 0;
    static final int FOG_EXP = 1;
    static final int FOG_EXP2 = 2;

    /**
     * Fog, computed per vertex like vanilla's fixed-function fog: terrain quads are one block, so
     * interpolating it is exact to the eye, and the fragment shader is left with one mix.
     */
    static final String FOG_PER_VERTEX = String.join("\n",
            "    float fogDistance = length(view.xyz);",
            "    v_fog = 1.0;",
            "    if (u_fogMode == 0) {",
            "        v_fog = clamp((u_fog.y - fogDistance) / max(u_fog.y - u_fog.x, 0.0001), 0.0, 1.0);",
            "    } else if (u_fogMode == 1) {",
            "        v_fog = clamp(exp(-u_fog.z * fogDistance), 0.0, 1.0);",
            "    } else if (u_fogMode == 2) {",
            "        float k = u_fog.z * fogDistance;",
            "        v_fog = clamp(exp(-k * k), 0.0, 1.0);",
            "    }");

    static final String VERTEX_SHADER = String.join("\n",
            "#version 120",
            "attribute vec3 a_pos;",
            "attribute vec4 a_color;",
            "attribute vec2 a_uv0;",
            "attribute vec2 a_uv2;",
            "uniform mat4 u_projection;",
            "uniform mat4 u_modelView;",
            "uniform vec3 u_offset;",
            "uniform int u_fogMode;",
            "uniform vec3 u_fog;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_fog;",
            "void main() {",
            "    vec4 view = u_modelView * vec4(a_pos + u_offset, 1.0);",
            "    gl_Position = u_projection * view;",
            "    v_color = a_color;",
            "    v_uv0 = a_uv0;",
            // Vanilla's lightmap texture matrix: scale 1/256, translate 8 texels.
            "    v_uv2 = (a_uv2 + vec2(8.0)) / 256.0;",
            FOG_PER_VERTEX,
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
            "uniform int u_fogMode;",
            "uniform vec3 u_fog;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_fog;",
            "void main() {",
            "    vec4 raw = a_pos + vec4(" + glslFloat(CompactTerrain.ATTRIBUTE_BIAS) + ");",
            "    vec3 local = raw.xyz / " + glslFloat(CompactTerrain.POSITION_SCALE) + " - vec3("
                    + glslFloat(CompactTerrain.POSITION_BIAS) + ");",
            "    vec4 view = u_modelView * vec4(local + u_offset, 1.0);",
            "    gl_Position = u_projection * view;",
            "    v_color = a_color;",
            "    v_uv0 = a_uv0;",
            "    float sky = floor(raw.w / 256.0);",
            "    v_uv2 = (vec2(raw.w - sky * 256.0, sky) + vec2(8.0)) / 256.0;",
            FOG_PER_VERTEX,
            "}",
            "");

    /** Cutout layers: texels under the cutoff are discarded. */
    static final String FRAGMENT_SHADER = String.join("\n",
            "#version 120",
            "uniform sampler2D u_blocks;",
            "uniform sampler2D u_lightmap;",
            "uniform float u_alphaCutoff;",
            "uniform vec4 u_fogColor;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_fog;",
            "void main() {",
            "    vec4 color = texture2D(u_blocks, v_uv0) * v_color;",
            "    if (color.a < u_alphaCutoff) {",
            "        discard;",
            "    }",
            "    color *= texture2D(u_lightmap, v_uv2);",
            "    gl_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, v_fog), color.a);",
            "}",
            "");

    /**
     * The solid layer: no {@code discard} anywhere in the shader. Mobile GPUs (Mali's Forward Pixel
     * Kill, early depth test on Adreno and PowerVR) switch hidden-surface removal off for any shader
     * that can discard, and solid terrain is most of the screen, so it gets its own program.
     */
    static final String FRAGMENT_SHADER_SOLID = String.join("\n",
            "#version 120",
            "uniform sampler2D u_blocks;",
            "uniform sampler2D u_lightmap;",
            "uniform vec4 u_fogColor;",
            "varying vec4 v_color;",
            "varying vec2 v_uv0;",
            "varying vec2 v_uv2;",
            "varying float v_fog;",
            "void main() {",
            "    vec4 color = texture2D(u_blocks, v_uv0) * v_color * texture2D(u_lightmap, v_uv2);",
            "    gl_FragColor = vec4(mix(u_fogColor.rgb, color.rgb, v_fog), color.a);",
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
        /** Vanilla's 32-byte layout, which has the separate lightmap attribute. */
        final boolean legacy;

        Program(final String name, final boolean legacy) {
            this.name = name;
            this.legacy = legacy;
        }
    }

    private static final Program LEGACY = new Program("legacy solid", true);
    private static final Program LEGACY_CUTOUT = new Program("legacy cutout", true);
    private static final Program COMPACT = new Program("compact solid", false);
    private static final Program COMPACT_CUTOUT = new Program("compact cutout", false);
    private static final Program[] PROGRAMS = {LEGACY, LEGACY_CUTOUT, COMPACT, COMPACT_CUTOUT};
    private static boolean programsReady;

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

    // Pipeline modes.
    public static final int MODE_OFF = 0;
    public static final int MODE_SHADER = 1;
    public static final int MODE_COMPATIBLE = 2;

    // Pipeline switch tracking (render thread).
    private static volatile int uploadMode = MODE_OFF;
    private static boolean compactUploaded;
    private static boolean shaderFailed;
    private static String shaderFailure = "";

    // Step-by-step log of the first shader-mode draws: a native crash leaves no Java stack trace,
    // so the last trace line in latest.log names the GL call that crashed.
    static final int TRACE_STEPS = 24;
    private static int traceLeft;
    private static boolean loggedGlInfo;
    private static boolean loggedFirstCompatibleLayer;

    // Projection captured from LevelRenderer.renderLevel's argument (no fixed-function query needed).
    private static final FloatBuffer PROJECTION = directFloats(16);
    private static boolean projectionCaptured;

    // Early-frame verification: GL errors are checked after the first layers that drew something.
    static final int ERROR_CHECK_LAYERS = 8;
    private static int errorChecksLeft = ERROR_CHECK_LAYERS;
    private static boolean loggedFirstUpload;
    private static boolean loggedFirstCompactLayer;

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
        if (!isDrawing() && PipelineCrashGuard.wasTripped()) {
            return "Vanilla (turned off: the game closed while the pipeline was drawing last time)";
        }
        if (isDrawing()) {
            final boolean compatible = uploadMode == MODE_COMPATIBLE;
            String why = "";
            if (compatible && shaderFailed) {
                why = "; shader mode failed: " + shaderFailure;
            }
            return "Aetherium pipeline, " + (compatible ? "compatible mode" : "shader mode") + " (" + sectionsLastLayer
                    + " sections in the last layer" + lastStats + why + ")";
        }
        return "Vanilla";
    }

    /**
     * How {@code renderChunkLayer} should draw this frame: {@link #MODE_OFF} (vanilla, or skip the
     * layer while compact buffers exist), {@link #MODE_SHADER} or {@link #MODE_COMPATIBLE}.
     */
    public static int drawMode(final boolean enabled) {
        if (!enabled || failed || uploadMode == MODE_SHADER && shaderFailed) {
            return MODE_OFF; // a failed shader mode switches to compatible at the next frame's rebuild
        }
        return uploadMode;
    }

    /** Scratch buffer the mixin stores the camera pose into (render thread only). */
    public static FloatBuffer poseBuffer() {
        POSE.clear();
        return POSE;
    }

    /**
     * Scratch buffer the mixin stores this frame's projection matrix into (column-major); call
     * {@link #projectionStored()} afterwards. Render thread only.
     */
    public static FloatBuffer projectionBuffer() {
        PROJECTION.clear();
        return PROJECTION;
    }

    public static void projectionStored() {
        projectionCaptured = true;
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
        if (failed || shaderFailed) {
            return false;
        }
        try {
            if (!programsReady && !createPrograms()) {
                return false;
            }
            if (errorChecksLeft > 0) {
                drainGlErrors();
            }
            MATRIX.clear();
            if (projectionCaptured) {
                PROJECTION.rewind();
                MATRIX.put(PROJECTION).flip();
            } else {
                GL11.glGetFloatv(GL_PROJECTION_MATRIX, MATRIX);
            }
            MATRIX.rewind();
            MODEL_VIEW.clear();
            modelView.rewind();
            MODEL_VIEW.put(modelView).flip();
            layerCutout = cutout;
            readFog();
            current = null;
            if (traceLeft > 0) {
                trace("begin layer (cutout " + cutout + ", fog mode " + fogMode + ", projection from renderLevel "
                        + projectionCaptured + ")");
            }
            PipelineCrashGuard.arm(PipelineCrashGuard.MODE_SHADER);
            GL20.glEnableVertexAttribArray(ATTR_POS);
            GL20.glEnableVertexAttribArray(ATTR_COLOR);
            GL20.glEnableVertexAttribArray(ATTR_UV0);
            trace("vertex attribute arrays 0-2 enabled");
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
        final Program program = layerCutout ? LEGACY_CUTOUT : LEGACY;
        use(program);
        GL20.glUniform3f(program.uOffset, dx, dy, dz);
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        pointAttributes(0L);
        if (traceLeft > 0) {
            trace("legacy section: buffer " + vertexBuffer + " bound, attribute pointers set; glDrawArrays(GL_QUADS, 0, "
                    + (vertexCount & ~3) + ")");
        }
        // The same draw call vanilla uses for these buffers: every GL layer that runs 1.16.5 supports it.
        GL11.glDrawArrays(GL_QUADS, 0, vertexCount & ~3);
        trace("legacy draw returned");
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
        final Program program = layerCutout ? COMPACT_CUTOUT : COMPACT;
        use(program);
        GL20.glUniform3f(program.uOffset, dx, dy, dz);
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        pointCompactAttributes(0L);
        for (int r = 0; r < runs; r++) {
            if (traceLeft > 0) {
                trace("compact section: buffer " + vertexBuffer + "; glDrawArrays(GL_TRIANGLES, "
                        + RUN_FIRST[r] * CompactTerrain.VERTICES_PER_QUAD + ", "
                        + RUN_COUNT[r] * CompactTerrain.VERTICES_PER_QUAD + ")");
            }
            // Plain triangles, no element buffer: quad q is vertices 6q .. 6q + 5.
            GL11.glDrawArrays(GL_TRIANGLES, RUN_FIRST[r] * CompactTerrain.VERTICES_PER_QUAD,
                    RUN_COUNT[r] * CompactTerrain.VERTICES_PER_QUAD);
            compactQuadsDrawn += RUN_COUNT[r];
            trace("compact draw returned");
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
            GL15.glBindBuffer(GL_ARRAY_BUFFER, 0);
            GL20.glUseProgram(0);
            trace("layer cleanup done");
        } catch (final RuntimeException | LinkageError error) {
            fail("cleanup failed: " + error);
        }
        current = null;
        if (!failed && errorChecksLeft > 0 && sectionsLastLayer > 0) {
            errorChecksLeft--;
            final int error = GL11.glGetError();
            if (error != 0) {
                fail("GL error 0x" + Integer.toHexString(error) + " while drawing terrain");
            } else if (compactSectionsLastLayer > 0 && !loggedFirstCompactLayer) {
                loggedFirstCompactLayer = true;
                LOG.info("Aetherium pipeline: first compact terrain layer drawn ({} sections, no GL error)",
                        compactSectionsLastLayer);
            }
        }
        drawingThisFrame = !failed && !shaderFailed;
        lastDrawFrame = frame;
        updateStats();
    }

    private static void updateStats() {
        if (compactQuadsTotal > 2_000_000L) {
            compactQuadsTotal >>= 1;
            compactQuadsDrawn >>= 1;
        }
        lastStats = compactSectionsLastLayer == 0 || compactQuadsTotal == 0 ? ""
                : ", " + (100 - (int) (compactQuadsDrawn * 100 / compactQuadsTotal)) + "% of faces skipped";
    }

    /** Called by the mixin when something outside this class threw mid-layer. */
    public static void fail(final String reason) {
        if (failed) {
            return;
        }
        if (uploadMode == MODE_SHADER && !shaderFailed) {
            // Shader mode only: the next frame rebuilds the chunks for compatible mode.
            shaderFailed = true;
            shaderFailure = reason.length() > 80 ? reason.substring(0, 80) : reason;
            LOG.warn("Aetherium pipeline shader mode failed, switching to compatible mode: {}", reason);
            PipelineCrashGuard.disarm();
            return;
        }
        failed = true;
        failure = reason.length() > 80 ? reason.substring(0, 80) : reason;
        LOG.warn("Experimental chunk renderer disabled, vanilla draws terrain: {}", reason);
        PipelineCrashGuard.disarm();
    }

    // ------------------------------------------------------------------ pipeline switching

    /**
     * Format for chunk uploads right now: {@link #MODE_OFF} (vanilla), {@link #MODE_SHADER}
     * (compact) or {@link #MODE_COMPATIBLE} (vanilla vertices sorted into face groups). Called by
     * the upload mixin on the render thread.
     */
    public static int uploadMode() {
        return uploadMode;
    }

    private static boolean loggedEmptyUpload;

    /** A chunk upload found its vertex builder empty and left the section empty instead of crashing. */
    public static void noteEmptyUpload() {
        if (!loggedEmptyUpload) {
            loggedEmptyUpload = true;
            LOG.warn("Aetherium pipeline: a chunk upload found its vertex builder empty; that section stays "
                    + "empty until it is rebuilt (reported once)");
        }
    }

    /** The upload mixin stored a compact buffer: from now on, turning the pipeline off needs a rebuild. */
    public static void noteCompactUpload(final int quads) {
        compactUploaded = true;
        if (!loggedFirstUpload) {
            loggedFirstUpload = true;
            errorChecksLeft = ERROR_CHECK_LAYERS; // verify the first frames that draw compact data too
            LOG.info("Aetherium pipeline: first compact chunk upload ({} quads, {} KB instead of {} KB)", quads,
                    (quads * CompactTerrain.QUAD_BYTES + 1023) / 1024, (quads * 4 * STRIDE + 1023) / 1024);
        }
    }

    /**
     * Once per frame before the world renders. Returns true when every chunk must be rebuilt
     * ({@code LevelRenderer.allChanged()}): the pipeline was switched on (so sections convert now
     * instead of trickling in) or off/failed while compact buffers exist (vanilla cannot draw them).
     */
    public static boolean updatePipeline(final boolean enabled, final boolean shader) {
        final int wanted = wantedMode(enabled, shader, failed, shaderFailed);
        if (wanted == uploadMode) {
            return false;
        }
        uploadMode = wanted;
        PipelineCrashGuard.disarm(); // the next draw arms it again with the new mode
        if (wanted == MODE_SHADER) {
            errorChecksLeft = ERROR_CHECK_LAYERS;
            traceLeft = TRACE_STEPS;
            LOG.info("Aetherium pipeline on (shader mode): rebuilding the loaded chunks in the compact format");
            return true;
        }
        if (wanted == MODE_COMPATIBLE) {
            // Compact buffers may stay until their section is rebuilt: compatible mode skips them.
            LOG.info("Aetherium pipeline on (compatible mode: vanilla vertex format and GL calls): rebuilding the "
                    + "loaded chunks sorted by face direction");
            return true;
        }
        final boolean rebuild = compactUploaded;
        compactUploaded = false;
        if (rebuild) {
            LOG.info("Aetherium pipeline off: rebuilding the loaded chunks for vanilla");
        }
        return rebuild;
    }

    /** Unit tests: back to a fresh session. */
    static void resetForTests() {
        failed = false;
        failure = "";
        shaderFailed = false;
        shaderFailure = "";
        uploadMode = MODE_OFF;
        compactUploaded = false;
        traceLeft = 0;
    }

    static int wantedMode(final boolean enabled, final boolean shader, final boolean anyFailed,
                          final boolean shaderBroken) {
        if (!enabled || anyFailed) {
            return MODE_OFF;
        }
        return shader && !shaderBroken ? MODE_SHADER : MODE_COMPATIBLE;
    }

    // ------------------------------------------------------------------ compatible mode

    /** A compatible-mode layer starts (the mixin then replays vanilla's draw calls). */
    public static void beginCompatible() {
        if (!loggedGlInfo) {
            logGlInfo();
        }
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        sectionsLastLayer = 0;
        compactSectionsLastLayer = 0;
    }

    /**
     * Visible quad ranges of one sorted section, read back with {@link #runFirstVertex} and
     * {@link #runVertexCount}. {@code dx, dy, dz} is the section origin minus the camera.
     *
     * @return the number of ranges (0 = nothing faces the camera), or -1 for an unsorted buffer
     *         (draw all of it)
     */
    public static int compatibleRuns(final CompactTerrain.Layout layout, final float dx, final float dy, final float dz) {
        if (layout == null) {
            return -1;
        }
        final int quads = layout.quads();
        if (quads == 0) {
            return 0;
        }
        final int mask = CompactTerrain.visibleGroups(layout.plane, -dx, -dy, -dz);
        int runs = CompactTerrain.runs(layout, mask, RUN_FIRST, RUN_COUNT);
        if (runs > 0) {
            runs = CompactTerrain.mergeRuns(RUN_FIRST, RUN_COUNT, runs, MERGE_GAP_QUADS);
        }
        for (int r = 0; r < runs; r++) {
            compactQuadsDrawn += RUN_COUNT[r];
        }
        compactQuadsTotal += quads;
        compactSectionsLastLayer++;
        return runs;
    }

    /** First vertex of range {@code r} (vanilla quads: 4 vertices each). */
    public static int runFirstVertex(final int r) {
        return RUN_FIRST[r] * 4;
    }

    public static int runVertexCount(final int r) {
        return RUN_COUNT[r] * 4;
    }

    public static void compatibleSectionDrawn() {
        sectionsLastLayer++;
    }

    public static void endCompatible() {
        drawingThisFrame = true;
        lastDrawFrame = frame;
        updateStats();
        if (!loggedFirstCompatibleLayer && sectionsLastLayer > 0) {
            loggedFirstCompatibleLayer = true;
            LOG.info("Aetherium pipeline: first compatible-mode terrain layer drawn ({} sections, {} sorted by face)",
                    sectionsLastLayer, compactSectionsLastLayer);
        }
    }

    /** Whether any compact buffers may still be live (then vanilla must not draw these layers). */
    public static boolean hasCompactBuffers() {
        return compactUploaded;
    }

    // ------------------------------------------------------------------ helpers

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
        // GL_SHORT, not GL_UNSIGNED_SHORT: GL4ES only passes attribute 0 through for byte, short,
        // fixed and float (see CompactTerrain); anything else takes a CPU path that crashes on VBOs.
        GL20.glVertexAttribPointer(ATTR_COMPACT_POS, 4, COMPACT_POS_TYPE, false, CompactTerrain.STRIDE,
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
        if (traceLeft > 0) {
            trace("glUseProgram(" + program.name + " " + program.id + ")");
        }
        GL20.glUseProgram(program.id);
        GL20.glUniformMatrix4fv(program.uProjection, false, MATRIX);
        MATRIX.rewind();
        GL20.glUniformMatrix4fv(program.uModelView, false, MODEL_VIEW);
        MODEL_VIEW.rewind();
        if (program.uAlphaCutoff >= 0) {
            GL20.glUniform1f(program.uAlphaCutoff, 0.5f);
        }
        GL20.glUniform1i(program.uFogMode, fogMode);
        if (fogMode != FOG_NONE) {
            GL20.glUniform3f(program.uFog, fogStart, fogEnd, fogDensity);
            GL20.glUniform4f(program.uFogColor, FOG_COLOR.get(0), FOG_COLOR.get(1), FOG_COLOR.get(2), FOG_COLOR.get(3));
        }
        // Attribute 3 (lightmap uv) exists only in vanilla's layout.
        if (program.legacy) {
            GL20.glEnableVertexAttribArray(ATTR_UV2);
        } else {
            GL20.glDisableVertexAttribArray(ATTR_UV2);
        }
        trace("uniforms uploaded");
    }

    private static void readFog() {
        if (FogTracker.seen()) {
            // What vanilla told GlStateManager: no GL query at all.
            fogMode = fogModeFor(FogTracker.enabled(), FogTracker.mode());
            fogStart = FogTracker.start();
            fogEnd = FogTracker.end();
            fogDensity = FogTracker.density();
            FogTracker.copyColor(FOG_COLOR);
            return;
        }
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

    private static void trace(final String step) {
        if (traceLeft > 0) {
            traceLeft--;
            LOG.info("Aetherium pipeline trace: {}", step);
        }
    }

    private static void logGlInfo() {
        loggedGlInfo = true;
        try {
            LOG.info("Aetherium pipeline GL: renderer '{}', version '{}', GLSL '{}'", GL11.glGetString(0x1F01),
                    GL11.glGetString(0x1F02), GL11.glGetString(0x8B8C)); // GL_RENDERER, GL_VERSION, GLSL version
        } catch (final RuntimeException | LinkageError error) {
            // diagnostics only
        }
    }

    private static void drainGlErrors() {
        for (int i = 0; i < 16 && GL11.glGetError() != 0; i++) {
            // errors left by earlier (vanilla or other mods') calls are not ours
        }
    }

    /** GLSL float literal (always has a decimal point). */
    static String glslFloat(final float value) {
        final String text = Float.toString(value);
        return text.indexOf('.') >= 0 || text.indexOf('E') >= 0 ? text : text + ".0";
    }

    private static boolean createPrograms() {
        if (!loggedGlInfo) {
            logGlInfo();
        }
        trace("compiling and linking the GLSL 1.20 programs");
        if (!link(LEGACY, VERTEX_SHADER, FRAGMENT_SHADER_SOLID, "a_pos", "a_color", "a_uv0", "a_uv2")
                || !link(LEGACY_CUTOUT, VERTEX_SHADER, FRAGMENT_SHADER, "a_pos", "a_color", "a_uv0", "a_uv2")
                || !link(COMPACT, COMPACT_VERTEX_SHADER, FRAGMENT_SHADER_SOLID, "a_pos", "a_color", "a_uv0", null)
                || !link(COMPACT_CUTOUT, COMPACT_VERTEX_SHADER, FRAGMENT_SHADER, "a_pos", "a_color", "a_uv0", null)) {
            for (final Program program : PROGRAMS) {
                if (program.id != 0) {
                    GL20.glDeleteProgram(program.id);
                    program.id = 0;
                }
            }
            return false;
        }
        programsReady = true;
        LOG.info("Aetherium pipeline ready (GLSL 1.20, 16-byte compact vertices, plain triangles, no index buffer, "
                + "discard-free solid program, per-vertex fog)");
        return true;
    }

    private static boolean link(final Program program, final String vertexSource, final String fragmentSource,
                                final String pos, final String color, final String uv0, final String uv2) {
        final int vertex = compile(GL_VERTEX_SHADER, vertexSource);
        final int fragment = vertex == 0 ? 0 : compile(GL_FRAGMENT_SHADER, fragmentSource);
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
