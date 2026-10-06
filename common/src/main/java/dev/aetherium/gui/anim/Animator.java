package dev.aetherium.gui.anim;

/** Time-based, frame-rate-independent scalar animator with easing. */
public final class Animator {
    public enum Easing { LINEAR, OUT_CUBIC, OUT_QUINT, OUT_BACK, IN_OUT_CUBIC, OUT_EXPO }

    private float from, to, value;
    private long startMs, durationMs;
    private Easing easing = Easing.OUT_QUINT;

    public Animator(float initial) { this.from = this.to = this.value = initial; }

    public Animator easing(Easing e) { this.easing = e; return this; }

    public void animateTo(float target, long durationMs) {
        this.from = value;
        this.to = target;
        this.startMs = now();
        this.durationMs = Math.max(1, durationMs);
    }

    public void snap(float v) { from = to = value = v; durationMs = 0; }

    public float value() {
        if (durationMs == 0) return value = to;
        float t = Math.min(1f, (now() - startMs) / (float) durationMs);
        value = from + (to - from) * ease(t);
        if (t >= 1f) durationMs = 0;
        return value;
    }

    public boolean isAnimating() { return durationMs != 0 && (now() - startMs) < durationMs; }
    public float target() { return to; }

    private float ease(float t) {
        return switch (easing) {
            case LINEAR -> t;
            case OUT_CUBIC -> 1f - (float) Math.pow(1 - t, 3);
            case OUT_QUINT -> 1f - (float) Math.pow(1 - t, 5);
            case OUT_EXPO -> t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t);
            case OUT_BACK -> { float c1 = 1.70158f, c3 = c1 + 1; yield 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2); }
            case IN_OUT_CUBIC -> t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
        };
    }

    private static long now() { return System.nanoTime() / 1_000_000L; }
}
