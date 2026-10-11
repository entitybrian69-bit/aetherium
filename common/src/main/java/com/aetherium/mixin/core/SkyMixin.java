package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.qol.Scenery;
//~ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.math.Matrix4f;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Sky toggles in {@code LevelRenderer.renderSky} (1.16.5): the whole sky, the sun / moon /
 * sunrise glow (the three {@code BufferUploader.end} draws) and the stars (the second
 * {@code VertexBuffer.draw}), plus the cloud height read by {@code renderClouds}. A skipped draw
 * still takes its vertices out of the builder so the next draw starts clean. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class SkyMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$sky(final PoseStack pose, final float partialTick, final CallbackInfo ci) {
        //~ if (!Scenery.sky) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/BufferUploader;end(Lcom/mojang/blaze3d/vertex/BufferBuilder;)V"), require = 0)
    //~ private void aetherium$sunMoon(final BufferBuilder builder, final Operation<Void> original) {
        //~ if (Scenery.sunMoon) {
            //~ original.call(builder);
        //~ } else {
            //~ builder.popNextBuffer();
        //~ }
    //~ }

    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw(Lcom/mojang/math/Matrix4f;I)V", ordinal = 1), require = 0)
    //~ private void aetherium$stars(final VertexBuffer buffer, final Matrix4f matrix, final int mode, final Operation<Void> original) {
        //~ if (Scenery.stars) {
            //~ original.call(buffer, matrix, mode);
        //~ }
    //~ }

    //~ @ModifyExpressionValue(method = "renderClouds", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"), require = 0)
    //~ private float aetherium$cloudHeight(final float original) {
        //~ return Scenery.cloudHeight(original);
    //~ }
    // @era:qol16-end
}
