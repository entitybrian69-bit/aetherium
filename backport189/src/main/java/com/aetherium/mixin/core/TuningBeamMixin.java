package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.entity.RenderDragon;
import net.minecraft.entity.boss.EntityDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End crystal beams off (the healing beam from a crystal to the dragon; 1.8.9 has no other beam). */
@Mixin(RenderDragon.class)
public abstract class TuningBeamMixin {

    @Inject(method = "drawRechargeRay", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$beams(final EntityDragon dragon, final double x, final double y, final double z,
                                 final float partialTicks, final CallbackInfo ci) {
        if (!Tuning.crystalBeams) {
            ci.cancel();
        }
    }
}
