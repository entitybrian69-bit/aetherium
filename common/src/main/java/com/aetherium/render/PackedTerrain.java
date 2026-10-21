package com.aetherium.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Packed terrain vertices (1.17.2): 20 bytes per vertex instead of vanilla's 32 for the opaque
 * terrain layers stored in region pages.
 *
 * <pre>
 *  0  short x, y, z   region-local position: (p - 64) * 256 (1/256 block steps, -64 .. 192)
 *  6  short 0         padding (keeps the next attribute 4-byte aligned)
 *  8  ubyte r, g, b, a
 * 12  short u, v      texture: round(uv * 32768), at most 32767
 * 16  short u, v      lightmap, exactly as vanilla stores it
 * </pre>
 *
 * <p>No normals: terrain is drawn with lighting off (shading is baked into the colour), the normal
 * array vanilla's format enables is never read. The fixed-function pipeline reads the shorts as
 * plain numbers; the draw undoes the scaling with matrices: the page's model-view matrix gets
 * {@code translate(64, 64, 64) * scale(1/256)}, texture unit 0 gets {@code scale(1/32768)}. Fog
 * still sees true eye-space distances because the scale is part of the model-view matrix.</p>
 *
 * <p>Block model coordinates are multiples of 1/16 and land exactly on the grid. Anything finer is
 * rounded to the nearest 1/256 block (at most 1/512 off, a thirtieth of a texture pixel), except
 * that a coordinate clearly off the 1/16 block grid is never rounded onto it: vanilla insets
 * some faces by a thousandth of a block (lava sides) so they do not flicker against a neighbour,
 * and such a face keeps a (1/256 block) inset. Identical input coordinates always give identical
 * output, so shared edges stay watertight.</p>
 *
 * <p>Minecraft-free; unit-tested.</p>
 */
public final class PackedTerrain {

    public static final int STRIDE = 20;
    /** Vanilla BLOCK vertex size. */
    public static final int SOURCE_STRIDE = 32;
    public static final float POSITION_BIAS = 64f;
    public static final float POSITION_SCALE = 256f;
    public static final float UV_SCALE = 32768f;

    /**
     * In 1/256 block units: closer to the grid than this (2/10000 block) is float noise of a grid
     * coordinate, not an intended inset (vanilla's smallest is 1/1000 block, 0.256 units).
     */
    static final float GRID_TOLERANCE = 0.05f;

    public static final int OFFSET_COLOR = 8;
    public static final int OFFSET_UV0 = 12;
    public static final int OFFSET_UV2 = 16;

    private PackedTerrain() {
    }

    /**
     * Packs {@code vertexCount} vanilla BLOCK vertices starting at {@code src.position()} into
     * {@code dst} starting at {@code dst.position()}, adding {@code (dx, dy, dz)} to every position
     * (the section's offset inside its region). Neither buffer's position or limit changes.
     *
     * @return false when a value does not fit the packed format (then {@code dst} holds partial
     *         data and the caller keeps the vanilla vertices)
     */
    public static boolean pack(final ByteBuffer src, final int vertexCount, final float dx, final float dy,
                               final float dz, final ByteBuffer dst) {
        if (vertexCount < 0 || src.remaining() < vertexCount * SOURCE_STRIDE
                || dst.remaining() < vertexCount * STRIDE) {
            return false;
        }
        final ByteBuffer s = src.duplicate().order(ByteOrder.nativeOrder());
        final ByteBuffer d = dst.duplicate().order(ByteOrder.nativeOrder());
        int in = src.position();
        int out = dst.position();
        for (int v = 0; v < vertexCount; v++, in += SOURCE_STRIDE, out += STRIDE) {
            final int x = position(s.getFloat(in) + dx);
            final int y = position(s.getFloat(in + 4) + dy);
            final int z = position(s.getFloat(in + 8) + dz);
            final int u = uv(s.getFloat(in + 16));
            final int w = uv(s.getFloat(in + 20));
            if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || z == Integer.MIN_VALUE
                    || u < 0 || w < 0) {
                return false;
            }
            d.putShort(out, (short) x);
            d.putShort(out + 2, (short) y);
            d.putShort(out + 4, (short) z);
            d.putShort(out + 6, (short) 0);
            d.putInt(out + OFFSET_COLOR, s.getInt(in + 12));
            d.putShort(out + OFFSET_UV0, (short) u);
            d.putShort(out + OFFSET_UV0 + 2, (short) w);
            d.putInt(out + OFFSET_UV2, s.getInt(in + 24));
        }
        return true;
    }

    /**
     * Region-local block coordinate to its packed short, or {@code Integer.MIN_VALUE} when it is out
     * of range or not a number.
     */
    static int position(final float p) {
        if (!(p >= -64f && p < 191.99f)) {
            return Integer.MIN_VALUE;
        }
        final float scaled = (p - POSITION_BIAS) * POSITION_SCALE;
        int q = Math.round(scaled);
        final float off = scaled - q;
        if ((q & 15) == 0 && (off > GRID_TOLERANCE || off < -GRID_TOLERANCE)) {
            // rounded onto the 1/16 block grid from clearly off it: keep the side the vertex was on
            q += off > 0f ? 1 : -1;
        }
        return q < -32768 || q > 32767 ? Integer.MIN_VALUE : q;
    }

    /** Atlas coordinate (0..1) to its packed short, or -1 when it is outside the atlas. */
    static int uv(final float t) {
        if (!(t >= -1.0E-4f && t <= 1.0001f)) {
            return -1;
        }
        final int q = Math.round(t * UV_SCALE);
        return q < 0 ? 0 : Math.min(q, 32767);
    }

    /** Inverse of the position packing (tests). */
    static float unpackPosition(final int q) {
        return q / POSITION_SCALE + POSITION_BIAS;
    }
}
