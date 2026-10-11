package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The visible-section grid behind entity occlusion culling: it may only ever hide what vanilla hid. */
final class SectionVisibilityTest {

    @Test
    @DisplayName("occlusion: before any frame, or with an empty list, everything is visible")
    void unknownMeansVisible() {
        final SectionVisibility grid = new SectionVisibility();
        assertTrue(grid.isBoxVisible(0, 0, 0, 1, 2, 1), "no frame collected yet");
        grid.begin(0, 4, 0);
        assertTrue(grid.isBoxVisible(0, 0, 0, 1, 2, 1), "vanilla produced no list (world still loading)");
        grid.mark(0, 4, 0);
        grid.invalidate();
        assertTrue(grid.isBoxVisible(500, 64, 500, 501, 66, 501), "invalidated frame");
    }

    @Test
    @DisplayName("occlusion: an entity in a hidden section is culled, one in a visible section is not")
    void hiddenSectionCulls() {
        final SectionVisibility grid = new SectionVisibility();
        grid.begin(0, 4, 0);
        for (int y = 0; y <= 8; y++) {
            grid.mark(0, y, 0);
            grid.mark(1, y, 0);
        }
        // zombie in a cave two sections east (x 32..47), not in vanilla's list
        assertFalse(grid.isBoxVisible(36.2, 20.0, 4.2, 36.8, 21.95, 4.8));
        // same zombie in the visible column
        assertTrue(grid.isBoxVisible(20.2, 20.0, 4.2, 20.8, 21.95, 4.8));
        assertEquals(18, grid.markedCount());
    }

    @Test
    @DisplayName("occlusion: a box straddling a hidden and a visible section stays visible")
    void straddlingBoxVisible() {
        final SectionVisibility grid = new SectionVisibility();
        grid.begin(0, 4, 0);
        grid.mark(0, 4, 0);
        grid.mark(0, 5, 0);
        // spans x 15.5..16.5: sections 0 (visible) and 1 (hidden)
        assertTrue(grid.isBoxVisible(15.5, 70, 3, 16.5, 72, 4));
        // negative coordinates floor correctly: x -0.5 is section -1, not 0
        assertFalse(grid.isBoxVisible(-0.9, 70, 3, -0.1, 72, 4));
        assertTrue(grid.isBoxVisible(-0.9, 70, 3, 0.1, 72, 4));
    }

    @Test
    @DisplayName("occlusion: rows above/below every visible section, off-grid boxes and huge boxes are never culled")
    void conservativeEdges() {
        final SectionVisibility grid = new SectionVisibility();
        grid.begin(0, 4, 0);
        grid.mark(0, 2, 0);
        grid.mark(0, 6, 0);
        assertTrue(grid.isBoxVisible(40, 7 * 16 + 1, 40, 41, 7 * 16 + 3, 41), "above the highest visible row");
        assertTrue(grid.isBoxVisible(40, 1 * 16 + 1, 40, 41, 1 * 16 + 3, 41), "below the lowest visible row");
        assertFalse(grid.isBoxVisible(40, 4 * 16 + 1, 40, 41, 4 * 16 + 3, 41), "inside the visible rows, hidden");
        final double far = (SectionVisibility.RADIUS_XZ + 2) * 16.0;
        assertTrue(grid.isBoxVisible(far, 65, 0, far + 1, 66, 1), "outside the grid");
        assertTrue(grid.isBoxVisible(40, 65, 40, 40 + 16 * SectionVisibility.MAX_SPAN, 66, 41), "huge box");
        assertTrue(grid.isBoxVisible(Double.NaN, 65, 40, 41, 66, 41), "NaN box");
        assertTrue(grid.isBoxVisible(-1.0E300, 65, 40, 1.0E300, 66, 41), "absurd box");
    }

    @Test
    @DisplayName("occlusion: the grid recentres on the camera and clears between frames")
    void recentresAndClears() {
        final SectionVisibility grid = new SectionVisibility();
        grid.begin(1000, 4, -1000);
        grid.mark(1003, 4, -1003);
        grid.mark(1003, 5, -1003);
        assertTrue(grid.isSectionVisible(1003, 4, -1003));
        assertFalse(grid.isSectionVisible(1002, 4, -1003));
        grid.begin(1000, 4, -1000);
        grid.mark(1002, 4, -1003);
        grid.mark(1002, 5, -1003);
        assertFalse(grid.isSectionVisible(1003, 4, -1003), "previous frame's bit must be gone");
        assertTrue(grid.isSectionVisible(1002, 4, -1003));
        // corners of the grid are addressable without overflowing into neighbours
        final int r = SectionVisibility.RADIUS_XZ;
        grid.begin(0, 0, 0);
        grid.mark(r, 0, r);
        grid.mark(-r, 1, -r);
        assertTrue(grid.isSectionVisible(r, 0, r));
        assertFalse(grid.isSectionVisible(r - 1, 0, r));
        assertFalse(grid.isSectionVisible(-r, 0, -r + 1));
        assertTrue(grid.isSectionVisible(-r, 1, -r));
    }
}
