package com.aetherium.qol;

/**
 * World point to GUI pixel, for the Quality of life overlays that are drawn on the HUD instead of
 * in the world (waypoint labels and beams, damage numbers) on Minecraft versions whose world
 * renderer is not hooked. Works from the camera alone: position, yaw and pitch in Minecraft's
 * convention (yaw 0 looks along +Z and grows clockwise seen from above, pitch grows looking down)
 * and the vertical field of view, so it needs nothing version specific. One instance per frame,
 * reused: {@link #setCamera} once, then {@link #project} per point (no allocation).
 */
public final class ScreenProjector {
    /** Points closer than this in front of the camera (blocks) count as behind it. */
    public static final double NEAR = 0.05;

    private double camX;
    private double camY;
    private double camZ;
    private double rightX;
    private double rightZ;
    private double upX;
    private double upY;
    private double upZ;
    private double forwardX;
    private double forwardY;
    private double forwardZ;
    private double scaleX;
    private double scaleY;
    private double width;
    private double height;

    /** Result of the last {@link #project}: GUI pixels, and the depth in front of the camera. */
    public double x;
    public double y;
    public double depth;

    /**
     * @param yawDegrees   camera yaw (Minecraft convention)
     * @param pitchDegrees camera pitch, positive looking down
     * @param fovDegrees   vertical field of view
     * @param guiWidth     GUI-scaled screen width
     * @param guiHeight    GUI-scaled screen height
     */
    public void setCamera(final double camX, final double camY, final double camZ, final double yawDegrees,
                          final double pitchDegrees, final double fovDegrees, final double guiWidth,
                          final double guiHeight) {
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        final double yaw = Math.toRadians(yawDegrees);
        final double pitch = Math.toRadians(pitchDegrees);
        final double sy = Math.sin(yaw);
        final double cy = Math.cos(yaw);
        final double sp = Math.sin(pitch);
        final double cp = Math.cos(pitch);
        this.forwardX = -sy * cp;
        this.forwardY = -sp;
        this.forwardZ = cy * cp;
        this.rightX = -cy;
        this.rightZ = -sy;
        // up = right x forward (right has no Y component)
        this.upX = -this.rightZ * this.forwardY;
        this.upY = this.rightZ * this.forwardX - this.rightX * this.forwardZ;
        this.upZ = this.rightX * this.forwardY;
        final double fov = Math.max(1.0, Math.min(179.0, fovDegrees));
        final double t = Math.tan(Math.toRadians(fov) / 2.0);
        final double aspect = guiHeight <= 0 ? 1.0 : guiWidth / guiHeight;
        this.scaleY = 1.0 / t;
        this.scaleX = 1.0 / (t * aspect);
        this.width = guiWidth;
        this.height = guiHeight;
    }

    /** Camera-space coordinates of a world point: right, up, depth (forward). */
    public double right(final double wx, final double wy, final double wz) {
        return (wx - this.camX) * this.rightX + (wz - this.camZ) * this.rightZ;
    }

    public double up(final double wx, final double wy, final double wz) {
        return (wx - this.camX) * this.upX + (wy - this.camY) * this.upY + (wz - this.camZ) * this.upZ;
    }

    public double forward(final double wx, final double wy, final double wz) {
        return (wx - this.camX) * this.forwardX + (wy - this.camY) * this.forwardY + (wz - this.camZ) * this.forwardZ;
    }

    /**
     * Projects a world point. Returns false (and leaves {@link #x}/{@link #y} undefined) when it
     * is behind the camera; a point in front may still be off screen.
     */
    public boolean project(final double wx, final double wy, final double wz) {
        final double d = forward(wx, wy, wz);
        this.depth = d;
        if (d < NEAR) {
            return false;
        }
        return toScreen(right(wx, wy, wz), up(wx, wy, wz), d);
    }

    /** Camera-space point (right, up, depth > 0) to GUI pixels in {@link #x}/{@link #y}. */
    public boolean toScreen(final double r, final double u, final double d) {
        this.depth = d;
        if (d < NEAR) {
            return false;
        }
        this.x = (r * this.scaleX / d + 1.0) * 0.5 * this.width;
        this.y = (1.0 - u * this.scaleY / d) * 0.5 * this.height;
        return true;
    }

    /** GUI pixels covered by one block at this depth in front of the camera. */
    public double pixelsPerBlock(final double depth) {
        return this.scaleY / Math.max(NEAR, depth) * 0.5 * this.height;
    }

    /** True when the last projected point lies within the screen, with a margin in GUI pixels. */
    public boolean onScreen(final double margin) {
        return this.x >= -margin && this.y >= -margin && this.x <= this.width + margin && this.y <= this.height + margin;
    }

    /**
     * The visible part of the segment a-b (world coordinates) after clipping it to the near plane,
     * projected: writes the two screen points into {@code out} (x0, y0, x1, y1) and the fraction
     * along a-b where each kept end lies (t0, t1) into {@code out[4..5]}. False when all of it is
     * behind the camera.
     */
    public boolean segment(final double ax, final double ay, final double az, final double bx, final double by,
                           final double bz, final double[] out) {
        final double ra = right(ax, ay, az);
        final double ua = up(ax, ay, az);
        final double da = forward(ax, ay, az);
        final double rb = right(bx, by, bz);
        final double ub = up(bx, by, bz);
        final double db = forward(bx, by, bz);
        double t0 = 0.0;
        double t1 = 1.0;
        if (da < NEAR && db < NEAR) {
            return false;
        }
        if (da < NEAR) {
            t0 = (NEAR - da) / (db - da);
        } else if (db < NEAR) {
            t1 = (NEAR - da) / (db - da);
        }
        toScreen(ra + (rb - ra) * t0, ua + (ub - ua) * t0, Math.max(NEAR, da + (db - da) * t0));
        out[0] = this.x;
        out[1] = this.y;
        toScreen(ra + (rb - ra) * t1, ua + (ub - ua) * t1, Math.max(NEAR, da + (db - da) * t1));
        out[2] = this.x;
        out[3] = this.y;
        out[4] = t0;
        out[5] = t1;
        return true;
    }
}
