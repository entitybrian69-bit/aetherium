package com.aetherium.perf;

/**
 * Distance limit for block-entity rendering (chests, signs, banners, heads, shulker boxes...).
 * Vanilla draws them out to 64 blocks; every one costs a model draw per frame, and storage rooms
 * or sign-heavy builds are a common frame-time sink on weak GPUs.
 */
public final class BlockEntityCull {
    public static final int VANILLA = 64;

    /** Squared limit, or {@code Double.MAX_VALUE} while the limit is vanilla (no work per call). */
    private static volatile double limitSq = Double.MAX_VALUE;
    private static volatile double viewX;
    private static volatile double viewY;
    private static volatile double viewZ;

    private BlockEntityCull() {
    }

    public static void setDistance(final int blocks) {
        limitSq = blocks <= 0 || blocks >= VANILLA ? Double.MAX_VALUE : (double) blocks * blocks;
    }

    public static boolean active() {
        return limitSq != Double.MAX_VALUE;
    }

    /** Called once per frame with the camera position. */
    public static void setView(final double x, final double y, final double z) {
        viewX = x;
        viewY = y;
        viewZ = z;
    }

    /** @return true when the block at (x, y, z) is farther than the limit (measured to the block centre). */
    public static boolean beyond(final int x, final int y, final int z) {
        final double limit = limitSq;
        if (limit == Double.MAX_VALUE) {
            return false;
        }
        final double dx = x + 0.5 - viewX;
        final double dy = y + 0.5 - viewY;
        final double dz = z + 0.5 - viewZ;
        return dx * dx + dy * dy + dz * dz > limit;
    }
}
