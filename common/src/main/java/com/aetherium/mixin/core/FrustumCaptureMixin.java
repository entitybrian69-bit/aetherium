package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:frustum-cap-begin joml|joml-ret
import com.aetherium.client.FrameFrustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:frustum-cap-else mojang
//~ import com.aetherium.client.FrameFrustum;
//~ import com.mojang.math.Matrix4f;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:frustum-cap-else none
// @era:frustum-cap-end

/**
 * Copies the two matrices of the camera's culling frustum into {@link FrameFrustum} as vanilla
 * builds it (only when {@link FrustumArmMixin} armed the capture this frame).
 */
@Mixin(targets = "net.minecraft.client.renderer.culling.Frustum")
public abstract class FrustumCaptureMixin {

    // @era:frustum-cap-begin joml|joml-ret
    @Inject(method = "calculateFrustum", at = @At("HEAD"), require = 0)
    private void aetherium$capture(final Matrix4f view, final Matrix4f projection, final CallbackInfo ci) {
        if (FrameFrustum.take()) {
            view.get(FrameFrustum.view());
            projection.get(FrameFrustum.projection());
            FrameFrustum.publish();
        }
    }
    // @era:frustum-cap-else mojang
    //~ @Inject(method = "calculateFrustum", at = @At("HEAD"), require = 0)
    //~ private void aetherium$capture(final Matrix4f view, final Matrix4f projection, final CallbackInfo ci) {
        //~ if (FrameFrustum.take()) {
            //~ view.store(FrameFrustum.viewBuffer());
            //~ projection.store(FrameFrustum.projectionBuffer());
            //~ FrameFrustum.publish();
        //~ }
    //~ }
    // @era:frustum-cap-else none
    // @era:frustum-cap-end
}
