package com.aetherium.perf;

/**
 * Mob model detail levels (1.11.0, Minecraft 1.16.5). While a mob farther than the entity detail
 * distance is drawn, model parts whose largest box side is at most {@link #NEAR_SKIP} pixels (ears,
 * noses, beaks, whiskers) are not drawn; beyond twice the distance, parts up to {@link #FAR_SKIP}
 * pixels are skipped too. At that range such a part covers a pixel or two, while every part still
 * costs a matrix transform and six faces. Players and armor stands always keep every part.
 *
 * <p>Render thread only: set at {@code LivingEntityRenderer.render} HEAD, cleared at RETURN, read
 * by {@code ModelPart.compile}.</p>
 */
public final class ModelLod {
    public static final float NEAR_SKIP = 2.0F;
    public static final float FAR_SKIP = 3.0F;

    /** Largest part size (pixels) skipped right now; 0 = draw everything. */
    private static float skipUpTo;

    private ModelLod() {
    }

    /** Before a mob is drawn. */
    public static void begin(final double distanceSq, final boolean exempt) {
        final double detail = Tuning.detailDistanceSq;
        if (!Tuning.modelLod || exempt || detail == Double.MAX_VALUE || distanceSq <= detail) {
            skipUpTo = 0.0F;
            return;
        }
        skipUpTo = distanceSq > 4.0 * detail ? FAR_SKIP : NEAR_SKIP;
    }

    /** After the mob was drawn (also block entities and items use model parts). */
    public static void end() {
        skipUpTo = 0.0F;
    }

    public static boolean active() {
        return skipUpTo > 0.0F;
    }

    /** True when a part whose largest box side is {@code size} pixels is skipped right now. */
    public static boolean skip(final float size) {
        return size <= skipUpTo && skipUpTo > 0.0F;
    }
}
