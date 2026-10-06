package dev.aetherium.engine;

import dev.aetherium.Aetherium;
import dev.aetherium.android.AndroidLauncherCompat;
import dev.aetherium.chunk.AetheriumChunkBuilder;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.gl.GlBackend;
import dev.aetherium.engine.vk.VulkanBackend;
import dev.aetherium.frame.FrameTimeTracker;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Zero-driver-overhead abstraction over {@link RenderBackend}s. Owns backend selection (OpenGL vs Vulkan vs Android
 * translation layers), the live Aetherium/Compatibility switch, and the per-frame lifecycle that the GameRenderer and
 * LevelRenderer mixins drive.
 */
public final class AetheriumRenderEngine {
    private static final AetheriumRenderEngine INSTANCE = new AetheriumRenderEngine();

    private final AtomicReference<RenderBackend> backend = new AtomicReference<>();
    private volatile GpuCapabilities caps;
    private volatile boolean initialized;
    private volatile boolean compatibilityMode;
    private volatile AetheriumConfig.GraphicsApi activeApi = AetheriumConfig.GraphicsApi.OPENGL;
    private volatile AetheriumConfig.GraphicsApi requestedApi = AetheriumConfig.GraphicsApi.AUTO;
    private final float[] viewProj = new float[16];
    private int frameWidth, frameHeight;
    private long frameIndex;

    private AetheriumRenderEngine() {}

    public static AetheriumRenderEngine get() { return INSTANCE; }

    // ------------------------------------------------------------------ lifecycle

    /** Render thread, GL context current. Idempotent. */
    public void initialize(AetheriumConfig config) {
        if (initialized) return;
        caps = GpuCapabilities.probe();
        Aetherium.LOGGER.info("[Aetherium] GPU: {}", caps);
        compatibilityMode = config.advanced.engineMode == AetheriumConfig.EngineMode.COMPATIBILITY;
        requestedApi = config.advanced.graphicsApi;
        selectBackend(config);
        AetheriumChunkBuilder.get().applyConfig(config);
        initialized = true;
    }

    private void selectBackend(AetheriumConfig config) {
        RenderBackend old = backend.getAndSet(null);
        if (old != null) old.close();

        AetheriumConfig.GraphicsApi api = resolveApi(config);
        RenderBackend chosen = null;
        if (api == AetheriumConfig.GraphicsApi.VULKAN) {
            VulkanBackend vk = new VulkanBackend();
            if (vk.initialize(config)) chosen = vk;
            else Aetherium.LOGGER.warn("[Aetherium] Vulkan unavailable; falling back to OpenGL");
        }
        if (chosen == null) {
            GlBackend gl = new GlBackend(caps);
            gl.initialize(config);
            chosen = gl;
            api = AetheriumConfig.GraphicsApi.OPENGL;
        }
        backend.set(chosen);
        activeApi = api;
        Aetherium.LOGGER.info("[Aetherium] Active backend: {}", chosen.describe());
    }

    private AetheriumConfig.GraphicsApi resolveApi(AetheriumConfig config) {
        AetheriumConfig.GraphicsApi api = config.advanced.graphicsApi;
        if (AndroidLauncherCompat.isAndroid()) {
            return switch (AndroidLauncherCompat.resolveBackend(config)) {
                case VULKAN_NATIVE -> AetheriumConfig.GraphicsApi.VULKAN;
                default -> AetheriumConfig.GraphicsApi.OPENGL;
            };
        }
        if (api == AetheriumConfig.GraphicsApi.AUTO) {
            // Desktop auto: GL 4.6 DSA is the proven path; Vulkan is opt-in until presentation is wired.
            return AetheriumConfig.GraphicsApi.OPENGL;
        }
        return api;
    }

    /** Hot-apply config: backend swap, engine-mode switch, and per-backend feature toggles, all without restart. */
    public void applyConfig(AetheriumConfig config) {
        if (!initialized) return;
        compatibilityMode = config.advanced.engineMode == AetheriumConfig.EngineMode.COMPATIBILITY;
        AetheriumChunkBuilder.get().applyConfig(config);
        if (config.advanced.graphicsApi != requestedApi) {
            requestedApi = config.advanced.graphicsApi;
            selectBackend(config);
        } else {
            RenderBackend b = backend.get();
            if (b != null) b.applyConfig(config);
        }
    }

    public void shutdown() {
        RenderBackend b = backend.getAndSet(null);
        if (b != null) b.close();
        AetheriumChunkBuilder.get().shutdown();
        initialized = false;
    }

    // ------------------------------------------------------------------ per-frame hooks (called from mixins)

    public void beginFrame(int fbWidth, int fbHeight) {
        FrameTimeTracker.get().markFrame();
        frameIndex++;
        frameWidth = fbWidth; frameHeight = fbHeight;
        if (compatibilityMode) return;
        RenderBackend b = backend.get();
        if (b != null) b.beginFrame(fbWidth, fbHeight);
        AetheriumChunkBuilder.get().drainCompleted();
    }

    public void setViewProjection(float[] columnMajor16) {
        System.arraycopy(columnMajor16, 0, viewProj, 0, 16);
    }

    /** Run after the opaque terrain pass has written depth; culls what the translucent/entity passes will submit. */
    public void occlusionPass(int depthTextureId) {
        if (compatibilityMode) return;
        RenderBackend b = backend.get();
        if (b != null) b.runOcclusionPass(depthTextureId, frameWidth, frameHeight, viewProj);
    }

    public void endFrame() {
        if (compatibilityMode) return;
        RenderBackend b = backend.get();
        if (b != null) b.endFrame();
    }

    // ------------------------------------------------------------------ queries

    public boolean isInitialized() { return initialized; }
    public boolean isCompatibilityMode() { return compatibilityMode; }
    public boolean isAetheriumActive() { return initialized && !compatibilityMode; }
    public AetheriumConfig.GraphicsApi activeApi() { return activeApi; }
    public GpuCapabilities capabilities() { return caps; }
    public RenderBackend backend() { return backend.get(); }
    public long frameIndex() { return frameIndex; }

    public String statusLine() {
        RenderBackend b = backend.get();
        return (compatibilityMode ? "Compatibility" : "Aetherium") + " | " + (b == null ? "no backend" : b.describe());
    }
}
