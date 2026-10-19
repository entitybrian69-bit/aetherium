package com.aetherium.qol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The AppleSkin-style hunger bar overlay: outline tracing, partial steps, flash, shake. */
final class FoodOverlayTest {
    private static final int N = FoodOverlay.SIZE;

    private static boolean solid(final boolean[] s, final int x, final int y) {
        return x >= 0 && y >= 0 && x < N && y < N && s[y * N + x];
    }

    @Test
    @DisplayName("the outline is exactly the rim of the silhouette, each pixel once, starting at the bone")
    void outlineIsTheRim() {
        final boolean[] s = FoodOverlay.fallbackSilhouette();
        final int[] outline = FoodOverlay.outline(s);
        final Set<Integer> seen = new HashSet<Integer>();
        for (final int p : outline) {
            final int x = FoodOverlay.sx(p);
            final int y = FoodOverlay.sy(p);
            assertTrue(solid(s, x, y), "outline pixel on the shank");
            assertTrue(!solid(s, x + 1, y) || !solid(s, x - 1, y) || !solid(s, x, y + 1) || !solid(s, x, y - 1),
                    "outline pixel touches the outside: " + x + "," + y);
            assertTrue(seen.add(Integer.valueOf(p)), "no pixel twice");
        }
        int rim = 0;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                if (solid(s, x, y) && (!solid(s, x + 1, y) || !solid(s, x - 1, y) || !solid(s, x, y + 1) || !solid(s, x, y - 1))) {
                    rim++;
                }
            }
        }
        assertEquals(rim, outline.length, "every rim pixel is in the outline");
        // The bone end: furthest down-right.
        int best = -1;
        for (final int p : outline) {
            final int k = FoodOverlay.sx(p) + FoodOverlay.sy(p);
            best = Math.max(best, k);
        }
        assertEquals(best, FoodOverlay.sx(outline[0]) + FoodOverlay.sy(outline[0]));
        // Partial outlines grow from the bone: the smallest step stays in the bone half.
        final int few = FoodOverlay.pixelsFor(0.1f, outline.length);
        for (int i = 0; i < few; i++) {
            assertTrue(FoodOverlay.sx(outline[i]) + FoodOverlay.sy(outline[i]) >= N - 1,
                    "a nearly empty shank shows only the bone end");
        }
    }

    @Test
    @DisplayName("every step of a partial outline is connected to the bone (no stray specks)")
    void prefixesAreConnected() {
        final int[] outline = FoodOverlay.outline(FoodOverlay.fallbackSilhouette());
        for (int count = 1; count <= outline.length; count++) {
            for (int i = 1; i < count; i++) {
                boolean touches = false;
                for (int j = 0; j < i && !touches; j++) {
                    touches = Math.abs(FoodOverlay.sx(outline[i]) - FoodOverlay.sx(outline[j])) <= 1
                            && Math.abs(FoodOverlay.sy(outline[i]) - FoodOverlay.sy(outline[j])) <= 1;
                }
                assertTrue(touches, "pixel " + i + " touches an earlier one");
            }
        }
    }

    @Test
    @DisplayName("an empty sprite gives no outline")
    void emptySprite() {
        assertEquals(0, FoodOverlay.outline(new boolean[N * N]).length);
    }

    @Test
    @DisplayName("four steps per shank like AppleSkin: a bit, a third, most, all")
    void steps() {
        assertEquals(0, FoodOverlay.pixelsFor(0f, 21));
        assertEquals(0, FoodOverlay.pixelsFor(-3f, 21));
        assertEquals(9, FoodOverlay.pixelsFor(0.1f, 21));
        assertEquals(9, FoodOverlay.pixelsFor(0.25f, 21));
        assertEquals(14, FoodOverlay.pixelsFor(0.3f, 21));
        assertEquals(17, FoodOverlay.pixelsFor(0.75f, 21));
        assertEquals(21, FoodOverlay.pixelsFor(1f, 21));
        assertEquals(21, FoodOverlay.pixelsFor(7f, 21));
        assertEquals(0, FoodOverlay.pixelsFor(1f, 0));
    }

    @Test
    @DisplayName("the preview flashes between invisible and 65 % every 32 ticks")
    void flash() {
        float min = 1f;
        float max = 0f;
        for (int t = 0; t < 64; t++) {
            final float a = FoodOverlay.flash(t, 0f);
            assertTrue(a >= 0f && a <= FoodOverlay.FLASH_MAX);
            assertEquals(a, FoodOverlay.flash(t + FoodOverlay.FLASH_PERIOD, 0f), 1e-6f);
            min = Math.min(min, a);
            max = Math.max(max, a);
        }
        assertEquals(0f, min, 1e-6f);
        assertEquals(FoodOverlay.FLASH_MAX, max, 1e-6f);
    }

    @Test
    @DisplayName("the overlay shakes exactly like vanilla's shanks (same random numbers)")
    void shake() {
        final int[] out = new int[10];
        FoodOverlay.shake(out, 31, 20, 10, 2f, 10);
        for (final int v : out) {
            assertEquals(0, v, "no shake while saturated");
        }
        // Saturation empty, food 10: vanilla shakes on ticks divisible by 31.
        FoodOverlay.shake(out, 62, 20, 10, 0f, 10);
        final Random vanilla = new Random(62L * 312871);
        for (int i = 0; i < 10; i++) {
            assertEquals(vanilla.nextInt(3) - 1, out[i]);
        }
        FoodOverlay.shake(out, 63, 20, 10, 0f, 10);
        for (final int v : out) {
            assertEquals(0, v, "only on vanilla's ticks");
        }
        // Low health: the heart bar draws its own random numbers first.
        FoodOverlay.shake(out, 62, 3, 12, 0f, 10);
        final Random low = new Random(62L * 312871);
        for (int i = 0; i < 12; i++) {
            low.nextInt(2);
        }
        for (int i = 0; i < 10; i++) {
            assertEquals(low.nextInt(3) - 1, out[i]);
        }
    }

    @Test
    @DisplayName("saturation after eating follows vanilla: capped by the new hunger level")
    void saturationAfter() {
        // Steak: 8 hunger, modifier 0.8 -> 12.8 saturation.
        assertEquals(14.8f, FoodOverlay.saturationAfter(10, 2f, 8, 0.8f), 1e-4f);
        assertEquals(20f, FoodOverlay.saturationAfter(18, 17f, 8, 0.8f), 1e-4f);
        // Cookie at 3 hunger: 3 + 2 = 5 hunger caps saturation 0.4 + 0.4 -> 0.8.
        assertEquals(0.8f, FoodOverlay.saturationAfter(3, 0.4f, 2, 0.1f), 1e-4f);
    }

    @Test
    @DisplayName("gold is lighter at the top-left of the shank than at the bone")
    void shading() {
        final int light = FoodOverlay.gold(0, 0);
        final int dark = FoodOverlay.gold(8, 8);
        assertTrue((light & 0xFF) + (light >> 8 & 0xFF) > (dark & 0xFF) + (dark >> 8 & 0xFF));
    }
}
