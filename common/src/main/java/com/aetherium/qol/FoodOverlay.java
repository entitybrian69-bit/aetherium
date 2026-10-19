package com.aetherium.qol;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Hunger bar overlay of the Quality of life tab (food info), in the style of AppleSkin: the
 * saturation you have is a gold outline traced around the hunger shanks, and while you hold food
 * the shanks and the saturation it would add flash on the bar.
 *
 * <p>Pure logic, no Minecraft classes. The outline is not a texture of its own: it is traced at
 * run time from the silhouette of the empty-shank sprite of whatever resource pack is loaded, so
 * it always fits the shank that is drawn. A shank that is only partly saturated shows part of the
 * outline, growing from the bone outwards in both directions (the four steps AppleSkin uses).</p>
 */
public final class FoodOverlay {
    /** Shank sprites are 9x9 GUI pixels. */
    public static final int SIZE = 9;
    /** Peak opacity of the flashing preview (AppleSkin's default). */
    public static final float FLASH_MAX = 0.65f;
    /** Ticks for one fade in and out of the flashing preview. */
    public static final int FLASH_PERIOD = 32;

    private FoodOverlay() {
    }

    /**
     * The outline of a 9x9 silhouette ({@code solid[y * 9 + x]}): every solid pixel next to an
     * empty one or the sprite edge (4-neighbourhood). Returned as {@code x | y << 4}, ordered by
     * distance along the outline from the bone end (the solid pixel furthest down and right),
     * so a prefix of the array is a partly drawn outline. Empty array for an empty silhouette.
     */
    public static int[] outline(final boolean[] solid) {
        final boolean[] edge = new boolean[SIZE * SIZE];
        int count = 0;
        int start = -1;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!solid(solid, x, y)) {
                    continue;
                }
                if (!solid(solid, x + 1, y) || !solid(solid, x - 1, y) || !solid(solid, x, y + 1) || !solid(solid, x, y - 1)) {
                    edge[y * SIZE + x] = true;
                    count++;
                    if (start < 0 || x + y > sx(start) + sy(start) || (x + y == sx(start) + sy(start) && x > sx(start))) {
                        start = x | y << 4;
                    }
                }
            }
        }
        if (count == 0) {
            return new int[0];
        }
        // Breadth-first along the outline (8-neighbourhood): both directions grow together.
        final int[] dist = new int[SIZE * SIZE];
        java.util.Arrays.fill(dist, -1);
        final ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        dist[sy(start) * SIZE + sx(start)] = 0;
        queue.add(Integer.valueOf(start));
        while (!queue.isEmpty()) {
            final int p = queue.poll().intValue();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    final int x = sx(p) + dx;
                    final int y = sy(p) + dy;
                    if (x < 0 || y < 0 || x >= SIZE || y >= SIZE || !edge[y * SIZE + x] || dist[y * SIZE + x] >= 0) {
                        continue;
                    }
                    dist[y * SIZE + x] = dist[sy(p) * SIZE + sx(p)] + 1;
                    queue.add(Integer.valueOf(x | y << 4));
                }
            }
        }
        // Pixels the walk did not reach (a detached speck) go last.
        final int[] out = new int[count];
        int n = 0;
        for (int d = 0; n < count && d <= SIZE * SIZE; d++) {
            // Same distance: the side further right and up first (up the meat before the bone end).
            for (int key = SIZE * 2; key >= -SIZE * 2; key--) {
                for (int y = 0; y < SIZE; y++) {
                    final int x = key + y;
                    if (x < 0 || x >= SIZE) {
                        continue;
                    }
                    final int i = y * SIZE + x;
                    if (edge[i] && dist[i] == d) {
                        out[n++] = x | y << 4;
                    }
                }
            }
        }
        for (int i = 0; i < SIZE * SIZE && n < count; i++) {
            if (edge[i] && dist[i] < 0) {
                out[n++] = (i % SIZE) | (i / SIZE) << 4;
            }
        }
        return out;
    }

    public static int sx(final int pixel) {
        return pixel & 15;
    }

    public static int sy(final int pixel) {
        return pixel >> 4;
    }

    private static boolean solid(final boolean[] solid, final int x, final int y) {
        return x >= 0 && y >= 0 && x < SIZE && y < SIZE && solid[y * SIZE + x];
    }

    /**
     * How many outline pixels a shank shows: {@code effective} is the saturation that falls on
     * this shank in shanks (1 = full). Four steps like AppleSkin's: a little of the bone end, a
     * third, most, all.
     */
    public static int pixelsFor(final float effective, final int total) {
        if (effective <= 0f || total <= 0) {
            return 0;
        }
        if (effective >= 1f) {
            return total;
        }
        final float share = effective > 0.5f ? 17f / 21f : effective > 0.25f ? 14f / 21f : 9f / 21f;
        return Math.max(1, Math.min(total, Math.round(total * share)));
    }

    /**
     * Gold of one outline pixel: lighter at the top-left of the shank, darker towards the bone,
     * so the outline reads as a rim of light like AppleSkin's.
     */
    public static int gold(final int x, final int y) {
        final int d = x + y; // 0 .. 16
        if (d <= 5) {
            return 0xE9D262;
        }
        if (d <= 9) {
            return 0xDDC241;
        }
        if (d <= 12) {
            return 0xC2A100;
        }
        return 0x9F8609;
    }

    /**
     * Opacity of the flashing preview at {@code ticks + partial}: fades in, holds, fades out and
     * pauses (AppleSkin's rhythm), peaking at {@link #FLASH_MAX}.
     */
    public static float flash(final long ticks, final float partial) {
        final float phase = (float) (((ticks % FLASH_PERIOD) + FLASH_PERIOD) % FLASH_PERIOD) + partial;
        final float half = FLASH_PERIOD / 2f;
        final float u = phase < half ? -0.5f + phase * 0.125f : 1.5f - (phase - half) * 0.125f;
        return Math.max(0f, Math.min(1f, u)) * FLASH_MAX;
    }

    /**
     * Vertical offsets of the ten hunger shanks this frame (vanilla shakes them when saturation is
     * empty). Replays the random numbers vanilla's hunger and health bars draw in the same frame
     * ({@code Gui.renderPlayerHealth}), so the overlay shakes with the shanks it covers.
     *
     * @param health  the player's health rounded up
     * @param hearts  the number of hearts drawn (max health plus absorption, halved, rounded up)
     */
    public static void shake(final int[] out, final int tickCount, final int health, final int hearts,
                             final float saturation, final int food) {
        java.util.Arrays.fill(out, 0);
        if (saturation > 0f || tickCount % (food * 3 + 1) != 0) {
            return;
        }
        final Random random = new Random((long) (tickCount * 312871));
        if (health <= 4) {
            for (int i = 0; i < Math.min(hearts, 1000); i++) {
                random.nextInt(2);
            }
        }
        for (int i = 0; i < out.length; i++) {
            out[i] = random.nextInt(3) - 1;
        }
    }

    /**
     * The saturation the held food leaves you with, the way vanilla's FoodData.eat does it:
     * capped by the new hunger level.
     */
    public static float saturationAfter(final int food, final float saturation, final int nutrition,
                                        final float modifier) {
        final int foodAfter = Math.min(20, food + nutrition);
        return Math.min(foodAfter, saturation + Qol.saturation(nutrition, modifier));
    }

    /** A plain drumstick silhouette, used only if the resource pack's sprite cannot be read. */
    public static boolean[] fallbackSilhouette() {
        final String[] rows = {
            "..##.....",
            ".####....",
            "#####....",
            "######...",
            ".######..",
            "..######.",
            "....#####",
            "......###",
            "......##.",
        };
        final boolean[] solid = new boolean[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                solid[y * SIZE + x] = rows[y].charAt(x) == '#';
            }
        }
        return solid;
    }
}
