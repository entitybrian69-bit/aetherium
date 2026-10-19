package com.aetherium.qol;

import java.util.Arrays;

/**
 * Custom crosshair shapes (Quality of life), as rectangles around the screen centre.
 *
 * <p>The shape is rebuilt only when a setting changes ({@link #shape}); drawing it is one fill per
 * rectangle, plus the outline pass. Coordinates are relative to the centre pixel, in GUI pixels:
 * {@code x1, y1} inclusive, {@code x2, y2} exclusive, like {@code GuiComponent.fill}.</p>
 */
public final class Crosshair {
    public enum Style { VANILLA, CROSS, CROSS_DOT, DOT, CIRCLE, CIRCLE_DOT, T_SHAPE }

    public enum Color { WHITE, RED, GREEN, CYAN, YELLOW, MAGENTA, BLACK }

    private static final int[] RGB = {0xFFFFFF, 0xFF4040, 0x40FF60, 0x40E0FF, 0xFFE040, 0xFF50FF, 0x000000};
    /** Red used while aiming at a mob or player (when that option is on). */
    public static final int TARGET_RGB = 0xFF3030;

    private Crosshair() {
    }

    public static int rgb(final Color color) {
        return RGB[color.ordinal()];
    }

    /** ARGB with the opacity (10..100 percent) applied. */
    public static int argb(final int rgb, final int opacityPercent) {
        final int alpha = Math.max(0, Math.min(255, Math.round(opacityPercent * 2.55f)));
        return alpha << 24 | rgb & 0xFFFFFF;
    }

    /**
     * Rectangles for a style: 4 ints each (x1, y1, x2, y2). {@code size} is the arm length or
     * radius (1..15), {@code thickness} the line width (1..4), {@code gap} the empty space between
     * the centre and the arms (0..8).
     */
    public static int[] shape(final Style style, final int size, final int thickness, final int gap) {
        final int s = clamp(size, 1, 15);
        final int t = clamp(thickness, 1, 4);
        final int g = clamp(gap, 0, 8);
        // Lines of even thickness lean one pixel up/left; odd ones are centred on the pixel.
        final int lo = -(t / 2);
        final int hi = lo + t;
        final Rects out = new Rects();
        switch (style) {
            case CROSS:
                arms(out, s, g, lo, hi, true);
                break;
            case CROSS_DOT:
                arms(out, s, g, lo, hi, true);
                out.add(lo, lo, hi, hi);
                break;
            case DOT:
                // A square dot: the line width, growing by 2 for every 5 steps of size.
                final int side = Math.max(t, 1 + 2 * (s / 5));
                final int d1 = -(side / 2);
                out.add(d1, d1, d1 + side, d1 + side);
                break;
            case CIRCLE:
                ring(out, Math.max(2, s), t);
                break;
            case CIRCLE_DOT:
                ring(out, Math.max(2, s), t);
                out.add(lo, lo, hi, hi);
                break;
            case T_SHAPE:
                arms(out, s, g, lo, hi, false);
                break;
            case VANILLA:
            default:
                break;
        }
        return out.toArray();
    }

    private static void arms(final Rects out, final int s, final int g, final int lo, final int hi, final boolean top) {
        out.add(lo - g - s, lo, lo - g, hi);   // left
        out.add(hi + g, lo, hi + g + s, hi);   // right
        out.add(lo, hi + g, hi, hi + g + s);   // down
        if (top) {
            out.add(lo, lo - g - s, hi, lo - g); // up
        }
    }

    /** A ring of the given radius and width, as one horizontal run per row (and its mirror). */
    private static void ring(final Rects out, final int radius, final int width) {
        final double outer = radius + 0.5;
        final double inner = Math.max(0, radius + 0.5 - width);
        for (int y = -radius; y <= radius; y++) {
            final double yy = y * y;
            final int xo = (int) Math.floor(Math.sqrt(Math.max(0, outer * outer - yy)));
            final double innerSq = inner * inner - yy;
            if (innerSq <= 0) {
                out.add(-xo, y, xo + 1, y + 1);
                continue;
            }
            final int xi = (int) Math.ceil(Math.sqrt(innerSq));
            if (xi > xo) {
                continue;
            }
            out.add(-xo, y, -xi + 1, y + 1);
            out.add(xi, y, xo + 1, y + 1);
        }
    }

    /**
     * The outline: every rectangle grown by one pixel. Drawn first in black, under the shape, so
     * the crosshair stays visible on bright and dark backgrounds.
     */
    public static int[] outline(final int[] rects) {
        final int[] out = new int[rects.length];
        for (int i = 0; i < rects.length; i += 4) {
            out[i] = rects[i] - 1;
            out[i + 1] = rects[i + 1] - 1;
            out[i + 2] = rects[i + 2] + 1;
            out[i + 3] = rects[i + 3] + 1;
        }
        return out;
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final class Rects {
        private int[] data = new int[64];
        private int size;

        void add(final int x1, final int y1, final int x2, final int y2) {
            if (x2 <= x1 || y2 <= y1) {
                return;
            }
            if (this.size + 4 > this.data.length) {
                this.data = Arrays.copyOf(this.data, this.data.length * 2);
            }
            this.data[this.size++] = x1;
            this.data[this.size++] = y1;
            this.data[this.size++] = x2;
            this.data[this.size++] = y2;
        }

        int[] toArray() {
            return Arrays.copyOf(this.data, this.size);
        }
    }
}
