package com.aetherium;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.compat.ModConflictScanner;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigStore;
import com.aetherium.hud.BenchmarkRecorder;
import com.aetherium.hud.FrameStats;
import com.aetherium.perf.RenderToggles;
import com.aetherium.platform.PlatformAdapter;
import com.aetherium.platform.PlatformServices;
import com.aetherium.shader.IrisBridge;
import com.aetherium.util.AetheriumLog;

/**
 * Loader-independent lifecycle. Holds no Minecraft types, so it loads in unit
 * tests and before the game window exists.
 *
 * <p>Design rule for this version: Aetherium never adds GPU work of its own.
 * Every feature either removes work from the vanilla frame (entity culling,
 * particle density, weather/vignette toggles, adaptive render distance, frame
 * caps) or piggybacks on work vanilla already does (dynamic lights ride on
 * the normal chunk mesher). The whole mod therefore needs nothing beyond
 * OpenGL 3.0 itself; the game's own minimum still applies (GL 2.1 for 1.16.5,
 * GL 3.2 core from 1.17).</p>
 */
public final class Aetherium {
    public static final String MOD_ID = "aetherium";
    public static final String MOD_NAME = "Aetherium";
    public static final String VERSION = versionFromManifest();
    /** The only graphics API level the mod itself relies on. */
    public static final String RENDERING_API = "OpenGL 3.0";

    private static final AetheriumLog LOGGER = AetheriumLog.of(Aetherium.class);
    private static final AtomicBoolean INITIALIZING = new AtomicBoolean();

    public enum State {
        /** Not initialised yet, or a non-client environment. */
        OFF,
        /** OpenGL 3.0 backend selected: all hooks live. */
        ACTIVE,
        /** User selected the Vanilla backend: hooks pass through. */
        VANILLA,
        /** A hard conflict (OptiFine, VulkanMod) owns the renderer. */
        INCOMPATIBLE,
        CLOSED
    }

    private static final AtomicReference<State> STATE = new AtomicReference<State>(State.OFF);
    private static final AtomicReference<Subsystems> SUBSYSTEMS = new AtomicReference<Subsystems>();

    private Aetherium() {
    }

    public static State getState() {
        return STATE.get();
    }

    public static boolean isActive() {
        return STATE.get() == State.ACTIVE;
    }

    public static boolean isVanillaPath() {
        return STATE.get() != State.ACTIVE;
    }

    // ------------------------------------------------------------- subsystems

    public static final class Subsystems {
        public final AetheriumConfig config;
        public final ConfigStore store;
        public final FrameStats frameStats;
        public final IrisBridge iris;
        public final ModConflictScanner conflicts;
        public final AndroidEnvironment android;
        public final PlatformAdapter platform;

        Subsystems(final AetheriumConfig config, final ConfigStore store, final FrameStats frameStats,
                   final IrisBridge iris, final ModConflictScanner conflicts, final AndroidEnvironment android,
                   final PlatformAdapter platform) {
            this.config = config;
            this.store = store;
            this.frameStats = frameStats;
            this.iris = iris;
            this.conflicts = conflicts;
            this.android = android;
            this.platform = platform;
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

    public static IrisBridge iris() {
        return require().iris;
    }

    public static ModConflictScanner conflicts() {
        return require().conflicts;
    }

    public static AndroidEnvironment android() {
        return require().android;
    }

    public static PlatformAdapter platform() {
        return require().platform;
    }

    private static Subsystems require() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            throw new IllegalStateException("Aetherium is not initialized yet; guard with Aetherium.subsystemsOrNull()");
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
            return STATE.get() == State.ACTIVE || STATE.get() == State.VANILLA;
        }
        if (!INITIALIZING.compareAndSet(false, true)) {
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
            LOGGER.info("Non-client environment ({}) - nothing to do", platform.platformName());
            return false;
        }

        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final ConfigStore store = new ConfigStore(platform.configDirectory().resolve("aetherium.json"), config);
        store.load();
        LOGGER.setDevEnabled(config.debugLogging.get().booleanValue());

        final ModConflictScanner conflicts = new ModConflictScanner(platform);
        conflicts.scan(config);

        System.setProperty("aetherium.minecraft.version", platform.minecraftVersion());
        com.aetherium.mixin.AetheriumMixinPlugin.announceMinecraftVersion(platform.minecraftVersion());
        if (conflicts.requiresIncompatible() && config.conflictAutoDelegate.get().booleanValue()) {
            com.aetherium.mixin.AetheriumMixinPlugin.setMixinActive(false);
        }

        final AndroidEnvironment android = AndroidEnvironment.probe(config);
        final IrisBridge iris = new IrisBridge(config);
        iris.bind();

        SUBSYSTEMS.set(new Subsystems(config, store, new FrameStats(), iris, conflicts, android, platform));
        BenchmarkRecorder.start();

        config.debugLogging.addListener((Boolean value) -> LOGGER.setDevEnabled(value.booleanValue()));
        config.enabled.addListener((Boolean value) -> refreshState());

        if (android.isAndroid()) {
            LOGGER.info("Android environment: launcher={}, renderer={}, heap budget={} MB",
                    android.getLauncher().getDisplayName(), android.getRenderer().getDisplayName(), android.getMemoryBudgetMb());
            android.applyRuntimeHints(config);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }, "Aetherium shutdown"));

        if (conflicts.requiresIncompatible()) {
            LOGGER.warn("Aetherium is standing down: {}", conflicts.summarize());
            STATE.set(State.INCOMPATIBLE);
            RenderToggles.refresh(config, false);
            return false;
        }
        STATE.set(State.VANILLA);
        refreshState();
        LOGGER.info("Aetherium {} ready on {} for MC {} - backend {} - state={}", VERSION, platform.platformName(),
                platform.minecraftVersion(), RENDERING_API, STATE.get());
        return true;
    }

    /** Re-derives ACTIVE/VANILLA from the master switch and republishes the hook flags. */
    public static void refreshState() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return;
        }
        final State state = STATE.get();
        if (state == State.ACTIVE || state == State.VANILLA) {
            STATE.set(local.config.enabled.get().booleanValue() ? State.ACTIVE : State.VANILLA);
        }
        RenderToggles.refresh(local.config, STATE.get() == State.ACTIVE);
    }

    public static void shutdown() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return;
        }
        STATE.set(State.CLOSED);
        RenderToggles.refresh(local.config, false);
        try {
            local.iris.shutdown();
        } catch (final RuntimeException error) {
            LOGGER.warn("Iris bridge did not shut down cleanly", error);
        }
        BenchmarkRecorder.stop();
        local.store.close();
        SUBSYSTEMS.set(null);
    }

    private static String versionFromManifest() {
        final Package pack = Aetherium.class.getPackage();
        final String declared = pack == null ? null : pack.getImplementationVersion();
        return declared == null || declared.isEmpty() ? "1.0.0" : declared;
    }

    public static String describeRuntime() {
        final Subsystems local = SUBSYSTEMS.get();
        if (local == null) {
            return "Aetherium " + VERSION + " (uninitialized)";
        }
        return "Aetherium " + VERSION + " state=" + STATE.get() + " api=" + RENDERING_API
                + " iris=" + (local.iris.isPresent() ? local.iris.getVersion() : "absent");
    }
}
