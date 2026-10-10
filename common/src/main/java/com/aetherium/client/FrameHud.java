package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.hud.FrameStats;

import net.minecraft.client.Minecraft;

/**
 * The corner FPS / frame-time overlay. Drawn from the Gui render hook.
 *
 * <p>Built for zero steady-state cost: the strings are rebuilt four times per
 * second, not per frame, and a frame is three fills and two text draws.</p>
 */
public final class FrameHud {

    private static final long REFRESH_NANOS = 250_000_000L;
    private static final int PAD = 3;
    private static final int GOOD = 0xFF22C55E;
    private static final int OK = 0xFFEAB308;
    private static final int BAD = 0xFFEF4444;

    private static long nextRefresh;
    private static String line1 = "";
    private static String line2 = "";
    private static int dotColor = GOOD;
    private static int boxWidth;

    private FrameHud() {
    }

    public static void render(final GuiCanvas canvas) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub == null || !Aetherium.isActive()) {
            return;
        }
        final AetheriumConfig config = sub.config;
        if (!config.showFrameHud.get().booleanValue()) {
            return;
        }
        final long now = System.nanoTime();
        if (now >= nextRefresh) {
            nextRefresh = now + REFRESH_NANOS;
            refresh(canvas, sub.frameStats);
        }
        final Minecraft mc = Minecraft.getInstance();
        final int sw = mc.getWindow().getGuiScaledWidth();
        final int sh = mc.getWindow().getGuiScaledHeight();
        final int w = boxWidth;
        final int h = PAD * 2 + 9 + 2 + 9;
        final String corner = config.hudCorner.get();
        final boolean right = corner.endsWith("right");
        final boolean bottom = corner.startsWith("bottom");
        final int x = right ? sw - w - 4 : 4;
        final int y = bottom ? sh - h - 4 : 4;

        canvas.fill(x, y, x + w, y + h, 0x90000000);
        canvas.fill(x + PAD, y + PAD + 2, x + PAD + 5, y + PAD + 7, dotColor);
        canvas.text(line1, x + PAD + 8, y + PAD, 0xFFFFFFFF);
        canvas.text(line2, x + PAD, y + PAD + 11, 0xFFD1D5DB);
    }

    private static void refresh(final GuiCanvas canvas, final FrameStats stats) {
        final int fps = (int) Math.round(stats.getFps());
        final int cap = ClientHooks.currentCap();
        line1 = fps + " FPS" + (cap > 0 ? "  (cap " + cap + ")" : "");
        line2 = String.format(java.util.Locale.ROOT, "%.1f ms  p99 %.1f ms", stats.getFrameMs(), stats.getP99Ms());
        dotColor = fps >= 55 ? GOOD : fps >= 30 ? OK : BAD;
        boxWidth = Math.max(canvas.textWidth(line1) + 8, canvas.textWidth(line2)) + PAD * 2;
    }
}
