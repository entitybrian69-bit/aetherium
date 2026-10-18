package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin v16
import com.aetherium.perf.Tuning;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-else none
// @era:qol16-end

/**
 * End crystal beams toggle (1.10.0, Minecraft 1.16.5): {@code renderCrystalBeams} draws both the
 * crystal-to-dragon beam and the beams of crystals placed on the exit portal.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EnderDragonRenderer")
public abstract class TuningBeamMixin {
    // @era:qol16-begin v16
    @Inject(method = "renderCrystalBeams", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$beams(final CallbackInfo ci) {
        if (!Tuning.crystalBeams) {
            ci.cancel();
        }
    }
    // @era:qol16-else none
    // @era:qol16-end
}
