package com.aetherium.gui;

/**
 * Colours of the Aetherium settings screen, in a light and a dark palette.
 *
 * <p>The public fields are the colours in effect <em>right now</em>; the view calls
 * {@link #apply(float)} once per frame with the animated light-to-dark position, and the
 * fields are only recomputed while that value actually moves (a theme switch cross-fades
 * over roughly a quarter second, then this is a single float compare per frame). Every
 * colour is ARGB. No Minecraft types, so every version delta shares the same look.</p>
 */
public final class AetheriumTheme {
    // Index of each colour in the palette tables below.
    private static final int I_BACKGROUND = 0;
    private static final int I_BACKGROUND_DOT = 1;
    private static final int I_SURFACE = 2;
    private static final int I_SURFACE_BORDER = 3;
    private static final int I_SURFACE_SHADOW = 4;
    private static final int I_DIVIDER = 5;
    private static final int I_INK = 6;
    private static final int I_INK_SOFT = 7;
    private static final int I_MUTED = 8;
    private static final int I_FAINT = 9;
    private static final int I_TAB_IDLE = 10;
    private static final int I_TAB_HOVER = 11;
    private static final int I_TAB_LIGHT_EDGE = 12;
    private static final int I_TAB_DARK_EDGE = 13;
    private static final int I_CONTROL_DARK = 14;
    private static final int I_CONTROL_DARK_HOVER = 15;
    private static final int I_CONTROL_TRACK = 16;
    private static final int I_CONTROL_TRACK_EDGE = 17;
    private static final int I_TOGGLE_ON = 18;
    private static final int I_TOGGLE_OFF = 19;
    private static final int I_ROW_HOVER = 20;
    private static final int I_SCROLL_TRACK = 21;
    private static final int I_SCROLL_THUMB = 22;
    private static final int I_SCROLL_THUMB_ACTIVE = 23;
    private static final int I_TITLE_SHADOW = 24;
    private static final int COUNT = 25;

    /** The light theme from the design mock-up. */
    static final int[] LIGHT = new int[COUNT];
    /** Dark theme: same contrast relationships, inverted lightness. */
    static final int[] DARK = new int[COUNT];

    static {
        set(I_BACKGROUND, 0xFFF3F4F6, 0xFF0E1014);
        set(I_BACKGROUND_DOT, 0xFFD9DCE1, 0xFF262A33);
        set(I_SURFACE, 0xFFFFFFFF, 0xFF171A21);
        set(I_SURFACE_BORDER, 0xFFE5E7EB, 0xFF2B303B);
        set(I_SURFACE_SHADOW, 0x14000000, 0x50000000);
        set(I_DIVIDER, 0xFFF0F1F3, 0xFF21252D);
        set(I_INK, 0xFF111827, 0xFFF3F4F6);
        set(I_INK_SOFT, 0xFF374151, 0xFFD1D5DB);
        set(I_MUTED, 0xFF6B7280, 0xFF9CA3AF);
        set(I_FAINT, 0xFF9CA3AF, 0xFF6B7280);
        set(I_TAB_IDLE, 0xFFE5E7EB, 0xFF1D2129);
        set(I_TAB_HOVER, 0xFFDCDFE4, 0xFF282D37);
        set(I_TAB_LIGHT_EDGE, 0xFFFFFFFF, 0xFF2F3541);
        set(I_TAB_DARK_EDGE, 0xFFC4C8CF, 0xFF0A0B0E);
        set(I_CONTROL_DARK, 0xFF1F2937, 0xFF3A4252);
        set(I_CONTROL_DARK_HOVER, 0xFF2B3647, 0xFF4A5366);
        set(I_CONTROL_TRACK, 0xFFE5E7EB, 0xFF252A33);
        set(I_CONTROL_TRACK_EDGE, 0xFFD1D5DB, 0xFF343A47);
        set(I_TOGGLE_ON, 0xFF22C55E, 0xFF22C55E);
        set(I_TOGGLE_OFF, 0xFFD1D5DB, 0xFF3A404C);
        set(I_ROW_HOVER, 0xFFF8F9FA, 0xFF1C2028);
        set(I_SCROLL_TRACK, 0xFFEDEEF1, 0xFF20242C);
        set(I_SCROLL_THUMB, 0xFFB4B9C2, 0xFF4B5261);
        set(I_SCROLL_THUMB_ACTIVE, 0xFF6B7280, 0xFF8B93A3);
        set(I_TITLE_SHADOW, 0x22000000, 0x66000000);
    }

