package com.aetherium.fabric;

import com.aetherium.client.ClientHooks;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;

/** Mod Menu integration (1.10.0): the "Configure" button opens Aetherium's settings screen. */
public final class AetheriumModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return new ConfigScreenFactory<Screen>() {
            @Override
            public Screen create(final Screen parent) {
                return ClientHooks.settingsScreen(parent);
            }
        };
    }
}
