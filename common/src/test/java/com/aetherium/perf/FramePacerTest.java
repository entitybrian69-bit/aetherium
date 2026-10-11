package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.7.2 frame pacing: a fixed cadence instead of vanilla's drifting limiter. */
final class FramePacerTest {

    private static final long P = 1_000_000_000L / 120; // 120 FPS

    @Test
    @DisplayName("frame pacing: frames end one period after the previous deadline, not after the wake-up")
    void cadence() {
        assertEquals(1000L, FramePacer.schedule(0L, 1000L, P), "the first frame starts the cadence");
        assertEquals(1000L + P, FramePacer.schedule(1000L, 1000L + P / 2, P), "early: wait for the slot");
        // Slightly late (a quarter period): the slot stays where it was, so the next wait is shorter
        // and the average stays at the limit (vanilla would move every later frame back).
        assertEquals(1000L + P, FramePacer.schedule(1000L, 1000L + P + P / 4, P));
    }

    @Test
    @DisplayName("frame pacing: a frame more than a period late starts a new cadence instead of a burst")
    void hitch() {
        final long now = 1000L + 5 * P;
        assertEquals(now, FramePacer.schedule(1000L, now, P));
    }

    @Test
    @DisplayName("frame pacing: waits for the slot and keeps the average at the limit")
    void realWaits() {
        FramePacer.resetForTests();
        final int fps = 200;
        assertTrue(FramePacer.pace(fps)); // starts the cadence
        final long start = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            assertTrue(FramePacer.pace(fps));
        }
        final long elapsed = System.nanoTime() - start;
        final long expected = 20L * (1_000_000_000L / fps); // 100 ms
        assertTrue(elapsed >= expected - 1_000_000L, "never faster than the limit: " + elapsed);
        assertTrue(elapsed < expected + 40_000_000L, "and not far slower: " + elapsed);
        FramePacer.resetForTests();
    }

    @Test
    @DisplayName("frame pacing: a nonsense limit leaves the wait to vanilla")
    void badLimit() {
        assertFalse(FramePacer.pace(0));
        assertFalse(FramePacer.pace(5000));
    }
}
