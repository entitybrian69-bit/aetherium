package dev.aetherium.engine.gl;

import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.GpuCapabilities;
import dev.aetherium.engine.RenderBackend;
import dev.aetherium.platform.Services;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL46;

import java.nio.FloatBuffer;
import java.nio.file.Path;

/**
 * OpenGL backend. On capable drivers (GL 4.5/4.6 + MDI + compute) it runs the "modern" path: persistent vertex
 * arena, indirect command batching, and HZB culling with GPU command compaction. On GL 3.3 (LTW, Zink on older
 * Mesa) it degrades to core-profile batching, and on GL 2.1 (GL4ES) it only applies CPU-side optimizations.
 */
public final class GlBackend implements RenderBackend {
    private final Kind kind;
    private GpuCapabilities caps;
    private ShaderCache shaderCache;
    private HierarchicalZBuffer hzb;
    private IndirectDrawBatcher opaqueBatcher;
    private IndirectDrawBatcher compactedBatcher;
    private PersistentMappedBuffer vertexArena;
    private boolean hzbEnabled;
    private boolean mdiEnabled;
    private int fbWidth, fbHeight;
    private int lastVisibleSections;
    private int lastSubmittedSections;

    public GlBackend(GpuCapabilities caps) {
        this.caps = caps;
        if (caps.supportsModernPath()) kind = Kind.OPENGL_MODERN;
        else if (caps.supportsCorePath()) kind = Kind.OPENGL_CORE;
        else kind = Kind.OPENGL_LEGACY;
    }

    @Override public Kind kind() { return kind; }

    @Override public boolean initialize(AetheriumConfig config) {
        Path cacheDir = Services.PLATFORM.getGameDirectory().resolve(".aetherium").resolve("shader-cache");
        shaderCache = new ShaderCache(cacheDir, caps.parallelShaderCompile(), caps.programBinary() && config.advanced.shaderBinaryCache);
        applyConfig(config);
        Aetherium.LOGGER.info("[Aetherium] GL backend online: {} -> {}", caps, kind);
        return true;
    }

    @Override public void applyConfig(AetheriumConfig config) {
        boolean mobile = config.android.mobileMemoryMode;
        boolean wantHzb = config.performance.hzbOcclusionCulling && kind == Kind.OPENGL_MODERN;
        boolean wantMdi = config.performance.multiDrawIndirect && kind == Kind.OPENGL_MODERN;
        boolean wantPersistent = config.performance.persistentMappedBuffers && caps.bufferStorage()
                && !(mobile && Runtime.getRuntime().maxMemory() < 4L * 1024 * 1024 * 1024);

        if (wantHzb != hzbEnabled || (hzb != null && mobile != hzbScaleIsMobile)) {
            if (hzb != null) { hzb.close(); hzb = null; }
            if (wantHzb) {
                hzb = new HierarchicalZBuffer(mobile ? 0.25f : 0.5f);
                hzbScaleIsMobile = mobile;
                hzb.requestPrograms(shaderCache);
                if (fbWidth > 0) hzb.resize(fbWidth, fbHeight);
            }
            hzbEnabled = wantHzb;
        }
        if (wantMdi != mdiEnabled) {
            if (opaqueBatcher != null) { opaqueBatcher.close(); opaqueBatcher = null; }
            if (compactedBatcher != null) { compactedBatcher.close(); compactedBatcher = null; }
            if (wantMdi) {
                int maxCmds = mobile ? 8192 : 32768;
                opaqueBatcher = new IndirectDrawBatcher(maxCmds);
                compactedBatcher = new IndirectDrawBatcher(maxCmds);
            }
            mdiEnabled = wantMdi;
        }
        long arenaBytes = arenaSizeBytes(config, mobile);
        if (wantPersistent && (vertexArena == null || vertexArena.regionSize() != alignArena(arenaBytes))) {
            if (vertexArena != null) vertexArena.close();
            vertexArena = new PersistentMappedBuffer(arenaBytes);
        } else if (!wantPersistent && vertexArena != null) {
            vertexArena.close();
            vertexArena = null;
        }
    }
    private boolean hzbScaleIsMobile;

