package com.aetherium.gui;

/**
 * Colours and metrics of the Aetherium settings screen (light theme from the
 * design mock-up). All colours are ARGB. Pure constants plus two colour
 * helpers, so every version delta shares the same look.
 */
public final class AetheriumTheme {
    public static final int BACKGROUND = 0xFFF3F4F6;
    public static final int BACKGROUND_DOT = 0xFFD9DCE1;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int SURFACE_BORDER = 0xFFE5E7EB;
    public static final int SURFACE_SHADOW = 0x14000000;
    public static final int DIVIDER = 0xFFF0F1F3;
    public static final int INK = 0xFF111827;
    public static final int INK_SOFT = 0xFF374151;
    public static final int MUTED = 0xFF6B7280;
    public static final int FAINT = 0xFF9CA3AF;

    public static final int TAB_IDLE = 0xFFE5E7EB;
    public static final int TAB_HOVER = 0xFFDCDFE4;
    public static final int TAB_LIGHT_EDGE = 0xFFFFFFFF;
    public static final int TAB_DARK_EDGE = 0xFFC4C8CF;

    public static final int CONTROL_DARK = 0xFF1F2937;
    public static final int CONTROL_DARK_HOVER = 0xFF2B3647;
    public static final int CONTROL_TRACK = 0xFFE5E7EB;
    public static final int CONTROL_TRACK_EDGE = 0xFFD1D5DB;
    public static final int TOGGLE_ON = 0xFF22C55E;
    public static final int TOGGLE_OFF = 0xFFD1D5DB;
    public static final int ROW_HOVER = 0xFFF8F9FA;
    public static final int BADGE_GOOD = 0xFF16A34A;
    public static final int BADGE_OK = 0xFFD97706;
    public static final int BADGE_BAD = 0xFFDC2626;
    public static final int ACCENT = 0xFF16A34A;

    private AetheriumTheme() {
    }

    /** Linear blend of two ARGB colours, t in 0..1. */
    public static int mix(final int from, final int to, final float t) {
        final float k = t < 0f ? 0f : (t > 1f ? 1f : t);
        final int a = (int) (((from >>> 24) & 0xFF) + ((((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * k));
        final int r = (int) (((from >>> 16) & 0xFF) + ((((to >>> 16) & 0xFF) - ((from >>> 16) & 0xFF)) * k));
        final int g = (int) (((from >>> 8) & 0xFF) + ((((to >>> 8) & 0xFF) - ((from >>> 8) & 0xFF)) * k));
        final int b = (int) ((from & 0xFF) + (((to & 0xFF) - (from & 0xFF)) * k));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Same colour with its alpha multiplied by {@code factor}. */
    public static int fade(final int argb, final float factor) {
        final float k = factor < 0f ? 0f : (factor > 1f ? 1f : factor);
        final int alpha = (int) (((argb >>> 24) & 0xFF) * k);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
