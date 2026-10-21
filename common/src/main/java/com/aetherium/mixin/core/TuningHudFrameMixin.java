package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.client.McCanvas;
//~ import com.aetherium.perf.HudRate;
//~ import com.aetherium.client.QolOverlay;
//~ import com.aetherium.util.AetheriumLog;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import com.mojang.blaze3d.platform.Window;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.Tesselator;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.Gui;
//~ import net.minecraft.client.gui.GuiComponent;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:boost16-end

/**
 * HUD refresh rate (1.17.2, Minecraft 1.16.5): wraps the HUD draw of {@code GameRenderer.render}.
 *
 * <ul>
 *   <li>Direct frames (option off, the game not clearly faster than the HUD rate, profiler charts
 *       open): vanilla.</li>
 *   <li>Refresh frames: the HUD is drawn as usual but into an off-screen picture, without the
 *       vignette and crosshair ({@link TuningHudMixin}) and with alpha-correct blending
 *       ({@link TuningHudBlendMixin}); then the frame is finished like a reuse frame.</li>
 *   <li>Reuse frames: the vignette is drawn live, the picture is laid over the world with one
 *       textured rectangle, then the crosshair and the world-anchored labels are drawn live.</li>
 * </ul>
 *
 * <p>Any failure turns the picture off and the frame falls back to vanilla's draw.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public abstract class TuningHudFrameMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Unique
    //~ private RenderTarget aetherium$hud;

    //~ @Unique
    //~ private boolean aetherium$broken;

    //~ @WrapOperation(method = "render(FJZ)V", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/Gui;render(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"), require = 0)
    //~ private void aetherium$hudRate(final Gui gui, final PoseStack pose, final float partialTick,
                                   //~ final Operation<Void> original) {
        //~ final Minecraft minecraft = Minecraft.getInstance();
        //~ if (HudRate.fps() == HudRate.OFF || this.aetherium$broken || minecraft.player == null) {
            //~ original.call(gui, pose, partialTick);
            //~ return;
        //~ }
        //~ final Window window = minecraft.getWindow();
        //~ final int w = window.getWidth();
        //~ final int h = window.getHeight();
        //~ final boolean live = minecraft.options.renderDebugCharts || minecraft.options.renderFpsChart || w <= 0 || h <= 0;
        //~ final int mode = HudRate.decide(System.nanoTime(), w, h, aetherium$stateKey(minecraft, window), live);
        //~ if (mode == HudRate.DIRECT) {
            //~ original.call(gui, pose, partialTick);
            //~ return;
        //~ }
        //~ if (mode == HudRate.REFRESH && !this.aetherium$capture(minecraft, gui, pose, partialTick, original, w, h)) {
            //~ return; // drawn directly instead
        //~ }
        //~ this.aetherium$present(minecraft, gui, pose, window);
    //~ }

    //~ /** Draws the HUD into the picture; false when it was drawn directly instead. */
    //~ @Unique
    //~ private boolean aetherium$capture(final Minecraft minecraft, final Gui gui, final PoseStack pose, final float partialTick,
                                      //~ final Operation<Void> original, final int w, final int h) {
        //~ RenderTarget target = this.aetherium$hud;
        //~ try {
            //~ if (target == null) {
                //~ target = new RenderTarget(w, h, true, Minecraft.ON_OSX);
                //~ target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                //~ this.aetherium$hud = target;
            //~ } else if (target.width != w || target.height != h) {
                //~ target.resize(w, h, Minecraft.ON_OSX);
            //~ }
            //~ target.clear(Minecraft.ON_OSX);
            //~ target.bindWrite(true);
        //~ } catch (final RuntimeException error) {
            //~ this.aetherium$disable(minecraft, error);
            //~ original.call(gui, pose, partialTick);
            //~ return false;
        //~ }
        //~ HudRate.setCapturing(true);
        //~ try {
            //~ original.call(gui, pose, partialTick);
        //~ } finally {
            //~ HudRate.setCapturing(false);
            //~ RenderSystem.defaultBlendFunc(); // the capture's alpha factors stay inside it
            //~ minecraft.getMainRenderTarget().bindWrite(true);
        //~ }
        //~ return true;
    //~ }

    //~ @Unique
    //~ private void aetherium$present(final Minecraft minecraft, final Gui gui, final PoseStack pose, final Window window) {
        //~ final RenderTarget target = this.aetherium$hud;
        //~ if (target == null) {
            //~ HudRate.refreshFailed();
            //~ return;
        //~ }
        //~ final TuningHudInvoker live = (TuningHudInvoker) (Object) gui;
        //~ RenderSystem.enableBlend();
        //~ if (Minecraft.useFancyGraphics()) {
            //~ live.aetherium$renderVignette(minecraft.getCameraEntity());
        //~ }
        //~ final float guiW = (float) (window.getWidth() / window.getGuiScale());
        //~ final float guiH = (float) (window.getHeight() / window.getGuiScale());
        //~ RenderSystem.disableDepthTest();
        //~ RenderSystem.depthMask(false);
        //~ RenderSystem.enableBlend();
        //~ RenderSystem.blendFuncSeparate(HudRate.GL_ONE, HudRate.GL_ONE_MINUS_SRC_ALPHA, HudRate.GL_ONE,
                //~ HudRate.GL_ONE_MINUS_SRC_ALPHA);
        //~ RenderSystem.disableAlphaTest();
        //~ RenderSystem.enableTexture();
        //~ RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        //~ RenderSystem.bindTexture(target.getColorTextureId());
        //~ final Tesselator tesselator = Tesselator.getInstance();
        //~ final BufferBuilder buffer = tesselator.getBuilder();
        //~ buffer.begin(7, DefaultVertexFormat.POSITION_TEX);
        //~ buffer.vertex(0.0, guiH, 0.0).uv(0.0f, 0.0f).endVertex();
        //~ buffer.vertex(guiW, guiH, 0.0).uv(1.0f, 0.0f).endVertex();
        //~ buffer.vertex(guiW, 0.0, 0.0).uv(1.0f, 1.0f).endVertex();
        //~ buffer.vertex(0.0, 0.0, 0.0).uv(0.0f, 1.0f).endVertex();
        //~ tesselator.end();
        //~ RenderSystem.enableAlphaTest();
        //~ RenderSystem.defaultBlendFunc();
        //~ RenderSystem.depthMask(true);
        //~ RenderSystem.enableDepthTest();
        //~ if (!minecraft.options.hideGui) {
            //~ // As vanilla sets up the crosshair draw (icons texture, blend, alpha test).
            //~ RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            //~ minecraft.getTextureManager().bind(GuiComponent.GUI_ICONS_LOCATION);
            //~ RenderSystem.enableBlend();
            //~ RenderSystem.enableAlphaTest();
            //~ live.aetherium$renderCrosshair(pose);
            //~ RenderSystem.defaultBlendFunc();
        //~ }
        //~ QolOverlay.render(McCanvas.INSTANCE.begin(pose));
    //~ }

    //~ /** Everything that must redraw the picture at once when it changes. */
    //~ @Unique
    //~ private static long aetherium$stateKey(final Minecraft minecraft, final Window window) {
        //~ long key = minecraft.player.inventory.selected & 0xF;
        //~ key |= ((long) window.getGuiScale() & 0xFF) << 4;
        //~ key |= (minecraft.options.renderDebug ? 1L : 0L) << 12;
        //~ key |= (minecraft.options.hideGui ? 1L : 0L) << 13;
        //~ key |= (minecraft.options.keyPlayerList.isDown() ? 1L : 0L) << 14;
        //~ key |= (long) (minecraft.options.getCameraType().ordinal() & 3) << 15;
        //~ key |= (long) System.identityHashCode(minecraft.screen) << 32;
        //~ return key;
    //~ }

    //~ @Unique
    //~ private void aetherium$disable(final Minecraft minecraft, final RuntimeException error) {
        //~ this.aetherium$broken = true;
        //~ HudRate.refreshFailed();
        //~ AetheriumLog.of(HudRate.class).warn("HUD refresh rate: off-screen HUD picture unavailable, drawing the HUD every frame", error);
        //~ try {
            //~ minecraft.getMainRenderTarget().bindWrite(true);
        //~ } catch (final RuntimeException ignored) {
            //~ // nothing more to restore
        //~ }
    //~ }
    // @era:boost16-end
}
