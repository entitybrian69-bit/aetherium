package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.client.MouseHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom: lower mouse sensitivity and the cinematic camera while zoomed, and the scroll wheel
 * adjusts the zoom instead of the hotbar. Expression modifiers rather than redirects, so other
 * mods hooking the same reads still chain. The sensitivity is the {@code Options.sensitivity}
 * double field until 1.18.2; from 1.19 it is {@code sensitivity().get()}, the first
 * OptionInstance.get() in turnPlayer on every version (probe dumps). onScroll(JDD)V and the
 * {@code smoothCamera} field read exist on all 33 versions.
 */
@Mixin(MouseHandler.class)
public abstract class ZoomMouseMixin {
    @ModifyExpressionValue(method = "turnPlayer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;smoothCamera:Z"), require = 0)
    private boolean aetherium$zoomCinematic(final boolean original) {
        return ZoomKeys.smoothCamera(original);
    }

    // @era:options-begin instances
    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0), require = 0)
    private Object aetherium$zoomSensitivity(final Object original) {
        if (!(original instanceof Double) || !ZoomKeys.isZooming()) {
            return original;
        }
        return Double.valueOf(ZoomKeys.sensitivity(((Double) original).doubleValue()));
    }
    // @era:options-else fields
    //~ @ModifyExpressionValue(method = "turnPlayer",
            //~ at = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;sensitivity:D"), require = 0)
    //~ private double aetherium$zoomSensitivity(final double original) {
        //~ return ZoomKeys.sensitivity(original);
    //~ }
    // @era:options-end

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$zoomScroll(final long window, final double xOffset, final double yOffset, final CallbackInfo ci) {
        if (ZoomKeys.onScroll(yOffset)) {
            ci.cancel();
        }
    }
}
