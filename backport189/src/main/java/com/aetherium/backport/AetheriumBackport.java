package com.aetherium.backport;

import com.aetherium.Aetherium;
import com.aetherium.client.FreelookHooks;
import com.aetherium.client.QolClient;
import com.aetherium.client.ZoomKeys;
import com.aetherium.platform.PlatformServices;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Aetherium Backport: Aetherium for Minecraft 1.8.9 (Forge). Client only. Mixin 0.7.11 is bundled
 * in the jar and started by its tweaker (manifest TweakClass), so nothing else is required.
 */
@Mod(modid = AetheriumBackport.MOD_ID, name = AetheriumBackport.NAME, version = AetheriumBackport.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*")
public final class AetheriumBackport {

    public static final String MOD_ID = "aetherium";
    public static final String NAME = "Aetherium Backport";
    public static final String VERSION = "1.1.0";

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        PlatformServices.register(new ForgePlatformAdapter());
        Aetherium.initialize();
        ZoomKeys.register();
        QolClient.registerKeys();
        FreelookHooks.register();
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
        MinecraftForge.EVENT_BUS.register(new QolEvents());
        SmokeTest.installIfRequested();
    }
}
