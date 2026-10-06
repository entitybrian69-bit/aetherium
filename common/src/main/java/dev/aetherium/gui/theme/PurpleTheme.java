package dev.aetherium.gui.theme;

/** Aetherium palette. Sodium is green, VulkanMod is red, Aetherium is purple — every colour on screen comes from here. */
public final class PurpleTheme {
    private PurpleTheme() {}

    public static final int BG_DEEP        = 0xF00B0614; // near-black violet backdrop
    public static final int PANEL          = 0xE6160B2A;
    public static final int PANEL_LIGHT    = 0xE6241240;
    public static final int SIDEBAR        = 0xF0110820;
    public static final int ACCENT         = 0xFF9B5CFF; // primary violet
    public static final int ACCENT_BRIGHT  = 0xFFC79BFF;
    public static final int ACCENT_DIM     = 0xFF5A2E9E;
    public static final int ACCENT_GLOW    = 0x669B5CFF;
    public static final int TEXT           = 0xFFF2EBFF;
    public static final int TEXT_MUTED     = 0xFFB9A6D9;
    public static final int TEXT_DISABLED  = 0xFF6F5F8C;
    public static final int OUTLINE        = 0xFF3B2160;
    public static final int WIDGET         = 0xFF1F1038;
    public static final int WIDGET_HOVER   = 0xFF2E1852;
    public static final int WIDGET_ACTIVE  = 0xFF3F2070;
    public static final int TOGGLE_ON      = 0xFF9B5CFF;
    public static final int TOGGLE_OFF     = 0xFF3A2B52;
    public static final int WARNING        = 0xFFFFC46B;
    public static final int DANGER         = 0xFFFF6B8A;
    public static final int GOOD           = 0xFFB08CFF;

    /** Linear blend of two ARGB colours. */
    public static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24), ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24), br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int withAlpha(int argb, float alpha) {
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int scaleAlpha(int argb, float factor) {
        int a = (int) (((argb >>> 24) / 255f) * Math.max(0f, Math.min(1f, factor)) * 255f);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** 0..1 "breathing" value with period {@code periodMs}. */
    public static float breathe(long nowMs, float periodMs) {
        return 0.5f + 0.5f * (float) Math.sin((nowMs % (long) periodMs) / periodMs * Math.PI * 2.0);
    }
}
