package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import com.aetherium.client.FrameHud;
import com.aetherium.client.McCanvas;

import net.minecraft.client.gui.Gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** In-game HUD: the frame-time overlay and the vignette toggle. Skipped on 26.2+ where neither hook exists. */
@Mixin(Gui.class)
public abstract class GuiMixin {

    // @era:hud-begin graphics-delta
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void aetherium$hud(final net.minecraft.client.gui.GuiGraphics graphics,
                               final net.minecraft.client.DeltaTracker delta, final CallbackInfo ci) {
        FrameHud.render(McCanvas.INSTANCE.begin(graphics));
    }
    // @era:hud-else graphics-float
    //~ @Inject(method = "render", at = @At("TAIL"), require = 0)
    //~ private void aetherium$hud(final net.minecraft.client.gui.GuiGraphics graphics, final float partialTick,
                               //~ final CallbackInfo ci) {
        //~ FrameHud.render(McCanvas.INSTANCE.begin(graphics));
    //~ }
    // @era:hud-else stack
    //~ @Inject(method = "render", at = @At("TAIL"), require = 0)
    //~ private void aetherium$hud(final com.mojang.blaze3d.vertex.PoseStack pose, final float partialTick,
                               //~ final CallbackInfo ci) {
        //~ FrameHud.render(McCanvas.INSTANCE.begin(pose));
    //~ }
    // @era:hud-else extractor
    //~ @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    //~ private void aetherium$hud(final net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                               //~ final net.minecraft.client.DeltaTracker delta, final CallbackInfo ci) {
        //~ FrameHud.render(McCanvas.INSTANCE.begin(graphics));
    //~ }
    // @era:hud-else none
    // @era:hud-end

    // @era:vignette-begin render
    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true, require = 0)
    // @era:vignette-else extract
    //~ @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true, require = 0)
    // @era:vignette-else none
    //~ @org.spongepowered.asm.mixin.Unique
    // @era:vignette-end
    private void aetherium$vignette(final CallbackInfo ci) {
        if (ClientHooks.hideVignette()) {
            ci.cancel();
        }
    }
}
