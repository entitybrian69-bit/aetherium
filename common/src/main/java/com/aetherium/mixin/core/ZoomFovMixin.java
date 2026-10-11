package com.aetherium.mixin.core;

import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
// @era:zoom-begin none
// @era:zoom-else v16
//~ import com.aetherium.client.ZoomKeys;
//~ import com.llamalad7.mixinextras.injector.ModifyReturnValue;

//~ import net.minecraft.client.Camera;

//~ import org.spongepowered.asm.mixin.injection.At;
// @era:zoom-end

/**
 * Zoom: scales the world field of view. Only the world FOV ({@code useFovSetting = true}); the
 * hand keeps its fixed 70 degrees, as with OptiFine and Zume. Allocation-free, twice per frame.
 */
@Mixin(GameRenderer.class)
public abstract class ZoomFovMixin {
    // @era:zoom-begin none
    // @era:zoom-else v16
    //~ @ModifyReturnValue(method = "getFov", at = @At("RETURN"), require = 0)
    //~ private double aetherium$zoomFov(final double original, final Camera camera, final float partialTicks,
                                     //~ final boolean useFovSetting) {
        //~ return useFovSetting ? ZoomKeys.fov(original) : original;
    //~ }
    // @era:zoom-end
}
