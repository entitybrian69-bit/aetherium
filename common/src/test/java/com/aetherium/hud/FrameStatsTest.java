package com.aetherium.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The frame-time aggregator behind the HUD, the F3 line and {@code tools/benchmark.sh}.
 * Two properties matter most: the histogram must stay exact when the ring buffer wraps
 * (a leaked bucket means the 99th percentile slowly drifts upward over a long session),
 * and a frame of 0 or negative length must not be counted at all.
 */
final class FrameStatsTest {
    private FrameStats stats;

    @BeforeEach
    void setUp() {
        this.stats = new FrameStats();
    }

    private void record(final FrameStats target, final int count, final long frameNanos) {
        for (int i = 0; i < count; i++) {
            target.record(frameNanos);
        }
    }

    @Test
    @DisplayName("a garbage frame is ignored, not averaged in")
    void ignoresNonPositiveFrames() {
        this.stats.record(0L);
        this.stats.record(-5_000_000L);
        assertEquals(0L, this.stats.getFramesTotal(), "a zero-length frame is a skipped frame, not a fast one");
        assertEquals(0.0d, this.stats.percentile(0.5), 1.0E-9);
        assertEquals(0.0d, this.stats.getLongestMs(), 1.0E-9);
        this.stats.record(8_000_000L);
        assertEquals(1L, this.stats.getFramesTotal());
    }

    @Test
    @DisplayName("the median lands on the bucket holding the real value")
    void percentileTracksTheHistogram() {
        // 2 ms frames dominate; the 1 us bucket midpoint is well inside the tolerance.
        record(this.stats, 99, 2_000_000L);
        this.stats.record(6_000_000L);
        assertEquals(2.0d, this.stats.percentile(0.50), 0.01d);
        assertEquals(2.0d, this.stats.percentile(0.90), 0.01d);
        assertTrue(this.stats.percentile(0.99) >= 2.0d, "p99 must not sit below the median");
        assertEquals(6.0d, this.stats.getLongestMs(), 0.01d);
    }

    @Test
    @DisplayName("spike counters are separate from the histogram and count both thresholds")
    void spikeCounters() {
        record(this.stats, 3, 10_000_000L);   // 10 ms: no spike
        record(this.stats, 2, 60_000_000L);   // 60 ms: > 50
        record(this.stats, 1, 150_000_000L);  // 150 ms: > 50 and > 100
        assertEquals(3, this.stats.getSpikesOver50ms());
        assertEquals(1, this.stats.getSpikesOver100ms());
        assertEquals(6L, this.stats.getFramesTotal());
    }

    @Test
    @DisplayName("the cached readouts refresh every 32 frames, and the mean is exact")
    void cachedReadoutsRefresh() {
        record(this.stats, 40, 16_000_000L); // 16 ms each, 40 frames >= the 32-frame period
        assertEquals(16.0d, this.stats.getFrameMs(), 1.0E-6, "the window mean must be exact");
        assertTrue(this.stats.getFps() > 0.0, "fps is a wall-clock ratio and cannot be asserted numerically here");
        assertTrue(this.stats.getP50Ms() > 0.0, "the percentile readouts are cached and must have been computed");
        assertEquals(this.stats.getP50Ms(), this.stats.getP99Ms(), 1.0E-9, "identical frames have identical percentiles");
        assertFalse(this.stats.isStale(), "a stats object that just recomputed is not stale");
    }

    @Test
    @DisplayName("a fresh window is stale, so the HUD can print STALE instead of a lie")
    void freshInstanceIsStale() {
        assertTrue(this.stats.isStale(), "no data yet must read as stale, not as 0 fps");
    }

    @Test
    @DisplayName("wrapping the ring buffer keeps the histogram exact")
    void histogramSurvivesWrap() {
        final int records = FrameStats.SAMPLE_CAPACITY + 4096;
        record(this.stats, records, 1_000_000L);
        assertEquals(records, this.stats.getFramesTotal());
        // copyRecentGraph is capped at 480 samples (the HUD graph width), so the graph
        // path cannot ask for 16k floats and stall a render frame.
        assertEquals(480, this.stats.copyRecentGraph(new double[FrameStats.SAMPLE_CAPACITY],
                FrameStats.SAMPLE_CAPACITY));
        // If the overwrite path failed to decrement the evicted bucket, this percentile
        // would keep growing with the number of wraps instead of staying on the sample.
        assertEquals(1.0d, this.stats.percentile(0.50), 0.01d);
        assertEquals(1.0d, this.stats.percentile(0.999), 0.01d);
    }

    @Test
    @DisplayName("the graph copy is oldest-first and capped at 480 samples")
    void graphCopy() {
        this.stats.record(1_000_000L);
        this.stats.record(2_000_000L);
        this.stats.record(3_000_000L);
        final double[] out = new double[600];
        assertEquals(3, this.stats.copyRecentGraph(out, 600));
        assertEquals(1.0d, out[0], 0.01d);
        assertEquals(3.0d, out[2], 0.01d);

        record(this.stats, 900, 4_000_000L);
        final double[] capped = new double[600];
        assertEquals(480, this.stats.copyRecentGraph(capped, 600), "the HUD graph is capped at 480 samples by design");
        assertEquals(4.0d, capped[479], 0.01d);
        assertEquals(0, this.stats.copyRecentGraph(new double[0], 0));
    }

    @Test
    @DisplayName("resetWindow keeps totals; resetAll clears them")
    void resetScopes() {
        record(this.stats, 40, 16_000_000L);
        record(this.stats, 1, 200_000_000L);
        this.stats.resetWindow();
        assertEquals(41L, this.stats.getFramesTotal(), "resetWindow is a HUD thing, it must not fake a shorter session");
        assertEquals(1, this.stats.getSpikesOver100ms(), "spikes are totals, not window state");
        assertEquals(0.0d, this.stats.getFrameMs(), 1.0E-9);

        this.stats.resetAll();
        assertEquals(0L, this.stats.getFramesTotal());
        assertEquals(0, this.stats.getSpikesOver100ms());
        assertEquals(0.0d, this.stats.getAverageFrameMsSinceStart(), 1.0E-9);
    }

    @Test
    @DisplayName("the HUD line and the benchmark row are formatted for their readers")
    void formatting() {
        record(this.stats, 40, 16_000_000L);
        final String hud = this.stats.formatHudLine();
        assertTrue(hud.contains("fps |"), "unexpected HUD line: " + hud);
        assertTrue(hud.contains("p99.9"), "the HUD must show the tail, not just the mean: " + hud);
        assertTrue(hud.matches("(?s).*\\d+ fps.*"), "no frame rate in: " + hud);

        final String row = this.stats.formatMarkdownRow("gl46-dsa");
        assertTrue(row.startsWith("| gl46-dsa |"), "not a markdown row: " + row);
        // Seven cells, eight boundaries: a markdown row is pipe-delimited at both ends, so the
        // count is cells + 1 - the number this assertion used to get wrong while the row it checks
        // was right (and the header table in BenchmarkRecorder has the same seven columns).
        assertEquals(8, row.chars().filter(value -> value == '|').count(),
                "a benchmark table row needs one pipe per boundary: " + row);
        assertTrue(row.endsWith("|"), "unterminated row: " + row);
    }

    @Test
    @DisplayName("a long session's mean stays honest")
    void averageSinceStart() {
        record(this.stats, 10, 10_000_000L);
        record(this.stats, 10, 30_000_000L);
        assertEquals(20.0d, this.stats.getAverageFrameMsSinceStart(), 0.01d);
    }
}
