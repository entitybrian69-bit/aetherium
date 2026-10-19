package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.entity.RenderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End crystal beams off (the healing beams to the dragon and the respawn beams). */
@Mixin(RenderDragon.class)
public abstract class TuningBeamMixin {
    @Inject(method = "renderCrystalBeams", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$beams(final double x, final double y, final double z, final float partialTicks,
                                        final double x2, final double y2, final double z2, final int ticks,
                                        final double x3, final double y3, final double z3, final CallbackInfo ci) {
        if (!Tuning.crystalBeams) {
            ci.cancel();
        }
    }
}
