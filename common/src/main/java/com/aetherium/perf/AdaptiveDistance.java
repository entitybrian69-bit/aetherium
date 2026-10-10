package com.aetherium.perf;

/**
 * Render-distance controller. Pure logic, fed once per evaluation window
 * (two seconds) with the measured FPS; returns the render distance to use.
 *
 * <p>Steps down one chunk when FPS stays below 90 % of the target, steps back
 * up when FPS exceeds 125 % of the target, never goes below {@link #MIN_DISTANCE}
 * nor above the distance the user chose. A cooldown after every change gives
 * the chunk loader time to settle, so it cannot oscillate.</p>
 */
public final class AdaptiveDistance {
    public static final int MIN_DISTANCE = 4;
    static final int COOLDOWN_WINDOWS = 2;

    private int cooldown;

    /**
     * @param fps         measured frames per second over the last window
     * @param target      target FPS
     * @param current     render distance in effect now
     * @param userChoice  the distance the user picked (upper bound)
     * @return new render distance (equal to {@code current} when unchanged)
     */
    public int evaluate(final double fps, final int target, final int current, final int userChoice) {
        final int ceiling = Math.max(MIN_DISTANCE, userChoice);
        if (current > ceiling) {
            this.cooldown = COOLDOWN_WINDOWS;
            return ceiling;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return current;
        }
        if (fps <= 0.0 || target <= 0) {
            return current;
        }
        if (fps < target * 0.90 && current > MIN_DISTANCE) {
            this.cooldown = COOLDOWN_WINDOWS;
            return current - 1;
        }
        if (fps > target * 1.25 && current < ceiling) {
            this.cooldown = COOLDOWN_WINDOWS;
            return current + 1;
        }
        return current;
    }

    public void reset() {
        this.cooldown = 0;
    }
}