    public static int BACKGROUND;
    public static int BACKGROUND_DOT;
    public static int SURFACE;
    public static int SURFACE_BORDER;
    public static int SURFACE_SHADOW;
    public static int DIVIDER;
    public static int INK;
    public static int INK_SOFT;
    public static int MUTED;
    public static int FAINT;

    public static int TAB_IDLE;
    public static int TAB_HOVER;
    public static int TAB_LIGHT_EDGE;
    public static int TAB_DARK_EDGE;

    public static int CONTROL_DARK;
    public static int CONTROL_DARK_HOVER;
    public static int CONTROL_TRACK;
    public static int CONTROL_TRACK_EDGE;
    public static int TOGGLE_ON;
    public static int TOGGLE_OFF;
    public static int ROW_HOVER;
    public static int SCROLL_TRACK;
    public static int SCROLL_THUMB;
    public static int SCROLL_THUMB_ACTIVE;
    public static int TITLE_SHADOW;

    // Theme-independent accents.
    public static final int BADGE_GOOD = 0xFF16A34A;
    public static final int BADGE_OK = 0xFFD97706;
    public static final int BADGE_BAD = 0xFFDC2626;
    public static final int ACCENT = 0xFF16A34A;
    public static final int SUN = 0xFFF59E0B;
    public static final int MOON = 0xFFC7D2FE;

    /** Light-to-dark position the fields were last computed for; NaN forces the first pass. */
    private static float applied = Float.NaN;

    static {
        apply(0f);
    }

    private AetheriumTheme() {
    }

    private static void set(final int index, final int light, final int dark) {
        LIGHT[index] = light;
        DARK[index] = dark;
    }

    /** 0 = light, 1 = dark, anything between is a cross-fade. Cheap when unchanged. */
    public static void apply(final float darkness) {
        final float t = darkness < 0f ? 0f : (darkness > 1f ? 1f : darkness);
        if (t == applied) {
            return;
        }
        applied = t;
        BACKGROUND = pick(I_BACKGROUND, t);
        BACKGROUND_DOT = pick(I_BACKGROUND_DOT, t);
        SURFACE = pick(I_SURFACE, t);
        SURFACE_BORDER = pick(I_SURFACE_BORDER, t);
        SURFACE_SHADOW = pick(I_SURFACE_SHADOW, t);
        DIVIDER = pick(I_DIVIDER, t);
        INK = pick(I_INK, t);
        INK_SOFT = pick(I_INK_SOFT, t);
        MUTED = pick(I_MUTED, t);
        FAINT = pick(I_FAINT, t);
        TAB_IDLE = pick(I_TAB_IDLE, t);
        TAB_HOVER = pick(I_TAB_HOVER, t);
        TAB_LIGHT_EDGE = pick(I_TAB_LIGHT_EDGE, t);
        TAB_DARK_EDGE = pick(I_TAB_DARK_EDGE, t);
        CONTROL_DARK = pick(I_CONTROL_DARK, t);
        CONTROL_DARK_HOVER = pick(I_CONTROL_DARK_HOVER, t);
        CONTROL_TRACK = pick(I_CONTROL_TRACK, t);
        CONTROL_TRACK_EDGE = pick(I_CONTROL_TRACK_EDGE, t);
        TOGGLE_ON = pick(I_TOGGLE_ON, t);
        TOGGLE_OFF = pick(I_TOGGLE_OFF, t);
        ROW_HOVER = pick(I_ROW_HOVER, t);
        SCROLL_TRACK = pick(I_SCROLL_TRACK, t);
        SCROLL_THUMB = pick(I_SCROLL_THUMB, t);
        SCROLL_THUMB_ACTIVE = pick(I_SCROLL_THUMB_ACTIVE, t);
        TITLE_SHADOW = pick(I_TITLE_SHADOW, t);
    }

    /** The light-to-dark position currently applied (0..1). */
    public static float darkness() {
        return Float.isNaN(applied) ? 0f : applied;
    }

    private static int pick(final int index, final float t) {
        if (t <= 0f) {
            return LIGHT[index];
        }
        if (t >= 1f) {
            return DARK[index];
        }
        return mix(LIGHT[index], DARK[index], t);
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
