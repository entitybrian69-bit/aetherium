package com.aetherium.mixin.core;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
// @era:zoom-begin none
// @era:zoom-else v16
//~ import com.aetherium.client.ZoomKeys;
//~ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:zoom-end

/**
 * Zoom: lower mouse sensitivity and the cinematic camera while zoomed, and the scroll wheel
 * adjusts the zoom instead of the hotbar. Expression modifiers rather than redirects, so other
 * mods hooking the same reads still chain.
 */
@Mixin(MouseHandler.class)
public abstract class ZoomMouseMixin {
    // @era:zoom-begin none
    // @era:zoom-else v16
    //~ @ModifyExpressionValue(method = "turnPlayer",
            //~ at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"), require = 0)
    //~ private boolean aetherium$zoomCinematic(final boolean original) {
        //~ return ZoomKeys.smoothCamera(original);
    //~ }

    //~ @ModifyExpressionValue(method = "turnPlayer",
            //~ at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;sensitivity:D"), require = 0)
    //~ private double aetherium$zoomSensitivity(final double original) {
        //~ return ZoomKeys.sensitivity(original);
    //~ }

    //~ @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$zoomScroll(final long window, final double xOffset, final double yOffset, final CallbackInfo ci) {
        //~ if (ZoomKeys.onScroll(yOffset)) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:zoom-end
}
