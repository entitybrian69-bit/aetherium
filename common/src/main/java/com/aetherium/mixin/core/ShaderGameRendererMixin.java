package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.Camera;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyArg;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): the hand pass (its programs, MC_HAND_DEPTH-scaled projection
 * instead of vanilla's depth clear) and the composite / final passes once the world and hand are
 * drawn. Once per frame each.
 */
@Mixin(GameRenderer.class)
public abstract class ShaderGameRendererMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Redirect(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"), require = 0)
    //~ private void aetherium$handDepthClear(final int mask, final boolean checkError) {
        //~ if (!ShaderRuntime.skipHandDepthClear(mask)) {
            //~ RenderSystem.clear(mask, checkError);
        //~ }
    //~ }

    //~ @Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
    //~ private void aetherium$shaderFrameEnd(final float partialTick, final long finishNanos, final PoseStack poseStack,
                                          //~ final CallbackInfo ci) {
        //~ ShaderRuntime.frameEnd();
    //~ }

    //~ @Inject(method = "renderItemInHand", at = @At("HEAD"), require = 0)
    //~ private void aetherium$handStart(final PoseStack poseStack, final Camera camera, final float partialTick,
                                     //~ final CallbackInfo ci) {
        //~ ShaderRuntime.handPhase(true);
    //~ }

    //~ @Inject(method = "renderItemInHand", at = @At("RETURN"), require = 0)
    //~ private void aetherium$handEnd(final PoseStack poseStack, final Camera camera, final float partialTick,
                                   //~ final CallbackInfo ci) {
        //~ ShaderRuntime.handPhase(false);
    //~ }

    //~ @ModifyArg(method = "renderItemInHand", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lcom/mojang/math/Matrix4f;)V"),
            //~ require = 0)
    //~ private Matrix4f aetherium$handProjection(final Matrix4f projection) {
        //~ return ShaderRuntime.handProjection(projection);
    //~ }
    // @era:shader-end
}
