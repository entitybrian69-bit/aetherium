package com.terraformersmc.modmenu.api;

import net.minecraft.client.gui.screens.Screen;

/**
 * Compile-time stand-in for Mod Menu's screen factory (see {@link ModMenuApi}); not shipped.
 *
 * @param <S> the screen type
 */
@FunctionalInterface
public interface ConfigScreenFactory<S extends Screen> {
    /**
     * Creates the settings screen.
     *
     * @param parent the screen to return to
     * @return the settings screen
     */
    S create(Screen parent);
}
