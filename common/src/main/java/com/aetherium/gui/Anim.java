package com.aetherium.gui;

/**
 * Frame-rate independent easing value. {@link #tick} moves the value toward its
 * target with exponential smoothing, so an animation takes the same wall-clock
 * time at 20 FPS and at 300 FPS and never overshoots.
 */
public final class Anim {
    private float value;
    private float target;
    private final float speed;

    /** @param speed larger is faster; 12 settles in roughly 0.3 s */
    public Anim(final float initial, final float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(final float target) {
        this.target = target;
    }

    public void snap(final float to) {
        this.value = to;
        this.target = to;
    }

    public float target() {
        return this.target;
    }

    public float get() {
        return this.value;
    }

    public boolean settled() {
        return Math.abs(this.target - this.value) < 0.001f;
    }

    public void tick(final float seconds) {
        if (seconds <= 0f) {
            return;
        }
        final float k = 1f - (float) Math.exp(-this.speed * Math.min(seconds, 0.25f));
        this.value += (this.target - this.value) * k;
        if (Math.abs(this.target - this.value) < 0.0005f) {
            this.value = this.target;
        }
    }

    /** Cubic ease-out of a 0..1 progress value. */
    public static float easeOut(final float t) {
        final float k = t < 0f ? 0f : (t > 1f ? 1f : t);
        final float inv = 1f - k;
        return 1f - inv * inv * inv;
    }
}
