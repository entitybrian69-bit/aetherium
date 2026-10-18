package com.aetherium.perf;

import java.util.Map;
import java.util.Set;

/**
 * Far terrain (1.11.0, Minecraft 1.16.5): the Minecraft-free half. Every chunk the client has
 * received is reduced to a 4x4 grid of columns (height of the top motion-blocking block and its
 * map colour); this class turns those grids into coloured quads, a top face per column plus a side
 * face down to each lower neighbour, so the outline has no holes where the ground steps down.
 *
 * <p>Far away (more than {@link #COARSE_MARGIN} chunks beyond the render distance) a chunk uses a
 * 2x2 grid of 8-block columns (the highest of each four), which cuts the vertex count by four
 * where a column is a few pixels on screen.</p>
 *
 * <p>Coordinates are block units relative to the origin chunk's corner; heights are absolute.</p>
 */
public final class FarTerrainMesh {
    /** Columns per chunk side in the stored grid. */
    public static final int GRID = 4;
    /** Blocks per stored column side. */
    public static final int CELL = 16 / GRID;
    /** Chunks beyond the render distance that still use the full 4x4 grid. */
    public static final int COARSE_MARGIN = 8;
    /** Side depth when the neighbouring chunk is unknown. */
    public static final int EDGE_DEPTH = 12;

    private FarTerrainMesh() {
    }

    /** One chunk's columns: index {@code z * GRID + x}. Immutable once stored. */
    public static final class Column {
        public final short[] heights = new short[GRID * GRID];
        public final int[] colors = new int[GRID * GRID];
    }

    /** Receives quads (four corners, one RGB colour). */
    public interface Sink {
        void quad(float x0, float y0, float z0, float x1, float y1, float z1,
                  float x2, float y2, float z2, float x3, float y3, float z3, int rgb);
    }

    /** Same packing as {@code ChunkPos.asLong}. */
    public static long key(final int chunkX, final int chunkZ) {
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    public static int keyX(final long key) {
        return (int) key;
    }

    public static int keyZ(final long key) {
        return (int) (key >>> 32);
    }

    /** True when a chunk at this offset from the camera chunk belongs to the far ring. */
    public static boolean inFarRing(final int dx, final int dz, final int renderDistance, final int far) {
        return (Math.abs(dx) > renderDistance || Math.abs(dz) > renderDistance) && dx * dx + dz * dz <= far * far;
    }

    /** Colour scaled by a brightness factor (0..1). */
    public static int shade(final int rgb, final float factor) {
        final int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * factor));
        final int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * factor));
        final int b = Math.min(255, (int) ((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Builds the quads for every stored chunk in the far ring around {@code (originX, originZ)}, plus
     * the chunks in {@code inside} (unbuilt chunks within the render distance).
     *
     * @return number of quads written
     */
    public static int build(final Map<Long, Column> columns, final int originX, final int originZ,
                            final int renderDistance, final int far, final Set<Long> inside, final Sink sink) {
        int quads = 0;
        for (final Map.Entry<Long, Column> entry : columns.entrySet()) {
            final long key = entry.getKey().longValue();
            final int cx = keyX(key);
            final int cz = keyZ(key);
            final int dx = cx - originX;
            final int dz = cz - originZ;
            final boolean ring = inFarRing(dx, dz, renderDistance, far);
            if (!ring && (inside == null || !inside.contains(entry.getKey()))) {
                continue;
            }
            final boolean coarse = ring && (Math.max(Math.abs(dx), Math.abs(dz)) > renderDistance + COARSE_MARGIN);
            quads += chunk(columns, entry.getValue(), cx, cz, dx * 16, dz * 16, coarse, sink);
        }
        return quads;
    }

    private static int chunk(final Map<Long, Column> columns, final Column column, final int cx, final int cz,
                             final int baseX, final int baseZ, final boolean coarse, final Sink sink) {
        final Column north = columns.get(Long.valueOf(key(cx, cz - 1)));
        final Column south = columns.get(Long.valueOf(key(cx, cz + 1)));
        final Column west = columns.get(Long.valueOf(key(cx - 1, cz)));
        final Column east = columns.get(Long.valueOf(key(cx + 1, cz)));
        final int step = coarse ? 2 : 1;
        final int size = CELL * step;
        int quads = 0;
        for (int gz = 0; gz < GRID; gz += step) {
            for (int gx = 0; gx < GRID; gx += step) {
                int top = Integer.MIN_VALUE;
                int color = 0;
                for (int sz = 0; sz < step; sz++) {
                    for (int sx = 0; sx < step; sx++) {
                        final int i = (gz + sz) * GRID + gx + sx;
                        if (column.heights[i] > top) {
                            top = column.heights[i];
                            color = column.colors[i];
                        }
                    }
                }
                final float x0 = baseX + gx * CELL;
                final float z0 = baseZ + gz * CELL;
                final float x1 = x0 + size;
                final float z1 = z0 + size;
                final float y = top;
                sink.quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0, color);
                quads++;
                // North (-z) and south (+z): the lowest neighbour column along the edge.
                int low = Integer.MAX_VALUE;
                for (int s = 0; s < step; s++) {
                    low = Math.min(low, neighbour(column, north, gx + s, gz - 1, top));
                }
                if (low < top) {
                    sink.quad(x0, low, z0, x0, y, z0, x1, y, z0, x1, low, z0, shade(color, 0.8F));
                    quads++;
                }
                low = Integer.MAX_VALUE;
                for (int s = 0; s < step; s++) {
                    low = Math.min(low, neighbour(column, south, gx + s, gz + step, top));
                }
                if (low < top) {
                    sink.quad(x1, low, z1, x1, y, z1, x0, y, z1, x0, low, z1, shade(color, 0.8F));
                    quads++;
                }
                // West (-x) and east (+x).
                low = Integer.MAX_VALUE;
                for (int s = 0; s < step; s++) {
                    low = Math.min(low, neighbourX(column, west, gx - 1, gz + s, top));
                }
                if (low < top) {
                    sink.quad(x0, low, z1, x0, y, z1, x0, y, z0, x0, low, z0, shade(color, 0.6F));
                    quads++;
                }
                low = Integer.MAX_VALUE;
                for (int s = 0; s < step; s++) {
                    low = Math.min(low, neighbourX(column, east, gx + step, gz + s, top));
                }
                if (low < top) {
                    sink.quad(x1, low, z0, x1, y, z0, x1, y, z1, x1, low, z1, shade(color, 0.6F));
                    quads++;
                }
            }
        }
        return quads;
    }

    /** Height of grid cell (gx, gz) where gz may step into the north/south neighbour chunk. */
    private static int neighbour(final Column self, final Column other, final int gx, final int gz, final int top) {
        if (gz >= 0 && gz < GRID) {
            return self.heights[gz * GRID + gx];
        }
        if (other == null) {
            return Math.max(0, top - EDGE_DEPTH);
        }
        return other.heights[(gz < 0 ? GRID - 1 : 0) * GRID + gx];
    }

    /** Height of grid cell (gx, gz) where gx may step into the west/east neighbour chunk. */
    private static int neighbourX(final Column self, final Column other, final int gx, final int gz, final int top) {
        if (gx >= 0 && gx < GRID) {
            return self.heights[gz * GRID + gx];
        }
        if (other == null) {
            return Math.max(0, top - EDGE_DEPTH);
        }
        return other.heights[gz * GRID + (gx < 0 ? GRID - 1 : 0)];
    }
}
