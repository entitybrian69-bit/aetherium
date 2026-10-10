package com.aetherium.perf;

/**
 * Render-distance controller. Pure logic, fed once per evaluation window
 * (two seconds) with the measured FPS; returns the render distance to use.
 *
 * <p>Every render-distance change makes vanilla rebuild every chunk section, which
 * is itself a multi-second FPS dip. A controller that reacts quickly therefore
 * reads its own rebuild as "too slow" and walks the distance down in a loop. That
 * is why this one demands sustained evidence: FPS must stay below 85 % of the
 * target for {@link #DOWN_WINDOWS} consecutive windows (10 s) before it drops one
 * chunk, and above 140 % for {@link #UP_WINDOWS} windows (30 s) before it adds one
 * back. After any change it ignores {@link #COOLDOWN_WINDOWS} windows (30 s) so
 * the rebuild never counts as evidence. It never goes below {@link #MIN_DISTANCE}
 * nor above the distance the user chose.</p>
 */
public final class AdaptiveDistance {
    public static final int MIN_DISTANCE = 4;
    static final int DOWN_WINDOWS = 5;
    static final int UP_WINDOWS = 15;
    static final int COOLDOWN_WINDOWS = 15;
    static final double DOWN_BELOW = 0.85;
    static final double UP_ABOVE = 1.40;

    private int cooldown;
    private int lowStreak;
    private int highStreak;

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
            return this.changed(ceiling);
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return current;
        }
        if (fps <= 0.0 || target <= 0) {
            this.lowStreak = 0;
            this.highStreak = 0;
            return current;
        }
        if (fps < target * DOWN_BELOW) {
            this.highStreak = 0;
            if (++this.lowStreak >= DOWN_WINDOWS && current > MIN_DISTANCE) {
                return this.changed(current - 1);
            }
        } else if (fps > target * UP_ABOVE) {
            this.lowStreak = 0;
            if (++this.highStreak >= UP_WINDOWS && current < ceiling) {
                return this.changed(current + 1);
            }
        } else {
            this.lowStreak = 0;
            this.highStreak = 0;
        }
        return current;
    }

    private int changed(final int next) {
        this.cooldown = COOLDOWN_WINDOWS;
        this.lowStreak = 0;
        this.highStreak = 0;
        return next;
    }

    public void reset() {
        this.cooldown = 0;
        this.lowStreak = 0;
        this.highStreak = 0;
    }
}
