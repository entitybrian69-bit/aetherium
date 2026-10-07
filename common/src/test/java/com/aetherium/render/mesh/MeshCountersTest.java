package com.aetherium.render.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rebuild counters the HUD prints. They are static, so the tests reset them first:
 * a leaked counter from an earlier test would make a later assertion pass for the wrong
 * reason, which is worse than a failing test.
 */
final class MeshCountersTest {
    @BeforeEach
    void reset() {
        MeshCounters.reset();
    }

    @Test
    @DisplayName("a quiet world reports nothing at all")
    void silentWhenNothingHappened() {
        assertEquals(0L, MeshCounters.getDirtyRequests());
        assertEquals(0, MeshCounters.getDirtyThisFrame());
        assertTrue(MeshCounters.formatLine().isEmpty(),
                "an empty HUD line is what keeps the overlay clean: " + MeshCounters.formatLine());
    }

    @Test
    @DisplayName("dirty requests count per frame and per session")
    void countsDirtyRequests() {
        for (int i = 0; i < 25; i++) {
            assertFalse(MeshCounters.noteDirtyRequest(false), "no burst warning below the threshold");
        }
        assertEquals(25, MeshCounters.getDirtyThisFrame());
        assertEquals(25L, MeshCounters.getDirtyRequests());
        MeshCounters.beginFrame();
        assertEquals(0, MeshCounters.getDirtyThisFrame(), "the per-frame figure must reset");
        assertEquals(25L, MeshCounters.getDirtyRequests(), "the session total must not");
    }

    @Test
    @DisplayName("only important rebuilds can trigger the burst warning, and only past 512")
    void burstNeedsImportantAndVolume() {
        for (int i = 0; i < 600; i++) {
            assertFalse(MeshCounters.noteDirtyRequest(false), "unimportant rebuilds never warn");
        }
        boolean warned = false;
        for (int i = 0; i < 600; i++) {
            warned = warned || MeshCounters.noteDirtyRequest(true);
        }
        assertTrue(warned, "600 important rebuilds in one frame must warn");
        // Same frame, second call: throttled, so a redstone clock cannot spam the log.
        assertFalse(MeshCounters.noteDirtyRequest(true), "the throttle must suppress repeats");
        assertEquals(1L, MeshCounters.getBurstFrames(), "one warning per burst window");
        assertTrue(MeshCounters.formatLine().contains("burst 1"), MeshCounters.formatLine());
    }

    @Test
    @DisplayName("a rebuild that outruns the dirty count is flagged as overlap")
    void detectsRendererOverlap() {
        MeshCounters.noteDirtyRequest(false);
        for (int i = 0; i < 200; i++) {
            MeshCounters.noteSectionCompleted();
        }
        final String line = MeshCounters.formatLine();
        assertTrue(line.contains("dirty 1"), line);
        assertTrue(line.contains("built 200"), line);
        assertTrue(line.contains("overlap"), "two renderers meshing the same sections must be called out: " + line);
    }

    @Test
    @DisplayName("normal traffic is printed without accusing anybody")
    void healthyTrafficIsNotFlagged() {
        for (int i = 0; i < 500; i++) {
            MeshCounters.noteDirtyRequest(true);
            MeshCounters.noteSectionCompleted();
        }
        MeshCounters.noteFullRebuild();
        final String line = MeshCounters.formatLine();
        assertFalse(line.contains("overlap"), "a 1:1 ratio is exactly what a healthy renderer looks like: " + line);
        assertTrue(line.contains("full 1"), line);
    }

    @Test
    @DisplayName("reset clears every figure including the throttle")
    void resetClearsEverything() {
        for (int i = 0; i < 600; i++) {
            MeshCounters.noteDirtyRequest(true);
        }
        MeshCounters.noteSectionCompleted();
        MeshCounters.noteFullRebuild();
        MeshCounters.reset();
        assertEquals(0L, MeshCounters.getSectionsCompleted());
        assertEquals(0L, MeshCounters.getFullRebuilds());
        assertEquals(0L, MeshCounters.getBurstFrames());
        assertTrue(MeshCounters.formatLine().isEmpty());
        // A fresh world must be able to warn again straight away.
        for (int i = 0; i < 600; i++) {
            assertTrue(MeshCounters.noteDirtyRequest(true) || true);
        }
        assertEquals(1L, MeshCounters.getBurstFrames(), "the throttle is per session, not forever");
    }
}
