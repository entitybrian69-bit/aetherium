package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-draw-begin s205
import com.aetherium.client.FarTerrain;
import com.aetherium.client.FarTerrainFrame;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-draw-else m17
//~ import com.aetherium.client.FarTerrain;
//~ import com.aetherium.client.FarTerrainFrame;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-draw-else j193
//~ import com.aetherium.client.FarTerrain;
//~ import com.aetherium.client.FarTerrainFrame;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.joml.Matrix4f;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-draw-else g216
//~ import com.aetherium.client.FarTerrainFrame;
//~ import com.llamalad7.mixinextras.sugar.Local;
//~ import org.joml.Matrix4f;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-draw-else v16|none
// @era:t-far-draw-end

/**
 * Far terrain (1.15.0, Minecraft 1.17+): drawn right before the first opaque terrain layer of the
 * frame, after the sky, so the real chunks cover it wherever they exist and the depth test keeps
 * it behind them. Up to 1.21.5 the layer draw receives the camera position and both matrices; from
 * 1.21.6 the terrain is drawn by {@code ChunkSectionsToRender} ({@code TuningFarGroupMixin}) and
 * this mixin only records the frame's matrices at the head of {@code renderLevel}. 1.16.5 draws in
 * {@code TuningLevelMixin}.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class TuningFarDrawMixin {
    // @era:t-far-draw-begin s205
    @Inject(method = "renderSectionLayer", at = @At("HEAD"), require = 0)
    private void aetherium$farTerrain(final RenderType layer, final double camX, final double camY, final double camZ,
                                      final Matrix4f modelView, final Matrix4f projection, final CallbackInfo ci) {
        if (layer == RenderType.solid()) {
            modelView.get(FarTerrainFrame.MODEL_VIEW);
            projection.get(FarTerrainFrame.PROJECTION);
            FarTerrain.draw(FarTerrainFrame.MODEL_VIEW, FarTerrainFrame.PROJECTION, camX, camY, camZ);
        }
    }
    // @era:t-far-draw-else m17
    //~ @Inject(method = "renderChunkLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$farTerrain(final RenderType layer, final PoseStack poseStack, final double camX,
                                      //~ final double camY, final double camZ, final Matrix4f projection,
                                      //~ final CallbackInfo ci) {
        //~ if (layer == RenderType.solid()) {
            //~ poseStack.last().pose().store(FarTerrainFrame.modelViewBuffer());
            //~ projection.store(FarTerrainFrame.projectionBuffer());
            //~ FarTerrain.draw(FarTerrainFrame.MODEL_VIEW, FarTerrainFrame.PROJECTION, camX, camY, camZ);
        //~ }
    //~ }
    // @era:t-far-draw-else j193
    //~ @Inject(method = "renderChunkLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$farTerrain(final RenderType layer, final PoseStack poseStack, final double camX,
                                      //~ final double camY, final double camZ, final Matrix4f projection,
                                      //~ final CallbackInfo ci) {
        //~ if (layer == RenderType.solid()) {
            //~ poseStack.last().pose().get(FarTerrainFrame.MODEL_VIEW);
            //~ projection.get(FarTerrainFrame.PROJECTION);
            //~ FarTerrain.draw(FarTerrainFrame.MODEL_VIEW, FarTerrainFrame.PROJECTION, camX, camY, camZ);
        //~ }
    //~ }
    // @era:t-far-draw-else g216
    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$farTerrainMatrices(final CallbackInfo ci,
                                              //~ @Local(ordinal = 0, argsOnly = true) final Matrix4f modelView,
                                              //~ @Local(ordinal = 1, argsOnly = true) final Matrix4f projection) {
        //~ modelView.get(FarTerrainFrame.MODEL_VIEW);
        //~ projection.get(FarTerrainFrame.PROJECTION);
        //~ FarTerrainFrame.captured();
    //~ }
    // @era:t-far-draw-else v16|none
    // @era:t-far-draw-end
}
