package com.aetherium.mixin.core;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
// @era:zoom-begin none
// @era:zoom-else v16
//~ import com.aetherium.client.ZoomKeys;

//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyArg;
// @era:zoom-end

/** Third-person zoom: the distance vanilla asks for (4 blocks) is scaled by the zoom keys. */
@Mixin(Camera.class)
public abstract class ZoomCameraMixin {
    // @era:zoom-begin none
    // @era:zoom-else v16
    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(D)D"),
            //~ require = 0)
    //~ private double aetherium$zoomDistance(final double original) {
        //~ return ZoomKeys.cameraDistance(original);
    //~ }
    // @era:zoom-end
}
