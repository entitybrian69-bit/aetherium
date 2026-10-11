package com.aetherium.perf;

/**
 * Free-list allocator over {@code [0, capacity)} for one shared terrain buffer
 * ({@code TerrainRegions}). Units are vertices; every block starts and ends on a multiple of
 * {@link #ALIGN} so a draw always starts on a quad boundary.
 *
 * <p>Best fit (the smallest free block that holds the request) keeps large holes for large
 * sections; freeing merges a block with its free neighbours. Free blocks are kept sorted by start,
 * which also answers {@link #allocated} with a binary search. Not thread-safe: render thread only.</p>
 */
public final class ArenaAllocator {

    /** Quads: four vertices. */
    public static final int ALIGN = 4;

    private final int capacity;
    private int[] starts = new int[8];
    private int[] sizes = new int[8];
    private int count;
    private int used;

    public ArenaAllocator(final int capacity) {
        if (capacity <= 0 || capacity % ALIGN != 0) {
            throw new IllegalArgumentException("capacity " + capacity);
        }
        this.capacity = capacity;
        this.starts[0] = 0;
        this.sizes[0] = capacity;
        this.count = 1;
    }

    public static int align(final int size) {
        return (size + ALIGN - 1) & -ALIGN;
    }

    public int capacity() {
        return this.capacity;
    }

    public int used() {
        return this.used;
    }

    public boolean isEmpty() {
        return this.used == 0;
    }

    /** Number of free blocks (for tests and the status line). */
    public int freeBlocks() {
        return this.count;
    }

    /**
     * @return the first vertex of a block of {@code align(size)} vertices, or -1 when no free block
     *         is large enough
     */
    public int allocate(final int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size " + size);
        }
        final int need = align(size);
        int best = -1;
        for (int i = 0; i < this.count; i++) {
            final int s = this.sizes[i];
            if (s >= need && (best < 0 || s < this.sizes[best])) {
                best = i;
                if (s == need) {
                    break;
                }
            }
        }
        if (best < 0) {
            return -1;
        }
        final int start = this.starts[best];
        if (this.sizes[best] == need) {
            remove(best);
        } else {
            this.starts[best] += need;
            this.sizes[best] -= need;
        }
        this.used += need;
        return start;
    }

    /** Returns a block from {@link #allocate} ({@code size} is the size that was asked for). */
    public void free(final int start, final int size) {
        final int length = align(size);
        if (start < 0 || length <= 0 || start + length > this.capacity) {
            throw new IllegalArgumentException("free " + start + "+" + length + " of " + this.capacity);
        }
        // Insertion point: first free block starting after this one.
        int i = 0;
        while (i < this.count && this.starts[i] < start) {
            i++;
        }
        if (i > 0 && this.starts[i - 1] + this.sizes[i - 1] > start
                || i < this.count && start + length > this.starts[i]) {
            throw new IllegalStateException("double free " + start + "+" + length);
        }
        final boolean joinLeft = i > 0 && this.starts[i - 1] + this.sizes[i - 1] == start;
        final boolean joinRight = i < this.count && start + length == this.starts[i];
        if (joinLeft && joinRight) {
            this.sizes[i - 1] += length + this.sizes[i];
            remove(i);
        } else if (joinLeft) {
            this.sizes[i - 1] += length;
        } else if (joinRight) {
            this.starts[i] = start;
            this.sizes[i] += length;
        } else {
            insert(i, start, length);
        }
        this.used -= length;
    }

    /**
     * Whether every vertex in {@code [start, end)} belongs to an allocated block: drawing through
     * such a gap only draws other live sections, never stale or uninitialised memory.
     */
    public boolean allocated(final int start, final int end) {
        if (start >= end) {
            return true;
        }
        if (start < 0 || end > this.capacity) {
            return false;
        }
        // Last free block starting before end.
        int lo = 0;
        int hi = this.count - 1;
        int found = -1;
        while (lo <= hi) {
            final int mid = (lo + hi) >>> 1;
            if (this.starts[mid] < end) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return found < 0 || this.starts[found] + this.sizes[found] <= start;
    }

    private void remove(final int i) {
        System.arraycopy(this.starts, i + 1, this.starts, i, this.count - i - 1);
        System.arraycopy(this.sizes, i + 1, this.sizes, i, this.count - i - 1);
        this.count--;
    }

    private void insert(final int i, final int start, final int size) {
        if (this.count == this.starts.length) {
            final int[] s = new int[this.count * 2];
            final int[] z = new int[this.count * 2];
            System.arraycopy(this.starts, 0, s, 0, this.count);
            System.arraycopy(this.sizes, 0, z, 0, this.count);
            this.starts = s;
            this.sizes = z;
        }
        System.arraycopy(this.starts, i, this.starts, i + 1, this.count - i);
        System.arraycopy(this.sizes, i, this.sizes, i + 1, this.count - i);
        this.starts[i] = start;
        this.sizes[i] = size;
        this.count++;
    }
}
