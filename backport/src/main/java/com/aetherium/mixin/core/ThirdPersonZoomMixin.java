package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Third-person zoom: the camera distance (local 10 of {@code orientCamera}) before the wall clip. */
@Mixin(EntityRenderer.class)
public abstract class ThirdPersonZoomMixin {

    @ModifyVariable(method = "orientCamera", at = @At(value = "STORE", ordinal = 0), index = 10, require = 0)
    private double aetherium$distance(final double distance) {
        return ZoomKeys.isZooming() ? ZoomKeys.cameraDistance(distance) : distance;
    }
}
