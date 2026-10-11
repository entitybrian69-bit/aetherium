package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyArg;
// @era:qol16-end

/**
 * Freelook: the camera takes the free yaw and pitch instead of the player's (the first
 * {@code setRotation} in {@code Camera.setup}; the third-person front view still mirrors it).
 * Applied only where {@code Capabilities.FREELOOK} is true.
 */
@Mixin(targets = "net.minecraft.client.Camera")
public abstract class FreelookCameraMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 0), index = 0, require = 0)
    //~ private float aetherium$freelookYaw(final float yaw) {
        //~ return QolHooks.cameraYaw(yaw);
    //~ }

    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 0), index = 1, require = 0)
    //~ private float aetherium$freelookPitch(final float pitch) {
        //~ return QolHooks.cameraPitch(pitch);
    //~ }
    // @era:qol16-end
}
