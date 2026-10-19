package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity detail distance: past it, name tags and round shadows are skipped (the entity itself is
 * still drawn). The coordinates here are already relative to the camera.
 */
@Mixin(Render.class)
public abstract class TuningDetailMixin {
    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$nameTag(final Entity entity, final String text, final double x, final double y,
                                   final double z, final int maxDistance, final CallbackInfo ci) {
        if (Tuning.beyondDetail(x * x + y * y + z * z)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$shadow(final Entity entity, final double x, final double y, final double z,
                                  final float alpha, final float partialTicks, final CallbackInfo ci) {
        if (Tuning.beyondDetail(x * x + y * y + z * z)) {
            ci.cancel();
        }
    }
}
