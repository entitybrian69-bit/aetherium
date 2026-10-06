package dev.aetherium.frame;

import java.util.Arrays;

/** Lock-free single-producer ring of frame times used by the debug overlay and the frame-time graph. */
public final class FrameTimeTracker {
    private static final FrameTimeTracker INSTANCE = new FrameTimeTracker(240);

    private final float[] ringMs;
    private int head;
    private long lastNanos;
    private final float[] scratch;

    private FrameTimeTracker(int capacity) {
        this.ringMs = new float[capacity];
        this.scratch = new float[capacity];
    }

    public static FrameTimeTracker get() { return INSTANCE; }

    public void markFrame() {
        long now = System.nanoTime();
        if (lastNanos != 0L) {
            ringMs[head] = (now - lastNanos) / 1_000_000f;
            head = (head + 1) % ringMs.length;
        }
        lastNanos = now;
    }

    public int capacity() { return ringMs.length; }

    /** Sample i frames ago (0 = most recent). */
    public float sample(int i) {
        int idx = Math.floorMod(head - 1 - i, ringMs.length);
        return ringMs[idx];
    }

    public float average() {
        float s = 0; int n = 0;
        for (float v : ringMs) if (v > 0) { s += v; n++; }
        return n == 0 ? 0 : s / n;
    }

    public float percentile(double p) {
        int n = 0;
        for (float v : ringMs) if (v > 0) scratch[n++] = v;
        if (n == 0) return 0;
        Arrays.sort(scratch, 0, n);
        int idx = (int) Math.min(n - 1, Math.round(p * (n - 1)));
        return scratch[idx];
    }

    public float fps() {
        float avg = average();
        return avg <= 0 ? 0 : 1000f / avg;
    }
}
