package com.aetherium.hud;

import java.util.Arrays;
import java.util.Locale;

import com.aetherium.util.AetheriumLog;

/**
 * Rolling frame-time statistics: FPS, median, 99th percentile and the p99.9
 * spike count, over a fixed ring of samples.
 *
 * <p>Why percentiles and not an average: the number that decides whether a
 * renderer feels smooth is the tail, not the mean, and a mean over a 5-second
 * window is dominated by the two frames where the world loaded. Aetherium's own
 * CI comparison in BENCHMARK.md therefore reports p50/p99/p99.9 and rejects any
 * claim that improves the mean while worsening p99.</p>
 *
 * <p>Implementation: a power-of-two ring of nanosecond durations plus a
 * counting histogram with 1 us buckets, so {@link #percentile} is O(buckets)
 * rather than O(n log n) — a full sort every frame on 12k samples is exactly the
 * kind of instrumentation that changes what it measures. Single writer (the
 * render thread); readers get a consistent snapshot because the only published
 * state is {@code volatile} scalars.</p>
 */
public final class FrameStats {
    private static final AetheriumLog LOGGER = AetheriumLog.of(FrameStats.class);

    /** 16384 frames ~ 68 s at 240 fps, 5 min at 60 fps. */
    public static final int SAMPLE_CAPACITY = 16384;
    private static final int SAMPLE_MASK = SAMPLE_CAPACITY - 1;
    private static final int HISTOGRAM_BUCKETS = 8192;
    /** 1 us per bucket, so the histogram tops out at 8.192 ms; longer frames go in the last bucket. */
    private static final long BUCKET_NANOS = 1_000L;

    private final long[] samples = new long[SAMPLE_CAPACITY];
    private final int[] histogram = new int[HISTOGRAM_BUCKETS];

    /** Write index into {@code samples}; render-thread only. */
    private int writeIndex;
    private int filled;
    private long totalNanos;
    private long longestNanos;
    private int spikesOver50ms;
    private int spikesOver100ms;

    /** Published snapshot; volatile so the GUI/HUD reader sees a consistent quad. */
    private volatile double fps;
    private volatile double p50Ms;
    private volatile double p99Ms;
    private volatile double p999Ms;
    private volatile double frameMs;
    private volatile long framesTotal;
    private volatile long lastUpdateNanos;

    private long windowStartNanos;
    private int windowSamples;
    private long windowNanos;
    private int recalculationCounter;

    public FrameStats() {
        this.windowStartNanos = System.nanoTime();
    }

    /** Called once per frame from the client hook, after the swap. */
    public void record(final long frameNanos) {
        if (frameNanos <= 0L) {
            return;
        }
        this.samples[this.writeIndex] = frameNanos;
        this.writeIndex = (this.writeIndex + 1) & SAMPLE_MASK;
        if (this.filled < SAMPLE_CAPACITY) {
            this.filled++;
            this.histogram[bucketFor(frameNanos)]++;
        } else {
            // Overwriting: decrement the old bucket so the histogram stays exact.
            final int overwrittenIndex = this.writeIndex;
            this.histogram[bucketFor(this.samples[overwrittenIndex])]--;
            this.histogram[bucketFor(frameNanos)]++;
        }
        this.totalNanos += frameNanos;
        this.framesTotal++;
        this.windowSamples++;
        this.windowNanos += frameNanos;
        if (frameNanos > this.longestNanos) {
            this.longestNanos = frameNanos;
        }
        if (frameNanos > 50_000_000L) {
            this.spikesOver50ms++;
        }
        if (frameNanos > 100_000_000L) {
            this.spikesOver100ms++;
        }

        // Recompute at most ~4 times a second. Percentile search over 8k buckets
        // is cheap but not free, and the HUD does not need 240 updates a second.
        if (++this.recalculationCounter >= 32) {
            this.recalculationCounter = 0;
            recompute();
        }
    }

    private int bucketFor(final long nanos) {
        final int bucket = (int) (nanos / BUCKET_NANOS);
        return Math.min(bucket, HISTOGRAM_BUCKETS - 1);
    }

    private void recompute() {
        final long now = System.nanoTime();
        final long windowElapsed = Math.max(1L, now - this.windowStartNanos);
        this.fps = this.windowNanos > 0L ? this.windowSamples * 1_000_000_000.0 / windowElapsed : 0.0;
        this.frameMs = this.windowSamples == 0 ? 0.0 : this.windowNanos / 1_000_000.0 / this.windowSamples;
        this.p50Ms = percentile(0.50);
        this.p99Ms = percentile(0.99);
        this.p999Ms = percentile(0.999);
        this.lastUpdateNanos = now;
        this.windowStartNanos = now;
        this.windowSamples = 0;
        this.windowNanos = 0L;
    }

