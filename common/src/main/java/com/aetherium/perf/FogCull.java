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

    // ------------------------------------------------------------------ distance fog (1.17+)
    //
    // Shader fog (1.17+) fades by distance, not view depth: spherical, or cylindrical
    // (max(|xz|, |y|)) for terrain from 1.18.2. The cylindrical distance is never larger than the
    // spherical one, so "nearest cylindrical distance past the limit" is safe for both shapes.
    // A fragment is solid fog colour past max(start, end) of a linear range with opaque colour.

    static final float DIST_MARGIN = 4.0f;
    private static final float NONE = Float.POSITIVE_INFINITY;

    private static float frameLimit = NONE;
    private static boolean frameSeen;
    private static float lastLimit = NONE;
    private static float usedLimit = NONE;

    /** Terrain fog written by vanilla this frame (one linear range). */
    public static void recordFogRange(final float start, final float end, final float alpha) {
        merge(alpha >= 0.999f ? rangeLimit(start, end) : NONE);
    }

    /** Terrain fog written this frame (1.21.6+: environmental and render-distance ranges, both applied). */
    public static void recordFogRanges(final float envStart, final float envEnd, final float rdStart,
                                       final float rdEnd) {
        merge(Math.min(rangeLimit(envStart, envEnd), rangeLimit(rdStart, rdEnd)));
    }

    private static float rangeLimit(final float start, final float end) {
        final float l = Math.max(start, end);
        return l > 0.0f && l < 1.0e7f ? l : NONE;
    }

    /** Several terrain-fog writes in one frame: keep the farthest, so nothing is culled nearer than any of them. */
    private static void merge(final float limit) {
        frameLimit = frameSeen ? Math.max(frameLimit, limit) : limit;
        frameSeen = true;
    }

    /** Frame start: the fog of the frame just finished becomes the limit; no fog seen means no limit. */
    public static void frame() {
        lastLimit = frameSeen ? frameLimit : NONE;
        frameSeen = false;
        frameLimit = NONE;
    }

    /**
     * Whether the visible list should be refiltered. Growth up to {@link #DIST_MARGIN} needs no
     * refilter: everything culled lay past the old limit plus the margin, so it is still solid fog
     * (1.17 refills the list with a whole visibility search, and underwater fog creeps for
     * seconds). Shrinking only costs draws, so it waits for a real change.
     */
    public static boolean distanceStale(final boolean toggle) {
        final float want = toggle ? lastLimit : NONE;
        if (want == usedLimit) {
            return false;
        }
        if (want == NONE || usedLimit == NONE) {
            return true;
        }
        return want > usedLimit + DIST_MARGIN * 0.75f || want < usedLimit - 16.0f;
    }

    /** Before a visible-list filter: fixes the limit it uses; false when nothing can be culled. */
    public static boolean beginDistance(final boolean toggle) {
        usedLimit = toggle ? lastLimit : NONE;
        return usedLimit != NONE;
    }

    /** A box (camera-relative min and max corners) wholly past the fog limit fixed by {@link #beginDistance}. */
    public static boolean distanceHidden(final float x0, final float y0, final float z0, final float x1,
                                         final float y1, final float z1) {
        if (usedLimit == NONE) {
            return false;
        }
        final float nx = x0 > 0.0f ? x0 : (x1 < 0.0f ? -x1 : 0.0f);
        final float ny = y0 > 0.0f ? y0 : (y1 < 0.0f ? -y1 : 0.0f);
        final float nz = z0 > 0.0f ? z0 : (z1 < 0.0f ? -z1 : 0.0f);
        final float limit = usedLimit + DIST_MARGIN;
        if (ny > limit || nx * nx + nz * nz > limit * limit) {
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
        frameLimit = NONE;
        frameSeen = false;
        lastLimit = NONE;
        usedLimit = NONE;
        active = false;
        limit = 0.0f;
        culled = 0;
    }
}
