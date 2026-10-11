package com.aetherium.perf;

import java.nio.FloatBuffer;

/**
 * The camera's view volume for one frame, for cheap visibility tests on the render thread:
 * particles (a sphere test against the side and near planes) and terrain sections entirely past
 * the fog (a depth test along the view direction).
 *
 * <p>Built from vanilla's own projection (bobbing included) and camera rotation, the matrices
 * {@code LevelRenderer.renderLevel} receives, so it matches what is drawn. Positions are relative
 * to the camera, as particles and sections are drawn. The far plane is not used: vanilla puts it
 * at four times the render distance.</p>
 */
public final class ViewFrustum {

    /** The frame being drawn (render thread). */
    public static final ViewFrustum FRAME = new ViewFrustum();

    private static final int PLANES = 5;

    /** a, b, c, d of left, right, bottom, top, near; (a, b, c) normalised. */
    private final float[] planes = new float[PLANES * 4];
    private final float[] m = new float[16];
    /** Row 2 of the camera rotation: view-space z of a direction (the camera looks along -z). */
    private float fx;
    private float fy;
    private float fz;
    private boolean valid;

    /**
     * @param projection column-major 4 x 4 (index column * 4 + row), as {@code Matrix4f.store} writes
     * @param view       camera rotation, same layout
     */
    public void update(final FloatBuffer projection, final FloatBuffer view) {
        final float[] mm = this.m;
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                float s = 0f;
                for (int k = 0; k < 4; k++) {
                    s += projection.get(k * 4 + r) * view.get(c * 4 + k);
                }
                mm[c * 4 + r] = s;
            }
        }
        boolean ok = true;
        ok &= this.plane(0, 0, 1f);  // left:   row3 + row0
        ok &= this.plane(1, 0, -1f); // right:  row3 - row0
        ok &= this.plane(2, 1, 1f);  // bottom: row3 + row1
        ok &= this.plane(3, 1, -1f); // top:    row3 - row1
        ok &= this.plane(4, 2, 1f);  // near:   row3 + row2
        this.fx = view.get(2);
        this.fy = view.get(6);
        this.fz = view.get(10);
        this.valid = ok;
    }

    private boolean plane(final int index, final int row, final float sign) {
        final float[] mm = this.m;
        final float a = mm[3] + sign * mm[row];
        final float b = mm[7] + sign * mm[4 + row];
        final float c = mm[11] + sign * mm[8 + row];
        final float d = mm[15] + sign * mm[12 + row];
        final float length = (float) Math.sqrt(a * a + b * b + c * c);
        if (!(length > 1.0E-6f)) {
            return false;
        }
        final int at = index * 4;
        this.planes[at] = a / length;
        this.planes[at + 1] = b / length;
        this.planes[at + 2] = c / length;
        this.planes[at + 3] = d / length;
        return true;
    }

    public boolean valid() {
        return this.valid;
    }

    /** Forget the frame (no visibility test rejects anything until the next {@link #update}). */
    public void invalidate() {
        this.valid = false;
    }

    /** Whether a sphere (camera-relative centre) can be on screen. True when no frame is known. */
    public boolean sphereVisible(final float x, final float y, final float z, final float radius) {
        if (!this.valid) {
            return true;
        }
        final float[] p = this.planes;
        for (int i = 0; i < PLANES * 4; i += 4) {
            if (p[i] * x + p[i + 1] * y + p[i + 2] * z + p[i + 3] < -radius) {
                return false;
            }
        }
        return true;
    }

    /**
     * Nearest depth (distance along the view direction) of a box, camera-relative centre and half
     * sizes. Depth is at most the straight-line distance, so a box whose nearest depth is past the
     * fog end is fully fogged whether the fog is measured by depth or by distance.
     */
    public float nearestDepth(final float cx, final float cy, final float cz, final float hx, final float hy,
                              final float hz) {
        final float centre = -(this.fx * cx + this.fy * cy + this.fz * cz);
        return centre - (Math.abs(this.fx) * hx + Math.abs(this.fy) * hy + Math.abs(this.fz) * hz);
    }
}
