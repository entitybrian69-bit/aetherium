package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-beam-begin v16|b2111
import com.aetherium.perf.Tuning;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-beam-else none
// @era:t-beam-end

/**
 * End crystal beams toggle (1.10.0, Minecraft 1.16.5): {@code renderCrystalBeams} draws both the
 * crystal-to-dragon beam and the beams of crystals placed on the exit portal.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EnderDragonRenderer")
public abstract class TuningBeamMixin {
    // @era:t-beam-begin v16
    @Inject(method = "renderCrystalBeams", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$beams(final CallbackInfo ci) {
        if (!Tuning.crystalBeams) {
            ci.cancel();
        }
    }
    // @era:t-beam-else none
    // @era:t-beam-else b2111
    //~ @Inject(method = "submitCrystalBeams", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$beams(final CallbackInfo ci) {
        //~ if (!Tuning.crystalBeams) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:t-beam-end
}
