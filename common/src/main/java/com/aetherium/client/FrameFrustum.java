package com.aetherium.client;

import com.aetherium.perf.ViewFrustum;

import java.nio.FloatBuffer;

/**
 * Feeds {@link ViewFrustum#FRAME} from the matrices vanilla builds its culling frustum from, on
 * the versions where {@code StreamingLevelMixin} does not (1.17..1.21.11).
 *
 * <p>{@code LevelRenderer.prepareCullFrustum} arms the capture; the next
 * {@code Frustum.calculateFrustum(view, projection)} on the render thread (the one vanilla's
 * method makes) takes it. Frustums other code builds at other times are ignored, so a minimap or
 * shader mod constructing its own cannot replace the camera's.</p>
 *
 * <p>Render thread only.</p>
 */
public final class FrameFrustum {

    private static final float[] VIEW = new float[16];
    private static final float[] PROJECTION = new float[16];
    private static final FloatBuffer VIEW_BUFFER = FloatBuffer.wrap(VIEW);
    private static final FloatBuffer PROJECTION_BUFFER = FloatBuffer.wrap(PROJECTION);

    private static boolean armed;

    private FrameFrustum() {
    }

    /** Called at the head of vanilla's frustum preparation. */
    public static void arm() {
        armed = true;
    }

    /** Whether the frustum being built now is the camera's; consumes the arm. */
    public static boolean take() {
        if (!armed) {
            return false;
        }
        armed = false;
        return true;
    }

    /** Column-major scratch arrays the caller fills before {@link #publish()}. */
    public static float[] view() {
        return VIEW;
    }

    public static float[] projection() {
        return PROJECTION;
    }

    public static FloatBuffer viewBuffer() {
        VIEW_BUFFER.clear();
        return VIEW_BUFFER;
    }

    public static FloatBuffer projectionBuffer() {
        PROJECTION_BUFFER.clear();
        return PROJECTION_BUFFER;
    }

    public static void publish() {
        VIEW_BUFFER.clear();
        PROJECTION_BUFFER.clear();
        ViewFrustum.FRAME.update(PROJECTION_BUFFER, VIEW_BUFFER);
    }
}
