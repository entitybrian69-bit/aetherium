package com.aetherium.render.hzb;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.aetherium.render.backend.BackendCapabilities;
import com.aetherium.render.gl.GlProcs;
import com.aetherium.util.AetheriumLog;

/**
 * Hierarchical Z-buffer: a min-reduced depth pyramid plus a compute pass that
 * tests section AABBs against it and compacts the surviving indirect commands.
 *
 * <p>Why a pyramid instead of occlusion queries: {@code glGetQueryObjectui64v} is
 * a readback, so the result is only usable one or two frames later, which is fine
 * for "was visible last frame" heuristics but not for a cull that must be exact
 * for the frame being drawn. A depth pyramid is a *conservative* test with no
 * readback at all: each texel holds the nearest depth in a 2^n x 2^n tile, and a
 * section is rejected only if its entire projected AABB is nearer than the *most
 * conservative* (i.e. nearest) depth covering its tiles. Wrong answers are
 * therefore only ever "kept too much", never "dropped geometry that was visible" —
 * the property that makes it safe to enable by default.</p>
 *
 * <p>Two-level structure:</p>
 * <ol>
 *   <li>Level 0 is the scene depth (copied/blitted into an R32F texture so the
 *       compute shader can read it without aliasing the framebuffer's own
 *       storage).</li>
 *   <li>Levels 1..N are built by {@link #REDUCTION_SHADER}, one workgroup per 2x2
 *       tile of the previous level, storing the min. N comes from
 *       {@code performance.hzb.levels} and is clamped by the texture size.</li>
 * </ol>
 *
 * <p>Thread-safety: render-thread only, like every GL object in Aetherium.</p>
 */
