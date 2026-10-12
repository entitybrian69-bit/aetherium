package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:frustum-cap-begin joml
import com.aetherium.client.FrameFrustum;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:frustum-cap-else mojang
//~ import com.aetherium.client.FrameFrustum;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:frustum-cap-else joml-ret
//~ import com.aetherium.client.FrameFrustum;
//~ import net.minecraft.client.renderer.culling.Frustum;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:frustum-cap-else none
// @era:frustum-cap-end

/** Marks the start of vanilla's per-frame culling-frustum construction (see {@link FrustumCaptureMixin}). */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class FrustumArmMixin {

    // @era:frustum-cap-begin joml
    @Inject(method = "prepareCullFrustum", at = @At("HEAD"), require = 0)
    private void aetherium$armFrustum(final CallbackInfo ci) {
        FrameFrustum.arm();
    }
    // @era:frustum-cap-else mojang
    //~ @Inject(method = "prepareCullFrustum", at = @At("HEAD"), require = 0)
    //~ private void aetherium$armFrustum(final CallbackInfo ci) {
        //~ FrameFrustum.arm();
    //~ }
    // @era:frustum-cap-else joml-ret
    //~ @Inject(method = "prepareCullFrustum", at = @At("HEAD"), require = 0)
    //~ private void aetherium$armFrustum(final CallbackInfoReturnable<Frustum> cir) {
        //~ FrameFrustum.arm();
    //~ }
    // @era:frustum-cap-else none
    // @era:frustum-cap-end
}
