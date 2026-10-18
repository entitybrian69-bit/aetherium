package com.aetherium.client;

import java.nio.FloatBuffer;

/**
 * The matrices the current frame's terrain is drawn with, for far terrain on the versions where
 * the terrain draw itself does not receive them (1.21.6+: captured at the head of
 * {@code LevelRenderer.renderLevel}). Column-major. Render thread only.
 */
public final class FarTerrainFrame {
    private FarTerrainFrame() {
    }

    public static final float[] MODEL_VIEW = new float[16];
    public static final float[] PROJECTION = new float[16];
    private static final FloatBuffer MODEL_VIEW_BUFFER = FloatBuffer.wrap(MODEL_VIEW);
    private static final FloatBuffer PROJECTION_BUFFER = FloatBuffer.wrap(PROJECTION);
    private static boolean captured;

    /** Column-major views of the two arrays for {@code com.mojang.math.Matrix4f.store} (1.17 - 1.19.2). */
    public static FloatBuffer modelViewBuffer() {
        MODEL_VIEW_BUFFER.clear();
        return MODEL_VIEW_BUFFER;
    }

    public static FloatBuffer projectionBuffer() {
        PROJECTION_BUFFER.clear();
        return PROJECTION_BUFFER;
    }

    /** The arrays now hold this frame's matrices. */
    public static void captured() {
        captured = true;
    }

    public static boolean ready() {
        return captured;
    }
}
