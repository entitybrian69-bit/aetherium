package com.aetherium.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The numbers the CI benchmark report prints. */
final class BenchStatsTest {

    @Test
    void steadyFramesGiveTheirRate() {
        final long[] frames = new long[100];
        java.util.Arrays.fill(frames, 10_000_000L);
        final BenchStats stats = BenchStats.of(frames, frames.length);
        assertEquals(100, stats.frames);
        assertEquals(100.0, stats.avgFps, 1e-9);
        assertEquals(100.0, stats.onePercentLowFps, 1e-9);
        assertEquals(10.0, stats.p99Ms, 1e-9);
        assertEquals(0, stats.spikesOver50ms);
    }

    @Test
    void oneSpikeShowsInTheTailOnly() {
        final long[] frames = new long[200];
        java.util.Arrays.fill(frames, 10_000_000L);
        frames[57] = 120_000_000L;
        final BenchStats stats = BenchStats.of(frames, frames.length);
        assertEquals(10.0, stats.p50Ms, 1e-9);
        assertEquals(120.0, stats.maxMs, 1e-9);
        assertEquals(1, stats.spikesOver50ms);
        assertEquals(1, stats.spikesOver100ms);
        // Slowest 1% = 2 frames: 120 ms + 10 ms -> 2 / 0.13 s.
        assertEquals(2 / 0.13, stats.onePercentLowFps, 1e-9);
    }

    @Test
    void onlyTheCountedPrefixIsUsedAndTheInputIsUntouched() {
        final long[] frames = {30_000_000L, 10_000_000L, 999_000_000L};
        final BenchStats stats = BenchStats.of(frames, 2);
        assertEquals(2, stats.frames);
        assertEquals(30.0, stats.maxMs, 1e-9);
        assertEquals(30_000_000L, frames[0]);
    }

    @Test
    void emptyIsZero() {
        assertEquals(0, BenchStats.of(new long[0], 0).frames);
    }
}
