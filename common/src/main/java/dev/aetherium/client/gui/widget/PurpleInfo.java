package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/** Read-only key/value row (detected launcher, renderer, heap...). */
public class PurpleInfo extends PurpleWidget {
    private final Supplier<String> value;

    public PurpleInfo(int x, int y, int w, int h, Component label, Supplier<String> value) {
        super(x, y, w, h, label);
        this.value = value;
        this.active = false;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), getX() + 6, getY() + (getHeight() - 8) / 2, PurpleTheme.scaleAlpha(PurpleTheme.TEXT_MUTED, panelAlpha), false);
        String v = value.get();
        g.drawString(font, v, getX() + getWidth() - 6 - font.width(v), getY() + (getHeight() - 8) / 2, PurpleTheme.scaleAlpha(PurpleTheme.TEXT, panelAlpha), false);
    }

    @Override public void onClick(double mouseX, double mouseY) {}
}
