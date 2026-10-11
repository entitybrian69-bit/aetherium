package com.aetherium.perf;

import java.util.Arrays;

/**
 * The vertex ranges to draw from one shared terrain buffer this layer ({@code TerrainRegions}).
 *
 * <p>Ranges arrive in visibility order, one or more per section. {@link #merge} sorts them by
 * position, joins ranges that touch or overlap, and also joins ranges separated by a small gap when
 * the gap is entirely live data ({@link ArenaAllocator#allocated}): drawing a few hundred extra
 * vertices of sections that are hidden or facing away costs far less than another draw call on a
 * GL translator, and never touches free (stale or uninitialised) memory. Finally every range longer
 * than {@code maxCount} is split: GL4ES splits large {@code GL_QUADS} draws itself but restarts the
 * pieces at vertex 0, which is only right for draws that start at 0.</p>
 *
 * <p>Packed as {@code first << 32 | count} so sorting is a primitive sort with no allocation.
 * Render thread only.</p>
 */
public final class DrawRanges {

    private long[] packed = new long[64];
    private int size;

    public void add(final int first, final int count) {
        if (count <= 0) {
            return;
        }
        if (this.size == this.packed.length) {
            this.packed = Arrays.copyOf(this.packed, this.size * 2);
        }
        this.packed[this.size++] = pack(first, count);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public int first(final int i) {
        return (int) (this.packed[i] >>> 32);
    }

    public int count(final int i) {
        return (int) this.packed[i];
    }

    public void clear() {
        this.size = 0;
    }

    /**
     * Sorts, merges and splits in place.
     *
     * @param maxGap   largest gap (vertices) drawn through to join two ranges
     * @param live     tells whether a gap is live data; null joins only touching ranges
     * @param maxCount largest vertex count of one range after splitting (a multiple of 4)
     * @return the new number of ranges
     */
    public int merge(final int maxGap, final ArenaAllocator live, final int maxCount) {
        if (this.size == 0) {
            return 0;
        }
        Arrays.sort(this.packed, 0, this.size);
        int out = 0;
        int curFirst = first(0);
        int curEnd = curFirst + count(0);
        for (int i = 1; i < this.size; i++) {
            final int f = first(i);
            final int e = f + count(i);
            final boolean join = f <= curEnd
                    || f - curEnd <= maxGap && live != null && live.allocated(curEnd, f);
            if (join) {
                if (e > curEnd) {
                    curEnd = e;
                }
            } else {
                this.packed[out++] = pack(curFirst, curEnd - curFirst);
                curFirst = f;
                curEnd = e;
            }
        }
        this.packed[out++] = pack(curFirst, curEnd - curFirst);
        this.size = out;
        split(maxCount);
        return this.size;
    }

    private void split(final int maxCount) {
        int extra = 0;
        for (int i = 0; i < this.size; i++) {
            final int c = count(i);
            if (c > maxCount) {
                extra += (c - 1) / maxCount;
            }
        }
        if (extra == 0) {
            return;
        }
        final int total = this.size + extra;
        if (total > this.packed.length) {
            this.packed = Arrays.copyOf(this.packed, Math.max(total, this.packed.length * 2));
        }
        // Back to front so nothing is overwritten before it is read.
        int w = total;
        for (int i = this.size - 1; i >= 0; i--) {
            final int f = first(i);
            final int c = count(i);
            final int pieces = c > maxCount ? (c + maxCount - 1) / maxCount : 1;
            for (int p = pieces - 1; p >= 0; p--) {
                final int pf = f + p * maxCount;
                final int pc = Math.min(maxCount, c - p * maxCount);
                this.packed[--w] = pack(pf, pc);
            }
        }
        this.size = total;
    }

    private static long pack(final int first, final int count) {
        return (long) first << 32 | count & 0xFFFFFFFFL;
    }
}
