package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.FramePacer;
//~ import com.aetherium.perf.RenderToggles;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * 1.7.2 (1.16.5): {@code RenderSystem.limitDisplayFPS} waits through {@link com.aetherium.perf.FramePacer}
 * (a fixed cadence) instead of vanilla's drifting {@code glfwWaitEventsTimeout} loop. Applied only
 * where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "com.mojang.blaze3d.systems.RenderSystem")
public abstract class FramePacingMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "limitDisplayFPS(I)V", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$pace(final int fps, final CallbackInfo ci) {
        //~ if (RenderToggles.framePacing && FramePacer.pace(fps)) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:qol16-end
}
