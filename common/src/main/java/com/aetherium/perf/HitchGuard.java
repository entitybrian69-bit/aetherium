package com.aetherium.perf;

/**
 * Hitch guard (1.10.0): after one slow frame, defer optional work for a moment.
 *
 * <p>A hitch is a frame that took at least {@link #MIN_HITCH_NANOS} and at least
 * {@link #RATIO} times the running average (a garbage collection, a world save, a burst of chunk
 * packets). Work queued behind it (chunk uploads, light updates) would otherwise all land in the
 * next frames and turn one hitch into a series. For {@link #RECOVERY_FRAMES} frames after a hitch,
 * the upload budget drops to its minimum and the client light engine gets a small per-frame quota.
 * Visible work still happens every frame; only its pace is reduced.</p>
 */
public final class HitchGuard {
    public static final long MIN_HITCH_NANOS = 40_000_000L;
    public static final double RATIO = 2.0;
    public static final int RECOVERY_FRAMES = 20;

    private static long lastFrame;
    private static int recovery;

    private HitchGuard() {
    }

    /**
     * Render thread, once per frame.
     *
     * @param now         System.nanoTime() at the start of this frame
     * @param averageNanos running average frame time (0 when unknown)
     * @return true when this frame follows a hitch (or is still recovering from one)
     */
    public static boolean frame(final long now, final double averageNanos) {
        final long last = lastFrame;
        lastFrame = now;
        if (last != 0L) {
            final long took = now - last;
            if (took >= MIN_HITCH_NANOS && averageNanos > 0 && took >= averageNanos * RATIO && took < 5_000_000_000L) {
                recovery = RECOVERY_FRAMES;
                return true;
            }
        }
        if (recovery > 0) {
            recovery--;
            return true;
        }
        return false;
    }

    public static boolean recovering() {
        return recovery > 0;
    }

    /** Light updates the client may run this frame (vanilla: unlimited). */
    public static int lightQuota() {
        return recovery > 0 ? 1024 : 16384;
    }

    static void resetForTests() {
        lastFrame = 0L;
        recovery = 0;
    }
}
