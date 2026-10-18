package com.aetherium.perf;

/**
 * Visual entity merging for item piles and mob farms (1.10.0).
 *
 * <p>Counts the entities that are about to render in each one-block cell this frame. Once a cell
 * holds more than its cap (items: {@link #ITEM_CAP}, mobs: {@link #MOB_CAP}), further entities in
 * that cell are not drawn: a pile of 300 dropped cobblestone stacks or a mob farm's kill chamber
 * looks the same with a few of them drawn, at a fraction of the cost. Nothing is removed from the
 * world, ticked differently or hidden from hit tests; it is purely what gets drawn.</p>
 *
 * <p>Allocation-free: an open-addressing table keyed by the packed cell, reset per frame by a
 * stamp instead of clearing. A full probe chain lets the entity render (never hides by mistake).</p>
 */
public final class CrowdCull {
    public static final int ITEM_CAP = 4;
    public static final int MOB_CAP = 6;
    private static final int SIZE = 4096; // power of two
    private static final int MASK = SIZE - 1;
    private static final int PROBES = 16;

    private static final long[] KEYS = new long[SIZE];
    private static final int[] COUNTS = new int[SIZE];
    private static final int[] STAMPS = new int[SIZE];
    private static int stamp = 1;

    private CrowdCull() {
    }

    /** Render thread, once per frame. */
    public static void newFrame() {
        stamp++;
        if (stamp == 0) {
            java.util.Arrays.fill(STAMPS, 0);
            stamp = 1;
        }
    }

    /**
     * Render thread. Counts one entity in the cell at (x, y, z).
     *
     * @param mob true for mobs (cap {@link #MOB_CAP}), false for item entities (cap {@link #ITEM_CAP})
     * @return true when the cell is already full and this entity should not be drawn
     */
    public static boolean crowded(final double x, final double y, final double z, final boolean mob) {
        final int bx = (int) Math.floor(x);
        final int by = (int) Math.floor(y);
        final int bz = (int) Math.floor(z);
        // Items and mobs are counted in separate cells (bit 63), so a mob farm's drops never hide its mobs.
        final long key = ((long) (bx & 0x3FFFFFF) << 38) | ((long) (bz & 0x3FFFFFF) << 12) | (by & 0xFFF)
                | (mob ? Long.MIN_VALUE : 0L);
        int slot = (int) (mix(key) & MASK);
        for (int i = 0; i < PROBES; i++, slot = (slot + 1) & MASK) {
            if (STAMPS[slot] != stamp) {
                STAMPS[slot] = stamp;
                KEYS[slot] = key;
                COUNTS[slot] = 1;
                return false;
            }
            if (KEYS[slot] == key) {
                return ++COUNTS[slot] > (mob ? MOB_CAP : ITEM_CAP);
            }
        }
        return false;
    }

    private static long mix(long h) {
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return h;
    }
}
