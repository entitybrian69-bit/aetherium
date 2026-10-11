package com.aetherium.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.perf.ArenaAllocator;
import com.aetherium.perf.DrawRanges;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.6.0 batched terrain: allocator, draw-range merging, region pages. */
final class TerrainRegionsTest {

    /** Records buffer calls; keeps the uploaded bytes per buffer. */
    private static final class FakeBuffers implements TerrainRegions.Buffers {
        int next = 1;
        final Map<Integer, ByteBuffer> live = new HashMap<Integer, ByteBuffer>();
        int deleted;

        @Override
        public int create(final int bytes) {
            final int id = this.next++;
            this.live.put(id, ByteBuffer.allocate(bytes).order(ByteOrder.nativeOrder()));
            return id;
        }

        @Override
        public void upload(final int id, final long offsetBytes, final ByteBuffer data) {
            final ByteBuffer target = this.live.get(id);
            assertNotNull(target, "upload to a deleted buffer");
            assertTrue(offsetBytes + data.remaining() <= target.capacity(), "upload past the end");
            final ByteBuffer t = target.duplicate();
            t.position((int) offsetBytes);
            t.put(data.duplicate());
        }

        @Override
        public void delete(final int id) {
            assertNotNull(this.live.remove(id), "double delete");
            this.deleted++;
        }
    }

    /** {@code quads} quads whose vertices sit at (1, 2, 3). */
    private static ByteBuffer mesh(final int quads) {
        final ByteBuffer buf = ByteBuffer.allocateDirect(quads * 4 * TerrainRegions.STRIDE).order(ByteOrder.nativeOrder());
        for (int v = 0; v < quads * 4; v++) {
            buf.putFloat(v * TerrainRegions.STRIDE, 1f);
            buf.putFloat(v * TerrainRegions.STRIDE + 4, 2f);
            buf.putFloat(v * TerrainRegions.STRIDE + 8, 3f);
        }
        return buf;
    }

    @Test
    @DisplayName("arena allocator: best fit, coalescing frees, live-gap queries, double free detected")
    void allocator() {
        final ArenaAllocator alloc = new ArenaAllocator(64);
        assertEquals(0, alloc.allocate(16));
        assertEquals(16, alloc.allocate(6)); // rounded up to 8
        assertEquals(24, alloc.allocate(8));
        assertEquals(32, alloc.used());
        assertTrue(alloc.allocated(0, 32));
        assertFalse(alloc.allocated(28, 36));
        alloc.free(16, 6);
        assertFalse(alloc.allocated(0, 32), "freed block in the gap");
        assertTrue(alloc.allocated(0, 16));
        assertTrue(alloc.allocated(24, 32));
        // Best fit: the 8-vertex hole, not the 32-vertex tail.
        assertEquals(16, alloc.allocate(8));
        alloc.free(0, 16);
        alloc.free(16, 8);
        alloc.free(24, 8);
        assertTrue(alloc.isEmpty());
        assertEquals(1, alloc.freeBlocks(), "everything merged back into one block");
        assertEquals(-1, alloc.allocate(68));
        assertEquals(0, alloc.allocate(64));
        boolean threw = false;
        alloc.free(0, 64);
        try {
            alloc.free(0, 4);
        } catch (final IllegalStateException expected) {
            threw = true;
        }
        assertTrue(threw, "double free");
    }

    @Test
    @DisplayName("draw ranges: sorted, touching joined, small live gaps drawn through, free gaps never, long ranges split")
    void drawRanges() {
        final ArenaAllocator alloc = new ArenaAllocator(1 << 17);
        alloc.allocate(80000);
        final DrawRanges r = new DrawRanges();
        r.add(100, 20);
        r.add(0, 100);   // touches the first
        r.add(200, 40);  // gap of 80 live vertices
        r.add(1000, 8);  // gap of 760: too large for 512
        assertEquals(2, r.merge(512, alloc, 32000));
        assertEquals(0, r.first(0));
        assertEquals(240, r.count(0));
        assertEquals(1000, r.first(1));

        // A free block inside the gap stops the join.
        final ArenaAllocator holes = new ArenaAllocator(1024);
        holes.allocate(64);
        holes.allocate(64);
        holes.allocate(64);
        holes.free(64, 64);
        r.clear();
        r.add(0, 64);
        r.add(128, 64);
        assertEquals(2, r.merge(512, holes, 32000), "gap is free memory");

        // Splitting at 32000, every piece on a quad boundary.
        r.clear();
        r.add(4, 70000);
        assertEquals(3, r.merge(512, alloc, 32000));
        assertEquals(4, r.first(0));
        assertEquals(32000, r.count(0));
        assertEquals(32004, r.first(1));
        assertEquals(64004, r.first(2));
        assertEquals(70000 - 64000, r.count(2));
    }

