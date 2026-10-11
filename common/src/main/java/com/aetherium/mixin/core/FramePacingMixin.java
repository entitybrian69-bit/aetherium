package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:pacing-begin rs
import com.aetherium.perf.FramePacer;
import com.aetherium.perf.RenderToggles;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:pacing-else none
// @era:pacing-end

/**
 * 1.7.2: {@code RenderSystem.limitDisplayFPS} waits through {@link com.aetherium.perf.FramePacer}
 * (a fixed cadence) instead of vanilla's drifting loop. Applied where {@code Capabilities.FRAME_PACING}
 * is true (1.16.5-1.21.11, where the method is {@code public static void limitDisplayFPS(int)}).
 */
@Mixin(targets = "com.mojang.blaze3d.systems.RenderSystem")
public abstract class FramePacingMixin {
    // @era:pacing-begin rs
    @Inject(method = "limitDisplayFPS(I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$pace(final int fps, final CallbackInfo ci) {
        if (RenderToggles.framePacing && FramePacer.pace(fps)) {
            ci.cancel();
        }
    }
    // @era:pacing-else none
    // @era:pacing-end
}
