package com.aetherium.mixin.core;

import com.aetherium.client.FreelookHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Freelook: while active, the camera uses the free angles instead of the player's. Only the
 * view-rotation call(s) are changed; the later setRotation calls (the mirrored front view, which
 * reads the camera's own angles, and the sleeping pose) keep vanilla behaviour.
 * Until 1.21.1 the view rotation is the first setRotation(FF)V in Camera.setup; from 1.21.2 the
 * first two (a minecart-interpolation branch and the normal branch), in Camera.alignWithEntity on
 * 26.x (probe dumps).
 */
@Mixin(targets = "net.minecraft.client.Camera")
public abstract class FreelookCameraMixin {
    // @era:freelook-begin v16
    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            ordinal = 0), index = 0, require = 0)
    private float aetherium$freelookYaw(final float yaw) {
        return FreelookHooks.cameraYaw(yaw);
    }

    @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            ordinal = 0), index = 1, require = 0)
    private float aetherium$freelookPitch(final float pitch) {
        return FreelookHooks.cameraPitch(pitch);
    }
    // @era:freelook-else minecart
    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 0), index = 0, require = 0)
    //~ private float aetherium$freelookYaw(final float yaw) {
        //~ return FreelookHooks.cameraYaw(yaw);
    //~ }

    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 0), index = 1, require = 0)
    //~ private float aetherium$freelookPitch(final float pitch) {
        //~ return FreelookHooks.cameraPitch(pitch);
    //~ }

    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 1), index = 0, require = 0)
    //~ private float aetherium$freelookYaw2(final float yaw) {
        //~ return FreelookHooks.cameraYaw(yaw);
    //~ }

    //~ @ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V",
            //~ ordinal = 1), index = 1, require = 0)
    //~ private float aetherium$freelookPitch2(final float pitch) {
        //~ return FreelookHooks.cameraPitch(pitch);
    //~ }
    // @era:freelook-else align
    //~ @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 0, require = 0)
    //~ private float aetherium$freelookYaw(final float yaw) {
        //~ return FreelookHooks.cameraYaw(yaw);
    //~ }

    //~ @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 0), index = 1, require = 0)
    //~ private float aetherium$freelookPitch(final float pitch) {
        //~ return FreelookHooks.cameraPitch(pitch);
    //~ }

    //~ @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 0, require = 0)
    //~ private float aetherium$freelookYaw2(final float yaw) {
        //~ return FreelookHooks.cameraYaw(yaw);
    //~ }

    //~ @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1), index = 1, require = 0)
    //~ private float aetherium$freelookPitch2(final float pitch) {
        //~ return FreelookHooks.cameraPitch(pitch);
    //~ }
    // @era:freelook-end
}
