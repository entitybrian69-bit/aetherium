package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hide vignette on 1.8.9: Forge 1.8.9 has no VIGNETTE overlay event, so the darkened screen edges
 * are skipped at the vanilla method that draws them (Forge's in-game GUI calls the same method).
 */
@Mixin(GuiIngame.class)
public abstract class VignetteMixin {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$hideVignette(final CallbackInfo ci) {
        if (ClientHooks.hideVignette()) {
            ci.cancel();
        }
    }
}
