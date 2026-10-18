package com.terraformersmc.modmenu.api;

/**
 * Compile-time stand-in for Mod Menu's API (TerraformersMC/ModMenu, same package and signature on
 * every Minecraft version Aetherium supports). Excluded from the built jar (fabric/build.gradle.kts):
 * at runtime the real interface from Mod Menu is used, and without Mod Menu the "modmenu"
 * entrypoint is simply never loaded.
 */
public interface ModMenuApi {
    /**
     * The screen factory for this mod's settings.
     *
     * @return the factory
     */
    default ConfigScreenFactory<?> getModConfigScreenFactory() {
        return null;
    }
}
