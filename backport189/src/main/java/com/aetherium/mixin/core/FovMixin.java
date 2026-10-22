package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom: the world FOV (not the hand's) follows the zoom controller. */
@Mixin(EntityRenderer.class)
public abstract class FovMixin {

    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$zoom(final float partialTicks, final boolean useFovSetting,
                                final CallbackInfoReturnable<Float> cir) {
        if (useFovSetting && ZoomKeys.isZooming()) {
            cir.setReturnValue(Float.valueOf((float) ZoomKeys.fov(cir.getReturnValue().floatValue())));
        }
    }
}
