package com.aetherium.perf;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.mixin.AetheriumMixinPlugin;

/**
 * Two render-thread stalls that 1.16.5 has and later versions fixed:
 *
 * <ol>
 *   <li><b>Build near.</b> Every dirty chunk section within about 28 blocks of the camera is
 *       re-meshed synchronously on the render thread, not just the ones the player edited. Chunks
 *       arriving around the player, light updates, water and redstone next to you, and every step
 *       you take with a torch (dynamic lights) each froze the frame for a full section mesh, often
 *       a dozen sections at once. With {@link #threaded()} only player edits stay synchronous
 *       (so placed and broken blocks still appear instantly); everything else is queued on the
 *       worker threads, nearest first, like vanilla 1.18's "Threaded" chunk builder.</li>
 *   <li><b>Client light updates.</b> {@code renderLevel} runs every pending light update in one
 *       go. They are now processed in slices until {@link #LIGHT_BUDGET_NANOS} is used up; the rest
 *       continues next frame. Incoming light data is always applied in full (the budget only
 *       limits propagation steps), so nothing is lost or reordered.</li>
 * </ol>
 */
public final class NearBuild {
    /** Render-thread time per frame for client light propagation. */
    public static final long LIGHT_BUDGET_NANOS = 2_000_000L;
    /** Light updates per slice; small enough that one slice never blows the budget by much. */
    public static final int LIGHT_SLICE = 1024;

    private static volatile boolean threaded = true;
    private static volatile boolean lightBudget = true;

    private NearBuild() {
    }

    public static void configure(final AetheriumConfig config) {
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        final boolean on = active && config.threadedChunkBuilder.get().booleanValue();
        threaded = on;
        lightBudget = on;
    }

    /** Non-player dirty sections near the camera are rebuilt on worker threads. */
    public static boolean threaded() {
        return threaded;
    }

    public static boolean lightBudget() {
        return lightBudget;
    }

    /** For tests. */
    public static void set(final boolean threadedValue, final boolean lightBudgetValue) {
        threaded = threadedValue;
        lightBudget = lightBudgetValue;
    }

    /** One slice of light work; returns vanilla's result for that slice. */
    public interface LightStep {
        int run(int maxUpdates);
    }

    /** Whether light work is still pending. */
    public interface Pending {
        boolean any();
    }

    public interface Clock {
        long nanoTime();
    }

    private static final Clock SYSTEM = new Clock() {
        @Override
        public long nanoTime() {
            return System.nanoTime();
        }
    };

    public static int runLight(final LightStep step, final Pending pending) {
        return runLight(step, pending, SYSTEM, LIGHT_BUDGET_NANOS);
    }

    /** Runs at least one slice, then more while work is pending and the budget lasts. */
    public static int runLight(final LightStep step, final Pending pending, final Clock clock, final long budgetNanos) {
        final long start = clock.nanoTime();
        int result = step.run(LIGHT_SLICE);
        while (pending.any() && clock.nanoTime() - start < budgetNanos) {
            result = step.run(LIGHT_SLICE);
        }
        return result;
    }
}
