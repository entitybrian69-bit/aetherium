package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import com.aetherium.shader.ShaderVertex;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.ViewArea;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): frame start (uniforms, target clears, shadow map), the render
 * phases that pick gbuffers programs (sky, clouds, weather, world border), the deferred passes
 * before translucent terrain, and the extended chunk vertex format. A handful of calls per frame.
 */
@Mixin(LevelRenderer.class)
public abstract class ShaderLevelMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Shadow
    //~ private ViewArea viewArea;

    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$shaderFrame(final PoseStack poseStack, final float partialTick, final long finishNanos,
                                       //~ final boolean renderOutline, final Camera camera, final GameRenderer gameRenderer,
                                       //~ final LightTexture lightTexture, final Matrix4f projection, final CallbackInfo ci) {
        //~ ShaderRuntime.frameStart(this.viewArea, poseStack, partialTick, camera, projection);
    //~ }

    //~ @Inject(method = "renderChunkLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$shaderLayer(final RenderType type, final PoseStack poseStack, final double x,
                                       //~ final double y, final double z, final CallbackInfo ci) {
        //~ ShaderRuntime.chunkLayer(type);
    //~ }

    //~ // renderChunkLayer reads its vertex layout from LevelRenderer.format (= BLOCK)
    //~ @Redirect(method = "renderChunkLayer", at = @At(value = "FIELD", opcode = 180 /* GETFIELD */,
            //~ target = "Lnet/minecraft/client/renderer/LevelRenderer;format:Lcom/mojang/blaze3d/vertex/VertexFormat;"),
            //~ require = 0)
    //~ private VertexFormat aetherium$shaderChunkFormat(final LevelRenderer self) {
        //~ return ShaderVertex.chunkFormat();
    //~ }

    //~ @Inject(method = "renderSky", at = @At("HEAD"), require = 0)
    //~ private void aetherium$skyStart(final PoseStack poseStack, final float partialTick, final CallbackInfo ci) {
        //~ ShaderRuntime.setPhase(1);
    //~ }

    //~ @Inject(method = "renderSky", at = @At("RETURN"), require = 0)
    //~ private void aetherium$skyEnd(final PoseStack poseStack, final float partialTick, final CallbackInfo ci) {
        //~ ShaderRuntime.endPhase(1);
    //~ }

    //~ @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$cloudsStart(final PoseStack poseStack, final float partialTick, final double x,
                                       //~ final double y, final double z, final CallbackInfo ci) {
        //~ if (ShaderRuntime.cloudsOff()) {
            //~ ci.cancel();
            //~ return;
        //~ }
        //~ ShaderRuntime.setPhase(2);
    //~ }

    //~ @Inject(method = "renderClouds", at = @At("RETURN"), require = 0)
    //~ private void aetherium$cloudsEnd(final PoseStack poseStack, final float partialTick, final double x,
                                     //~ final double y, final double z, final CallbackInfo ci) {
        //~ ShaderRuntime.endPhase(2);
    //~ }

    //~ @Inject(method = "renderSnowAndRain", at = @At("HEAD"), require = 0)
    //~ private void aetherium$weatherStart(final LightTexture lightTexture, final float partialTick, final double x,
                                        //~ final double y, final double z, final CallbackInfo ci) {
        //~ ShaderRuntime.setPhase(3);
    //~ }

    //~ @Inject(method = "renderSnowAndRain", at = @At("RETURN"), require = 0)
    //~ private void aetherium$weatherEnd(final LightTexture lightTexture, final float partialTick, final double x,
                                      //~ final double y, final double z, final CallbackInfo ci) {
        //~ ShaderRuntime.endPhase(3);
    //~ }

    //~ @Inject(method = "renderWorldBounds", at = @At("HEAD"), require = 0)
    //~ private void aetherium$borderStart(final Camera camera, final CallbackInfo ci) {
        //~ ShaderRuntime.setPhase(5);
    //~ }

    //~ @Inject(method = "renderWorldBounds", at = @At("RETURN"), require = 0)
    //~ private void aetherium$borderEnd(final Camera camera, final CallbackInfo ci) {
        //~ ShaderRuntime.endPhase(5);
    //~ }
    // @era:shader-end
}
