package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens the Aetherium screen wherever vanilla would open Video Settings (the
 * Options button, mods' shortcuts, anything), so there is no button hack to
 * break between versions. Screen ownership moved to {@code Gui} in 26.2.
 */
// @era:screen-owner-begin minecraft
@Mixin(Minecraft.class)
// @era:screen-owner-else gui
//~ @Mixin(net.minecraft.client.gui.Gui.class)
// @era:screen-owner-end
public abstract class ScreenSwapMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$swapVideoSettings(final Screen screen, final CallbackInfo ci) {
        final Minecraft mc = Minecraft.getInstance();
        final Screen replacement = ClientHooks.replaceScreen(mc, screen);
        if (replacement != null) {
            ci.cancel();
            ClientHooks.setScreen(mc, replacement);
        }
    }
}
