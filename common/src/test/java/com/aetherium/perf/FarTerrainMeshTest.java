package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.qol.ParticleFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.11.0: far terrain meshing, the redstone particle limiter and the block entity limit. */
final class FarTerrainMeshTest {

    private static FarTerrainMesh.Column flat(final int height) {
        final FarTerrainMesh.Column c = new FarTerrainMesh.Column();
        for (int i = 0; i < c.heights.length; i++) {
            c.heights[i] = (short) height;
            c.colors[i] = 0x7FB238;
        }
        return c;
    }

    /** Counts quads and checks every quad is a proper rectangle inside the world. */
    private static final class Counter implements FarTerrainMesh.Sink {
        int quads;
        int sides;
        float minY = Float.MAX_VALUE;

        @Override
        public void quad(final float x0, final float y0, final float z0, final float x1, final float y1, final float z1,
                         final float x2, final float y2, final float z2, final float x3, final float y3, final float z3,
                         final int rgb) {
            quads++;
            if (y0 != y1 || y1 != y2) {
                sides++;
            }
            minY = Math.min(minY, Math.min(Math.min(y0, y1), Math.min(y2, y3)));
            assertTrue(rgb >= 0 && rgb <= 0xFFFFFF, "colour is plain RGB");
        }
    }

    @Test
    @DisplayName("far terrain: key packing matches ChunkPos and the ring excludes the render distance")
    void keysAndRing() {
        final long k = FarTerrainMesh.key(-5, 123456);
        assertEquals(-5, FarTerrainMesh.keyX(k));
        assertEquals(123456, FarTerrainMesh.keyZ(k));
        assertFalse(FarTerrainMesh.inFarRing(8, 8, 8, 24), "inside the render distance square");
        assertTrue(FarTerrainMesh.inFarRing(9, 0, 8, 24));
        assertFalse(FarTerrainMesh.inFarRing(20, 20, 8, 24), "outside the far circle");
    }

    @Test
    @DisplayName("far terrain: flat ground is one top quad per column, sides only toward unknown chunks")
    void flatGround() {
        final Map<Long, FarTerrainMesh.Column> map = new HashMap<Long, FarTerrainMesh.Column>();
        // A 3x3 block of chunks at distance 10..12 on x: entirely in the ring (render distance 8).
        for (int x = 10; x <= 12; x++) {
            for (int z = -1; z <= 1; z++) {
                map.put(Long.valueOf(FarTerrainMesh.key(x, z)), flat(64));
            }
        }
        final Counter c = new Counter();
        final int n = FarTerrainMesh.build(map, 0, 0, 8, 24, null, c);
        assertEquals(c.quads, n);
        // 9 chunks x 16 tops, plus a side for every column on the outer border (12 columns per edge).
        assertEquals(9 * 16 + 4 * 12, n);
        assertEquals(4 * 12, c.sides);
        assertEquals(64 - FarTerrainMesh.EDGE_DEPTH, c.minY, 0.0F);
    }

    @Test
    @DisplayName("far terrain: a step down gets one side, coarse chunks use 2x2 columns, inside chunks only on request")
    void stepsCoarseAndInside() {
        final Map<Long, FarTerrainMesh.Column> map = new HashMap<Long, FarTerrainMesh.Column>();
        final FarTerrainMesh.Column hill = flat(64);
        hill.heights[0] = 70; // north-west column raised
        map.put(Long.valueOf(FarTerrainMesh.key(10, 0)), hill);
        map.put(Long.valueOf(FarTerrainMesh.key(10, -1)), flat(64));
        map.put(Long.valueOf(FarTerrainMesh.key(9, 0)), flat(64));
        Counter c = new Counter();
        FarTerrainMesh.build(map, 0, 0, 8, 24, null, c);
        // hill chunk: 16 tops; raised column: sides to the east and south (inside), north and west (neighbours at 64).
        // Plus border sides toward unknown chunks for all three chunks.
        assertTrue(c.sides >= 4);

        final Map<Long, FarTerrainMesh.Column> far = new HashMap<Long, FarTerrainMesh.Column>();
        far.put(Long.valueOf(FarTerrainMesh.key(20, 0)), flat(64));
        c = new Counter();
        FarTerrainMesh.build(far, 0, 0, 8, 24, null, c);
        assertEquals(4 + 4 * 2, c.quads, "coarse: 4 tops, 2 border sides per edge");

        final Map<Long, FarTerrainMesh.Column> inside = new HashMap<Long, FarTerrainMesh.Column>();
        inside.put(Long.valueOf(FarTerrainMesh.key(2, 2)), flat(64));
        c = new Counter();
        assertEquals(0, FarTerrainMesh.build(inside, 0, 0, 8, 24, null, c), "inside the render distance: skipped");
        final Set<Long> unbuilt = new HashSet<Long>();
        unbuilt.add(Long.valueOf(FarTerrainMesh.key(2, 2)));
        c = new Counter();
        assertEquals(16 + 4 * 4, FarTerrainMesh.build(inside, 0, 0, 8, 24, unbuilt, c), "unbuilt chunk: full grid");
    }