    /**
     * @param fraction 0..1
     * @return the duration in ms at that percentile, or 0 when nothing is recorded
     */
    public double percentile(final double fraction) {
        if (this.filled == 0) {
            return 0.0;
        }
        final int target = (int) Math.max(1L, Math.round(this.filled * Math.min(Math.max(fraction, 0.0), 1.0)));
        int running = 0;
        for (int bucket = 0; bucket < HISTOGRAM_BUCKETS; bucket++) {
            running += this.histogram[bucket];
            if (running >= target) {
                // Bucket midpoint: a 1 us granularity is far below measurement noise.
                return (bucket + 0.5) * BUCKET_NANOS / 1_000_000.0;
            }
        }
        return this.longestNanos / 1_000_000.0;
    }

    /** Copies the recent window for the FPS graph; returns valid sample count. */
    public int copyRecentGraph(final double[] millisOut, final int maxSamples) {
        final int count = Math.min(Math.min(maxSamples, this.filled), 480);
        if (count == 0) {
            return 0;
        }
        final int start = (this.writeIndex - count + SAMPLE_CAPACITY * 2) & SAMPLE_MASK;
        for (int i = 0; i < count; i++) {
            millisOut[i] = this.samples[(start + i) & SAMPLE_MASK] / 1_000_000.0;
        }
        return count;
    }

    public double getFps() {
        return this.fps;
    }

    public double getFrameMs() {
        return this.frameMs;
    }

    public double getP50Ms() {
        return this.p50Ms;
    }

    public double getP99Ms() {
        return this.p99Ms;
    }

    public double getP999Ms() {
        return this.p999Ms;
    }

    public double getLongestMs() {
        return this.longestNanos / 1_000_000.0;
    }

    public int getSpikesOver50ms() {
        return this.spikesOver50ms;
    }

    public int getSpikesOver100ms() {
        return this.spikesOver100ms;
    }

    public long getFramesTotal() {
        return this.framesTotal;
    }

    public double getAverageFrameMsSinceStart() {
        final long frames = this.framesTotal;
        return frames == 0L ? 0.0 : this.totalNanos / 1_000_000.0 / frames;
    }

    /** Clears the rolling window; called on world change so a load hitch does not follow you. */
    public void resetWindow() {
        Arrays.fill(this.histogram, 0);
        Arrays.fill(this.samples, 0L);
        this.writeIndex = 0;
        this.filled = 0;
        this.windowSamples = 0;
        this.windowNanos = 0L;
        this.windowStartNanos = System.nanoTime();
        // Window-derived display state: after a world change the HUD must not show the
        // previous world's frame time for one more frame, so the last-frame value and
        // the percentiles/fps computed from the (now empty) window go to zero too.
        // Totals (framesTotal, spikes, totalNanos) deliberately survive - see resetAll.
        this.frameMs = 0.0;
        this.fps = 0.0;
        this.p50Ms = 0.0;
        this.p99Ms = 0.0;
        this.p999Ms = 0.0;
        LOGGER.dev("Frame statistics window reset");
    }

    /** Full reset including totals and spikes, for a benchmark run boundary. */
    public void resetAll() {
        resetWindow();
        this.totalNanos = 0L;
        this.longestNanos = 0L;
        this.spikesOver50ms = 0;
        this.spikesOver100ms = 0;
        this.framesTotal = 0;
        this.fps = 0.0;
        this.p50Ms = 0.0;
        this.p99Ms = 0.0;
        this.p999Ms = 0.0;
    }

    /** One-line HUD string; the caller draws it, this class does not know about fonts. */
    public String formatHudLine() {
        return String.format(Locale.ROOT, "%d fps | %.2f ms | p50 %.2f | p99 %.2f | p99.9 %.2f",
                (int) Math.round(this.fps), this.frameMs, this.p50Ms, this.p99Ms, this.p999Ms);
    }

    /** Markdown row for {@code tools/benchmark.sh}. */
    public String formatMarkdownRow(final String label) {
        return String.format(Locale.ROOT, "| %s | %d | %.2f | %.2f | %.2f | %.2f | %d |",
                label, (int) Math.round(this.fps), this.p50Ms, this.p99Ms, this.p999Ms,
                this.longestNanos / 1_000_000.0, this.spikesOver100ms);
    }

    public boolean isStale() {
        return System.nanoTime() - this.lastUpdateNanos > 1_000_000_000L;
    }
}
