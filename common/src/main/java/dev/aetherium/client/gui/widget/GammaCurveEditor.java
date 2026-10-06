package dev.aetherium.client.gui.widget;

import dev.aetherium.gamma.GammaCurve;
import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Five draggable control points with a live preview of the resulting curve. */
public class GammaCurveEditor extends PurpleWidget {
    private final Supplier<double[]> getter;
    private final Consumer<double[]> setter;
    private int dragIndex = -1;

    public GammaCurveEditor(int x, int y, int w, int h, Component label, Supplier<double[]> getter, Consumer<double[]> setter) {
        super(x, y, w, h, label);
        this.getter = getter; this.setter = setter;
    }

    private int plotX() { return getX() + 8; }
    private int plotY() { return getY() + 14; }
    private int plotW() { return getWidth() - 16; }
    private int plotH() { return getHeight() - 22; }

    @Override
    protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTick, float hoverT) {
        var font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), getX() + 6, getY() + 3, textColor(), false);
        int px = plotX(), py = plotY(), pw = plotW(), ph = plotH();
        g.fill(px, py, px + pw, py + ph, PurpleTheme.scaleAlpha(0xFF0D0718, panelAlpha));
        for (int i = 1; i < 4; i++) {
            g.fill(px + pw * i / 4, py, px + pw * i / 4 + 1, py + ph, PurpleTheme.scaleAlpha(PurpleTheme.OUTLINE, panelAlpha));
            g.fill(px, py + ph * i / 4, px + pw, py + ph * i / 4 + 1, PurpleTheme.scaleAlpha(PurpleTheme.OUTLINE, panelAlpha));
        }
        double[] pts = getter.get();
        GammaCurve curve = new GammaCurve(pts);
        int prevX = px, prevY = py + ph - (int) (curve.evaluate(0) * (ph - 1));
        for (int i = 1; i <= pw; i++) {
            int cx = px + i, cy = py + ph - (int) (curve.evaluate(i / (double) pw) * (ph - 1));
            int lo = Math.min(prevY, cy), hi = Math.max(prevY, cy);
            g.fill(cx, lo, cx + 1, hi + 1, PurpleTheme.scaleAlpha(PurpleTheme.ACCENT, panelAlpha));
            prevX = cx; prevY = cy;
        }
        for (int i = 0; i < 5; i++) {
            int cx = px + pw * i / 4, cy = py + ph - (int) (pts[i] * (ph - 1));
            boolean hot = i == dragIndex || (Math.abs(mouseX - cx) <= 4 && Math.abs(mouseY - cy) <= 4);
            g.fill(cx - 3, cy - 3, cx + 3, cy + 3, PurpleTheme.scaleAlpha(hot ? PurpleTheme.ACCENT_BRIGHT : PurpleTheme.TEXT, panelAlpha));
        }
    }

    private int nearest(double mx) {
        int best = 0; double bd = Double.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            double d = Math.abs(mx - (plotX() + plotW() * i / 4.0));
            if (d < bd) { bd = d; best = i; }
        }
        return best;
    }

    private void drag(double my) {
        if (dragIndex < 0) return;
        double[] pts = getter.get().clone();
        pts[dragIndex] = Math.max(0, Math.min(1, (plotY() + plotH() - my) / (double) (plotH() - 1)));
        setter.accept(pts);
    }

    @Override public void onClick(double mouseX, double mouseY) { dragIndex = nearest(mouseX); drag(mouseY); }
    @Override protected void onDrag(double mouseX, double mouseY, double dx, double dy) { drag(mouseY); }
    @Override public void onRelease(double mouseX, double mouseY) { dragIndex = -1; }
}
