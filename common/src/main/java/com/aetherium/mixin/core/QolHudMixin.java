package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261|none
// @era:q-hud-else x262|x263
//~ import com.aetherium.client.FrameHud;
//~ import com.aetherium.client.McCanvas;
//~ import com.aetherium.client.QolHud;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hud-end

/**
 * Minecraft 26.2+ moved the in-game HUD out of Gui into its own class, Hud. Same hooks as GuiMixin
 * and QolGuiMixin have on older versions: the frame-time box and Quality of life HUD lines and
 * overlays after the HUD is extracted, the custom crosshair in place of vanilla's sprite, and the
 * hunger overlay after the health and hunger bars.
 */
@Mixin(targets = "net.minecraft.client.gui.Hud")
public abstract class QolHudMixin {
    // @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261|none
    // @era:q-hud-else x262|x263
    //~ @Shadow
    //~ private int tickCount;

    //~ @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    //~ private void aetherium$hud(final net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                               //~ final net.minecraft.client.DeltaTracker delta, final CallbackInfo ci) {
        //~ FrameHud.render(McCanvas.INSTANCE.begin(graphics));
    //~ }
    // @era:q-hud-end

    // @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261|none
    // @era:q-hud-else x262
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphicsExtractor graphics, final com.mojang.blaze3d.pipeline.RenderPipeline pipeline, final net.minecraft.resources.Identifier sprite,
                                     //~ final int x, final int y, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blitSprite(pipeline, sprite, x, y, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "extractPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphicsExtractor context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else x263
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "extractCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphicsExtractor graphics, final com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline, final net.minecraft.resources.Identifier sprite,
                                     //~ final int x, final int y, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blitSprite(pipeline, sprite, x, y, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "extractPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphicsExtractor context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-end
}
