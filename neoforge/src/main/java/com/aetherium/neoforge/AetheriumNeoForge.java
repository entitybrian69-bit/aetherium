package com.aetherium.neoforge;

import com.aetherium.Aetherium;
import com.aetherium.platform.PlatformServices;
import com.aetherium.util.AetheriumLog;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge mod entry point.
 *
 * <p>Everything Aetherium does per frame or per tick is driven by its mixins (see
 * {@code com.aetherium.client.ClientHooks}), identically on both loaders, so this class only
 * registers the platform adapter and loads the config. No event-bus listeners: the old
 * {@code RenderLevelStageEvent} hook was removed upstream in 1.21.6 and was the reason six
 * NeoForge rows failed to compile. {@link Aetherium#initialize()} touches no GL state, so it is
 * safe to run from the mod constructor.</p>
 *
 * <p>The mod id literal must equal {@code mod_id} in gradle.properties (expanded into
 * {@code META-INF/neoforge.mods.toml}).</p>
 */
@Mod(AetheriumNeoForge.MOD_ID)
public final class AetheriumNeoForge {
    public static final String MOD_ID = "aetherium";

    private static final AetheriumLog LOGGER = AetheriumLog.of(AetheriumNeoForge.class);

    public AetheriumNeoForge(final ModContainer container) {
        PlatformServices.register(new NeoForgePlatformAdapter(container));
        LOGGER.info("Aetherium {} constructed on NeoForge (mod container {})", Aetherium.VERSION,
                container == null || container.getModInfo() == null ? "unavailable" : container.getModInfo().getModId());
        Aetherium.initialize();
    }
}
