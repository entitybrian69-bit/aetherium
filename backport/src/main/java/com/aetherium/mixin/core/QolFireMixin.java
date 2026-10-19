package com.aetherium.mixin.core;

import com.aetherium.qol.Qol;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fire overlay height (Quality of life) on 1.12.2: while burning, the two flame sheets in front
 * of the camera are lowered so less of the screen is covered (100% = vanilla, 0% = not drawn).
 * The burning itself, its sound and damage are unchanged.
 */
@Mixin(ItemRenderer.class)
public abstract class QolFireMixin {
    /** How far the flames sink at 0% (their sheet is one unit tall, centred on the view). */
    private static final double DROP = 0.75;
    private boolean aetherium$pushed;

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$fireStart(final CallbackInfo ci) {
        final int height = Qol.fireOverlay;
        if (height >= 100) {
            return;
        }
        if (height <= 0) {
            ci.cancel();
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0, -DROP * (1.0 - height / 100.0), 0.0);
        this.aetherium$pushed = true;
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("RETURN"), require = 0)
    private void aetherium$fireEnd(final CallbackInfo ci) {
        if (this.aetherium$pushed) {
            this.aetherium$pushed = false;
            GlStateManager.popMatrix();
        }
    }
}
