package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.Camera;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Zoom: scales the world field of view. Only the world FOV ({@code useFovSetting = true}); the
 * hand keeps its fixed 70 degrees, as with OptiFine and Zume. Allocation-free, twice per frame.
 * GameRenderer.getFov returns double until 1.21.1 and float until 1.21.11; 26.x computes the world
 * FOV in Camera.calculateFov(F)F (the hand FOV is the separate calculateHudFov).
 */
// @era:zoom-fov-begin double
@Mixin(net.minecraft.client.renderer.GameRenderer.class)
public abstract class ZoomFovMixin {
    @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    private double aetherium$zoomFov(final double original, final Camera camera, final float partialTicks,
                                     final boolean useFovSetting) {
        return useFovSetting ? ZoomKeys.fov(original) : original;
    }
}
// @era:zoom-fov-else float
//~ @Mixin(net.minecraft.client.renderer.GameRenderer.class)
//~ public abstract class ZoomFovMixin {
    //~ @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    //~ private float aetherium$zoomFov(final float original, final Camera camera, final float partialTicks,
                                    //~ final boolean useFovSetting) {
        //~ return useFovSetting ? (float) ZoomKeys.fov(original) : original;
    //~ }
//~ }
// @era:zoom-fov-else camera
//~ @Mixin(Camera.class)
//~ public abstract class ZoomFovMixin {
    //~ @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"), require = 0)
    //~ private float aetherium$zoomFov(final float original) {
        //~ return (float) ZoomKeys.fov(original);
    //~ }
//~ }
// @era:zoom-fov-end
