package com.aetherium.render.gl;

import java.util.Objects;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.render.backend.BackendCapabilities;
import com.aetherium.render.backend.BackendProber;
import com.aetherium.render.backend.RenderBackend;
import com.aetherium.render.hzb.HierarchicalDepthBuffer;
import com.aetherium.util.AetheriumLog;

/**
 * Owns every GL object Aetherium creates, and is the only object that may be
 * constructed or destroyed while a context is current.
 *
 * <p>Lifetime: created on the render thread at {@code Minecraft#onContextReady}
 * (GLFW window created, capabilities current), destroyed at
 * {@code Aetherium#shutdown} and before every hot-swap. Worker threads never
 * receive a {@code GlDevice}; they hand immutable meshes to the arena, and only
 * the render thread issues GL calls. That single-owner rule is what allows the
 * arenas below to have no synchronization at all.</p>
 */
public final class GlDevice implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlDevice.class);

    private final BackendCapabilities capabilities;
    private final RenderBackend backend;
    private final AndroidEnvironment android;

    /** Owned objects; null after {@link #close()}. Read only on the render thread. */
    private GlPersistentArena uploadArena;
    private GlProgramCache programCache;
    private GlAsyncShaderCompiler shaderCompiler;
    private GlIndirectBatch indirectBatch;
    private HierarchicalDepthBuffer hzb;

    /** Set by {@link #attach} — the live config, retained for per-frame reads. */
    private volatile AetheriumConfig config;

    private boolean attached;
    private int framesRecorded;
    private long glErrorsObserved;
    private long mappedBytesPeak;

    public GlDevice(final AndroidEnvironment android) {
        this.android = android;
        this.capabilities = BackendProber.probe(android);
        this.backend = com.aetherium.render.backend.BackendSelector.resolve(
                AetheriumConfig.BackendChoice.AUTO, this.capabilities);
        LOGGER.info("GlDevice created for {} — {}", this.backend.getId(), this.capabilities.describe());
    }

    public GlDevice(final AndroidEnvironment android, final AetheriumConfig config) {
        this.android = android;
        this.capabilities = BackendProber.probe(android);
        this.backend = com.aetherium.render.backend.BackendSelector.resolve(config.backend.get(), this.capabilities);
        com.aetherium.render.backend.BackendSelector.apply(config, this.capabilities, android);
        LOGGER.info("GlDevice created for {} — {}", this.backend.getId(), this.capabilities.describe());
    }

    public BackendCapabilities getCapabilities() {
        return this.capabilities;
    }

    public RenderBackend getBackend() {
        return this.backend;
    }

    public AndroidEnvironment getAndroid() {
        return this.android;
    }

    public GlPersistentArena getUploadArena() {
        return this.uploadArena;
    }

    public GlProgramCache getProgramCache() {
        return this.programCache;
    }

    public GlIndirectBatch getIndirectBatch() {
        return this.indirectBatch;
    }

    public HierarchicalDepthBuffer getDepthPyramid() {
        return this.hzb;
    }

    public boolean isAttached() {
        return this.attached;
    }

    /**
     * Builds the GPU-side resources. Called with a current context. Idempotent:
     * a second call only refreshes the config reference, which is what lets the
     * mixin that notices context loss re-attach safely.
     */
    public void attach(final AetheriumConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        if (this.attached) {
            return;
        }
        final boolean wantPersistent = config.persistentBuffers.get() && this.capabilities.supportsPersistentMapping();
        final long arenaBytes = desiredArenaBytes(config);
        try {
            this.uploadArena = new GlPersistentArena(arenaBytes, wantPersistent, this.backend.supportsPersistentBuffers());
            LOGGER.info("Upload arena: {} MB, persistent={} (mapped={} )",
                    arenaBytes / (1024 * 1024), wantPersistent, this.uploadArena.isPersistent());
        } catch (final RuntimeException error) {
            // Arena creation failing must not be fatal: the fallback is a smaller
            // sub-data arena, which is what GL_CORE would use anyway.
            LOGGER.warn("Persistent arena failed, rebuilding with buffer-sub-data uploads", error);
            this.uploadArena = new GlPersistentArena(Math.min(arenaBytes, 8L * 1024 * 1024), false, false);
        }

        if (config.programBinaryCache.get()) {
            try {
                this.programCache = new GlProgramCache(java.nio.file.Path.of(System.getProperty("user.dir", "."), "aetherium", "program-cache"), this.backend);
            } catch (final RuntimeException error) {
                LOGGER.warn("Program binary cache disabled after an init failure", error);
                this.programCache = null;
            }
        }
        if (config.asyncShaderCompile.get() && this.capabilities.isAsyncShaderCompileUsable()) {
            this.shaderCompiler = new GlAsyncShaderCompiler(this.programCache);
        }
        if (config.indirectDraw.get() && this.backend.supportsIndirectDraw()) {
            this.indirectBatch = new GlIndirectBatch(config.indirectBatchCapacity.get(), this.uploadArena, this.capabilities);
        }
        if (config.hzb.get() && this.capabilities.supportsHierarchicalZ()) {
            this.hzb = new HierarchicalDepthBuffer(config.hzbDepth.get(), this.capabilities);
        }
        this.attached = true;
        LOGGER.info("GlDevice attached: arena, programCache={}, asyncCompile={}, indirect={}, hzb={}",
                this.programCache != null, this.shaderCompiler != null, this.indirectBatch != null, this.hzb != null);
    }

    /**
     * Per-frame entry, called from the tail of {@code GameRenderer#renderLevel}
     * (see {@code GameRendererMixin}) so all Aetherium work happens after vanilla
     * has submitted its own commands and before the swap.
     */
    public void beginFrame() {
        if (!this.attached) {
            return;
        }
        this.framesRecorded++;
        if (this.uploadArena != null) {
            this.uploadArena.beginFrame();
        }
        if (this.shaderCompiler != null) {
            this.shaderCompiler.poll();
        }
        this.mappedBytesPeak = Math.max(this.mappedBytesPeak, this.uploadArena == null ? 0 : this.uploadArena.getMappedBytes());
    }

    public void endFrame() {
        if (!this.attached) {
            return;
        }
        if (this.indirectBatch != null) {
            // The returned label is for logs; the HUD reads the batch's own counters
            // (getStagedCommands/getFramesWithDraws), so nothing here consumes the string.
            this.indirectBatch.submit(this.hzb, GlProcs.GL_UNSIGNED_SHORT);
        }
        if (this.uploadArena != null) {
            this.uploadArena.endFrame();
        }
        if (this.config != null && this.config.glErrors.get()) {
            final String errors = GlProcs.drainErrors();
            if (errors != null) {
                this.glErrorsObserved++;
                // First three occurrences at warn, then dev: a driver that disagrees
                // with one optional call would otherwise flood the log every frame.
                if (this.glErrorsObserved <= 3) {
                    LOGGER.warn("GL error after frame {}: {}", this.framesRecorded, errors);
                } else {
                    LOGGER.dev("GL error after frame {}: {} (suppressed after 3)", this.framesRecorded, errors);
                }
            }
        }
    }

    /** Runs the shadow occlusion pass: HZB build + command compaction, no drawing. */
    public void runShadowPasses(final int framebufferWidth, final int framebufferHeight) {
        if (!this.attached || this.hzb == null || !this.config.hzb.get()) {
            return;
        }
        this.hzb.buildPyramid(framebufferWidth, framebufferHeight);
        if (this.indirectBatch != null) {
            this.indirectBatch.compact(this.hzb);
        }
    }

    public long getGlErrorsObserved() {
        return this.glErrorsObserved;
    }

    public long getMappedBytesPeak() {
        return this.mappedBytesPeak;
    }

    public int getFramesRecorded() {
        return this.framesRecorded;
    }

    /**
     * Arena sizing. Mobile gets a hard ceiling: the whole point of persistent
     * mapping is to never allocate during a frame, and a 96 MB arena on a device
     * with a 1 GB heap would make the *first* frame the expensive one.
     */
    private long desiredArenaBytes(final AetheriumConfig config) {
        final long fromBudget = Math.max(1L, config.uploadBudgetKb.get().longValue()) * 1024L * 4L;
        if (this.android != null && this.android.isAndroid()) {
            final long fromHeap = Math.max(8L, this.android.getMemoryBudgetMb()) * 1024L * 1024L / 16L;
            return Math.min(fromBudget, Math.max(4L * 1024 * 1024, fromHeap));
        }
        return Math.max(16L * 1024 * 1024, fromBudget);
    }

    /** Releases every GPU object. Safe to call twice, and safe if the context died. */
    @Override
    public void close() {
        if (!this.attached) {
            return;
        }
        this.attached = false;
        // Reverse construction order: consumers before the buffers they read.
        if (this.hzb != null) {
            this.hzb.close();
            this.hzb = null;
        }
        if (this.indirectBatch != null) {
            this.indirectBatch.close();
            this.indirectBatch = null;
        }
        if (this.shaderCompiler != null) {
            this.shaderCompiler.close();
            this.shaderCompiler = null;
        }
        if (this.programCache != null) {
            this.programCache.close();
            this.programCache = null;
        }
        if (this.uploadArena != null) {
            this.uploadArena.close();
            this.uploadArena = null;
        }
        LOGGER.info("GlDevice released after {} frames (peak mapped {} KB)", this.framesRecorded, this.mappedBytesPeak / 1024);
    }

    /** One-line summary for the GUI footer and the F3 overlay. */
    public String describe() {
        return this.backend.getDisplayName()
                + (this.uploadArena != null && this.uploadArena.isPersistent() ? " + PMB" : "")
                + (this.indirectBatch != null ? " + MDI" : "")
                + (this.hzb != null ? " + HZB" : "")
                + (this.programCache != null ? " + bin-cache" : "");
    }
}
