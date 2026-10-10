package com.aetherium.lighting;

/**
 * The dynamic-light field, read by the chunk mesher and the entity renderer.
 *
 * <p>This class is deliberately free of Minecraft types so it is unit-testable
 * and identical in every version delta. The tracker builds a fresh immutable
 * {@link Source} array on the client thread and publishes it with one volatile
 * write; mesher worker threads read the array reference once per lookup and
 * never see a half-built state. With no sources the hot path is a single
 * volatile read and a length check, so the hook costs nothing while the
 * feature is idle.</p>
 */
public final class LightField {
    /** Shared empty snapshot; published whenever the feature is off. */
    private static final Source[] NONE = new Source[0];

    private static volatile Source[] sources = NONE;

    private LightField() {
    }

    /** One emitter. Immutable; coordinates are world-space block centres. */
    public static final class Source {
        public final double x;
        public final double y;
        public final double z;
        public final int level;
        final int minX;
        final int minY;
        final int minZ;
        final int maxX;
        final int maxY;
        final int maxZ;

        public Source(final double x, final double y, final double z, final int level) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.level = Math.max(0, Math.min(15, level));
            final int reach = this.level;
            this.minX = (int) Math.floor(x) - reach;
            this.minY = (int) Math.floor(y) - reach;
            this.minZ = (int) Math.floor(z) - reach;
            this.maxX = (int) Math.floor(x) + reach;
            this.maxY = (int) Math.floor(y) + reach;
            this.maxZ = (int) Math.floor(z) + reach;
        }

        /** Light this source contributes at block (bx, by, bz), 0..15. */
        public int lightAt(final int bx, final int by, final int bz) {
            if (bx < this.minX || bx > this.maxX || by < this.minY || by > this.maxY || bz < this.minZ || bz > this.maxZ) {
                return 0;
            }
            final double dx = bx + 0.5 - this.x;
            final double dy = by + 0.5 - this.y;
            final double dz = bz + 0.5 - this.z;
            final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            final int value = (int) (this.level - distance + 0.5);
            return value <= 0 ? 0 : Math.min(15, value);
        }

        int blockX() {
            return (int) Math.floor(this.x);
        }

        int blockY() {
            return (int) Math.floor(this.y);
        }

        int blockZ() {
            return (int) Math.floor(this.z);
        }
    }

    /** Publishes a new snapshot. The array must not be mutated afterwards. */
    public static void publish(final Source[] snapshot) {
        sources = snapshot == null || snapshot.length == 0 ? NONE : snapshot;
    }

    public static void clear() {
        sources = NONE;
    }

    public static boolean isEmpty() {
        return sources.length == 0;
    }

    public static Source[] snapshot() {
        return sources;
    }

    /** Highest dynamic block light at a block position, 0..15. */
    public static int blockLightAt(final int x, final int y, final int z) {
        final Source[] local = sources;
        if (local.length == 0) {
            return 0;
        }
        int best = 0;
        for (final Source source : local) {
            final int value = source.lightAt(x, y, z);
            if (value > best) {
                best = value;
                if (best >= 15) {
                    break;
                }
            }
        }
        return best;
    }

    /**
     * Raises the block-light nibble of a packed light value ({@code block << 4 | sky << 20})
     * to the dynamic light at that position. Values are never lowered.
     */
    public static int adjustPacked(final int packed, final int x, final int y, final int z) {
        final Source[] local = sources;
        if (local.length == 0) {
            return packed;
        }
        final int block = (packed & 0xFFFF) >> 4;
        if (block >= 15) {
            return packed;
        }
        final int dynamic = blockLightAt(x, y, z);
        if (dynamic <= block) {
            return packed;
        }
        return (packed & 0xFFFF0000) | (dynamic << 4);
    }

    /** Raises an unpacked 0..15 block-light value (entity renderer path). */
    public static int adjustLevel(final int level, final int x, final int y, final int z) {
        if (level >= 15 || sources.length == 0) {
            return level;
        }
        return Math.max(level, blockLightAt(x, y, z));
    }
}
