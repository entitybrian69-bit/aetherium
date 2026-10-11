package com.aetherium.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The compact terrain format: lossless where it matters, conservative where it culls. */
final class CompactTerrainTest {

    // Vanilla FaceInfo vertex orders for a unit cube (counter-clockwise seen from outside).
    private static final float[][] UP_FACE = {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
    private static final float[][] DOWN_FACE = {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}};
    private static final float[][] NORTH_FACE = {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}};
    private static final float[][] SOUTH_FACE = {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}};
    private static final float[][] WEST_FACE = {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}};
    private static final float[][] EAST_FACE = {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}};

    private static final class Mesh {
        final ByteBuffer data = ByteBuffer.allocateDirect(64 * 1024).order(ByteOrder.nativeOrder());

        Mesh quad(final float[][] corners, final float ox, final float oy, final float oz, final int nx, final int ny, final int nz,
                  final int block, final int sky) {
            for (int v = 0; v < 4; v++) {
                data.putFloat(corners[v][0] + ox).putFloat(corners[v][1] + oy).putFloat(corners[v][2] + oz);
                data.put((byte) 200).put((byte) 150).put((byte) 100).put((byte) 255);
                data.putFloat(0.25f + v * 0.001f).putFloat(0.5f);
                data.putShort((short) block).putShort((short) sky);
                data.put((byte) nx).put((byte) ny).put((byte) nz).put((byte) 0);
            }
            return this;
        }

        ByteBuffer done() {
            final ByteBuffer out = data.duplicate().order(ByteOrder.nativeOrder());
            out.flip();
            return out;
        }
    }

    private static Mesh cube(final float ox, final float oy, final float oz) {
        return new Mesh()
                .quad(UP_FACE, ox, oy, oz, 0, 127, 0, 240, 128)
                .quad(DOWN_FACE, ox, oy, oz, 0, -127, 0, 0, 0)
                .quad(NORTH_FACE, ox, oy, oz, 0, 0, -127, 16, 32)
                .quad(SOUTH_FACE, ox, oy, oz, 0, 0, 127, 16, 32)
                .quad(WEST_FACE, ox, oy, oz, -127, 0, 0, 16, 32)
                .quad(EAST_FACE, ox, oy, oz, 127, 0, 0, 16, 32);
    }

    @Test
    @DisplayName("compact terrain: a cube's six faces land in their six groups with exact attributes")
    void cubeRoundTrip() {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        final ByteBuffer out = CompactTerrain.encode(cube(3, 4, 5).done(), new CompactTerrain.Scratch(), layout);
        assertNotNull(out);
        assertEquals(6 * CompactTerrain.QUAD_BYTES, out.remaining(), "16 bytes per vertex, half of vanilla");
        assertEquals(6, layout.quads());
        for (final int g : new int[] {CompactTerrain.UP, CompactTerrain.DOWN, CompactTerrain.NORTH, CompactTerrain.SOUTH,
                CompactTerrain.WEST, CompactTerrain.EAST}) {
            assertEquals(1, layout.quadsIn(g), "group " + g);
        }
        assertEquals(0, layout.quadsIn(CompactTerrain.ANY));
        // UP quad is first; its first vertex is (3, 5, 5) with light 240/128.
        final int up = layout.start[CompactTerrain.UP] * CompactTerrain.QUAD_BYTES;
        assertEquals(3.0f, CompactTerrain.dequantisePosition(out.getShort(up)), 0.0f);
        assertEquals(5.0f, CompactTerrain.dequantisePosition(out.getShort(up + 2)), 0.0f);
        assertEquals(5.0f, CompactTerrain.dequantisePosition(out.getShort(up + 4)), 0.0f);
        final int light = out.getShort(up + 6) & 0xFFFF;
        assertEquals(240, light & 0xFF);
        assertEquals(128, light >> 8);
        assertEquals((byte) 200, out.get(up + CompactTerrain.OFFSET_COLOR));
        assertEquals((byte) 255, out.get(up + CompactTerrain.OFFSET_COLOR + 3));
        assertEquals(0.25f, (out.getShort(up + CompactTerrain.OFFSET_UV) & 0xFFFF) / 65535.0f, 1.0f / 65535.0f);
        // Planes: UP is the minimum y of up faces (5), DOWN the maximum y of down faces (4), etc.
        assertEquals(5.0f, layout.plane[CompactTerrain.UP], 0.0f);
        assertEquals(4.0f, layout.plane[CompactTerrain.DOWN], 0.0f);
        assertEquals(4.0f, layout.plane[CompactTerrain.EAST], 0.0f);
        assertEquals(3.0f, layout.plane[CompactTerrain.WEST], 0.0f);
        assertEquals(6.0f, layout.plane[CompactTerrain.SOUTH], 0.0f);
        assertEquals(5.0f, layout.plane[CompactTerrain.NORTH], 0.0f);
    }

    @Test
    @DisplayName("compact terrain: positions keep 1/1024-block precision across the whole range, shared vertices stay shared")
    void positionPrecision() {
        for (float p = -8.0f; p < 55.99f; p += 0.0625f) {
            assertEquals(p, CompactTerrain.dequantisePosition(CompactTerrain.quantisePosition(p)), 0.0f, "1/16 grid is exact: " + p);
        }
        for (float p = -7.9f; p < 55.9f; p += 0.37f) {
            assertEquals(p, CompactTerrain.dequantisePosition(CompactTerrain.quantisePosition(p)), 0.5f / 1024.0f + 1e-5f);
        }
        assertEquals(CompactTerrain.quantisePosition(16.0f), CompactTerrain.quantisePosition(15.0f + 1.0f));
    }

    @Test
    @DisplayName("compact terrain: diagonal plants and back-to-back fluid faces are never put in a culled group")
    void mislabelledQuadsGoToAny() {
        // Cross-model plant quad: vanilla writes the NORTH normal, geometry is diagonal.
        final float[][] diagonal = {{0.15f, 1, 0.15f}, {0.15f, 0, 0.15f}, {0.85f, 0, 0.85f}, {0.85f, 1, 0.85f}};
        // Lava's top face drawn again from below: same up normal, reversed winding.
        final float[][] reversedTop = {UP_FACE[3], UP_FACE[2], UP_FACE[1], UP_FACE[0]};
        // Fluid side face carrying vanilla's constant up normal.
        final ByteBuffer src = new Mesh()
                .quad(diagonal, 0, 0, 0, 0, 0, -127, 0, 0)
                .quad(reversedTop, 0, 0, 0, 0, 127, 0, 0, 0)
                .quad(NORTH_FACE, 0, 0, 0, 0, 127, 0, 0, 0)
                .quad(UP_FACE, 0, 0, 0, 30, 120, 0, 0, 0)
                .done();
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        assertNotNull(CompactTerrain.encode(src, new CompactTerrain.Scratch(), layout));
        assertEquals(4, layout.quadsIn(CompactTerrain.ANY), "all four are drawn regardless of camera position");
    }

    @Test
    @DisplayName("compact terrain: data the format cannot hold exactly is refused (vanilla upload instead)")
    void refusesUnfitData() {
        final CompactTerrain.Scratch scratch = new CompactTerrain.Scratch();
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        assertNull(CompactTerrain.encode(new Mesh().quad(UP_FACE, 60, 0, 0, 0, 127, 0, 0, 0).done(), scratch, layout), "x beyond 56");
        assertNull(CompactTerrain.encode(new Mesh().quad(UP_FACE, -9, 0, 0, 0, 127, 0, 0, 0).done(), scratch, layout), "x below -8");
        assertNull(CompactTerrain.encode(new Mesh().quad(UP_FACE, 0, 0, 0, 0, 127, 0, 300, 0).done(), scratch, layout), "light > 255");
        final ByteBuffer three = new Mesh().quad(UP_FACE, 0, 0, 0, 0, 127, 0, 0, 0).done();
        three.limit(three.limit() - CompactTerrain.SOURCE_STRIDE);
        assertNull(CompactTerrain.encode(three, scratch, layout), "not whole quads");
        assertNotNull(CompactTerrain.encode(cube(0, 0, 0).done(), scratch, layout), "scratch still usable");
    }

    @Test
    @DisplayName("compact terrain: a group is skipped only when the camera is behind every face in it")
    void groupVisibility() {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        CompactTerrain.encode(cube(3, 4, 5).done(), new CompactTerrain.Scratch(), layout);
        // Camera above, south-east of the cube: UP, SOUTH, EAST face it.
        int mask = CompactTerrain.visibleGroups(layout.plane, 20, 30, 20);
        assertEquals((1 << CompactTerrain.UP) | (1 << CompactTerrain.SOUTH) | (1 << CompactTerrain.EAST) | (1 << CompactTerrain.ANY), mask);
        // Camera below, north-west: DOWN, NORTH, WEST.
        mask = CompactTerrain.visibleGroups(layout.plane, -20, -30, -20);
        assertEquals((1 << CompactTerrain.DOWN) | (1 << CompactTerrain.NORTH) | (1 << CompactTerrain.WEST) | (1 << CompactTerrain.ANY), mask);
        // Camera level with the top face plane (edge-on) keeps it: conservative by PLANE_EPSILON.
        mask = CompactTerrain.visibleGroups(layout.plane, 3.5f, 5.0f, 5.5f);
        assertTrue((mask & (1 << CompactTerrain.UP)) != 0);
        // Inside a closed cube every face points away: only the always-drawn ANY group remains.
        mask = CompactTerrain.visibleGroups(layout.plane, 3.5f, 4.5f, 5.5f);
        assertEquals(1 << CompactTerrain.ANY, mask);
        // Between two cubes stacked 3 blocks apart: the lower cube's top and the upper cube's bottom both face the camera.
        final CompactTerrain.Layout pair = new CompactTerrain.Layout();
        final Mesh two = cube(0, 0, 0);
        two.quad(UP_FACE, 0, 3, 0, 0, 127, 0, 0, 0).quad(DOWN_FACE, 0, 3, 0, 0, -127, 0, 0, 0);
        CompactTerrain.encode(two.done(), new CompactTerrain.Scratch(), pair);
        mask = CompactTerrain.visibleGroups(pair.plane, 0.5f, 2.0f, 0.5f);
        assertTrue((mask & (1 << CompactTerrain.UP)) != 0 && (mask & (1 << CompactTerrain.DOWN)) != 0);
        // An empty group (planes at +/- infinity) is never "visible" but costs nothing either.
        final CompactTerrain.Layout empty = new CompactTerrain.Layout();
        CompactTerrain.encode(new Mesh().quad(UP_FACE, 0, 0, 0, 0, 127, 0, 0, 0).done(), new CompactTerrain.Scratch(), empty);
        assertEquals(0, CompactTerrain.visibleGroups(empty.plane, 0, 100, 0) & (1 << CompactTerrain.DOWN));
    }

    @Test
    @DisplayName("compact terrain: visible groups become at most a few contiguous runs, empty groups do not split them")
    void runsAreContiguous() {
        final CompactTerrain.Layout layout = new CompactTerrain.Layout();
        // group sizes UP=2, NORTH=0, WEST=3, ANY=1, EAST=4, SOUTH=0, DOWN=5
        final int[] sizes = {2, 0, 3, 1, 4, 0, 5};
        for (int g = 0; g < CompactTerrain.GROUPS; g++) {
            layout.start[g + 1] = layout.start[g] + sizes[g];
        }
        final int[] first = new int[4];
        final int[] count = new int[4];
        assertEquals(1, CompactTerrain.runs(layout, CompactTerrain.ALL_GROUPS, first, count));
        assertEquals(0, first[0]);
        assertEquals(15, count[0]);
        // UP + WEST + ANY (NORTH empty in between) = one run of 6 quads.
        int mask = (1 << CompactTerrain.UP) | (1 << CompactTerrain.WEST) | (1 << CompactTerrain.ANY) | (1 << CompactTerrain.NORTH);
        assertEquals(1, CompactTerrain.runs(layout, mask, first, count));
        assertEquals(6, count[0]);
        // UP + ANY + DOWN: three runs (WEST and EAST skipped).
        mask = (1 << CompactTerrain.UP) | (1 << CompactTerrain.ANY) | (1 << CompactTerrain.DOWN);
        assertEquals(3, CompactTerrain.runs(layout, mask, first, count));
        assertEquals(0, first[0]);
        assertEquals(2, count[0]);
        assertEquals(5, first[1]);
        assertEquals(1, count[1]);
        assertEquals(10, first[2]);
        assertEquals(5, count[2]);
        int total = 0;
        for (int i = 0; i < 3; i++) {
            total += count[i];
        }
        assertEquals(8, total, "exactly the visible quads");
    }

    @Test
    @DisplayName("compact terrain: runs separated by small gaps are merged into one draw")
    void smallGapsMerge() {
        final int[] first = {0, 5, 10, 400};
        final int[] count = {2, 1, 5, 7};
        assertEquals(2, CompactTerrain.mergeRuns(first, count, 4, 4));
        assertEquals(0, first[0]);
        assertEquals(15, count[0]);
        assertEquals(400, first[1]);
        assertEquals(7, count[1]);
        final int[] f2 = {0, 5};
        final int[] c2 = {2, 1};
        assertEquals(2, CompactTerrain.mergeRuns(f2, c2, 2, 2), "gap of 3 > 2 stays split");
        assertEquals(1, CompactTerrain.mergeRuns(f2, c2, 2, 3));
        assertEquals(6, c2[0]);
        assertEquals(1, CompactTerrain.mergeRuns(new int[] {7}, new int[] {3}, 1, 0));
    }
}
