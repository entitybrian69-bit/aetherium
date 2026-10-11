package com.aetherium.perf;

/**
 * Corners of a camera-facing particle quad without allocating. Vanilla's
 * {@code SingleQuadParticle.render} builds an array of four {@code Vector3f} and rotates each by
 * the camera quaternion (three more quaternions per corner), for every particle every frame.
 * Here the camera's right and up axes come from the quaternion once per particle and the corners
 * are combinations of them, the same rotation. Corner order is vanilla's:
 * (-1, -1), (-1, 1), (1, 1), (1, -1).
 */
public final class Billboard {

    private static final float[] CX = {-1f, -1f, 1f, 1f};
    private static final float[] CY = {-1f, 1f, 1f, -1f};

    private Billboard() {
    }

    /**
     * @param qi    camera rotation quaternion (i, j, k, r), unit length
     * @param roll  particle roll in radians, 0 for none (vanilla skips the roll when its current
     *              roll is 0, whatever the previous one was)
     * @param size  {@code getQuadSize(partialTicks)}
     * @param out   12 floats: x, y, z offset of each corner from the particle position
     */
    public static void corners(final float qi, final float qj, final float qk, final float qr, final float roll,
                               final float size, final float[] out) {
        // Rotation matrix columns 0 (right) and 1 (up) of the unit quaternion.
        final float rx = 1f - 2f * (qj * qj + qk * qk);
        final float ry = 2f * (qi * qj + qk * qr);
        final float rz = 2f * (qi * qk - qj * qr);
        final float ux = 2f * (qi * qj - qk * qr);
        final float uy = 1f - 2f * (qi * qi + qk * qk);
        final float uz = 2f * (qj * qk + qi * qr);
        final boolean rolled = roll != 0f;
        final float cos = rolled ? (float) Math.cos(roll) : 1f;
        final float sin = rolled ? (float) Math.sin(roll) : 0f;
        for (int c = 0; c < 4; c++) {
            final float lx = CX[c] * cos - CY[c] * sin;
            final float ly = CX[c] * sin + CY[c] * cos;
            out[c * 3] = (rx * lx + ux * ly) * size;
            out[c * 3 + 1] = (ry * lx + uy * ly) * size;
            out[c * 3 + 2] = (rz * lx + uz * ly) * size;
        }
    }
}
