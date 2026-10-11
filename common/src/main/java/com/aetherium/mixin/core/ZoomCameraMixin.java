package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Third-person zoom: the distance vanilla asks for (4 blocks, times the entity scale on 1.20.5+) is
 * scaled by the zoom keys. Camera.getMaxZoom is (D)D until 1.20.6 and (F)F after; it is called from
 * Camera.setup until 1.21.11 and from Camera.alignWithEntity on 26.x.
 */
@Mixin(Camera.class)
public abstract class ZoomCameraMixin {
    // @era:zoom-cam-begin float
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"),
            require = 0)
    private float aetherium$zoomDistance(final float original) {
        return (float) ZoomKeys.cameraDistance(original);
    }
    // @era:zoom-cam-else double
    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"),
            //~ require = 0)
    //~ private double aetherium$zoomDistance(final double original) {
        //~ return ZoomKeys.cameraDistance(original);
    //~ }
    // @era:zoom-cam-else align
    //~ @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"), require = 0)
    //~ private float aetherium$zoomDistance(final float original) {
        //~ return (float) ZoomKeys.cameraDistance(original);
    //~ }
    // @era:zoom-cam-end
}
