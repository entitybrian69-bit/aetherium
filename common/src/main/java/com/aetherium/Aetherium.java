package com.aetherium;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.compat.ModConflictScanner;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigStore;
import com.aetherium.gamma.GammaApplier;
import com.aetherium.hud.BenchmarkRecorder;
import com.aetherium.hud.FrameStats;
import com.aetherium.lighting.DynamicLightEngine;
import com.aetherium.platform.PlatformAdapter;
import com.aetherium.platform.PlatformServices;
import com.aetherium.render.backend.BackendCapabilities;
import com.aetherium.render.backend.RenderBackend;
import com.aetherium.render.gl.GlDevice;
import com.aetherium.shader.IrisBridge;
import com.aetherium.util.AetheriumLog;

/**
 * Engine-wide ownership and lifecycle. Everything Aetherium owns is reachable
 * from here, which is what lets the whole renderer be torn down and rebuilt for
 * a backend hot-swap without a game restart.
 *
 * <p>Lifecycle (render thread unless noted):</p>
 * <ol>
 *   <li>{@link #initialize()} — idempotent; called from the loader entry point
 *       and again defensively from the first mixin touch (mixin ordering differs
 *       between loaders and versions, so both paths must be safe).</li>
 *   <li>{@link #onContextReady(GlDevice)} — after the GL context exists: probe
 *       GPU, resolve backend, build arenas, cache program binaries.</li>
 *   <li>{@link #onWorldEnter()} / {@link #onWorldLeave()} — light engine and
 *       mesh scheduler scope.</li>
 *   <li>{@link #shutdown()} — deterministically, on the render thread.</li>
 * </ol>
 *
 * <p>Shared-state rules: {@code #state} is an {@link AtomicReference} so a
 * worker thread reading it during a swap sees a consistent value; the subsystem
 * fields are {@code volatile} references swapped as a unit by
 * {@link #swapSubsystems(Subsystems)} while the render thread is blocked at
 * {@link RenderSafety#beginSwap()}/{@link RenderSafety#endSwap()} — the swap
 * never races a draw.</p>
 */
public final class Aetherium {
    public static final String MOD_ID = "aetherium";
    public static final String MOD_NAME = "Aetherium";
    /** Replaced by processResources from gradle.properties in a real build. */
    public static final String VERSION = versionFromManifest();

    private static final AetheriumLog LOGGER = AetheriumLog.of(Aetherium.class);
    private static final AtomicBoolean INITIALIZING = new AtomicBoolean();

    /** Coarse engine state; the GUI and every mixin branch on this. */
    public enum State {
        /** Not initialized: all hooks pass through to vanilla. */
        OFF,
        /** All Aetherium features active. */
        ACTIVE,
        /** Vanilla renderer path retained; Aetherium measures only. */
        SHADOW,
        /** Disabled by a conflict or an unsupported GPU. */
        INCOMPATIBLE,
        /** Shutdown complete; further calls are no-ops. */
        CLOSED
    }

    private static final AtomicReference<State> STATE = new AtomicReference<>(State.OFF);
    private static final AtomicReference<Subsystems> SUBSYSTEMS = new AtomicReference<>();
    private static final RenderSafety RENDER_SAFETY = new RenderSafety();

    private Aetherium() {
    }

    public static String getModId() {
        return MOD_ID;
    }

    public static State getState() {
        return STATE.get();
    }

    public static boolean isActive() {
        return STATE.get() == State.ACTIVE;
    }

    /** True when Aetherium must not touch rendering at all (compat / conflicts). */
    public static boolean isVanillaPath() {
        final State state = STATE.get();
        return state == State.OFF || state == State.CLOSED || state == State.INCOMPATIBLE;
    }

    public static RenderSafety renderSafety() {
        return RENDER_SAFETY;
    }

    // ------------------------------------------------------------- subsystems

    /** Immutable bundle so a hot-swap publishes a consistent set of references. */
    public static final class Subsystems {
        private final AetheriumConfig config;
        private final ConfigStore store;
        private final FrameStats frameStats;
        private final DynamicLightEngine lights;
        private final GammaApplier gamma;
        private final IrisBridge iris;
        private final ModConflictScanner conflicts;

        Subsystems(final AetheriumConfig config, final ConfigStore store, final FrameStats frameStats,
                   final DynamicLightEngine lights, final GammaApplier gamma, final IrisBridge iris,
                   final ModConflictScanner conflicts) {
            this.config = config;
            this.store = store;
            this.frameStats = frameStats;
            this.lights = lights;
            this.gamma = gamma;
            this.iris = iris;
            this.conflicts = conflicts;
        }
    }

    public static AetheriumConfig config() {
        return require().config;
    }

    public static ConfigStore store() {
        return require().store;
    }

    public static FrameStats frameStats() {
        return require().frameStats;
    }

    public static DynamicLightEngine lights() {
        return require().lights;
    }

