package com.aetherium.perf;

/**
 * Time budget for the client's queued main-thread tasks (mostly network packets).
 *
 * <p>Vanilla runs every queued packet task at the start of each frame, however many there are.
 * Walking into unloaded terrain makes the server send chunk, light and entity packets in bursts,
 * and a burst of chunk packets can take 50-150 ms on a phone, inside one frame. That is the
 * hitch felt when crossing into new chunks. The budget spreads a burst over a few frames: at least
 * {@link #MIN_TASKS} tasks run every frame, then tasks keep running until the time is used up.
 * While the queue keeps outlasting the budget, the budget grows frame by frame (up to
 * {@link #MAX_NANOS}), so the queue can never grow without bound; it drops back as soon as the
 * queue is empty. The queue length is never asked for: vanilla's queue walks every node to count.</p>
 */
public final class PacketBudget {
    /** Tasks always run per frame, whatever the time. */
    public static final int MIN_TASKS = 8;
    /** Base budget per frame. */
    public static final long BASE_NANOS = 4_000_000L;
    /** Added per consecutive frame that ended with tasks still queued. */
    public static final long STEP_NANOS = 2_000_000L;
    /** The budget never exceeds this. */
    public static final long MAX_NANOS = 14_000_000L;
    static final int MAX_PRESSURE = (int) ((MAX_NANOS - BASE_NANOS) / STEP_NANOS);

    private static volatile boolean enabled = true;
    private static int pressure;

    /** Polls one task; false when the queue is empty. */
    public interface Poller {
        boolean pollTask();
    }

    /** Monotonic clock (tests substitute one). */
    public interface Clock {
        long nanoTime();
    }

    public static final Clock SYSTEM = new Clock() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    };

    private PacketBudget() {
    }

    public static void setEnabled(final boolean on) {
        enabled = on;
        if (!on) {
            pressure = 0;
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Budget for the current pressure (frames in a row that ended with work left). */
    public static long budgetFor(final int pressure) {
        return Math.min(MAX_NANOS, BASE_NANOS + Math.max(0, pressure) * STEP_NANOS);
    }

    public static int pressure() {
        return pressure;
    }

    /** Test seam. */
    static void resetPressure() {
        pressure = 0;
    }

    /**
     * Runs queued tasks within this frame's budget (render thread).
     *
     * @return the number of tasks run
     */
    public static int drain(final Poller poller, final Clock clock) {
        final long start = clock.nanoTime();
        final long budget = budgetFor(pressure);
        int ran = 0;
        while (poller.pollTask()) {
            ran++;
            if (ran >= MIN_TASKS && clock.nanoTime() - start >= budget) {
                // Stopped with the queue possibly non-empty: allow more next frame.
                pressure = Math.min(MAX_PRESSURE, pressure + 1);
                return ran;
            }
        }
        pressure = 0;
        return ran;
    }
}
