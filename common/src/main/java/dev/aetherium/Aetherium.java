package dev.aetherium;

import dev.aetherium.android.AndroidLauncherCompat;
import dev.aetherium.compat.ConflictDetector;
import dev.aetherium.compat.IrisCompat;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.AetheriumRenderEngine;
import dev.aetherium.gamma.GammaUtilities;
import dev.aetherium.light.DynamicLightEngine;
import dev.aetherium.platform.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-agnostic entry point. Each loader calls {@link #init()} once on the client. */
public final class Aetherium {
    public static final String MOD_ID = "aetherium";
    public static final String MOD_NAME = "Aetherium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static volatile boolean initialized;

    private Aetherium() {}

    public static void init() {
        if (initialized) return;
        initialized = true;

        LOGGER.info("[Aetherium] Initializing on {} ({})", Services.PLATFORM.getPlatformName(),
                Services.PLATFORM.isDevelopmentEnvironment() ? "dev" : "prod");

        AetheriumConfig.get(); // load from disk (creates defaults on first run)
        ConflictDetector.scanAndReport();
        AndroidLauncherCompat.detect();
        AndroidLauncherCompat.applyEnvironmentOverrides(AetheriumConfig.get());

        GammaUtilities.get().applyConfig(AetheriumConfig.get());
        DynamicLightEngine.get().applyConfig(AetheriumConfig.get());
        IrisCompat.get().detect();

        AetheriumConfig.get().addListener(cfg -> {
            GammaUtilities.get().applyConfig(cfg);
            DynamicLightEngine.get().applyConfig(cfg);
            AetheriumRenderEngine.get().applyConfig(cfg);
        });
    }

    /** Called from the render thread once a GL context exists (first GameRenderer#render). */
    public static void initRenderThread() {
        AetheriumRenderEngine.get().initialize(AetheriumConfig.get());
    }
}