    @Test
    @DisplayName("redstone limiter: a fixed budget per tick, unlimited at 64, off category stays off")
    void redstoneLimiter() {
        try {
            ParticleFilter.setRedstoneLimit(3);
            assertTrue(ParticleFilter.anyDisabled(), "the hook must run while a limit is set");
            ParticleFilter.newTick();
            assertTrue(ParticleFilter.allowed(ParticleFilter.REDSTONE));
            assertTrue(ParticleFilter.allowed(ParticleFilter.REDSTONE));
            assertTrue(ParticleFilter.allowed(ParticleFilter.REDSTONE));
            assertFalse(ParticleFilter.allowed(ParticleFilter.REDSTONE), "fourth in the same tick");
            assertTrue(ParticleFilter.allowed(ParticleFilter.SMOKE), "other categories are not limited");
            ParticleFilter.newTick();
            assertTrue(ParticleFilter.allowed(ParticleFilter.REDSTONE), "new tick, new budget");
            ParticleFilter.setRedstoneLimit(ParticleFilter.REDSTONE_UNLIMITED);
            assertFalse(ParticleFilter.anyDisabled());
            for (int i = 0; i < 1000; i++) {
                assertTrue(ParticleFilter.allowed(ParticleFilter.REDSTONE));
            }
            ParticleFilter.set(ParticleFilter.REDSTONE, false);
            assertFalse(ParticleFilter.allowed(ParticleFilter.REDSTONE));
        } finally {
            ParticleFilter.set(ParticleFilter.REDSTONE, true);
            ParticleFilter.setRedstoneLimit(ParticleFilter.REDSTONE_UNLIMITED);
        }
    }

    @Test
    @DisplayName("block entity limit: counted per frame, unlimited when off")
    void blockEntityLimit() {
        final com.aetherium.config.AetheriumConfig c = new com.aetherium.config.AetheriumConfig();
        c.blockEntityLimit.set(Integer.valueOf(64));
        Tuning.configure(c, true);
        try {
            Tuning.newFrame();
            for (int i = 0; i < 64; i++) {
                assertTrue(Tuning.allowBlockEntity());
            }
            assertFalse(Tuning.allowBlockEntity(), "65th block entity in one frame");
            Tuning.newFrame();
            assertTrue(Tuning.allowBlockEntity());
            c.blockEntityLimit.set(Integer.valueOf(Tuning.BLOCK_ENTITIES_UNLIMITED));
            Tuning.configure(c, true);
            Tuning.newFrame();
            for (int i = 0; i < 5000; i++) {
                assertTrue(Tuning.allowBlockEntity());
            }
            c.blockEntityLimit.set(Integer.valueOf(64));
            Tuning.configure(c, false);
            Tuning.newFrame();
            for (int i = 0; i < 5000; i++) {
                assertTrue(Tuning.allowBlockEntity(), "inactive = vanilla");
            }
        } finally {
            Tuning.configure(new com.aetherium.config.AetheriumConfig(), false);
            ParticleFilter.setRedstoneLimit(ParticleFilter.REDSTONE_UNLIMITED);
        }
    }
}
