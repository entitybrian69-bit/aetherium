package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.17.2 performance features: occlusion culling bookkeeping and batched chests. */
final class Tuning117Test {

    @Test
    @DisplayName("occlusion culling: sections near the camera are never culled")
    void nearSections() {
        assertTrue(OcclusionCulling.near(-8f, -8f, -8f), "camera inside the section");
        assertTrue(OcclusionCulling.near(20f, 0f, 0f), "20 blocks in front");
        assertTrue(OcclusionCulling.near(-40f, 0f, 0f), "box spans -40..-24: edge is 24 away");
        assertFalse(OcclusionCulling.near(25f, 0f, 0f));
        assertFalse(OcclusionCulling.near(-42f, 0f, 0f));
        assertFalse(OcclusionCulling.near(20f, 20f, 0f), "diagonal: about 28 blocks");
    }

    @Test
    @DisplayName("occlusion culling: two hidden results hide a section, one visible result shows it")
    void hideAndShow() {
        final OcclusionCulling.Entry entry = new OcclusionCulling.Entry(0);
        OcclusionCulling.record(entry, 0, 10);
        assertFalse(entry.hidden, "one zero result is not enough");
        OcclusionCulling.record(entry, 0, 11);
        assertTrue(entry.hidden);
        assertTrue(OcclusionCulling.shouldSkip(entry.hidden, entry.resultFrame, 12, 100f, 0f, 0f));
        assertFalse(OcclusionCulling.shouldSkip(entry.hidden, entry.resultFrame, 12, 10f, 0f, 0f), "near: drawn");
        assertFalse(OcclusionCulling.shouldSkip(entry.hidden, entry.resultFrame, 11 + OcclusionCulling.STALE_FRAMES + 1,
                100f, 0f, 0f), "stale result: drawn");
        OcclusionCulling.record(entry, 5, 13);
        assertFalse(entry.hidden);
        assertEquals(0, entry.zeros);
    }

    @Test
    @DisplayName("occlusion culling: hidden sections are queried every frame, visible ones every fourth")
    void queryRate() {
        int visible = 0;
        for (long frame = 0; frame < 16; frame++) {
            assertTrue(OcclusionCulling.decideQuery(false, true, 1, frame));
            assertFalse(OcclusionCulling.decideQuery(true, true, 1, frame), "never while a query is in flight");
            if (OcclusionCulling.decideQuery(false, false, 1, frame)) {
                visible++;
            }
        }
        assertEquals(16 / OcclusionCulling.VISIBLE_PERIOD, visible);
    }

    @Test
    @DisplayName("batched chests: block-entity shading (top bright, bottom 0.4)")
    void chestShade() {
        assertEquals(1.0, ChestBaking.shade(0f, 1f, 0f), 1e-6);
        assertEquals(0.4, ChestBaking.shade(0f, -1f, 0f), 1e-6);
        final float north = ChestBaking.shade(0f, 0f, -1f);
        final float south = ChestBaking.shade(0f, 0f, 1f);
        assertEquals(north, south, 1e-6);
        assertTrue(north > 0.4f && north < 1f, "side faces are in between: " + north);
        final float east = ChestBaking.shade(1f, 0f, 0f);
        assertTrue(east > 0.4f && east < north, "east/west faces are darker than north/south: " + east);
    }

    @Test
    @DisplayName("batched chests: closed only when the lid is still and nobody has it open")
    void chestClosed() {
        assertTrue(ChestBaking.closed(0f, 0f, 0));
        assertFalse(ChestBaking.closed(0f, 0f, 1), "just opened");
        assertFalse(ChestBaking.closed(0f, 0.1f, 0), "last closing frame");
        assertFalse(ChestBaking.closed(0.5f, 0.6f, 0), "closing");
    }

    @Test
    @DisplayName("batched chests: breaking progress is tracked per breaker")
    void chestBreaking() {
        ChestBaking.resetForTests();
        assertEquals(Long.MIN_VALUE, ChestBaking.destroyProgress(7, 100L, 0));
        assertTrue(ChestBaking.destroying(100L));
        assertEquals(Long.MIN_VALUE, ChestBaking.destroyProgress(7, 100L, 5), "same block, more cracks");
        assertEquals(100L, ChestBaking.destroyProgress(7, 200L, 1), "moved to another block: the first is released");
        assertFalse(ChestBaking.destroying(100L));
        assertTrue(ChestBaking.destroying(200L));
        assertEquals(200L, ChestBaking.destroyProgress(7, 200L, -1), "stopped");
        assertFalse(ChestBaking.destroying(200L));
        assertEquals(Long.MIN_VALUE, ChestBaking.destroyProgress(7, 200L, 10), "nothing to release");
    }

    @Test
    @DisplayName("batched chests: toggling reports a change once; a failure turns it off for good")
    void chestConfigure() {
        ChestBaking.resetForTests();
        assertFalse(ChestBaking.configure(false));
        assertTrue(ChestBaking.configure(true));
        assertTrue(ChestBaking.enabled());
        assertFalse(ChestBaking.configure(true));
        ChestBaking.fail();
        assertFalse(ChestBaking.enabled());
        assertTrue(ChestBaking.configure(true), "the failure rebuilds once without baked chests");
        assertFalse(ChestBaking.enabled());
        assertFalse(ChestBaking.configure(true));
        ChestBaking.resetForTests();
    }
}
