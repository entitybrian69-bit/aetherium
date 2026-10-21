package com.aetherium.client;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): a closed chest, trapped chest or ender chest is
 * meshed into its chunk section like a normal block (one draw with the rest of the terrain)
 * instead of being drawn on its own every frame. The moment a chest starts to open, or a player
 * starts breaking it, its section is rebuilt without it and the chest is drawn the normal way
 * again (lid animation, breaking cracks); once it is fully closed and untouched it goes back into
 * the section.
 *
 * <p>The baked chest uses the same model, texture (stitched into the block atlas), light and
 * the same two-light shading the game applies to block entities, so it looks the same. Chests
 * whose renderer another mod replaced, chests with a missing texture and anything else are left
 * alone. Applies when the option is turned on or off (all sections rebuild).</p>
 *
 * <p>This class is the Minecraft-free part (state, shading); the meshing lives in
 * {@code TuningChestBakeMixin}.</p>
 */
public final class ChestBaking {
    /** Light directions of block-entity shading, normalised (world space). */
    private static final float L0X;
    private static final float L0Y;
    private static final float L0Z;
    private static final float L1X;
    private static final float L1Y;
    private static final float L1Z;

    static {
        final float l0 = (float) Math.sqrt(0.2 * 0.2 + 1.0 + 0.7 * 0.7);
        L0X = 0.2f / l0;
        L0Y = 1.0f / l0;
        L0Z = -0.7f / l0;
        L1X = -0.2f / l0;
        L1Y = 1.0f / l0;
        L1Z = 0.7f / l0;
    }

    private static volatile boolean enabled;
    private static volatile boolean failed;
    private static boolean applied;
    /** Breaker id to packed block position of every block being broken right now. */
    private static final Map<Integer, Long> DESTROYING = new ConcurrentHashMap<Integer, Long>();
    /** One chest model per mesh worker: the model's lid angle is shared state. */
    private static final ThreadLocal<Object> MODELS = new ThreadLocal<Object>();

    private ChestBaking() {
    }

    /**
     * Once per frame (render thread). Returns whether the effective state changed, in which case
     * every section is rebuilt.
     */
    public static boolean configure(final boolean wanted) {
        final boolean now = wanted && !failed;
        enabled = now;
        if (now == applied) {
            return false;
        }
        applied = now;
        return true;
    }

    /** Mesh workers: whether chests are baked into sections. */
    public static boolean enabled() {
        return enabled;
    }

    private static final AtomicLong BAKED = new AtomicLong();

    /** Counts a chest drawn into its section's mesh (shown by the benchmark). */
    public static void countBake() {
        BAKED.incrementAndGet();
    }

    /** Chests drawn into section meshes since the game started. */
    public static long baked() {
        return BAKED.get();
    }

    /** A bake threw: chests are drawn the normal way for the rest of the session. */
    public static void fail() {
        failed = true;
        enabled = false;
    }

    public static boolean failed() {
        return failed;
    }

    /** The calling worker's chest model, or null before {@link #setModel}. */
    public static Object model() {
        return MODELS.get();
    }

    public static void setModel(final Object model) {
        MODELS.set(model);
    }

    /**
     * Records a breaking-progress update. Returns the position that stopped being broken by this
     * breaker (to rebuild), or {@code Long.MIN_VALUE}.
     */
    public static long destroyProgress(final int breaker, final long pos, final int progress) {
        final Integer key = Integer.valueOf(breaker);
        final Long old = progress >= 0 && progress < 10 ? DESTROYING.put(key, Long.valueOf(pos)) : DESTROYING.remove(key);
        return old == null || old.longValue() == pos && progress >= 0 && progress < 10 ? Long.MIN_VALUE : old.longValue();
    }

    /** Whether someone is breaking the block at {@code pos}. */
    public static boolean destroying(final long pos) {
        if (DESTROYING.isEmpty()) {
            return false;
        }
        final Iterator<Long> it = DESTROYING.values().iterator();
        while (it.hasNext()) {
            if (it.next().longValue() == pos) {
                return true;
            }
        }
        return false;
    }

    /** New world. */
    public static void clearDestroying() {
        DESTROYING.clear();
    }

    /**
     * Brightness factor of block-entity shading for a face with normal {@code (nx, ny, nz)}: 0.4
     * ambient plus 0.6 per light facing it, at most 1.
     */
    public static float shade(final float nx, final float ny, final float nz) {
        final float d0 = Math.max(0f, nx * L0X + ny * L0Y + nz * L0Z);
        final float d1 = Math.max(0f, nx * L1X + ny * L1Y + nz * L1Z);
        return Math.min(1f, 0.4f + 0.6f * (d0 + d1));
    }

    /** Whether a chest with these lid values is closed and still (bakeable). */
    public static boolean closed(final float openness, final float previousOpenness, final int openCount) {
        return openCount <= 0 && openness <= 0f && previousOpenness <= 0f;
    }

    /** Tests. */
    static void resetForTests() {
        enabled = false;
        failed = false;
        applied = false;
        DESTROYING.clear();
    }
}
