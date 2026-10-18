package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-world-begin w21
import com.aetherium.client.ColorFilter;
import com.aetherium.client.WorldCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-world-else none
// @era:t-world-else v16
//~ import com.aetherium.client.ColorFilter;
//~ import com.aetherium.client.WorldCache;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.screens.ReceivingLevelScreen;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-world-else w182
//~ import com.aetherium.client.ColorFilter;
//~ import com.aetherium.client.WorldCache;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-world-else w206
//~ import com.aetherium.client.ColorFilter;
//~ import com.aetherium.client.WorldCache;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-world-end

/**
 * The world pass of {@code GameRenderer.render} (1.11.0, Minecraft 1.16.5): behind a menu the
 * stored world picture may be drawn instead ({@code WorldCache}); after a real world pass the
 * colour filter grades it ({@code ColorFilter}) and, with a menu open, the picture is stored.
 *
 * <p>Faster loading (1.12.0): behind the opaque "Loading terrain" screen of a join or dimension
 * change the world is not drawn at all; vanilla draws it every frame although nothing of it is
 * visible, which slows down the chunk work the screen is waiting for.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public abstract class TuningWorldMixin {
    // @era:t-world-begin w21
    @Redirect(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V"),
            require = 0)
    private void aetherium$world(final GameRenderer renderer, final DeltaTracker delta) {
        final Minecraft mc = Minecraft.getInstance();
        if (WorldCache.reuse(mc)) {
            return;
        }
        renderer.renderLevel(delta);
        ColorFilter.apply(mc);
        WorldCache.capture(mc);
    }
    // @era:t-world-else none
    // @era:t-world-else v16
    //~ @Redirect(method = "render(FJZ)V", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"),
            //~ require = 0)
    //~ private void aetherium$world(final GameRenderer renderer, final float partialTicks, final long finishNanos,
                                 //~ final PoseStack poseStack) {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ if (Tuning.fastLoading && mc.screen instanceof ReceivingLevelScreen) {
            //~ return;
        //~ }
        //~ if (WorldCache.reuse(mc)) {
            //~ return;
        //~ }
        //~ renderer.renderLevel(partialTicks, finishNanos, poseStack);
        //~ ColorFilter.apply(mc);
        //~ WorldCache.capture(mc);
    //~ }
    // @era:t-world-else w182
    //~ @Redirect(method = "render(FJZ)V", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V"),
            //~ require = 0)
    //~ private void aetherium$world(final GameRenderer renderer, final float partialTicks, final long finishNanos,
                                 //~ final PoseStack poseStack) {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ if (WorldCache.reuse(mc)) {
            //~ return;
        //~ }
        //~ renderer.renderLevel(partialTicks, finishNanos, poseStack);
        //~ ColorFilter.apply(mc);
        //~ WorldCache.capture(mc);
    //~ }
    // @era:t-world-else w206
    //~ @Redirect(method = "render(FJZ)V", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJ)V"),
            //~ require = 0)
    //~ private void aetherium$world(final GameRenderer renderer, final float partialTicks, final long finishNanos) {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ if (WorldCache.reuse(mc)) {
            //~ return;
        //~ }
        //~ renderer.renderLevel(partialTicks, finishNanos);
        //~ ColorFilter.apply(mc);
        //~ WorldCache.capture(mc);
    //~ }
    // @era:t-world-end
}
