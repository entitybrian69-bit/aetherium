package com.aetherium.perf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Direct writes of vanilla's NEW_ENTITY vertices (the mob model format), for the fast model path:
 * position (3 floats), colour (4 bytes), texture (2 floats), overlay (2 shorts), light (2 shorts),
 * normal (3 bytes) and one padding byte: 36 bytes.
 *
 * <p>The bytes are the ones {@code BufferBuilder}'s own fast path writes for the same arguments,
 * with whole ints where it writes pairs of shorts or four bytes (little-endian buffers only; the
 * caller falls back to vanilla otherwise). The padding byte, which nothing reads, is written as 0.</p>
 */
public final class ModelVertexWriter {

    public static final int STRIDE = 36;

    private ModelVertexWriter() {
    }

    /** Whether a buffer can take these writes. */
    public static boolean usable(final ByteBuffer buffer) {
        return buffer != null && buffer.order() == ByteOrder.LITTLE_ENDIAN;
    }

    /** {@code (byte) (int) (f * 255)} per channel, as BufferBuilder writes colour floats, packed little-endian. */
    public static int packColor(final float r, final float g, final float b, final float a) {
        return ((int) (r * 255.0F) & 0xFF) | ((int) (g * 255.0F) & 0xFF) << 8 | ((int) (b * 255.0F) & 0xFF) << 16
                | ((int) (a * 255.0F) & 0xFF) << 24;
    }

    /** {@code BufferVertexConsumer.normalIntValue} per component, packed little-endian, padding 0. */
    public static int packNormal(final float x, final float y, final float z) {
        return normalByte(x) | normalByte(y) << 8 | normalByte(z) << 16;
    }

    static int normalByte(final float f) {
        final float c = f < -1.0F ? -1.0F : (f > 1.0F ? 1.0F : f);
        return (int) (c * 127.0F) & 0xFF;
    }

    /**
     * One vertex at byte {@code at}. {@code m} is the pose matrix, column-major; (x, y, z) is the
     * model-space position already divided by 16, transformed as {@code Vector4f.transform} does.
     */
    public static void vertex(final ByteBuffer b, final int at, final float[] m, final float x, final float y,
                              final float z, final int color, final float u, final float v, final int overlay,
                              final int light, final int normal) {
        b.putFloat(at, m[0] * x + m[4] * y + m[8] * z + m[12]);
        b.putFloat(at + 4, m[1] * x + m[5] * y + m[9] * z + m[13]);
        b.putFloat(at + 8, m[2] * x + m[6] * y + m[10] * z + m[14]);
        b.putInt(at + 12, color);
        b.putFloat(at + 16, u);
        b.putFloat(at + 20, v);
        b.putInt(at + 24, overlay);
        b.putInt(at + 28, light);
        b.putInt(at + 32, normal);
    }

    /**
     * All faces of one packed cube ({@link ModelCubeData}), as {@code ModelPart.compile} writes
     * them: positions through the pose matrix, normals through the normal matrix (row-major
     * {@code n}: m00 m01 m02 m10 ... m22, as {@code Vector3f.transform(Matrix3f)} multiplies), the
     * normal packed once per face.
     *
     * @return bytes written ({@code faces * 4 * STRIDE})
     */
    public static int writeFaces(final ByteBuffer b, final int at, final float[] m, final float[] n,
                                 final float[] faces, final int color, final int overlay, final int light) {
        return writeFaces(b, at, m, n, faces, color, overlay, light, false);
    }

    /**
     * As above; with {@code normalize} each transformed normal is scaled to unit length first, as
     * joml's {@code Vector3f.normalize()} does (1.20.5+ untrusted poses).
     */
    public static int writeFaces(final ByteBuffer b, final int at, final float[] m, final float[] n,
                                 final float[] faces, final int color, final int overlay, final int light,
                                 final boolean normalize) {
        int out = at;
        for (int f = 0; f + ModelCubeData.FACE_FLOATS <= faces.length; f += ModelCubeData.FACE_FLOATS) {
            final float x = faces[f + 20];
            final float y = faces[f + 21];
            final float z = faces[f + 22];
            float nx = n[0] * x + n[1] * y + n[2] * z;
            float ny = n[3] * x + n[4] * y + n[5] * z;
            float nz = n[6] * x + n[7] * y + n[8] * z;
            if (normalize) {
                final float inv = 1.0F / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                nx *= inv;
                ny *= inv;
                nz *= inv;
            }
            final int normal = packNormal(nx, ny, nz);
            for (int v = f; v < f + 20; v += 5) {
                vertex(b, out, m, faces[v], faces[v + 1], faces[v + 2], color, faces[v + 3], faces[v + 4], overlay,
                        light, normal);
                out += STRIDE;
            }
        }
        return out - at;
    }
}
