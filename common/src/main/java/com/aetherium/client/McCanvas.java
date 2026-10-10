package com.aetherium.client;

import com.aetherium.gui.GuiCanvas;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
// @era:gui-begin graphics
import net.minecraft.client.gui.GuiGraphics;
// @era:gui-else stack
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.gui.GuiComponent;
// @era:gui-else extractor
//~ import net.minecraft.client.gui.GuiGraphicsExtractor;
// @era:gui-end

/**
 * {@link GuiCanvas} on top of the stock GUI drawing API of this Minecraft version.
 *
 * <p>One instance is reused for every frame (no per-frame allocation). Each primitive is a single
 * vanilla call: {@code fill} for rectangles, the unshadowed string draw for text and the vanilla
 * scissor for clips. Nothing here binds textures, changes GL state or compiles shaders.</p>
 */
public final class McCanvas extends GuiCanvas {
    public static final McCanvas INSTANCE = new McCanvas();

    private Font font;
    // @era:gui-begin graphics
    private GuiGraphics graphics;

    /** Points the canvas at this frame's draw context. */
    public McCanvas begin(final GuiGraphics graphics) {
        this.graphics = graphics;
        this.font = Minecraft.getInstance().font;
        beginFrame();
        return this;
    }

    @Override
    protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
        this.graphics.fill(x1, y1, x2, y2, argb);
    }

    @Override
    protected void rawText(final String text, final int x, final int y, final int argb) {
        this.graphics.drawString(this.font, text, x, y, argb, false);
    }

    @Override
    protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
        this.graphics.enableScissor(x1, y1, x2, y2);
    }

    @Override
    protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
        this.graphics.disableScissor();
    }

    @Override
    protected void rawLayer() {
        // @era:gui-layer-begin none
        // Immediate-mode GUI: draw order is already layer order.
        // @era:gui-layer-else stratum
        //~ this.graphics.nextStratum();
        // @era:gui-layer-end
    }
    // @era:gui-else stack
    //~ private PoseStack pose;
    //~
    //~ /** Points the canvas at this frame's pose stack. */
    //~ public McCanvas begin(final PoseStack pose) {
    //~     this.pose = pose;
    //~     this.font = Minecraft.getInstance().font;
    //~     beginFrame();
    //~     return this;
    //~ }
    //~
    //~ @Override
    //~ protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
    //~     GuiComponent.fill(this.pose, x1, y1, x2, y2, argb);
    //~ }
    //~
    //~ @Override
    //~ protected void rawText(final String text, final int x, final int y, final int argb) {
    //~     this.font.draw(this.pose, text, (float) x, (float) y, argb);
    //~ }
    //~
    //~ @Override
    //~ protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
    //~     scissor(x1, y1, x2, y2);
    //~ }
    //~
    //~ @Override
    //~ protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
    //~     if (restore) {
    //~         scissor(x1, y1, x2, y2);
    //~     } else {
    //~         RenderSystem.disableScissor();
    //~     }
    //~ }
    //~
    //~ /** RenderSystem's scissor takes framebuffer pixels with a bottom-left origin. */
    //~ private static void scissor(final int x1, final int y1, final int x2, final int y2) {
    //~     final com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
    //~     final double scale = window.getGuiScale();
    //~     final int px = (int) Math.floor(x1 * scale);
    //~     final int py = (int) Math.floor(window.getHeight() - y2 * scale);
    //~     final int pw = Math.max(0, (int) Math.ceil((x2 - x1) * scale));
    //~     final int ph = Math.max(0, (int) Math.ceil((y2 - y1) * scale));
    //~     RenderSystem.enableScissor(px, Math.max(0, py), pw, ph);
    //~ }
    // @era:gui-else extractor
    //~ private GuiGraphicsExtractor graphics;
    //~
    //~ /** Points the canvas at this frame's draw context. */
    //~ public McCanvas begin(final GuiGraphicsExtractor graphics) {
    //~     this.graphics = graphics;
    //~     this.font = Minecraft.getInstance().font;
    //~     beginFrame();
    //~     return this;
    //~ }
    //~
    //~ @Override
    //~ protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
    //~     this.graphics.fill(x1, y1, x2, y2, argb);
    //~ }
    //~
    //~ @Override
    //~ protected void rawText(final String text, final int x, final int y, final int argb) {
    //~     this.graphics.text(this.font, text, x, y, argb, false);
    //~ }
    //~
    //~ @Override
    //~ protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
    //~     this.graphics.enableScissor(x1, y1, x2, y2);
    //~ }
    //~
    //~ @Override
    //~ protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
    //~     this.graphics.disableScissor();
    //~ }
    //~
    //~ @Override
    //~ protected void rawLayer() {
    //~     this.graphics.nextStratum();
    //~ }
    // @era:gui-end

    @Override
    protected int rawTextWidth(final String text) {
        return this.font == null ? text.length() * 6 : this.font.width(text);
    }

    private McCanvas() {
    }
}
