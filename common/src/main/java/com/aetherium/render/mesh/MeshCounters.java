package com.aetherium.render.mesh;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counters for section-rebuild traffic on the vanilla render path.
 *
 * <p>They exist because the number a user actually needs when chunks flicker is not
 * "how fast is the mesher" but "how many sections did the game mark dirty and how many
 * did somebody rebuild". A renderer overlap bug (two mods meshing the same section each
 * frame) shows up here as {@code built} running far ahead of {@code dirty}, and that is
 * the one symptom the HUD can report without guessing.</p>
 *
 * <p>Thread-safety: written from the render thread only in practice, but read from
 * {@code AetheriumHudRenderer} and the benchmark harness, which can be a different thread
 * after a hot-swap. {@link AtomicLong} with plain increments is therefore the right
 * primitive: no locks in a per-section path, and a torn read is impossible.</p>
 */
public final class MeshCounters {
    private static final AtomicLong DIRTY_REQUESTS = new AtomicLong();
    private static final AtomicLong SECTIONS_COMPLETED = new AtomicLong();
    private static final AtomicLong FULL_REBUILDS = new AtomicLong();
    private static final AtomicLong BURST_FRAMES = new AtomicLong();

    /** Render-thread only, so a plain volatile is enough. */
    private static volatile int dirtyThisFrame;
    private static volatile long lastBurstWarnNanos;

    /** A burst = this many important rebuilds inside one frame. */
    private static final int BURST_THRESHOLD = 512;
    private static final long BURST_WARN_INTERVAL_NANOS = 5_000_000_000L;

    private MeshCounters() {
    }

    /**
     * Called once per {@code LevelRenderer#setSectionDirty}.
     *
     * @return true when this call completed a burst of important rebuilds and the caller
     *         should warn about it; the throttling lives here so the mixin has exactly one
     *         decision to make and no magic numbers of its own
     */
    public static boolean noteDirtyRequest(final boolean important) {
        DIRTY_REQUESTS.incrementAndGet();
        dirtyThisFrame = dirtyThisFrame + 1;
        if (important && dirtyThisFrame > BURST_THRESHOLD) {
            return noteBurst();
        }
        return false;
    }

    /** Called once per completed vanilla section build. */
    public static void noteSectionCompleted() {
        SECTIONS_COMPLETED.incrementAndGet();
    }

    /** Called from {@code LevelRenderer#allChanged} (render distance / graphics change). */
    public static void noteFullRebuild() {
        FULL_REBUILDS.incrementAndGet();
    }

    /** Called at the start of the render tick so the per-frame figure means what it says. */
    public static void beginFrame() {
        dirtyThisFrame = 0;
    }

    /** @return the number of dirty requests seen in the current frame */
    public static int getDirtyThisFrame() {
        return dirtyThisFrame;
    }

    public static long getDirtyRequests() {
        return DIRTY_REQUESTS.get();
    }

    public static long getSectionsCompleted() {
        return SECTIONS_COMPLETED.get();
    }

    public static long getFullRebuilds() {
        return FULL_REBUILDS.get();
    }

    public static long getBurstFrames() {
        return BURST_FRAMES.get();
    }

    /** Rate-limited gate for a piston-style rebuild storm. */
    private static boolean noteBurst() {
        final long now = System.nanoTime();
        if (now - lastBurstWarnNanos < BURST_WARN_INTERVAL_NANOS) {
            return false;
        }
        lastBurstWarnNanos = now;
        BURST_FRAMES.incrementAndGet();
        return true;
    }

    /** One HUD line, or empty when nothing has been observed yet. */
    public static String formatLine() {
        final long dirty = DIRTY_REQUESTS.get();
        if (dirty == 0L && FULL_REBUILDS.get() == 0L) {
            return "";
        }
        final long built = SECTIONS_COMPLETED.get();
        final String overlap = built > dirty * 2L + 64L
                ? String.format(Locale.ROOT, "  [overlap? built %d > dirty %d]", built, dirty)
                : "";
        return String.format(Locale.ROOT, "mesh  dirty %d  built %d  full %d  burst %d%s",
                dirty, built, FULL_REBUILDS.get(), BURST_FRAMES.get(), overlap);
    }

    /** Test and world-change hook: a stale counter across worlds reads like a leak. */
    public static void reset() {
        DIRTY_REQUESTS.set(0L);
        SECTIONS_COMPLETED.set(0L);
        FULL_REBUILDS.set(0L);
        BURST_FRAMES.set(0L);
        dirtyThisFrame = 0;
        lastBurstWarnNanos = 0L;
    }
}
