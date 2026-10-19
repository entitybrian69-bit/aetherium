package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261
import com.aetherium.client.McCanvas;
import com.aetherium.client.QolHud;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hud-else x262|x263|none
// @era:q-hud-end

/**
 * Custom crosshair and AppleSkin-style hunger bar (Quality of life, every version up to 26.1; on
 * 26.2+ the HUD is its own class, see QolHudMixin). Two hooks in vanilla's HUD: the crosshair
 * sprite is replaced while the custom crosshair is on, and the hunger overlay is drawn right after
 * the health and hunger bars. The drawing is shared ({@link com.aetherium.client.QolHud}); only
 * the hook signatures follow the GUI API of each version.
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class QolGuiMixin {
    // @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261
    @Shadow
    private int tickCount;

    // @era:q-hud-else x262|x263|none
    // @era:q-hud-end
    // @era:q-hud-begin g202|g21
    /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0),
            require = 0)
    private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphics graphics, final net.minecraft.resources.ResourceLocation sprite, final int x, final int y, final int w, final int h) {
        if (!QolHud.crosshair()) {
            graphics.blitSprite(sprite, x, y, w, h);
            return;
        }
        QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    }

    /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphics context, final CallbackInfo ci) {
        QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    }
    // @era:q-hud-else v16|s17
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.Gui gui, final com.mojang.blaze3d.vertex.PoseStack pose, final int x, final int y, final int u, final int v, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ gui.blit(pose, x, y, u, v, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(pose), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final com.mojang.blaze3d.vertex.PoseStack context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else s194
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final com.mojang.blaze3d.vertex.PoseStack pose, final int x, final int y, final int u, final int v, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ net.minecraft.client.gui.GuiComponent.blit(pose, x, y, u, v, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(pose), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final com.mojang.blaze3d.vertex.PoseStack context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else g20
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphics graphics, final net.minecraft.resources.ResourceLocation texture, final int x, final int y, final int u, final int v, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blit(texture, x, y, u, v, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphics context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else g212
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphics graphics, final java.util.function.Function<net.minecraft.resources.ResourceLocation, net.minecraft.client.renderer.RenderType> type,
                                     //~ final net.minecraft.resources.ResourceLocation sprite, final int x, final int y, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blitSprite(type, sprite, x, y, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphics context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else g216
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphics graphics, final com.mojang.blaze3d.pipeline.RenderPipeline pipeline, final net.minecraft.resources.ResourceLocation sprite,
                                     //~ final int x, final int y, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blitSprite(pipeline, sprite, x, y, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphics context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else g2111
    //~ /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final net.minecraft.client.gui.GuiGraphics graphics, final com.mojang.blaze3d.pipeline.RenderPipeline pipeline, final net.minecraft.resources.Identifier sprite,
                                     //~ final int x, final int y, final int w, final int h) {
        //~ if (!QolHud.crosshair()) {
            //~ graphics.blitSprite(pipeline, sprite, x, y, w, h);
            //~ return;
        //~ }
        //~ QolHud.crosshair(McCanvas.INSTANCE.begin(graphics), x, y);
    //~ }

    //~ /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final net.minecraft.client.gui.GuiGraphics context, final CallbackInfo ci) {
        //~ QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    //~ }
    // @era:q-hud-else x261
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
    // @era:q-hud-else x262|x263|none
    // @era:q-hud-end
}
