package com.aetherium.perf;

/**
 * Skips chunk sections that lie entirely past the end of linear fog: they would be drawn in solid
 * fog colour, and the visible-section search no longer walks through them. Decided once per frame
 * from the fog the game actually set this frame, so fog changes by other mods (or by Aetherium's
 * own fog options) are respected; exponential fog (underwater) and frames without a fresh fog end
 * cull nothing. Fog pushed past the render distance (fog options, no-fog mods) culls nothing either:
 * the far corners of the loaded square are then really visible. Render thread only.
 */
public final class FogCull {

    static final int GL_LINEAR = 0x2601;
    /**
     * Blocks of slack past the fog end: a culled section is not meshed either, so sections are let
     * in one section early to have their mesh ready before they could show through the fog.
     */
    static final float MARGIN = 16.0f;

    private static boolean active;
    private static float limit;
    private static long culled;

    private FogCull() {
    }

    /**
     * @param toggle           the option is on
     * @param fogSetThisFrame  the fog end was written since the frame started
     * @param mode             GL fog mode
     * @param renderDistance   render distance in blocks
     */
    public static void begin(final boolean toggle, final boolean fogSetThisFrame, final int mode, final float start,
                             final float end, final float renderDistance, final ViewFrustum frame) {
        active = toggle && fogSetThisFrame && mode == GL_LINEAR && end > start && end > 0.0f
                && end <= renderDistance && frame.valid();
        limit = end + MARGIN;
    }

    public static boolean active() {
        return active;
    }

    /** A box (camera-relative centre, half sizes) wholly past the fog end. */
    public static boolean hidden(final ViewFrustum frame, final float cx, final float cy, final float cz,
                                 final float hx, final float hy, final float hz) {
        if (!active) {
            return false;
        }
        if (frame.nearestDepth(cx, cy, cz, hx, hy, hz) > limit) {
            culled++;
            return true;
        }
        return false;
    }

    /** Sections culled since start (diagnostics). */
    public static long culled() {
        return culled;
    }

    static void resetForTests() {
        active = false;
        limit = 0.0f;
        culled = 0;
    }
}
