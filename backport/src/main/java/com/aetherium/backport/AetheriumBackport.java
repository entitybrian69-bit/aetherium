package com.aetherium.backport;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = AetheriumBackport.MOD_ID, name = "Aetherium Backport", version = AetheriumBackport.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mixinbooter@[8.0,)")
public final class AetheriumBackport {
    public static final String MOD_ID = "aetherium";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
    }
}
