package com.aetherium.mixin.core;

import com.aetherium.perf.FramePacer;
import com.aetherium.perf.RenderToggles;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7.2: the frame limiter waits through {@link com.aetherium.perf.FramePacer} (a fixed cadence)
 * instead of vanilla's drifting loop. The limiter is {@code public static void limitDisplayFPS(int)}
 * in {@code RenderSystem} on 1.16.5-1.21.11 and in {@code net.minecraft.client.FramerateLimiter} on
 * 26.x (probe dumps). Applied where {@code Capabilities.FRAME_PACING} is true.
 */
// @era:pacing-begin rs
@Mixin(targets = "com.mojang.blaze3d.systems.RenderSystem")
// @era:pacing-else limiter
//~ @Mixin(targets = "net.minecraft.client.FramerateLimiter")
// @era:pacing-end
public abstract class FramePacingMixin {
    @Inject(method = "limitDisplayFPS(I)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$pace(final int fps, final CallbackInfo ci) {
        if (RenderToggles.framePacing && FramePacer.pace(fps)) {
            ci.cancel();
        }
    }
}
