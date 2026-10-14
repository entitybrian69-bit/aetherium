package com.aetherium.backport;

import com.aetherium.Aetherium;
import com.aetherium.client.FreelookHooks;
import com.aetherium.client.ZoomKeys;
import com.aetherium.platform.PlatformServices;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Aetherium Backport: Aetherium for Minecraft 1.12.2 (Forge). Client only.
 */
@Mod(modid = AetheriumBackport.MOD_ID, name = AetheriumBackport.NAME, version = AetheriumBackport.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mixinbooter@[8.0,)")
public final class AetheriumBackport {

    public static final String MOD_ID = "aetherium";
    public static final String NAME = "Aetherium Backport";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        PlatformServices.register(new ForgePlatformAdapter());
        Aetherium.initialize();
        ZoomKeys.register();
        FreelookHooks.register();
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }
}
