package com.aetherium.perf;

/**
 * Merges the repeated rebuilds a chunk section gets while terrain streams in.
 *
 * <p>When a chunk arrives, vanilla marks its sections and every neighbouring section dirty;
 * the neighbours' own chunk and light packets then mark them dirty again. Without help a section
 * at the edge of loaded terrain is meshed several times within a fraction of a second, and every
 * mesh costs a worker build plus a render-thread upload. A section that was dirtied recently and
 * is not near the player, or has never been built at all, now waits until it has been quiet for
 * {@link #QUIET_NANOS} (never longer than {@link #MAX_DELAY_NANOS} in total), and is then built
 * once.</p>
 *
 * <p>Never deferred: sections changed by the player (block placing and breaking stays
 * instant) and already-built sections within {@link #NEAR_BLOCKS} blocks.</p>
 */
public final class RebuildCoalescer {
    public static final long QUIET_NANOS = 120_000_000L;
    public static final long MAX_DELAY_NANOS = 600_000_000L;
    public static final double NEAR_BLOCKS = 28.0;
    public static final double NEAR_SQ = NEAR_BLOCKS * NEAR_BLOCKS;
    /** Lazy far rebuilds (1.11.0): built sections at least this far away wait longer. */
    public static final double LAZY_FAR_BLOCKS = 80.0;
    public static final double LAZY_FAR_SQ = LAZY_FAR_BLOCKS * LAZY_FAR_BLOCKS;
    /** How long a far, already-built section collects changes before it is rebuilt once. */
    public static final long LAZY_FAR_NANOS = 1_000_000_000L;

    private static volatile boolean enabled = true;
    private static volatile boolean lazyFar;
    private static int deferredThisFrame;

    private RebuildCoalescer() {
    }

    public static void setEnabled(final boolean on) {
        enabled = on;
    }

    public static boolean enabled() {
        return enabled;
    }

    /**
     * Lazy far rebuilds (Minecraft 1.16.5, 1.11.0): an already-built section at least
     * {@link #LAZY_FAR_BLOCKS} blocks away keeps its old mesh for up to {@link #LAZY_FAR_NANOS}
     * after its first change, so light flicker, growing crops and neighbouring chunk arrivals far
     * away cost one rebuild per second at most. Only with coalescing on; player edits stay instant.
     */
    public static void setLazyFar(final boolean on) {
        lazyFar = on;
    }

    /**
     * @param now        current {@code System.nanoTime()}
     * @param firstDirty when the section first became dirty since its last build (0 unknown)
     * @param lastDirty  when it was last marked dirty
     * @param player     dirtied by the player's own block change
     * @param compiled   the section has been built before
     * @param distSq     squared distance from the camera to the section centre
     * @return true to leave the section for a later frame
     */
    public static boolean defer(final long now, final long firstDirty, final long lastDirty, final boolean player,
                                final boolean compiled, final double distSq) {
        if (!enabled || player || firstDirty == 0L) {
            return false;
        }
        if (compiled && distSq < NEAR_SQ) {
            return false;
        }
        if (lazyFar && compiled && distSq >= LAZY_FAR_SQ && now - firstDirty < LAZY_FAR_NANOS) {
            deferredThisFrame++;
            return true;
        }
        if (now - firstDirty >= MAX_DELAY_NANOS) {
            return false;
        }
        if (now - lastDirty < QUIET_NANOS) {
            deferredThisFrame++;
            return true;
        }
        return false;
    }

    /** Sections deferred since the last call (render thread, per frame). */
    public static int takeDeferred() {
        final int n = deferredThisFrame;
        deferredThisFrame = 0;
        return n;
    }
}
