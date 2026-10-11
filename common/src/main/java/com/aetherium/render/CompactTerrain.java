package com.aetherium.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Aetherium's terrain mesh format and the CPU side of its pipeline: re-encodes vanilla's 32-byte
 * BLOCK vertices into 16-byte vertices, sorted into seven facing groups, and decides per frame
 * which groups of a section can face the camera at all.
 *
 * <h2>Vertex (16 bytes, every attribute 4-byte aligned)</h2>
 * <pre>
 *  0  u16 x, y, z   position * 1024 + 8192  (-8 .. 56 blocks around the section origin, 1/1024 steps)
 *  6  u16 light     block light | sky light << 8 (vanilla's lightmap coordinates, 0..240 each)
 *  8  u8  r, g, b, a  colour with baked ambient occlusion, as vanilla wrote it
 * 12  u16 u, v      atlas coordinates * 65535
 * </pre>
 * The normal is dropped: 1.16.5 terrain is drawn without fixed-function lighting, shading is
 * baked into the colour.
 *
 * <h2>Facing groups</h2>
 * Quads are stored grouped as {@code UP, NORTH, WEST, ANY, EAST, SOUTH, DOWN}. A quad joins an
 * axis group only when its geometry proves it: the normal vanilla wrote names the axis, all four
 * vertices lie in one plane perpendicular to it, and the winding faces the same way. Diagonal
 * plants (vanilla labels them with a cardinal direction) and fluid faces drawn back-to-back fall
 * into {@code ANY}, which is always drawn. Per axis group the outermost plane is recorded, so the
 * group is skipped only when the camera is behind every face in it; back-face culling would have
 * discarded those triangles anyway, after the GPU had already transformed their vertices.
 *
 * <p>Anything the format cannot hold exactly enough (positions outside -8..56, UVs outside 0..1,
 * light values above 255, a non-quad vertex count) makes {@link #encode} return -1 and the
 * caller uploads vanilla's data unchanged.</p>
 */
public final class CompactTerrain {

    public static final int SOURCE_STRIDE = 32;
    public static final int STRIDE = 16;
    public static final int QUAD_BYTES = STRIDE * 4;

    public static final int OFFSET_POS = 0;
    public static final int OFFSET_COLOR = 8;
    public static final int OFFSET_UV = 12;

    public static final float POSITION_SCALE = 1024.0f;
    public static final float POSITION_BIAS = 8.0f;

    public static final int UP = 0;
    public static final int NORTH = 1;
    public static final int WEST = 2;
    public static final int ANY = 3;
    public static final int EAST = 4;
    public static final int SOUTH = 5;
    public static final int DOWN = 6;
    public static final int GROUPS = 7;
    public static final int ALL_GROUPS = (1 << GROUPS) - 1;

    /** Slack, in blocks, before a group counts as facing away (covers float error and quantisation). */
    static final float PLANE_EPSILON = 0.0625f;

    // Source (vanilla DefaultVertexFormat.BLOCK) offsets.
    private static final int SRC_POS = 0;
    private static final int SRC_COLOR = 12;
    private static final int SRC_UV0 = 16;
    private static final int SRC_UV2 = 24;
    private static final int SRC_NORMAL = 28;

    private CompactTerrain() {
    }

    /** Per-buffer result of {@link #encode}: where each group starts and its outermost plane. */
    public static final class Layout {
        /** Quad index where group g starts; {@code start[GROUPS]} is the total quad count. */
        public final int[] start = new int[GROUPS + 1];
        /** Outermost plane per axis group, relative to the section origin (see class comment). */
        public final float[] plane = new float[GROUPS];

        public int quads() {
            return this.start[GROUPS];
        }

        public int quadsIn(final int group) {
            return this.start[group + 1] - this.start[group];
        }

        public void copyFrom(final Layout other) {
            System.arraycopy(other.start, 0, this.start, 0, this.start.length);
            System.arraycopy(other.plane, 0, this.plane, 0, this.plane.length);
        }
    }

    /** Reusable scratch for {@link #encode} (render thread only, one instance). */
    public static final class Scratch {
        private byte[] groupOf = new byte[4096];
        private ByteBuffer out = ByteBuffer.allocateDirect(256 * 1024).order(ByteOrder.nativeOrder());

        byte[] groups(final int quads) {
            if (this.groupOf.length < quads) {
                this.groupOf = new byte[Math.max(quads, this.groupOf.length * 2)];
            }
            return this.groupOf;
        }

        ByteBuffer output(final int bytes) {
            if (this.out.capacity() < bytes) {
                int capacity = this.out.capacity();
                while (capacity < bytes) {
                    capacity *= 2;
                }
                this.out = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
            }
            this.out.clear();
            this.out.limit(bytes);
            return this.out;
        }
    }

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    /** Scratch buffers of the calling thread (uploads normally all happen on the render thread). */
    public static Scratch scratch() {
        return SCRATCH.get();
    }

    /**
     * Encodes vanilla BLOCK vertices.
     *
     * @param source  vertex data from {@code source.position()} to {@code limit()}, native order
     * @param scratch reusable buffers; the returned buffer belongs to it and is valid until the next call
     * @param layout  receives group starts and planes
     * @return the encoded vertices (position 0, limit = bytes) or null when the data does not fit the format
     */
    public static ByteBuffer encode(final ByteBuffer source, final Scratch scratch, final Layout layout) {
        final ByteBuffer src = source.duplicate().order(ByteOrder.nativeOrder());
        final int base = src.position();
        final int bytes = src.remaining();
        if (bytes % (SOURCE_STRIDE * 4) != 0) {
            return null;
        }
        final int quads = bytes / (SOURCE_STRIDE * 4);
        final byte[] groupOf = scratch.groups(quads);
        final int[] count = new int[GROUPS];
        final float[] plane = layout.plane;
        for (int g = 0; g < GROUPS; g++) {
            plane[g] = isMinPlane(g) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
        }

        // Pass 1: validate, classify, count, track planes.
        for (int q = 0; q < quads; q++) {
            final int quad = base + q * SOURCE_STRIDE * 4;
            for (int v = 0; v < 4; v++) {
                final int at = quad + v * SOURCE_STRIDE;
                if (!fitsPosition(src.getFloat(at + SRC_POS)) || !fitsPosition(src.getFloat(at + SRC_POS + 4))
                        || !fitsPosition(src.getFloat(at + SRC_POS + 8)) || !fitsUv(src.getFloat(at + SRC_UV0))
                        || !fitsUv(src.getFloat(at + SRC_UV0 + 4)) || (src.getShort(at + SRC_UV2) & 0xFFFF) > 255
                        || (src.getShort(at + SRC_UV2 + 2) & 0xFFFF) > 255) {
                    return null;
                }
            }
            final int group = classify(src, quad);
            groupOf[q] = (byte) group;
            count[group]++;
            if (group != ANY) {
                final float p = src.getFloat(quad + SRC_POS + 4 * axisOf(group));
                plane[group] = isMinPlane(group) ? Math.min(plane[group], p) : Math.max(plane[group], p);
            }
        }

        final int[] start = layout.start;
        start[0] = 0;
        for (int g = 0; g < GROUPS; g++) {
            start[g + 1] = start[g] + count[g];
        }
        final int[] cursor = new int[GROUPS];
        System.arraycopy(start, 0, cursor, 0, GROUPS);

        // Pass 2: write each quad into its group's slot.
        final ByteBuffer out = scratch.output(quads * QUAD_BYTES);
        for (int q = 0; q < quads; q++) {
            final int quad = base + q * SOURCE_STRIDE * 4;
            final int dst = cursor[groupOf[q]]++ * QUAD_BYTES;
            for (int v = 0; v < 4; v++) {
                final int at = quad + v * SOURCE_STRIDE;
                final int to = dst + v * STRIDE;
                out.putShort(to, quantisePosition(src.getFloat(at + SRC_POS)));
                out.putShort(to + 2, quantisePosition(src.getFloat(at + SRC_POS + 4)));
                out.putShort(to + 4, quantisePosition(src.getFloat(at + SRC_POS + 8)));
                out.putShort(to + 6, (short) ((src.getShort(at + SRC_UV2) & 0xFF) | ((src.getShort(at + SRC_UV2 + 2) & 0xFF) << 8)));
                out.putInt(to + OFFSET_COLOR, src.getInt(at + SRC_COLOR));
                out.putShort(to + OFFSET_UV, quantiseUv(src.getFloat(at + SRC_UV0)));
                out.putShort(to + OFFSET_UV + 2, quantiseUv(src.getFloat(at + SRC_UV0 + 4)));
            }
        }
        out.position(0);
        out.limit(quads * QUAD_BYTES);
        return out;
    }

    /**
     * Bit g set when group g may face the camera. {@code rx, ry, rz} is the camera position
     * relative to the section origin.
     */
    public static int visibleGroups(final float[] plane, final float rx, final float ry, final float rz) {
        int mask = 1 << ANY;
        if (ry > plane[UP] - PLANE_EPSILON) {
            mask |= 1 << UP;
        }
        if (ry < plane[DOWN] + PLANE_EPSILON) {
            mask |= 1 << DOWN;
        }
        if (rx > plane[EAST] - PLANE_EPSILON) {
            mask |= 1 << EAST;
        }
        if (rx < plane[WEST] + PLANE_EPSILON) {
            mask |= 1 << WEST;
        }
        if (rz > plane[SOUTH] - PLANE_EPSILON) {
            mask |= 1 << SOUTH;
        }
        if (rz < plane[NORTH] + PLANE_EPSILON) {
            mask |= 1 << NORTH;
        }
        return mask;
    }

    /**
     * Turns the visible, non-empty groups into contiguous quad runs.
     *
     * @return number of runs written to {@code first}/{@code count} (at most 4: seven groups, gaps between)
     */
    public static int runs(final Layout layout, final int mask, final int[] first, final int[] count) {
        int runs = 0;
        int open = -1;
        for (int g = 0; g <= GROUPS; g++) {
            final boolean draw = g < GROUPS && (mask & (1 << g)) != 0 && layout.quadsIn(g) > 0;
            final boolean empty = g < GROUPS && layout.quadsIn(g) == 0;
            if (draw) {
                if (open < 0) {
                    open = layout.start[g];
                }
            } else if (!empty && open >= 0) {
                first[runs] = open;
                count[runs] = layout.start[g] - open;
                runs++;
                open = -1;
            }
        }
        return runs;
    }

    /**
     * Joins neighbouring runs whose gap is at most {@code maxGapQuads}: drawing a few hidden
     * back faces (the GPU culls them before shading) is far cheaper than another draw call,
     * especially through GL4ES.
     *
     * @return the new run count; runs stay sorted and cover every quad they covered before
     */
    public static int mergeRuns(final int[] first, final int[] count, final int runs, final int maxGapQuads) {
        if (runs <= 1) {
            return runs;
        }
        int out = 0;
        for (int i = 1; i < runs; i++) {
            final int end = first[out] + count[out];
            if (first[i] - end <= maxGapQuads) {
                count[out] = first[i] + count[i] - first[out];
            } else {
                out++;
                first[out] = first[i];
                count[out] = count[i];
            }
        }
        return out + 1;
    }

    // ------------------------------------------------------------------ classification

    static int classify(final ByteBuffer src, final int quad) {
        final int nx = src.get(quad + SRC_NORMAL);
        final int ny = src.get(quad + SRC_NORMAL + 1);
        final int nz = src.get(quad + SRC_NORMAL + 2);
        final int group = groupForNormal(nx, ny, nz);
        if (group == ANY) {
            return ANY;
        }
        final int axis = axisOf(group);
        final float p0 = src.getFloat(quad + SRC_POS + 4 * axis);
        for (int v = 1; v < 4; v++) {
            if (src.getFloat(quad + v * SOURCE_STRIDE + SRC_POS + 4 * axis) != p0) {
                return ANY; // not a plane perpendicular to the labelled axis (diagonal plants, slopes)
            }
        }
        float winding = windingAlong(src, quad, axis, 0, 1, 2);
        if (winding == 0.0f) {
            winding = windingAlong(src, quad, axis, 0, 2, 3);
        }
        final boolean positive = group == UP || group == EAST || group == SOUTH;
        if (winding == 0.0f || (winding > 0.0f) != positive) {
            return ANY; // degenerate, or wound against its label (back faces of fluids)
        }
        return group;
    }

    static int groupForNormal(final int nx, final int ny, final int nz) {
        if (nx == 0 && nz == 0) {
            return ny == 127 ? UP : ny == -127 ? DOWN : ANY;
        }
        if (nx == 0 && ny == 0) {
            return nz == 127 ? SOUTH : nz == -127 ? NORTH : ANY;
        }
        if (ny == 0 && nz == 0) {
            return nx == 127 ? EAST : nx == -127 ? WEST : ANY;
        }
        return ANY;
    }

    /** 0 = x, 1 = y, 2 = z. */
    static int axisOf(final int group) {
        switch (group) {
            case UP:
            case DOWN:
                return 1;
            case EAST:
            case WEST:
                return 0;
            default:
                return 2;
        }
    }

    /** Axis groups whose recorded plane is the minimum (faces pointing towards +axis). */
    static boolean isMinPlane(final int group) {
        return group == UP || group == EAST || group == SOUTH;
    }

    /** Component of (b - a) x (c - a) along the axis: positive when the triangle faces +axis (CCW front). */
    private static float windingAlong(final ByteBuffer src, final int quad, final int axis, final int a, final int b, final int c) {
        final int ia = quad + a * SOURCE_STRIDE;
        final int ib = quad + b * SOURCE_STRIDE;
        final int ic = quad + c * SOURCE_STRIDE;
        final float ax = src.getFloat(ia);
        final float ay = src.getFloat(ia + 4);
        final float az = src.getFloat(ia + 8);
        final float ux = src.getFloat(ib) - ax;
        final float uy = src.getFloat(ib + 4) - ay;
        final float uz = src.getFloat(ib + 8) - az;
        final float vx = src.getFloat(ic) - ax;
        final float vy = src.getFloat(ic + 4) - ay;
        final float vz = src.getFloat(ic + 8) - az;
        switch (axis) {
            case 0:
                return uy * vz - uz * vy;
            case 1:
                return uz * vx - ux * vz;
            default:
                return ux * vy - uy * vx;
        }
    }

    // ------------------------------------------------------------------ quantisation

    static boolean fitsPosition(final float p) {
        return p >= -POSITION_BIAS && p < 64.0f - POSITION_BIAS - 0.001f;
    }

    static boolean fitsUv(final float uv) {
        return uv >= 0.0f && uv <= 1.0f;
    }

    static short quantisePosition(final float p) {
        return (short) Math.round((p + POSITION_BIAS) * POSITION_SCALE);
    }

    static float dequantisePosition(final short s) {
        return (s & 0xFFFF) / POSITION_SCALE - POSITION_BIAS;
    }

    static short quantiseUv(final float uv) {
        return (short) Math.round(uv * 65535.0f);
    }
}
