package com.aetherium.client;

import java.nio.FloatBuffer;

/**
 * The matrices and camera position the current frame's terrain is drawn with, for far terrain on
 * the versions where the terrain draw itself does not receive them: 1.21.6 - 1.21.11 capture them
 * at the head of {@code LevelRenderer.renderLevel}; 26.x takes the projection (with view bobbing)
 * on its way into {@code ProjectionMatrixBuffer} and the rest from the {@code CameraRenderState}.
 * Column-major. Render thread only.
 */
public final class FarTerrainFrame {
    private FarTerrainFrame() {
    }

    public static final float[] MODEL_VIEW = new float[16];
    public static final float[] PROJECTION = new float[16];
    private static final FloatBuffer MODEL_VIEW_BUFFER = FloatBuffer.wrap(MODEL_VIEW);
    private static final FloatBuffer PROJECTION_BUFFER = FloatBuffer.wrap(PROJECTION);
    private static boolean captured;
    private static double camX;
    private static double camY;
    private static double camZ;

    /** Column-major views of the two arrays for {@code com.mojang.math.Matrix4f.store} (1.17 - 1.19.2). */
    public static FloatBuffer modelViewBuffer() {
        MODEL_VIEW_BUFFER.clear();
        return MODEL_VIEW_BUFFER;
    }

    public static FloatBuffer projectionBuffer() {
        PROJECTION_BUFFER.clear();
        return PROJECTION_BUFFER;
    }

    /** The arrays now hold this frame's matrices; the camera is at {@code x, y, z}. */
    public static void captured(final double x, final double y, final double z) {
        camX = x;
        camY = y;
        camZ = z;
        captured = true;
    }

    public static double camX() {
        return camX;
    }

    public static double camY() {
        return camY;
    }

    public static double camZ() {
        return camZ;
    }

    public static boolean ready() {
        return captured;
    }
}
