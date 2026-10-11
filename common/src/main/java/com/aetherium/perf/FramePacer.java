package com.aetherium.perf;

import java.util.concurrent.locks.LockSupport;

/**
 * Even frame spacing for the frame-rate limit (1.7.2, 1.16.5).
 *
 * <p>Vanilla's limiter waits with {@code glfwWaitEventsTimeout} until the previous frame's end plus
 * one period, then stores the time it actually woke up. Every oversleep therefore pushes all later
 * frames back: the average rate lands below the limit and the spacing wanders with the sleep
 * accuracy, which on Android launchers (an emulated GLFW) is poor.</p>
 *
 * <p>This pacer keeps a fixed cadence instead: each frame ends one period after the previous
 * frame's <em>scheduled</em> end, so a frame that ran late is made up by the next wait being shorter.
 * A frame more than a whole period late starts a new cadence (never a burst of rushed frames). It
 * sleeps with {@link LockSupport#parkNanos} until shortly before the deadline and yields for the
 * rest. Input is still read every frame: vanilla polls events when it swaps buffers.</p>
 *
 * <p>Render thread only.</p>
 */
public final class FramePacer {

    /** The last stretch before a deadline is yielded instead of slept: sleeps overshoot. */
    static final long SPIN_NANOS = 300_000L;

    private static long lastDeadline;

    private FramePacer() {
    }

    /**
     * Waits for this frame's slot at {@code fps} frames per second.
     *
     * @return false (vanilla's limiter runs instead) for a limit outside 1..1000
     */
    public static boolean pace(final int fps) {
        if (fps <= 0 || fps > 1000) {
            return false;
        }
        final long period = 1_000_000_000L / fps;
        final long deadline = schedule(lastDeadline, System.nanoTime(), period);
        waitUntil(deadline);
        lastDeadline = deadline;
        return true;
    }

    /**
     * When the current frame should end.
     *
     * @param previous the previous frame's deadline, 0 for none
     * @param now      the current time
     * @param period   nanoseconds per frame
     */
    static long schedule(final long previous, final long now, final long period) {
        if (previous == 0L) {
            return now;
        }
        final long target = previous + period;
        if (now - target > period) {
            return now; // more than a frame behind (a hitch, a pause, a lower limit): new cadence
        }
        return target; // in the past when slightly late: no wait, and the next wait is shorter
    }

    private static void waitUntil(final long deadline) {
        long remaining = deadline - System.nanoTime();
        while (remaining > SPIN_NANOS && !Thread.currentThread().isInterrupted()) {
            LockSupport.parkNanos(remaining - SPIN_NANOS);
            remaining = deadline - System.nanoTime();
        }
        while (deadline - System.nanoTime() > 0L) {
            Thread.yield();
        }
    }

    /** Unit tests: forget the cadence. */
    static void resetForTests() {
        lastDeadline = 0L;
    }
}
