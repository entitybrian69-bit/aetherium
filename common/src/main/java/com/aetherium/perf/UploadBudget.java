package com.aetherium.perf;

import java.util.Queue;

/**
 * Spreads finished chunk meshes over several frames.
 *
 * <p>Vanilla drains the whole upload queue in one frame. When a burst of sections finishes at
 * once (joining a world, flying into new terrain, world generation catching up) that single frame
 * can spend tens of milliseconds in {@code glBufferData}, far worse on GL-over-GLES layers, which
 * shows up as the classic chunk-loading stutter. Here each frame uploads at least
 * {@link #MIN_PER_FRAME} meshes, so block edits next to the player still appear immediately,
 * then continues only while the frame's time budget lasts. Small queues are always drained.</p>
 */
public final class UploadBudget {
    /** Always uploaded per frame regardless of time: a block edit touches up to ~5 layer buffers. */
    public static final int MIN_PER_FRAME = 8;
    /** Queues this short are drained completely; there is no stutter to remove. */
    public static final int DRAIN_ALL_BELOW = 24;
    /** Time budget per frame for uploads beyond the minimum. */
    public static final long BUDGET_NANOS = 3_000_000L;

    /** Adaptive budget bounds: {@link #adapt} picks a share of the average frame time within them. */
    public static final long MIN_BUDGET_NANOS = 1_500_000L;
    public static final long MAX_BUDGET_NANOS = 4_000_000L;
    /** Share of the average frame time spent on uploads beyond the minimum. */
    static final double FRAME_SHARE = 0.12;

    private static volatile boolean enabled = true;
    private static volatile long budgetNanos = BUDGET_NANOS;
    /** Set when a mesh was built on the render thread: the next drain runs the whole queue. */
    private static volatile boolean drainAllOnce;

    private UploadBudget() {
    }

    public static void setEnabled(final boolean value) {
        enabled = value;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Clock seam for tests. */
    public interface Clock {
        long nanoTime();
    }

    private static final Clock SYSTEM = new Clock() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    };

    /** Budget for a given average frame time: about 12 % of it, 1.5-4 ms. */
    public static long budgetFor(final double averageFrameNanos) {
        final long share = (long) (averageFrameNanos * FRAME_SHARE);
        return Math.max(MIN_BUDGET_NANOS, Math.min(MAX_BUDGET_NANOS, share));
    }

    /** Once per frame with the running average frame time. */
    public static void adapt(final double averageFrameNanos) {
        budgetNanos = budgetFor(averageFrameNanos);
    }

    /** Hitch guard: the minimum budget for this frame (after {@link #adapt}). */
    public static void throttle() {
        budgetNanos = MIN_BUDGET_NANOS;
    }

    public static long currentBudgetNanos() {
        return budgetNanos;
    }

    /**
     * A chunk section was rebuilt on the render thread (a block edit next to the player). In
     * 1.16.5 those meshes are built in the vertex builders the entity and held-item renderers
     * draw with too, so their uploads must run in this frame, before anything else uses those
     * builders: the next drain runs the whole queue, in order.
     */
    public static void requestDrainAll() {
        drainAllOnce = true;
    }

    /** Runs queued uploads within this frame's budget; the rest stay queued for the next frame. */
    public static int drain(final Queue<Runnable> queue) {
        return drain(queue, SYSTEM, budgetNanos);
    }

    /**
     * {@code queue.size() < limit} without the full walk: vanilla's queue is a
     * {@code ConcurrentLinkedQueue}, whose {@code size()} visits every node.
     */
    static boolean fewerThan(final Queue<?> queue, final int limit) {
        int seen = 0;
        for (final java.util.Iterator<?> it = queue.iterator(); it.hasNext(); it.next()) {
            if (++seen >= limit) {
                return false;
            }
        }
        return true;
    }

    public static int drain(final Queue<Runnable> queue, final Clock clock, final long budgetNanos) {
        final boolean all = drainAllOnce || fewerThan(queue, DRAIN_ALL_BELOW);
        drainAllOnce = false;
        final long start = clock.nanoTime();
        int ran = 0;
        Runnable task;
        while ((task = queue.poll()) != null) {
            task.run();
            ran++;
            if (!all && ran >= MIN_PER_FRAME && clock.nanoTime() - start >= budgetNanos) {
                break;
            }
        }
        return ran;
    }
}
