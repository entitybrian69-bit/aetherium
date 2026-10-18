package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-proj-begin none
// @era:t-far-proj-else b26|p263
//~ import com.aetherium.client.FarTerrainFrame;
//~ import org.joml.Matrix4f;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyArg;
// @era:t-far-proj-end

/**
 * Far terrain, 26.x: the projection the level is drawn with (view bobbing and the nausea warp
 * included) only exists as a local in {@code GameRenderer.renderLevel}, on its way into the
 * projection uniform; it is copied here, untouched. Up to 1.21.11 {@code TuningFarDrawMixin}
 * receives it directly.
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public abstract class TuningFarProjMixin {
    // @era:t-far-proj-begin none
    // @era:t-far-proj-else b26
    //~ @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
            //~ require = 0)
    //~ private Matrix4f aetherium$farTerrainProjection(final Matrix4f projection) {
        //~ projection.get(FarTerrainFrame.PROJECTION);
        //~ return projection;
    //~ }
    // @era:t-far-proj-else p263
    //~ @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
            //~ require = 0)
    //~ private Matrix4f aetherium$farTerrainProjection(final Matrix4f projection) {
        //~ projection.get(FarTerrainFrame.PROJECTION);
        //~ return projection;
    //~ }
    // @era:t-far-proj-end
}
