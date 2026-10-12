package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.perf.RenderToggles;
import com.aetherium.render.CompactTerrain;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    /** One axis-aligned quad of vanilla's 32-byte block vertex, normal along y. */
    private static void quadY(final ByteBuffer data, final float y, final int ny) {
        final float[][] corners = ny > 0
                ? new float[][] {{0, y, 0}, {0, y, 1}, {1, y, 1}, {1, y, 0}}
                : new float[][] {{0, y, 1}, {0, y, 0}, {1, y, 0}, {1, y, 1}};
        for (int v = 0; v < 4; v++) {
            data.putFloat(corners[v][0]).putFloat(corners[v][1]).putFloat(corners[v][2]);
            data.putInt(-1).putFloat(0.5f).putFloat(0.5f).putInt(0);
            data.put((byte) 0).put((byte) ny).put((byte) 0).put((byte) 0);
        }
    }

    @Test
    @DisplayName("renderer 1.21.5+: a finished mesh is reordered in place, same bytes, same size")
    void sortInPlace() {
        final ByteBuffer data = ByteBuffer.allocateDirect(2 * 4 * 32).order(ByteOrder.nativeOrder());
        quadY(data, 0.0f, -127); // down first in the source...
        quadY(data, 1.0f, 127);
        data.flip();
        assertNull(SectionCull.sortVertices(data, true), "renderer off: left alone");
        assertEquals(-127, data.get(28 + 1));
        RenderToggles.aetheriumRenderer = true;
        try {
            final CompactTerrain.Layout layout = SectionCull.sortVertices(data, true);
            assertNotNull(layout);
            assertEquals(2, layout.quads());
            assertEquals(0, data.position(), "caller's buffer position untouched");
            assertEquals(2 * 4 * 32, data.remaining());
            assertEquals(127, data.get(28 + 1), "...up first after sorting");
            assertEquals(1.0f, data.getFloat(4), 0.0f);
            assertEquals(-127, data.get(4 * 32 + 28 + 1));
            assertEquals(0.0f, data.getFloat(4 * 32 + 4), 0.0f);
            final ByteBuffer partial = data.duplicate().order(ByteOrder.nativeOrder());
            partial.limit(partial.limit() - 32);
            assertNull(SectionCull.sortVertices(partial, true), "not whole quads: left alone");
        } finally {
            RenderToggles.aetheriumRenderer = false;
        }
    }

    @Test
    @DisplayName("renderer 1.21.5+: a draw record is narrowed only between begin and end, for sequential indices from 0")
    void records() {
        final CompactTerrain.Layout layout = layout(10);
        assertEquals(SectionCull.FULL, SectionCull.planRecord(layout, true, 0, 420), "renderer off");
        RenderToggles.aetheriumRenderer = true;
        try {
            SectionCull.beginRecords(116.0, 116.0, -84.0); // section at (16, 16, 16): above, east, north
            assertEquals(SectionCull.FULL, SectionCull.planRecord(layout, true, 0, 420), "no origin yet");
            SectionCull.setOrigin(16, 16, 16);
            assertEquals(SectionCull.RANGE, SectionCull.planRecord(layout, true, 0, 420));
            assertEquals(0, SectionCull.rangeFirstIndex());
            assertEquals(50 * 6, SectionCull.rangeIndexCount());
            assertEquals(SectionCull.FULL, SectionCull.planRecord(layout, false, 0, 420), "custom index buffer");
            assertEquals(SectionCull.FULL, SectionCull.planRecord(layout, true, 6, 420), "offset indices");
            assertEquals(SectionCull.FULL, SectionCull.planRecord(null, true, 0, 420), "unsorted mesh");
            SectionCull.endRecords();
            assertEquals(SectionCull.FULL, SectionCull.planRecord(layout, true, 0, 420), "after end");
        } finally {
            RenderToggles.aetheriumRenderer = false;
            SectionCull.frame();
        }
        assertTrue(SectionCull.terrainLayerName("SOLID"));
        assertTrue(SectionCull.terrainLayerName("CUTOUT_MIPPED"));
        assertTrue(SectionCull.terrainLayerName("CUTOUT"));
        assertFalse(SectionCull.terrainLayerName("TRANSLUCENT"));
        assertFalse(SectionCull.terrainLayerName("TRIPWIRE"));
    }
}
