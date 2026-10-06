package dev.aetherium.client.gui.widget;

import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

/** Drag slider with step snapping, keyboard arrows and swipe support (no modifiers needed on touch). */
public class PurpleSlider extends PurpleWidget {
    private final double min, max, step;
    private final DoubleSupplier getter;
    private final DoubleConsumer setter;
    private final DoubleFunction<String> formatter;
    private boolean dragging;

    public PurpleSlider(int x, int y, int w, int h, Component label, double min, double max, double step,
                        DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<String> formatter) {
        super(x, y, w, h, label);
        this.min = min; this.max = max; this.step = step;
        this.getter = getter; this.setter = setter; this.formatter = formatter;
    }

    private double norm() { return (getter.getAsDouble() - min) / (max - min); }

    private void setFromMouse(double mouseX) {
        int trackX = getX() + 4, trackW = getWidth() - 8;
        double t = Math.max(0, Math.min(1, (mouseX - trackX) / trackW));
        apply(min + t * (max - min));
    }

    private void apply(double raw) {
        double snapped = step > 0 ? Math.round(raw / step) * step : raw;
        snapped = Math.max(min, Math.min(max, snapped));
        if (Math.abs(snapped - getter.getAsDouble()) > 1e-9) setter.accept(snapped);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        float t = (float) norm();
        int fillW = Math.round((w - 8) * t);
        // filled track
        int fill = PurpleTheme.lerp(PurpleTheme.ACCENT_DIM, PurpleTheme.ACCENT, hoverT);
        g.fill(x + 4, y + h - 5, x + 4 + fillW, y + h - 3, PurpleTheme.scaleAlpha(fill, panelAlpha));
        g.fill(x + 4 + fillW, y + h - 5, x + w - 4, y + h - 3, PurpleTheme.scaleAlpha(PurpleTheme.OUTLINE, panelAlpha));
        // knob
        int kx = x + 4 + fillW - 2;
        g.fill(kx, y + 3, kx + 4, y + h - 3, PurpleTheme.scaleAlpha(PurpleTheme.lerp(PurpleTheme.ACCENT, PurpleTheme.ACCENT_BRIGHT, hoverT), panelAlpha));
        // label + value
        var font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), x + 6, y + 3, textColor(), false);
        String val = formatter.apply(getter.getAsDouble());
        g.drawString(font, val, x + w - 6 - font.width(val), y + 3, PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_BRIGHT, panelAlpha), false);
    }

    @Override public void onClick(double mouseX, double mouseY) { dragging = true; setFromMouse(mouseX); }
    @Override protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) { if (dragging) setFromMouse(mouseX); }
    @Override public void onRelease(double mouseX, double mouseY) { dragging = false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263) { apply(getter.getAsDouble() - (step > 0 ? step : (max - min) / 20)); return true; } // left
        if (keyCode == 262) { apply(getter.getAsDouble() + (step > 0 ? step : (max - min) / 20)); return true; } // right
        return false;
    }
}
