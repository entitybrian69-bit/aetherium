package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.render.CompactTerrain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SectionCullTest {
    private static CompactTerrain.Layout layout(final int perGroup) {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        for (int g = 0; g <= CompactTerrain.GROUPS; g++) {
            layout.start[g] = g * perGroup;
        }
        layout.plane[CompactTerrain.UP] = 16.0f;
        layout.plane[CompactTerrain.DOWN] = 0.0f;
        layout.plane[CompactTerrain.EAST] = 16.0f;
        layout.plane[CompactTerrain.WEST] = 0.0f;
        layout.plane[CompactTerrain.SOUTH] = 16.0f;
        layout.plane[CompactTerrain.NORTH] = 0.0f;
        return layout;
    }

    @Test
    @DisplayName("renderer 1.17+: one index range per section, leaving out groups facing away")
    void range() {
        final CompactTerrain.Layout layout = layout(10); // 70 quads, 420 indices
        // Camera above, east and north of the section: up, north, any, east (groups 0, 1, 3, 4).
        assertEquals(SectionCull.RANGE, SectionCull.planRelative(layout, 100.0f, 100.0f, -100.0f, 420));
        assertEquals(0, SectionCull.rangeFirstIndex());
        assertEquals(50 * 6, SectionCull.rangeIndexCount());
        // Camera below, west and south: west, any, south, down (groups 2, 3, 5, 6).
        assertEquals(SectionCull.RANGE, SectionCull.planRelative(layout, -100.0f, -100.0f, 100.0f, 420));
        assertEquals(20 * 6, SectionCull.rangeFirstIndex());
        assertEquals(50 * 6, SectionCull.rangeIndexCount());
    }

    @Test
    @DisplayName("renderer 1.17+: a layout for another mesh, or none, draws the full section")
    void mismatch() {
        assertEquals(SectionCull.FULL, SectionCull.planRelative(null, 0.0f, 0.0f, 0.0f, 420));
        assertEquals(SectionCull.FULL, SectionCull.planRelative(layout(10), 100.0f, 100.0f, -100.0f, 426));
        assertEquals(SectionCull.FULL, SectionCull.planRelative(layout(0), 100.0f, 100.0f, -100.0f, 0));
    }

    @Test
    @DisplayName("renderer 1.17+: a section with only hidden faces is skipped")
    void skip() {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        // 10 down-facing quads only (group 6); camera far above.
        for (int g = 0; g <= CompactTerrain.GROUPS; g++) {
            layout.start[g] = g <= CompactTerrain.DOWN ? 0 : 10;
        }
        layout.plane[CompactTerrain.DOWN] = 0.0f;
        assertEquals(SectionCull.SKIP, SectionCull.planRelative(layout, 8.0f, 100.0f, 8.0f, 60));
    }

    @Test
    @DisplayName("renderer 1.17+: index sizes and status")
    void misc() {
        assertEquals(1, SectionCull.indexBytes(0x1401));
        assertEquals(2, SectionCull.indexBytes(0x1403));
        assertEquals(4, SectionCull.indexBytes(0x1405));
        assertEquals(0, SectionCull.indexBytes(0));
        assertFalse(SectionCull.hasFailed());
        assertTrue(SectionCull.statusText().startsWith("Vanilla"));
    }
}