    private static long arenaSizeBytes(AetheriumConfig cfg, boolean mobile) {
        if (cfg.advanced.vertexArenaMb > 0) return cfg.advanced.vertexArenaMb * 1024L * 1024L;
        long heap = Runtime.getRuntime().maxMemory();
        long mb = Math.max(64, Math.min(mobile ? 256 : 1024, heap / (1024 * 1024) / 8));
        return mb * 1024L * 1024L;
    }
    private static long alignArena(long v) { return (v + 255) & ~255L; }

    @Override public void beginFrame(int framebufferWidth, int framebufferHeight) {
        fbWidth = framebufferWidth; fbHeight = framebufferHeight;
        shaderCache.poll();
        if (hzb != null) hzb.resize(framebufferWidth, framebufferHeight);
        if (vertexArena != null) vertexArena.nextFrame();
        if (opaqueBatcher != null) { opaqueBatcher.beginFrame(); compactedBatcher.beginFrame(); }
    }

    /** Submit a section for this frame's opaque pass; bounds are camera-relative. */
    public boolean submitSection(FloatBuffer boundsOut, int indexCount, int firstIndex, int baseVertex) {
        if (opaqueBatcher == null) return false;
        return opaqueBatcher.push(indexCount, 1, firstIndex, baseVertex, opaqueBatcher.pendingCount());
    }

    @Override public void runOcclusionPass(int depthTextureId, int width, int height, float[] viewProjection) {
        if (hzb == null || !hzb.ready() || opaqueBatcher == null) return;
        int n = opaqueBatcher.pendingCount();
        lastSubmittedSections = n;
        if (n == 0) return;
        hzb.build(depthTextureId);
        hzb.cull(n, viewProjection, opaqueBatcher.commandBufferId(), opaqueBatcher.commandBufferOffset(),
                compactedBatcher.commandBufferId(), compactedBatcher.commandBufferOffset());
    }

    /** Draw the compacted command list with the GPU-written count (GL_ARB_indirect_parameters). */
    public void drawCompacted(int maxDraws) {
        if (hzb == null || compactedBatcher == null) return;
        GL30.glBindBuffer(GL46.GL_DRAW_INDIRECT_BUFFER, compactedBatcher.commandBufferId());
        GL30.glBindBuffer(GL46.GL_PARAMETER_BUFFER, hzb.counterBuffer());
        GL46.glMultiDrawElementsIndirectCount(GL11.GL_TRIANGLES, GL11.GL_UNSIGNED_INT,
                compactedBatcher.commandBufferOffset(), 0L, maxDraws, 0);
    }

    @Override public void endFrame() {
        if (vertexArena != null) vertexArena.fenceCurrent();
    }

    public void uploadBounds(FloatBuffer bounds, int count) { if (hzb != null) hzb.uploadBounds(bounds, count); }
    public PersistentMappedBuffer vertexArena() { return vertexArena; }
    public IndirectDrawBatcher opaqueBatcher() { return opaqueBatcher; }
    public ShaderCache shaderCache() { return shaderCache; }
    public GpuCapabilities capabilities() { return caps; }
    public int lastSubmittedSections() { return lastSubmittedSections; }
    public int lastVisibleSections() { return lastVisibleSections; }

    public void refreshDebugCounters() {
        if (hzb != null && lastSubmittedSections > 0) lastVisibleSections = hzb.countVisible(lastSubmittedSections);
    }

    @Override public String describe() {
        return "OpenGL " + caps.majorVersion() + "." + caps.minorVersion() + " [" + kind + "] HZB=" + hzbEnabled + " MDI=" + mdiEnabled
                + " PMB=" + (vertexArena != null);
    }

    @Override public void close() {
        if (hzb != null) hzb.close();
        if (opaqueBatcher != null) opaqueBatcher.close();
        if (compactedBatcher != null) compactedBatcher.close();
        if (vertexArena != null) vertexArena.close();
        if (shaderCache != null) shaderCache.close();
    }
}
