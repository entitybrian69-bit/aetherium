package com.aetherium.hud;

import java.util.Arrays;
import java.util.Locale;

/**
 * Summary of one benchmark scenario's frame times (1.9.0 auto benchmark). Pure Java so the
 * numbers in the CI report are unit-tested.
 */
public final class BenchStats {
    public final int frames;
    public final double seconds;
    public final double avgFps;
    public final double onePercentLowFps;
    public final double p50Ms;
    public final double p99Ms;
    public final double p999Ms;
    public final double maxMs;
    public final int spikesOver50ms;
    public final int spikesOver100ms;

    private BenchStats(final int frames, final double seconds, final double avgFps, final double onePercentLowFps,
                       final double p50Ms, final double p99Ms, final double p999Ms, final double maxMs,
                       final int spikesOver50ms, final int spikesOver100ms) {
        this.frames = frames;
        this.seconds = seconds;
        this.avgFps = avgFps;
        this.onePercentLowFps = onePercentLowFps;
        this.p50Ms = p50Ms;
        this.p99Ms = p99Ms;
        this.p999Ms = p999Ms;
        this.maxMs = maxMs;
        this.spikesOver50ms = spikesOver50ms;
        this.spikesOver100ms = spikesOver100ms;
    }

    /** @param nanos frame durations; only the first {@code count} are used and the array is not modified */
    public static BenchStats of(final long[] nanos, final int count) {
        if (count <= 0) {
            return new BenchStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0);
        }
        final long[] sorted = Arrays.copyOf(nanos, count);
        Arrays.sort(sorted);
        long total = 0L;
        int over50 = 0;
        int over100 = 0;
        for (int i = 0; i < count; i++) {
            final long n = sorted[i];
            total += n;
            if (n > 50_000_000L) {
                over50++;
            }
            if (n > 100_000_000L) {
                over100++;
            }
        }
        // 1% low: the average frame rate over the slowest 1% of frames (at least one frame).
        final int slowCount = Math.max(1, count / 100);
        long slowTotal = 0L;
        for (int i = count - slowCount; i < count; i++) {
            slowTotal += sorted[i];
        }
        final double seconds = total / 1e9;
        return new BenchStats(count, seconds, count / seconds, slowCount / (slowTotal / 1e9),
                ms(percentile(sorted, 0.50)), ms(percentile(sorted, 0.99)), ms(percentile(sorted, 0.999)),
                ms(sorted[count - 1]), over50, over100);
    }

    /** Nearest-rank percentile of an ascending array. */
    static long percentile(final long[] sorted, final double fraction) {
        final int rank = (int) Math.ceil(fraction * sorted.length);
        return sorted[Math.min(sorted.length - 1, Math.max(0, rank - 1))];
    }

    private static double ms(final long nanos) {
        return nanos / 1e6;
    }

    /** {@code | label | avg FPS | 1% low | p50 | p99 | p99.9 | max | >50 ms | >100 ms |} */
    public String markdownRow(final String label) {
        return String.format(Locale.ROOT, "| %s | %.1f | %.1f | %.2f | %.2f | %.2f | %.1f | %d | %d |",
                label, avgFps, onePercentLowFps, p50Ms, p99Ms, p999Ms, maxMs, spikesOver50ms, spikesOver100ms);
    }

    public static String markdownHeader() {
        return "| Scenario | Avg FPS | 1% low FPS | p50 ms | p99 ms | p99.9 ms | Max ms | Frames > 50 ms | Frames > 100 ms |\n"
                + "|---|---:|---:|---:|---:|---:|---:|---:|---:|";
    }

    /** One JSON object (no external JSON library in common). */
    public String json(final String id) {
        return String.format(Locale.ROOT,
                "{\"id\":\"%s\",\"frames\":%d,\"seconds\":%.3f,\"avgFps\":%.2f,\"onePercentLowFps\":%.2f,"
                        + "\"p50Ms\":%.3f,\"p99Ms\":%.3f,\"p999Ms\":%.3f,\"maxMs\":%.3f,\"spikes50\":%d,\"spikes100\":%d}",
                id, frames, seconds, avgFps, onePercentLowFps, p50Ms, p99Ms, p999Ms, maxMs, spikesOver50ms, spikesOver100ms);
    }
}
