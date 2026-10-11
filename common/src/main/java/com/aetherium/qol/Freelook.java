package com.aetherium.qol;

/**
 * Freelook state: while active the mouse turns a free camera instead of the player, so you can
 * look around (or behind you) while walking, building or flying straight.
 */
public final class Freelook {
    /** Same mouse-to-degrees factor as vanilla's {@code Entity.turn}. */
    static final double DEGREES_PER_UNIT = 0.15;

    private boolean active;
    private boolean wasDown;
    private float yaw;
    private float pitch;

    /** Result of {@link #update}. */
    public static final int NO_CHANGE = 0;
    public static final int STARTED = 1;
    public static final int STOPPED = 2;

    /**
     * Per frame.
     *
     * @param keyDown the Freelook key is held (and no screen is open)
     * @param toggle  press once to start, again to stop
     */
    public int update(final boolean keyDown, final boolean toggle) {
        final boolean pressed = keyDown && !this.wasDown;
        this.wasDown = keyDown;
        final boolean want = toggle ? (pressed ? !this.active : this.active) : keyDown;
        if (want == this.active) {
            return NO_CHANGE;
        }
        this.active = want;
        return want ? STARTED : STOPPED;
    }

    /** Starts from the player's current view. */
    public void begin(final float playerYaw, final float playerPitch) {
        this.yaw = playerYaw;
        this.pitch = playerPitch;
    }

    /** Mouse movement, in the units vanilla passes to {@code Entity.turn}. */
    public void turn(final double dx, final double dy) {
        this.yaw += (float) (dx * DEGREES_PER_UNIT);
        this.pitch = Math.max(-90.0f, Math.min(90.0f, this.pitch + (float) (dy * DEGREES_PER_UNIT)));
    }

    public void stop() {
        this.active = false;
    }

    public boolean active() {
        return this.active;
    }

    public float yaw() {
        return this.yaw;
    }

    public float pitch() {
        return this.pitch;
    }
}
