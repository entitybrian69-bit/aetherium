package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-sky-begin none
// @era:t-sky-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.Tesselator;
//~ import com.mojang.blaze3d.vertex.VertexFormat;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Constant;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.Slice;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-sky-end

/**
 * Sky and clouds in {@code LevelRenderer} (1.12.0, Minecraft 1.16.5).
 *
 * <p>End sky in one draw (End optimizations): vanilla draws the six faces of the End sky box with
 * a begin/end pair each, six immediate-mode uploads and draws per frame. Every vertex is already
 * transformed on the CPU by that face's matrix, so all six faces go into one buffer and one draw:
 * the picture is identical.</p>
 *
 * <p>Cloud distance: fancy clouds are built from 8 x 8 cells, {@code -3..4} around the camera's
 * cell. The two loop bounds (read between the {@code FANCY} check and the first vertex) are
 * narrowed to {@code 1-r..r} cells, r = 1..4 for 25..100 %, and the cloud buffer is rebuilt when
 * the setting changes. Fast clouds are four large quads already and stay as they are.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class TuningSkyMixin {
    // @era:t-sky-begin none
    // @era:t-sky-else v16
    //~ @Shadow
    //~ private boolean generateClouds;

    //~ /** Render thread only: faces of the End sky drawn so far this frame, and whether they are batched. */
    //~ @Unique
    //~ private static int aetherium$endFace;
    //~ @Unique
    //~ private static boolean aetherium$endBatch;
    //~ @Unique
    //~ private static int aetherium$cloudCells = 4;

    //~ /** Once per frame: a frame that was interrupted mid-sky can never leave the face count behind. */
    //~ @Inject(method = "renderEndSky", at = @At("HEAD"), require = 0)
    //~ private void aetherium$endStart(final PoseStack poseStack, final CallbackInfo ci) {
        //~ aetherium$endFace = 0;
    //~ }

    //~ @Redirect(method = "renderEndSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;begin(ILcom/mojang/blaze3d/vertex/VertexFormat;)V"),
            //~ require = 0)
    //~ private void aetherium$endBegin(final BufferBuilder builder, final int mode, final VertexFormat format) {
        //~ if (aetherium$endFace == 0) {
            //~ aetherium$endBatch = Tuning.endOptimizations;
            //~ builder.begin(mode, format);
        //~ } else if (!aetherium$endBatch) {
            //~ builder.begin(mode, format);
        //~ }
    //~ }

    //~ @Redirect(method = "renderEndSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/Tesselator;end()V"), require = 0)
    //~ private void aetherium$endEnd(final Tesselator tesselator) {
        //~ aetherium$endFace++;
        //~ final boolean last = aetherium$endFace >= 6;
        //~ if (!aetherium$endBatch || last) {
            //~ tesselator.end();
        //~ }
        //~ if (last) {
            //~ aetherium$endFace = 0;
        //~ }
    //~ }

    //~ /** A cloud distance change rebuilds the cloud buffer, exactly like a cloud mode change does. */
    //~ @Inject(method = "renderClouds", at = @At("HEAD"), require = 0)
    //~ private void aetherium$cloudDistance(final PoseStack poseStack, final float partialTicks, final double x,
                                         //~ final double y, final double z, final CallbackInfo ci) {
        //~ final int cells = Math.max(1, Math.min(4, (Tuning.cloudDistance + 12) / 25));
        //~ if (cells != aetherium$cloudCells) {
            //~ aetherium$cloudCells = cells;
            //~ generateClouds = true;
        //~ }
    //~ }

    //~ @ModifyConstant(method = "buildClouds",
            //~ slice = @Slice(
                    //~ from = @At(value = "FIELD", target = "Lnet/minecraft/client/CloudStatus;FANCY:Lnet/minecraft/client/CloudStatus;"),
                    //~ to = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0)),
            //~ constant = @Constant(intValue = -3), require = 0)
    //~ private int aetherium$cloudsFrom(final int vanilla) {
        //~ return 1 - aetherium$cloudCells;
    //~ }

    //~ @ModifyConstant(method = "buildClouds",
            //~ slice = @Slice(
                    //~ from = @At(value = "FIELD", target = "Lnet/minecraft/client/CloudStatus;FANCY:Lnet/minecraft/client/CloudStatus;"),
                    //~ to = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;vertex(DDD)Lcom/mojang/blaze3d/vertex/VertexConsumer;", ordinal = 0)),
            //~ constant = @Constant(intValue = 4), require = 0)
    //~ private int aetherium$cloudsTo(final int vanilla) {
        //~ return aetherium$cloudCells;
    //~ }
    // @era:t-sky-end
}
