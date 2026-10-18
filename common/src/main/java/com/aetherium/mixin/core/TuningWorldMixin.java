package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.ColorFilter;
//~ import com.aetherium.client.WorldCache;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.screens.ReceivingLevelScreen;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

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
    // @era:qol16-begin none
    // @era:qol16-else v16
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
    // @era:qol16-end
}