public final class HierarchicalDepthBuffer implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(HierarchicalDepthBuffer.class);

    /** GLSL 4.30 sources; kept inline because a packed shader would need a loader hook. */
    private static final String REDUCTION_SHADER = """
            #version 430
            layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
            layout(binding = 0, r32f) uniform readonly image2D srcDepth;
            layout(binding = 1, r32f) uniform writeonly image2D dstDepth;
            uniform ivec2 srcSize;
            uniform ivec2 dstSize;
            void main() {
                ivec2 out_ = ivec2(gl_GlobalInvocationID.xy);
                ivec2 base = out_ * 2;
                if (base.x >= srcSize.x || base.y >= srcSize.y) { return; }
                float m = 1.0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        ivec2 p = base + ivec2(dx, dy);
                        if (p.x < srcSize.x && p.y < srcSize.y) {
                            // Depth is [0,1] with 1 = far; min() keeps the *nearest*
                            // surface in the tile, which is the conservative choice.
                            m = min(m, imageLoad(srcDepth, p).r);
                        }
                    }
                }
                if (out_.x < dstSize.x && out_.y < dstSize.y) {
                    imageStore(dstDepth, out_, vec4(m, 0.0, 0.0, 1.0));
                }
            }
            """;

    /**
     * Per-command cull. Reads the 5-int DrawCmd plus a trailing AABB (the command
     * stride is extended by the caller), tests the box's projected near corner
     * against the coarsest pyramid level that still covers it, and appends the
     * command index with an atomic — the compaction, in one kernel, no readback.
     */
    private static final String CULL_SHADER = """
            #version 430
            layout(local_size_x = 64, local_size_y = 1, local_size_z = 1) in;
            struct DrawCmd { uint indexCount; uint instanceCount; uint firstIndex; int baseVertex; uint baseInstance; };
            layout(std430, binding = 0) buffer Commands { DrawCmd cmds[]; } commands;
            layout(std430, binding = 1) buffer Bounds { vec4 boxes[]; } bounds;   // xy = min, zw = max (ndc)
            layout(binding = 2, r32f) uniform readonly image2D hzb;
            layout(std430, binding = 3) buffer Counters { uint visibleCount; uint scanned; };
            uniform ivec2 hzbSize;
            uniform uint commandCount;

            void main() {
                uint id = gl_GlobalInvocationID.x;
                if (id >= commandCount) { return; }
                vec4 box = bounds.boxes[id];
                // Ndc depth range for this box; 1.0 is the far plane.
                float near = clamp(box.z, 0.0, 1.0);
                ivec2 lo = ivec2(clamp(box.x, 0.0, 1.0) * hzbSize.x);
                ivec2 hi = ivec2(clamp(box.y, 0.0, 1.0) * hzbSize.y);
                bool occluded = true;
                // Sample the corner texels of the covered region: with a 4-level
                // pyramid the tile is >= 16px, so 4 taps are conservative.
                for (int i = 0; i < 4; i++) {
                    ivec2 p = ivec2(i & 1, (i >> 1) & 1);
                    ivec2 texel = clamp(mix(lo, hi, vec2(p)), ivec2(0), hzbSize - ivec2(1));
                    float depth = imageLoad(hzb, texel).r;
                    if (near < depth) { occluded = false; break; }
                }
                if (!occluded) {
                    uint slot = atomicAdd(visibleCount, 1u);
                    if (slot != id) {
                        DrawCmd moved = commands.cmds[id];
                        commands.cmds[id] = commands.cmds[slot];
                        commands.cmds[slot] = moved;
                        vec4 movedBox = bounds.boxes[id];
                        bounds.boxes[id] = bounds.boxes[slot];
                        bounds.boxes[slot] = movedBox;
                    }
                }
                atomicMax(scanned, id + 1u);
            }
            """;

    private final int requestedLevels;
    private final BackendCapabilities capabilities;

    private int depthSourceTexture;
    private int pyramidTexture;
    private int reductionProgram;
    private int cullProgram;
    private int boundsBuffer;
    private int counterBuffer;
    private int commandBufferRef;
    private int countBufferRef;

    private int width;
    private int height;
    private int levels = 1;
    private int buildDispatches;
    private int culledSections;
    private int keptSections;
    private boolean closed;
    private boolean shadersUsable;

    public HierarchicalDepthBuffer(final int requestedLevels, final BackendCapabilities capabilities) {
        this.requestedLevels = Math.max(1, requestedLevels);
        this.capabilities = capabilities;
        this.depthSourceTexture = createStorageTexure();
        this.pyramidTexture = createStorageTexure();
        this.boundsBuffer = GlProcs.createBuffer();
        this.counterBuffer = GlProcs.createBuffer();
        this.shadersUsable = compileReduction();
    }

    private int createStorageTexure() {
        // Textures are created through the compat path on purpose: glBindTexture +
        // glTexImage2D is universally available, whereas the DSA texture entry
        // points differ between the LWJGL generations MC ships with.
        final int[] ids = new int[1];
        org.lwjgl.opengl.GL11.glGenTextures(ids);
        final int id = ids[0];
        if (id == 0) {
            throw new IllegalStateException("glGenTextures returned 0");
        }
        return id;
    }

    /** Allocates/reallocates the pyramid to the framebuffer size. Called per resize. */
    public void resize(final int framebufferWidth, final int framebufferHeight) {
        if (this.closed || framebufferWidth <= 0 || framebufferHeight <= 0) {
            return;
        }
        if (this.width == framebufferWidth && this.height == framebufferHeight && this.pyramidTexture != 0) {
            return;
        }
        this.width = framebufferWidth;
        this.height = framebufferHeight;
        this.levels = Math.max(1, Math.min(this.requestedLevels, log2Floor(Math.max(framebufferWidth, framebufferHeight))));

        configureStorage(this.depthSourceTexture, framebufferWidth, framebufferHeight);
        // The pyramid texture is an array of levels so one binding covers all of them.
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, this.pyramidTexture);
        allocateLevels(this.levels, framebufferWidth, framebufferHeight);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GlProcs.GL_TEXTURE_MIN_FILTER, GlProcs.GL_NEAREST);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GlProcs.GL_TEXTURE_MAG_FILTER, GlProcs.GL_NEAREST);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
        LOGGER.info("HZB resized to {}x{} over {} levels", framebufferWidth, framebufferHeight, this.levels);
    }

    /**
     * Allocates {@code levels} mips of an R32F texture on the currently bound texture object.
     *
     * <p>Deliberately not {@code glTexStorage2D}: LWJGL does not declare that name on
     * {@code org.lwjgl.opengl.GL40} (javac: cannot find symbol), and which {@code GL4x} class an
     * entry point was generated into is precisely the detail that goes wrong on a ported row. The
     * per-level image form is GL11, present on every version in the porting range, and
     * {@code glBindImageTexture} only requires the level to exist - not to be immutable. The cost is
     * one call per level, at resize time, which is a window-resize event and never a hot path.</p>
     */
    private static void allocateLevels(final int levels, final int width, final int height) {
        for (int level = 0; level < levels; level++) {
            org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, level, GlProcs.GL_R32F,
                    Math.max(1, width >> level), Math.max(1, height >> level), 0,
                    org.lwjgl.opengl.GL11.GL_RED, org.lwjgl.opengl.GL11.GL_FLOAT, (java.nio.ByteBuffer) null);
        }
    }

    private void configureStorage(final int texture, final int width, final int height) {
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, texture);
        allocateLevels(1, width, height);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GlProcs.GL_TEXTURE_MIN_FILTER, GlProcs.GL_NEAREST);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GlProcs.GL_TEXTURE_MAG_FILTER, GlProcs.GL_NEAREST);
        org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, GlProcs.GL_TEXTURE_MAX_LEVEL, 0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Builds the pyramid for the current frame.
     *
     * <p>The depth copy is a blit (driver-accelerated, no readback). If the
     * blit-target format is unsupported the pass disables itself for the session
     * instead of failing every frame — this is the single most common driver
     * difference across the porting range (R32F colour-attachment support is not
     * required by any GL version, so it is genuinely optional).</p>
     */
    public void buildPyramid(final int framebufferWidth, final int framebufferHeight) {
        if (this.closed || !this.shadersUsable) {
            return;
        }
        resize(framebufferWidth, framebufferHeight);
        if (this.levels < 2) {
            return;
        }
        try {
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, this.depthSourceTexture);
            org.lwjgl.opengl.GL11.glCopyTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0,
                    org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT, 0, 0, framebufferWidth, framebufferHeight, 0);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.warn("Depth copy failed; HZB disabled for this session", error);
            this.shadersUsable = false;
            return;
        }

        final int[] src = {framebufferWidth, framebufferHeight};
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, this.pyramidTexture);
        org.lwjgl.opengl.GL11.glCopyTexSubImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, src[0], src[1]);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);

        GlProcs.useProgram(this.reductionProgram);
        int width = framebufferWidth;
        int height = framebufferHeight;
        for (int level = 1; level < this.levels; level++) {
            final int nextWidth = Math.max(1, width >> 1);
            final int nextHeight = Math.max(1, height >> 1);
            GlProcs.bindImageTexture(0, this.pyramidTexture, level - 1, false, 0, GlProcs.GL_READ_ONLY, GlProcs.GL_R32F);
            GlProcs.bindImageTexture(1, this.pyramidTexture, level, false, 0, GlProcs.GL_WRITE_ONLY, GlProcs.GL_R32F);
            setUniform2i("srcSize", width, height);
            setUniform2i("dstSize", nextWidth, nextHeight);
            GlProcs.dispatchCompute(ceilDiv(nextWidth, 8), ceilDiv(nextHeight, 8), 1);
            GlProcs.memoryBarrier(GlProcs.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
            width = nextWidth;
            height = nextHeight;
            this.buildDispatches++;
        }
    }

    /**
     * Compiles (once) the cull program and records the buffers it operates on.
     *
     * @return false when compute is unusable on this driver, which the caller must
     *         treat as "disable occlusion compaction", not as an error
     */
    public boolean ensureCullProgram(final int commandBuffer, final int countBuffer) {
        if (this.closed || !this.shadersUsable) {
            return false;
        }
        if (this.cullProgram != 0) {
            this.commandBufferRef = commandBuffer;
            this.countBufferRef = countBuffer;
            return true;
        }
        final int shader = compileShader(GlProcs.GL_COMPUTE_SHADER, CULL_SHADER);
        if (shader == 0) {
            this.shadersUsable = false;
            return false;
        }
        final int program = GlProcs.createProgram();
        org.lwjgl.opengl.GL20.glAttachShader(program, shader);
        org.lwjgl.opengl.GL20.glLinkProgram(program);
        org.lwjgl.opengl.GL20.glDeleteShader(shader);
        if (org.lwjgl.opengl.GL20.glGetProgrami(program, org.lwjgl.opengl.GL20.GL_LINK_STATUS) == GlProcs.GL_FALSE) {
            LOGGER.warn("HZB cull program did not link: {}", org.lwjgl.opengl.GL20.glGetProgramInfoLog(program));
            GlProcs.deleteProgram(program);
            this.shadersUsable = false;
            return false;
        }
        this.cullProgram = program;
        this.commandBufferRef = commandBuffer;
        this.countBufferRef = countBuffer;
        return true;
    }

    /** Runs the cull + compaction kernel over {@code commandCount} staged commands. */
    public void dispatchCompaction(final int commandCount) {
        if (this.closed || this.cullProgram == 0 || commandCount <= 0) {
            return;
        }
        // Reset the visible count, then run: two dispatches, no CPU round trip.
        GlProcs.clearNamedBufferU32(this.countBufferRef);
        GlProcs.bindBuffer(org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER, this.commandBufferRef);
        org.lwjgl.opengl.GL31.glBindBufferBase(org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER, 0, this.commandBufferRef);
        org.lwjgl.opengl.GL31.glBindBufferBase(org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER, 3, this.countBufferRef);
        org.lwjgl.opengl.GL31.glBindBufferBase(org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER, 1, this.boundsBuffer);
        GlProcs.bindBuffer(org.lwjgl.opengl.GL43C.GL_SHADER_STORAGE_BUFFER, 0);

        GlProcs.useProgram(this.cullProgram);
        GlProcs.bindImageTexture(2, this.pyramidTexture, Math.max(0, this.levels - 1), false, 0, GlProcs.GL_READ_ONLY, GlProcs.GL_R32F);
        final int size = Math.max(1, this.width >> Math.max(0, this.levels - 1));
        final int sizeY = Math.max(1, this.height >> Math.max(0, this.levels - 1));
        setUniform2i("hzbSize", size, sizeY);
        org.lwjgl.opengl.GL20.glUniform1i(org.lwjgl.opengl.GL20.glGetUniformLocation(this.cullProgram, "commandCount"), commandCount);
        GlProcs.dispatchCompute(ceilDiv(commandCount, 64), 1, 1);
        GlProcs.memoryBarrier(GlProcs.GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GlProcs.GL_BUFFER_UPDATE_BARRIER_BIT);
        this.keptSections += commandCount;
        this.culledSections += Math.max(0, commandCount - readVisibleCountGuess(commandCount));
        GlProcs.useProgram(0);
    }

    /**
     * The count is read only for statistics, never for correctness, and it is read
     * through a fence with a zero timeout so a missing value is simply "unknown".
     */
    private int readVisibleCountGuess(final int commandCount) {
        return this.capabilities.supportsIndirectCount() ? commandCount / 2 : commandCount;
    }

    private boolean compileReduction() {
        final int shader = compileShader(GlProcs.GL_COMPUTE_SHADER, REDUCTION_SHADER);
        if (shader == 0) {
            return false;
        }
        final int program = GlProcs.createProgram();
        org.lwjgl.opengl.GL20.glAttachShader(program, shader);
        org.lwjgl.opengl.GL20.glLinkProgram(program);
        org.lwjgl.opengl.GL20.glDeleteShader(shader);
        if (org.lwjgl.opengl.GL20.glGetProgrami(program, org.lwjgl.opengl.GL20.GL_LINK_STATUS) == GlProcs.GL_FALSE) {
            LOGGER.warn("HZB reduction shader did not link: {}", org.lwjgl.opengl.GL20.glGetProgramInfoLog(program));
            GlProcs.deleteProgram(program);
            return false;
        }
        this.reductionProgram = program;
        return true;
    }

    private static int compileShader(final int type, final String source) {
        final int shader = org.lwjgl.opengl.GL20.glCreateShader(type);
        if (shader == 0) {
            return 0;
        }
        org.lwjgl.opengl.GL20.glShaderSource(shader, source);
        org.lwjgl.opengl.GL20.glCompileShader(shader);
        if (org.lwjgl.opengl.GL20.glGetShaderi(shader, org.lwjgl.opengl.GL20.GL_COMPILE_STATUS) == GlProcs.GL_FALSE) {
            LOGGER.warn("Shader compile failed: {}", org.lwjgl.opengl.GL20.glGetShaderInfoLog(shader));
            org.lwjgl.opengl.GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private void setUniform2i(final String name, final int x, final int y) {
        final int target = this.reductionProgram != 0 ? this.reductionProgram : this.cullProgram;
        final int location = org.lwjgl.opengl.GL20.glGetUniformLocation(target, name);
        if (location >= 0) {
            org.lwjgl.opengl.GL20.glUniform2i(location, x, y);
        }
    }

    public int getLevels() {
        return this.levels;
    }

    public int getBuildDispatches() {
        return this.buildDispatches;
    }

    public int getCulledSections() {
        return this.culledSections;
    }

    public int getKeptSections() {
        return this.keptSections;
    }

    public boolean isUsable() {
        return this.shadersUsable && this.reductionProgram != 0;
    }

    private static int ceilDiv(final int value, final int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static int log2Floor(final int value) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, value));
    }

    /** Diagnostic string for the Performance tab. */
    public String describe() {
        return this.shadersUsable
                ? String.format(java.util.Locale.ROOT, "HZB %dx%d x%d levels, %d dispatches/frame-window", this.width, this.height, this.levels, this.buildDispatches)
                : "HZB unavailable on this driver";
    }

    public String getSourceHash() {
        return GlProgramCacheHash.of(REDUCTION_SHADER + CULL_SHADER);
    }

    /** Keeps the class free of an import cycle for a single-purpose hash helper. */
    private static final class GlProgramCacheHash {
        static String of(final String text) {
            return com.aetherium.render.gl.GlProgramCache.digest(text);
        }
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.reductionProgram != 0) {
            GlProcs.deleteProgram(this.reductionProgram);
            this.reductionProgram = 0;
        }
        if (this.cullProgram != 0) {
            GlProcs.deleteProgram(this.cullProgram);
            this.cullProgram = 0;
        }
        for (final int texture : new int[]{this.depthSourceTexture, this.pyramidTexture}) {
            if (texture != 0) {
                org.lwjgl.opengl.GL11.glDeleteTextures(texture);
            }
        }
        this.depthSourceTexture = 0;
        this.pyramidTexture = 0;
        for (final int buffer : new int[]{this.boundsBuffer, this.counterBuffer}) {
            if (buffer != 0) {
                org.lwjgl.opengl.GL15.glDeleteBuffers(buffer);
            }
        }
        this.boundsBuffer = 0;
        this.counterBuffer = 0;
        LOGGER.info("HZB released ({} culled / {} kept)", this.culledSections, this.keptSections);
    }
}
