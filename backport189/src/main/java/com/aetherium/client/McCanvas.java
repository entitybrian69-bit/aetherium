package com.aetherium.client;

import com.aetherium.gui.GuiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.opengl.GL11;

/**
 * {@link GuiCanvas} on top of Minecraft 1.12.2's GUI drawing: {@code Gui.drawRect} for rectangles,
 * the unshadowed {@code FontRenderer.drawString} for text and a GL scissor for clips. One instance,
 * no per-frame allocation.
 */
public final class McCanvas extends GuiCanvas {

    public static final McCanvas INSTANCE = new McCanvas();

    private FontRenderer font;
    private int scale = 1;

    /** Points the canvas at this frame; {@code scaleFactor} is the GUI scale. */
    public McCanvas begin(final int scaleFactor) {
        this.font = Minecraft.getMinecraft().fontRendererObj;
        this.scale = Math.max(1, scaleFactor);
        beginFrame();
        return this;
    }

    @Override
    protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
        Gui.drawRect(x1, y1, x2, y2, argb);
    }

    @Override
    protected void rawText(final String text, final int x, final int y, final int argb) {
        // FontRenderer treats an alpha below 4 as "opaque"; a fully faded label must stay invisible.
        if ((argb >>> 24) < 4) {
            return;
        }
        this.font.drawString(text, x, y, argb, false);
    }

    @Override
    protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
        scissor(x1, y1, x2, y2);
    }

    @Override
    protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
        if (restore) {
            scissor(x1, y1, x2, y2);
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /** GL scissor in framebuffer pixels, bottom-left origin. */
    private void scissor(final int x1, final int y1, final int x2, final int y2) {
        final Minecraft mc = Minecraft.getMinecraft();
        final int px = x1 * this.scale;
        final int py = mc.displayHeight - y2 * this.scale;
        final int pw = Math.max(0, (x2 - x1) * this.scale);
        final int ph = Math.max(0, (y2 - y1) * this.scale);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(px, Math.max(0, py), pw, ph);
    }

    @Override
    protected int rawTextWidth(final String text) {
        return this.font == null ? text.length() * 6 : this.font.getStringWidth(text);
    }

    private McCanvas() {
    }
}
