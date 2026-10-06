package com.aetherium.fabric;

import com.aetherium.Aetherium;
import com.aetherium.platform.PlatformServices;
import com.aetherium.util.AetheriumLog;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric client entry point.
 *
 * <p>Registered as {@code "client"} in {@code fabric.mod.json}, so Fabric never calls
 * it on a dedicated server and the mod needs no {@code @Environment} annotation —
 * which matters because {@code net.fabricmc.api.Environment} has moved packages twice
 * in the porting range while {@code ClientModInitializer} has not changed since
 * 0.3.x.</p>
 *
 * <p>Initialization is deferred rather than eager: this method must not build GL
 * resources (there is no GL context during entry-point dispatch on Fabric; the window
 * is created after mods initialise), so it only registers the platform adapter and
 * records the version contract. Everything GPU-side happens at the first frame, from
 * {@code MinecraftMixin}/{@code ClientHooks}.</p>
 */
public final class AetheriumFabric implements ClientModInitializer {
    private static final AetheriumLog LOGGER = AetheriumLog.of(AetheriumFabric.class);

    @Override
    public void onInitializeClient() {
        final FabricLoader loader = FabricLoader.getInstance();
        PlatformServices.register(new FabricPlatformAdapter(loader));
        LOGGER.info("Aetherium {} entry point ran on Fabric Loader {} for Minecraft {}",
                Aetherium.VERSION,
                loader.getModContainer("fabricloader")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown"),
                loader.getModContainer("minecraft")
                        .map(container -> container.getMetadata().getVersion().getFriendlyString())
                        .orElse("unknown"));
        // Deliberately NOT calling Aetherium.initialize() here: config load is safe
        // but the backend probe needs a live context. initialize() is idempotent, so
        // the first render hook completes startup.
        Aetherium.initialize();
    }
}
