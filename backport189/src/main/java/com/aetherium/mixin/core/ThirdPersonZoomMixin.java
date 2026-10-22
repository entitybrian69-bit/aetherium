package com.aetherium.mixin.core;

import com.aetherium.client.ZoomKeys;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Third-person zoom on 1.8.9: {@code orientCamera} computes the camera distance (before the wall
 * clip) from {@code thirdPersonDistanceTemp} and {@code thirdPersonDistance}, which 1.8.9 keeps
 * equal (4 blocks). Both reads are zoomed the same way, so the distance becomes exactly the zoomed
 * one, without depending on the method's local variable layout.
 */
@Mixin(EntityRenderer.class)
public abstract class ThirdPersonZoomMixin {

    @Shadow
    private float thirdPersonDistance;

    @Shadow
    private float thirdPersonDistanceTemp;

    @Redirect(method = "orientCamera", require = 0, at = @At(value = "FIELD", opcode = 180,
            target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistance:F"))
    private float aetherium$distance(final EntityRenderer renderer) {
        return this.aetherium$zoomed(this.thirdPersonDistance);
    }

    @Redirect(method = "orientCamera", require = 0, at = @At(value = "FIELD", opcode = 180,
            target = "Lnet/minecraft/client/renderer/EntityRenderer;thirdPersonDistanceTemp:F"))
    private float aetherium$distanceTemp(final EntityRenderer renderer) {
        return this.aetherium$zoomed(this.thirdPersonDistanceTemp);
    }

    private float aetherium$zoomed(final float distance) {
        return ZoomKeys.isZooming() ? (float) ZoomKeys.cameraDistance(distance) : distance;
    }
}
