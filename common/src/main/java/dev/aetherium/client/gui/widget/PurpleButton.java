package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PurpleButton extends PurpleWidget {
    private final Runnable onPress;

    public PurpleButton(int x, int y, int w, int h, Component label, Runnable onPress) {
        super(x, y, w, h, label);
        this.onPress = onPress;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        int color = PurpleTheme.lerp(textColor(), PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_BRIGHT, panelAlpha), hoverT);
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        onPress.run();
    }
}
