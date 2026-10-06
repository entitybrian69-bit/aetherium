package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.anim.Animator;
import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Base for every Aetherium control: animated hover glow, violet outline, touch-friendly hit box. */
public abstract class PurpleWidget extends AbstractWidget {
    protected final Animator hover = new Animator(0f).easing(Animator.Easing.OUT_CUBIC);
    protected float panelAlpha = 1f;

    protected PurpleWidget(int x, int y, int w, int h, Component message) {
        super(x, y, w, h, message);
    }

    public void setPanelAlpha(float a) { this.panelAlpha = a; }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean over = isHoveredOrFocused() && active;
        if (over != (hover.target() > 0.5f)) hover.animateTo(over ? 1f : 0f, over ? 120 : 220);
        float h = hover.value();
        int x = getX(), y = getY(), w = getWidth(), hh = getHeight();

        if (h > 0.01f) {
            int glow = PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_GLOW, h * panelAlpha);
            g.fill(x - 2, y - 2, x + w + 2, y + hh + 2, glow);
        }
        int bg = PurpleTheme.lerp(PurpleTheme.WIDGET, PurpleTheme.WIDGET_HOVER, h);
        if (!active) bg = PurpleTheme.WIDGET;
        g.fill(x, y, x + w, y + hh, PurpleTheme.scaleAlpha(bg, panelAlpha));
        int outline = PurpleTheme.lerp(PurpleTheme.OUTLINE, PurpleTheme.ACCENT, h);
        g.renderOutline(x, y, w, hh, PurpleTheme.scaleAlpha(outline, panelAlpha));
        renderContent(g, mouseX, mouseY, partialTick, h);
    }

    protected abstract void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT);

    protected int textColor() {
        return PurpleTheme.scaleAlpha(active ? PurpleTheme.TEXT : PurpleTheme.TEXT_DISABLED, panelAlpha);
    }

    protected void drawLabelLeft(GuiGraphics g, Component text, int pad) {
        g.drawString(Minecraft.getInstance().font, text, getX() + pad, getY() + (getHeight() - 8) / 2, textColor(), false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }
}
