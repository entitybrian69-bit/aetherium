package com.aetherium.perf;

import java.util.Arrays;

/**
 * Which 16x16x16 sections vanilla decided are visible this frame, as a bit grid centred on the
 * camera, so an entity can be skipped when every section its box touches is hidden behind terrain.
 *
 * <p>Vanilla already computes the visible-section list for terrain every frame (occlusion graph
 * plus frustum). Entities were only frustum-tested, so mobs in caves under the player, behind
 * hills or inside other buildings were still fully drawn. This class only <em>reads</em> that
 * list; it never adds a section vanilla would not draw, so the worst case of a stale list is the
 * same one-frame pop-in terrain already has.</p>
 *
 * <p>Conservative by construction: anything the grid cannot answer for certain counts as
 * visible. That covers sections outside the grid, rows above or below every visible section
 * (entities flying above the build limit, looking straight down), boxes spanning too many
 * sections, and frames where no list was collected yet.</p>
 *
 * <p>Render thread only. Cost per frame: one {@code Arrays.fill} of ~38 KB plus one bit store per
 * visible section; per entity: one to eight bit reads. No allocation after construction.</p>
 */
public final class SectionVisibility {

    /** Horizontal half-size in sections: render distance 32 plus a margin. */
    static final int RADIUS_XZ = 34;
    /** Vertical half-size in sections: covers a 1024-block tall world from any camera height. */
    static final int RADIUS_Y = 32;
    private static final int SIZE_XZ = RADIUS_XZ * 2 + 1;
    private static final int SIZE_Y = RADIUS_Y * 2 + 1;
    /** Boxes spanning more sections than this per axis are never culled (huge or odd entities). */
    static final int MAX_SPAN = 4;

    private final long[] bits = new long[(SIZE_XZ * SIZE_XZ * SIZE_Y + 63) >>> 6];
    private int originX;
    private int originY;
    private int originZ;
    private int minY;
    private int maxY;
    private int marked;
    private boolean ready;

    /** Starts a new frame centred on the camera's section; clears every bit. */
    public void begin(final int cameraSectionX, final int cameraSectionY, final int cameraSectionZ) {
        Arrays.fill(this.bits, 0L);
        this.originX = cameraSectionX - RADIUS_XZ;
        this.originY = cameraSectionY - RADIUS_Y;
        this.originZ = cameraSectionZ - RADIUS_XZ;
        this.minY = Integer.MAX_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.marked = 0;
        this.ready = true;
    }

    /** Marks one section visible. Out-of-grid sections are ignored (they read as visible anyway). */
    public void mark(final int sectionX, final int sectionY, final int sectionZ) {
        final int index = index(sectionX, sectionY, sectionZ);
        if (index < 0) {
            return;
        }
        this.bits[index >>> 6] |= 1L << index;
        if (sectionY < this.minY) {
            this.minY = sectionY;
        }
        if (sectionY > this.maxY) {
            this.maxY = sectionY;
        }
        this.marked++;
    }

    /** Forget the frame: everything reads as visible until the next {@link #begin}. */
    public void invalidate() {
        this.ready = false;
    }

    public boolean isReady() {
        return this.ready;
    }

    public int markedCount() {
        return this.marked;
    }

    /** Whether the section could be visible (false only when vanilla is certain it is not drawn). */
    public boolean isSectionVisible(final int sectionX, final int sectionY, final int sectionZ) {
        if (!this.ready || this.marked == 0 || sectionY < this.minY || sectionY > this.maxY) {
            return true;
        }
        final int index = index(sectionX, sectionY, sectionZ);
        return index < 0 || (this.bits[index >>> 6] & (1L << index)) != 0L;
    }

    /** Whether any section touched by the block-space box could be visible. */
    public boolean isBoxVisible(final double minX, final double minY, final double minZ,
                                final double maxX, final double maxY, final double maxZ) {
        if (!this.ready || this.marked == 0) {
            return true;
        }
        if (Double.isNaN(minX + minY + minZ + maxX + maxY + maxZ)) {
            return true;
        }
        final int x0 = floorSection(minX);
        final int y0 = floorSection(minY);
        final int z0 = floorSection(minZ);
        final int x1 = floorSection(maxX);
        final int y1 = floorSection(maxY);
        final int z1 = floorSection(maxZ);
        if (x1 - x0 >= MAX_SPAN || y1 - y0 >= MAX_SPAN || z1 - z0 >= MAX_SPAN) {
            return true;
        }
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (isSectionVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int index(final int sectionX, final int sectionY, final int sectionZ) {
        final int x = sectionX - this.originX;
        final int y = sectionY - this.originY;
        final int z = sectionZ - this.originZ;
        if (x < 0 || x >= SIZE_XZ || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_XZ) {
            return -1;
        }
        return (y * SIZE_XZ + z) * SIZE_XZ + x;
    }

    /** Block coordinate to section coordinate, correct for negatives and huge values. */
    public static int floorSection(final double block) {
        final double clamped = Math.max(-3.0E7, Math.min(3.0E7, block));
        return ((int) Math.floor(clamped)) >> 4;
    }
}
