package com.aetherium.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Bitmap glyphs and icons drawn with plain rectangle fills, so the screen needs
 * no textures (no atlas, no resource reload, identical on every version and
 * every GL driver). Each bitmap is pre-compressed into horizontal runs at class
 * load, which keeps the per-frame fill count small: the 9-letter title is
 * about 60 fills, an icon about 12.
 */
public final class PixelArt {
    /** One horizontal run: x, y, width (in bitmap pixels). */
    public static final class Bitmap {
        public final int width;
        public final int height;
        final int[] runs;

        Bitmap(final String[] rows) {
            this.height = rows.length;
            int maxWidth = 0;
            final List<int[]> found = new ArrayList<int[]>();
            for (int y = 0; y < rows.length; y++) {
                final String row = rows[y];
                maxWidth = Math.max(maxWidth, row.length());
                int x = 0;
                while (x < row.length()) {
                    if (row.charAt(x) != '#') {
                        x++;
                        continue;
                    }
                    final int start = x;
                    while (x < row.length() && row.charAt(x) == '#') {
                        x++;
                    }
                    found.add(new int[]{start, y, x - start});
                }
            }
            this.width = maxWidth;
            this.runs = new int[found.size() * 3];
            for (int i = 0; i < found.size(); i++) {
                final int[] run = found.get(i);
                this.runs[i * 3] = run[0];
                this.runs[i * 3 + 1] = run[1];
                this.runs[i * 3 + 2] = run[2];
            }
        }

        public int runCount() {
            return this.runs.length / 3;
        }

        /** Draws the bitmap with its top-left corner at (x, y), each pixel {@code scale} units. */
        public void draw(final GuiCanvas canvas, final int x, final int y, final int scale, final int color) {
            for (int i = 0; i < this.runs.length; i += 3) {
                final int rx = x + this.runs[i] * scale;
                final int ry = y + this.runs[i + 1] * scale;
                canvas.fill(rx, ry, rx + this.runs[i + 2] * scale, ry + scale, color);
            }
        }
    }

    private static final String GLYPH_CHARS = "AEHIMRTU";
    private static final Bitmap[] GLYPHS = {
            new Bitmap(new String[]{".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"}),
            new Bitmap(new String[]{"#####", "#....", "#....", "####.", "#....", "#....", "#####"}),
            new Bitmap(new String[]{"#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"}),
            new Bitmap(new String[]{"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "#####"}),
            new Bitmap(new String[]{"#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"}),
            new Bitmap(new String[]{"####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"}),
            new Bitmap(new String[]{"#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."}),
            new Bitmap(new String[]{"#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."}),
    };

    public static final Bitmap GEAR = new Bitmap(new String[]{
            "....####....",
            "..#.####.#..",
            ".##########.",
            "..###..###..",
            "####....####",
            "###......###",
            "###......###",
            "####....####",
            "..###..###..",
            ".##########.",
            "..#.####.#..",
            "....####....",
    });
    public static final Bitmap SPARKLES = new Bitmap(new String[]{
            ".....#......",
            ".....#......",
            "....###.....",
            "..#######...",
            "....###.....",
            ".....#...#..",
            ".....#...#..",
            "........###.",
            "..#......#..",
            ".###.....#..",
            "..#.........",
            "............",
    });
    public static final Bitmap BARS = new Bitmap(new String[]{
            "............",
            ".........##.",
            ".........##.",
            "......##.##.",
            "......##.##.",
            "...##.##.##.",
            "...##.##.##.",
            "##.##.##.##.",
            "##.##.##.##.",
            "##.##.##.##.",
            "############",
            "............",
    });
    public static final Bitmap CUBE = new Bitmap(new String[]{
            ".....##.....",
            "...##..##...",
            ".##......##.",
            "#.##....##.#",
            "#...####...#",
            "#.....#....#",
            "#.....#....#",
            "#.....#....#",
            "#.....#....#",
            ".##...#..##.",
            "...##.#.##..",
            ".....###....",
    });
    public static final Bitmap SUN = new Bitmap(new String[]{
            ".....##.....",
            ".#...##...#.",
            "..#......#..",
            "....####....",
            "...######...",
            "##.######.##",
            "##.######.##",
            "...######...",
            "....####....",
            "..#......#..",
            ".#...##...#.",
            ".....##.....",
    });
    public static final Bitmap ROBOT = new Bitmap(new String[]{
            "..#......#..",
            "...#....#...",
            "..########..",
            ".##########.",
            ".##.####.##.",
            ".##########.",
            "............",
            "#.########.#",
            "#.########.#",
            "#.########.#",
            "..##....##..",
            "..##....##..",
    });
    public static final Bitmap CHEVRON = new Bitmap(new String[]{
            "#...#",
            ".#.#.",
            "..#..",
    });
    /** 7x7 sun for the theme switch in the header. */
    public static final Bitmap SUN_SMALL = new Bitmap(new String[]{
            "...#...",
            ".#...#.",
            "..###..",
            "#.###.#",
            "..###..",
            ".#...#.",
            "...#...",
    });
    /** 7x7 crescent moon for the theme switch in the header. */
    public static final Bitmap MOON_SMALL = new Bitmap(new String[]{
            "...###.",
            "..##...",
            ".##....",
            ".##....",
            ".##....",
            "..##...",
            "...###.",
    });
    public static final Bitmap CHECK = new Bitmap(new String[]{
            "......#",
            ".....#.",
            "#...#..",
            ".#.#...",
            "..#....",
    });

    private PixelArt() {
    }

    public static Bitmap glyph(final char c) {
        final int index = GLYPH_CHARS.indexOf(Character.toUpperCase(c));
        return index < 0 ? null : GLYPHS[index];
    }

    /** Width of a pixel-font string in units, including 1-pixel letter spacing. */
    public static int textWidth(final String text, final int scale) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            final Bitmap glyph = glyph(text.charAt(i));
            width += ((glyph == null ? 3 : glyph.width) + 1) * scale;
        }
        return Math.max(0, width - scale);
    }

    public static void drawText(final GuiCanvas canvas, final String text, final int x, final int y, final int scale,
                                final int color) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            final Bitmap glyph = glyph(text.charAt(i));
            if (glyph != null) {
                glyph.draw(canvas, cursor, y, scale, color);
                cursor += (glyph.width + 1) * scale;
            } else {
                cursor += 4 * scale;
            }
        }
    }
}