    @Test
    @DisplayName("regions: positions become region-local, sections share pages, pages grow and are freed")
    void regions() {
        final FakeBuffers gl = new FakeBuffers();
        TerrainRegions.resetForTests(gl);
        // Section (17, 5, -3) -> region (2, 1, -1), local section (1, 1, 5).
        final ByteBuffer data = mesh(2);
        final TerrainRegions.Slot slotA = TerrainRegions.store(null, 0, 17 * 16, 5 * 16, -3 * 16, data, 8);
        assertNotNull(slotA);
        assertEquals(1, TerrainRegions.pageCount());
        assertEquals(1f + 16, data.getFloat(0), 0f);
        assertEquals(2f + 16, data.getFloat(4), 0f);
        assertEquals(3f + 5 * 16, data.getFloat(8), 0f);
        assertEquals(0, data.position(), "the caller's buffer is not consumed");
        final ByteBuffer stored = gl.live.get(slotA.page.id);
        assertEquals(1f + 16, stored.getFloat(slotA.first * TerrainRegions.STRIDE), 0f);
        assertEquals(2 << 7, slotA.page.originX());
        assertEquals(1 << 6, slotA.page.originY());
        assertEquals(-1 << 7, slotA.page.originZ());

        // Same region and layer: same page. Other layer: its own page. Other region: its own page.
        final TerrainRegions.Slot slotB = TerrainRegions.store(null, 0, 16 * 16, 4 * 16, -8 * 16, mesh(1), 4);
        assertTrue(slotB.page == slotA.page);
        final TerrainRegions.Slot slotC = TerrainRegions.store(null, 1, 16 * 16, 4 * 16, -8 * 16, mesh(1), 4);
        assertFalse(slotC.page == slotA.page);
        final TerrainRegions.Slot slotD = TerrainRegions.store(null, 0, 24 * 16, 4 * 16, -8 * 16, mesh(1), 4);
        assertFalse(slotD.page == slotA.page);
        assertEquals(2, TerrainRegions.regionCount(), "two regions");

        // Too big for a page, or empty: not stored.
        assertNull(TerrainRegions.store(null, 0, 0, 0, 0, mesh(1), 0));
        assertNull(TerrainRegions.store(null, 0, 0, 0, 0, ByteBuffer.allocateDirect(4), TerrainRegions.PAGE_MAX_VERTICES + 4));

        // Re-storing a section replaces its slot; releasing everything deletes every buffer.
        final TerrainRegions.Slot slotA2 = TerrainRegions.store(slotA, 0, 17 * 16, 5 * 16, -3 * 16, mesh(3), 12);
        assertTrue(slotA.released);
        TerrainRegions.release(slotA2);
        TerrainRegions.release(slotB);
        TerrainRegions.release(slotC);
        TerrainRegions.release(slotD);
        assertEquals(0, TerrainRegions.pageCount());
        assertEquals(0L, TerrainRegions.pageBytes());
        assertTrue(gl.live.isEmpty(), "all buffers deleted");
        assertTrue(TerrainRegions.isEmpty());
    }

    @Test
    @DisplayName("regions: page capacities double from 2048 up to 16384 vertices")
    void pageGrowth() {
        assertEquals(2048, TerrainRegions.pageCapacity(100, 0));
        assertEquals(4096, TerrainRegions.pageCapacity(100, 2048));
        assertEquals(16384, TerrainRegions.pageCapacity(10000, 2048));
        assertEquals(16384, TerrainRegions.pageCapacity(100, 16384));
        assertEquals(16384, TerrainRegions.pageCapacity(16384, 0));
    }

    @Test
    @DisplayName("regions: queued ranges merge per page; a page emptied while queued is deleted after the layer")
    void drawing() {
        final FakeBuffers gl = new FakeBuffers();
        TerrainRegions.resetForTests(gl);
        final TerrainRegions.Slot slotA = TerrainRegions.store(null, 0, 0, 0, 0, mesh(4), 16);
        final TerrainRegions.Slot slotB = TerrainRegions.store(null, 0, 16, 0, 0, mesh(4), 16);
        TerrainRegions.beginLayer();
        TerrainRegions.queueAll(slotB);
        TerrainRegions.queue(slotA, 4, 8);
        assertEquals(1, TerrainRegions.preparePages());
        final TerrainRegions.RegionPage page = TerrainRegions.page(0);
        // slotA = [0,16), slotB = [16,32): slotA's [4,12) and slotB's [16,32) are 4 live vertices apart: one draw.
        assertEquals(1, page.ranges());
        assertEquals(4, page.rangeFirst(0));
        assertEquals(28, page.rangeCount(0));
        TerrainRegions.release(slotA);
        TerrainRegions.release(slotB);
        assertEquals(1, TerrainRegions.pageCount(), "still queued: kept until the layer ends");
        TerrainRegions.endLayer();
        assertEquals(0, TerrainRegions.pageCount());
        assertEquals(1, gl.deleted);
    }

    @Test
    @DisplayName("regions: switching batching rebuilds only when it matters")
    void configure() {
        TerrainRegions.resetForTests(new FakeBuffers());
        assertFalse(TerrainRegions.configure(false, true));
        assertTrue(TerrainRegions.configure(true, true));
        assertFalse(TerrainRegions.configure(true, true));
        assertFalse(TerrainRegions.configure(false, false), "pipeline off and nothing batched");
        TerrainRegions.resetForTests(TerrainRegions.GL);
    }
}
