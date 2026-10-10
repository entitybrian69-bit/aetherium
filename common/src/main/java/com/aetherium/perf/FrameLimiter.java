package com.aetherium.perf;

import java.util.concurrent.locks.LockSupport;

/**
 * Soft frame cap used by battery saver and the thermal guard. It sleeps at the
 * start of a frame instead of changing the user's Max Framerate option, so
 * nothing is written to options.txt and the cap disappears the moment the
 * condition clears. The last half millisecond is spent yielding rather than
 * parked, because parkNanos overshoots by ~0.1-1 ms on most kernels.
 */
public final class FrameLimiter {
    private long lastFrameStart;
    private volatile int capFps;

    public void setCap(final int fps) {
        this.capFps = Math.max(0, fps);
    }

    public int getCap() {
        return this.capFps;
    }

    /** Called at the very start of a frame on the render thread. */
    public void beforeFrame() {
        final int cap = this.capFps;
        long now = System.nanoTime();
        if (cap > 0 && this.lastFrameStart != 0L) {
            final long target = 1_000_000_000L / cap;
            final long deadline = this.lastFrameStart + target;
            long remaining = deadline - now;
            if (remaining > 0L && remaining < 1_000_000_000L) {
                if (remaining > 600_000L) {
                    LockSupport.parkNanos(remaining - 500_000L);
                }
                while ((remaining = deadline - System.nanoTime()) > 0L) {
                    Thread.yield();
                }
                now = System.nanoTime();
            }
        }
        this.lastFrameStart = now;
    }
}