    public static GammaApplier gamma() {
        return require().gamma;
    }

    public static IrisBridge iris() {
        return require().iris;
    }

    public static ModConflictScanner conflicts() {
        return require().conflicts;
    }

    private static Subsystems require() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            throw new IllegalStateException("Aetherium is not initialized yet — access from a mixin must be guarded by Aetherium.getState()");
        }
        return local;
    }

    public static Subsystems subsystemsOrNull() {
        return SUBSYSTEMS.get();
    }

    // ------------------------------------------------------------------- boot

    /**
     * Loader-independent boot. Safe to call twice; the second call is a no-op.
     *
     * @return true when Aetherium is usable after this call
     */
    public static boolean initialize() {
        if (STATE.get() != State.OFF) {
            return STATE.get() == State.ACTIVE || STATE.get() == State.SHADOW;
        }
        if (!INITIALIZING.compareAndSet(false, true)) {
            LOGGER.dev("initialize() already in progress on another thread; deferring");
            return false;
        }
        try {
            return doInitialize();
        } finally {
            INITIALIZING.set(false);
        }
    }

    private static boolean doInitialize() {
        final PlatformAdapter platform = PlatformServices.currentOrNull();
        if (platform == null) {
            LOGGER.warn("No platform adapter; Aetherium stays off (expected in unit tests / datagen only)");
            STATE.set(State.INCOMPATIBLE);
            return false;
        }
        if (!platform.isClient()) {
            LOGGER.info("Non-client environment ({}) — nothing to do", platform.platformName());
            STATE.set(State.OFF);
            return false;
        }

        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final ConfigStore store = new ConfigStore(platform.configDirectory().resolve("aetherium.json"), config);
        store.load();
        LOGGER.setDevEnabled(config.debugLogging.get());

        final ModConflictScanner conflicts = new ModConflictScanner(platform);
        conflicts.scan(config);

        // Tell the mixin plugin which game version this is, and stand the mixins down
        // for a HARD conflict. Timing note that makes this more than cosmetics: Mixin
        // transforms a class the first time it is loaded, and the render classes
        // (GameRenderer, LevelRenderer, Gui, LightTexture, OptionsScreen) are all first
        // loaded *after* this call, so refusing them here takes effect on this launch.
        // MinecraftMixin is already applied by now - which is why its hooks are guarded
        // by isVanillaPath() instead of relying on the veto.
        System.setProperty("aetherium.minecraft.version", platform.minecraftVersion());
        com.aetherium.mixin.AetheriumMixinPlugin.announceMinecraftVersion(platform.minecraftVersion());
        if (conflicts.requiresIncompatible() && config.conflictAutoDelegate.get()) {
            com.aetherium.mixin.AetheriumMixinPlugin.setMixinActive(false);
        }

        final AndroidEnvironment android = AndroidEnvironment.probe(config);
        final FrameStats frameStats = new FrameStats();
        final DynamicLightEngine lights = new DynamicLightEngine(config, android);
        final GammaApplier gamma = new GammaApplier(config);
        final IrisBridge iris = new IrisBridge(config);
        iris.bind();

        SUBSYSTEMS.set(new Subsystems(config, store, frameStats, lights, gamma, iris, conflicts));

        // tools/benchmark.sh --mc sets this property; without it the recorder holds no
        // file handle and adds nothing to the frame loop.
        BenchmarkRecorder.start();

        // Wire config -> behaviour. Kept in one place so a delta that adds an
        // option has exactly one obvious insertion point.
        config.debugLogging.addListener((Boolean value) -> LOGGER.setDevEnabled(value.booleanValue()));
        config.enabled.addListener((Boolean value) -> LOGGER.info("Renderer {} — state is now {}", value ? "enabled" : "disabled (Compatibility mode)", STATE.get()));
        config.dynamicLights.addListener((Boolean value) -> {
            if (!value.booleanValue()) {
                lights.clear();
            }
        });

        if (android.isAndroid()) {
            LOGGER.info("Android environment: launcher={}, renderer={}, heap budget={} MB",
                    android.getLauncher().getDisplayName(), android.getRenderer().getDisplayName(), android.getMemoryBudgetMb());
            android.applyRuntimeHints(config);
        }

        if (conflicts.requiresIncompatible()) {
            LOGGER.warn("Aetherium is standing down: {}", conflicts.summarize());
            STATE.set(State.INCOMPATIBLE);
            return false;
        }

        STATE.set(config.enabled.get().booleanValue() ? State.ACTIVE : State.SHADOW);
        if (config.experimentalFullRenderer.get().booleanValue()) {
            // See docs/ARCHITECTURE.md "Honest status": the geometry path that
            // replaces vanilla section rendering is not shipped, so this is
            // refused at runtime rather than half-enabled.
            LOGGER.error("advanced.experimental_full_renderer=true is not supported in {} for MC {}: the "
                            + "section-geometry replacement is incomplete (docs/ARCHITECTURE.md, 'Honest status'). "
                            + "Running in shadow mode instead.",
                    VERSION, platform.minecraftVersion());
            config.enabled.set(false);
            STATE.set(State.SHADOW);
        }
        LOGGER.info("Aetherium {} ready on {} for MC {} — state={}", VERSION, platform.platformName(), platform.minecraftVersion(), STATE.get());
        return true;
    }

    /**
     * Second boot phase, run once a GL context is current. Resolves the actual
     * backend from the probe result; failure degrades to vanilla rather than
     * crashing unless the user asked for fail-fast.
     */
    public static void onContextReady(final GlDevice device) {
        final Subsystems local = SUBSYSTEMS.get();
        Objects.requireNonNull(device, "device");
        if (local == null || STATE.get() == State.CLOSED || STATE.get() == State.INCOMPATIBLE) {
            return;
        }
        final BackendCapabilities capabilities = device.getCapabilities();
        final RenderBackend resolved = capabilities.getResolved();
        if (!capabilities.isAnyBackendUsable()) {
            local.config.failFastUnsupportedGpu.addListener(Boolean::booleanValue);
            if (local.config.failFastUnsupportedGpu.get().booleanValue()) {
                throw new IllegalStateException("No usable backend. Probe result: " + capabilities.describe());
            }
            LOGGER.warn("No usable backend ({}); falling back to vanilla rendering", capabilities.describe());
            STATE.set(State.INCOMPATIBLE);
            return;
        }
        LOGGER.info("Backend resolved: {} ({}), extensions={}, GPU={}",
                resolved.getId(), resolved.getDisplayName(), capabilities.getExtensionCount(), capabilities.getGpu().describe());
        if (resolved.isVulkan()) {
            LOGGER.warn("Vulkan backend is selected but the Vulkan device layer is not implemented in this build; "
                    + "rendering continues on the GL path. See docs/ARCHITECTURE.md.");
        }
        device.attach(local.config);
    }

    public static void onWorldEnter() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null || STATE.get() == State.CLOSED) {
            return;
        }
        local.lights.onWorldEnter();
        if (local.config.reloadShadersOnWorldChange.get().booleanValue()) {
            local.iris.onWorldChange();
        }
    }

    public static void onWorldLeave() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null || STATE.get() == State.CLOSED) {
            return;
        }
        local.lights.onWorldLeave();
    }

    /**
     * Rebuilds the render-side state after a backend change. The caller must hold
     * the render-thread safe point (see {@link RenderSafety}); this is how
     * "hot-swap from config without restart" is implemented — config is stored
     * immediately, the swap happens at the next frame boundary.
     */
    public static void hotSwapBackend(final String reason) {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return;
        }
        LOGGER.info("Hot-swapping backend ({})", reason);
        local.lights.clear();
        local.gamma.invalidate();
        local.iris.onRendererChanged();
        STATE.set(local.config.enabled.get().booleanValue() ? State.ACTIVE : State.SHADOW);
        LOGGER.info("Backend swap complete; state={}", STATE.get());
    }

    public static void shutdown() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return;
        }
        if (!STATE.compareAndSet(State.ACTIVE, State.CLOSED) && !STATE.compareAndSet(State.SHADOW, State.CLOSED)) {
            STATE.set(State.CLOSED);
        }
        try {
            local.lights.shutdown();
        } catch (final RuntimeException error) {
            LOGGER.warn("Dynamic light engine did not shut down cleanly", error);
        }
        try {
            local.iris.shutdown();
        } catch (final RuntimeException error) {
            LOGGER.warn("Iris bridge did not shut down cleanly", error);
        }
        // Last partial interval first: a benchmark row for the tail of the run is data,
        // and dropping it would bias the file toward the fast early frames.
        BenchmarkRecorder.stop();
        local.store.close();
        SUBSYSTEMS.set(null);
        LOGGER.info("Aetherium shut down");
    }

    /** Typed accessor so mixins read one volatile, not a chain. */
    public static boolean shouldHookRender() {
        final Subsystems local = SUBSYSTEMS.get();
        return local != null && STATE.get() == State.ACTIVE && local.config.enabled.get().booleanValue();
    }

    private static String versionFromManifest() {
        final Package pack = Aetherium.class.getPackage();
        final String declared = pack == null ? null : pack.getImplementationVersion();
        return declared == null || declared.isEmpty() ? "0.1.0-dev" : declared;
    }

    /** Convenience for log lines and the GUI header. */
    public static String describeRuntime() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return "Aetherium " + VERSION + " (uninitialized)";
        }
        final AetheriumConfig.BackendChoice backend = local.config.backend.get();
        return "Aetherium " + VERSION + " state=" + STATE.get() + " backend=" + backend
                + " iris=" + (local.iris.isPresent() ? local.iris.getVersion() : "absent");
    }
}
