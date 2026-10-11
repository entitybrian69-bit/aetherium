package com.aetherium.mixin.core;

import com.aetherium.qol.Scenery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
// @era:clouds-begin level
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
// @era:clouds-else renderer
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:clouds-end

/**
 * The cloud height setting. Up to 1.21.1 {@code LevelRenderer.renderClouds} reads the
 * dimension's height itself; from 1.21.2 it is the first float argument of
 * {@code CloudRenderer.render}, whatever the caller looked it up from. A dimension without
 * clouds (NaN / no render call) stays without. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
// @era:clouds-begin level
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
// @era:clouds-else renderer
//~ @Mixin(targets = "net.minecraft.client.renderer.CloudRenderer")
// @era:clouds-end
public abstract class CloudHeightMixin {
    // @era:clouds-begin level
    @ModifyExpressionValue(method = "renderClouds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"), require = 0)
    // @era:clouds-else renderer
    //~ @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    // @era:clouds-end
    private float aetherium$cloudHeight(final float original) {
        return Scenery.cloudHeight(original);
    }
}
