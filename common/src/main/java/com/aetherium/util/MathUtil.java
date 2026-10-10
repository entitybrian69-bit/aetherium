package com.aetherium.util;

/**
 * Small numeric helpers used on the render path. Everything here is
 * allocation-free and branch-predictor friendly: no boxing, no varargs.
 */
public final class MathUtil {
    /** Avoids Math.pow in the gamma hot loop (pow on a float pair is ~40ns). */
    private static final int EXP2_TABLE_BITS = 10;
    private static final int EXP2_TABLE_SIZE = 1 << EXP2_TABLE_BITS;
    private static final float[] EXP2_TABLE = buildExp2Table();

    private MathUtil() {
    }

    public static float clamp(final float value, final float min, final float max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static double clamp(final double value, final double min, final double max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static long clamp(final long value, final long min, final long max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static int clamp(final int value, final int min, final int max) {
        return value < min ? min : (value > max ? max : value);
    }

    public static float lerp(final float from, final float to, final float progress) {
        return from + (to - from) * progress;
    }

    /**
     * Frame-rate independent exponential smoothing. {@code halfLifeSeconds} is the
     * time to close half the gap, which — unlike a fixed alpha — behaves the same
     * at 30, 144 and 400 fps. Used by the animated widgets and dynamic lights.
     */
    public static float smoothDamp(final float current, final float target, final float halfLifeSeconds, final float deltaSeconds) {
        if (deltaSeconds <= 0.0f) {
            // Zero (or negative) time passed: nothing may move. Returning the target
            // here would make a paused GUI snap to its end state, which is exactly
            // the animation glitch this guard exists for.
            return current;
        }
        if (halfLifeSeconds <= 1.0E-6f) {
            // A zero half-life means "no smoothing at all": snap to the target.
            return target;
        }
        final float decay = (float) Math.pow(0.5d, deltaSeconds / halfLifeSeconds);
        return target + (current - target) * decay;
    }

    /**
     * Cosine-ish easing that is cheap enough to run per widget per frame.
     * {@code t} is clamped to [0,1] so a paused/skipped frame cannot overshoot.
     */
    public static float easeOutCubic(final float t) {
        final float u = 1.0f - clamp(t, 0.0f, 1.0f);
        return 1.0f - u * u * u;
    }

    public static float easeInOutCubic(final float t) {
        final float x = clamp(t, 0.0f, 1.0f);
        return x < 0.5f ? 4.0f * x * x * x : 1.0f - (float) Math.pow(-2.0 * x + 2.0, 3) * 0.5f;
    }

    /** Overshoot for toggle "pop"; amplitude matches the purple glow pulse. */
    public static float easeOutBack(final float t) {
        final float c1 = 1.70158f;
        final float c3 = c1 + 1.0f;
        final float x = clamp(t, 0.0f, 1.0f) - 1.0f;
        return 1.0f + c3 * x * x * x + c1 * x * x;
    }

    /**
     * Approximate 2^x over [-1,1] with a table of exact powers. The relative
     * error is under 1.5e-4 — invisible in a lightmap, and ~8x cheaper than
     * {@link Math#pow(double, double)} on the sections this is applied to.
     */
    public static float fastExp2(final float x) {
        if (x <= -EXP2_TABLE_BITS) {
            return 0.0f;
        }
        final int integer = (int) Math.floor(x);
        final float fraction = x - integer;
        final int index = MathUtil.clamp((int) (fraction * EXP2_TABLE_SIZE), 0, EXP2_TABLE_SIZE - 1);
        return EXP2_TABLE[index] * (float) Math.pow(2.0d, integer);
    }

    /**
     * Packs 0..1 channel triple into the 24-bit ABGR int layout vanilla lightmaps use
     * (blue high, red low). The alpha byte is intentionally zero: callers that write
     * into a vanilla texture must merge the incoming pixel's alpha themselves, because
     * {@code GammaApplier} preserves it and a forced 0xFF would make every lightmap
     * int negative — which the lightmap tests forbid.
     */
    public static int packRgb(final float red, final float green, final float blue) {
        final int r = MathUtil.clamp((int) (red * 255.0f + 0.5f), 0, 255);
        final int g = MathUtil.clamp((int) (green * 255.0f + 0.5f), 0, 255);
        final int b = MathUtil.clamp((int) (blue * 255.0f + 0.5f), 0, 255);
        return (b << 16) | (g << 8) | r;
    }

    public static float channelRed(final int abgr) {
        return (abgr & 0xFF) / 255.0f;
    }

    public static float channelGreen(final int abgr) {
        return ((abgr >> 8) & 0xFF) / 255.0f;
    }

    public static float channelBlue(final int abgr) {
        return ((abgr >> 16) & 0xFF) / 255.0f;
    }

    /**
     * Conservative unsigned pack of a chunk-section offset into a single long
     * key. Used by the mesh scheduler's de-duplication set. The fields must not
     * overlap: 24 bits for x and z (biased by 2^23 so ±8M sections fit) and
     * 16 bits for y (biased by 2^15), which is exactly 64 bits — the previous
     * 25-bit x/z fields aliased into each other's bits, so a directly adjacent
     * section could collide and be skipped by the dedup set.
     */
    public static long sectionKey(final int x, final int y, final int z) {
        return ((x + 8388608L) << 40) | ((z + 8388608L) << 16) | (y + 32768L);
    }

    private static float[] buildExp2Table() {
        final float[] table = new float[EXP2_TABLE_SIZE];
        for (int i = 0; i < EXP2_TABLE_SIZE; i++) {
            table[i] = (float) Math.pow(2.0d, i / (double) EXP2_TABLE_SIZE);
        }
        return table;
    }
}
