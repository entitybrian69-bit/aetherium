package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.render.CompactTerrain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.5.0 terrain streaming: packet budget, rebuild coalescing, prepared uploads, pools, stutter log. */
final class StreamingTest {

    /** A queue of tasks that each advance a fake clock. */
    private static final class FakeTasks implements PacketBudget.Poller, PacketBudget.Clock {
        long now;
        int left;
        final long cost;
        int ran;

        FakeTasks(final int count, final long cost) {
            this.left = count;
            this.cost = cost;
        }

        @Override
        public boolean pollTask() {
            if (this.left == 0) {
                return false;
            }
            this.left--;
            this.ran++;
            this.now += this.cost;
            return true;
        }

        @Override
        public long nanoTime() {
            return this.now;
        }
    }

    @Test
    @DisplayName("packet budget: small queues drain fully, a slow burst is spread over frames")
    void packetBudgetSpreadsBursts() {
        PacketBudget.resetPressure();
        final FakeTasks small = new FakeTasks(5, 100_000L);
        assertEquals(5, PacketBudget.drain(small, small));
        assertEquals(0, PacketBudget.pressure());

        // 3 ms per chunk packet: the guaranteed minimum runs, then the budget stops the frame.
        final FakeTasks slow = new FakeTasks(100, 3_000_000L);
        assertEquals(PacketBudget.MIN_TASKS, PacketBudget.drain(slow, slow));
        assertEquals(1, PacketBudget.pressure(), "work left over raises the next frame's budget");
        assertEquals(92, slow.left);

        // Cheap tasks: as many as fit in the budget (4 ms + one 2 ms step = 6 ms / 0.1 ms).
        final FakeTasks fast = new FakeTasks(1000, 100_000L);
        assertEquals(60, PacketBudget.drain(fast, fast));
        assertEquals(2, PacketBudget.pressure());

        // An empty queue resets the pressure.
        final FakeTasks none = new FakeTasks(0, 0L);
        assertEquals(0, PacketBudget.drain(none, none));
        assertEquals(0, PacketBudget.pressure());
    }

    @Test
    @DisplayName("packet budget: grows with sustained backlog but never beyond the cap")
    void packetBudgetIsCapped() {
        PacketBudget.resetPressure();
        assertEquals(PacketBudget.BASE_NANOS, PacketBudget.budgetFor(0));
        assertEquals(PacketBudget.MAX_NANOS, PacketBudget.budgetFor(1000));
        for (int frame = 0; frame < 50; frame++) {
            final FakeTasks backlog = new FakeTasks(100_000, 100_000L);
            final int ran = PacketBudget.drain(backlog, backlog);
            assertTrue(ran * 100_000L <= PacketBudget.MAX_NANOS + 100_000L, "frame " + frame + " ran " + ran);
        }
        assertEquals(PacketBudget.MAX_NANOS, PacketBudget.budgetFor(PacketBudget.pressure()));
        PacketBudget.resetPressure();
    }

    @Test
    @DisplayName("rebuild coalescing: player edits and near built sections never wait; settling sections do")
    void rebuildCoalescing() {
        RebuildCoalescer.setEnabled(true);
        final long ms = 1_000_000L;
        final long t0 = 10_000 * ms;
        final double far = 64.0 * 64.0;
        final double near = 10.0 * 10.0;
        assertFalse(RebuildCoalescer.defer(t0 + 10 * ms, t0, t0 + 5 * ms, true, true, far), "player edit");
        assertFalse(RebuildCoalescer.defer(t0 + 10 * ms, t0, t0 + 5 * ms, false, true, near), "near and built");
        assertTrue(RebuildCoalescer.defer(t0 + 10 * ms, t0, t0 + 5 * ms, false, false, near), "near but never built");
        assertTrue(RebuildCoalescer.defer(t0 + 10 * ms, t0, t0 + 5 * ms, false, true, far), "far, just dirtied");
        assertFalse(RebuildCoalescer.defer(t0 + 200 * ms, t0, t0 + 50 * ms, false, true, far), "quiet long enough");
        assertFalse(RebuildCoalescer.defer(t0 + 700 * ms, t0, t0 + 690 * ms, false, true, far),
                "never delayed past the maximum, even while still being dirtied");
        assertFalse(RebuildCoalescer.defer(t0, 0L, t0, false, true, far), "unknown dirty time");
        RebuildCoalescer.takeDeferred();
        RebuildCoalescer.setEnabled(false);
        assertFalse(RebuildCoalescer.defer(t0 + 10 * ms, t0, t0 + 5 * ms, false, false, far), "disabled");
        RebuildCoalescer.setEnabled(true);
    }

    @Test
    @DisplayName("direct pool: power-of-two buckets, reuse, and foreign buffers are ignored")
    void directPool() {
        assertEquals(48L << 20, DirectPool.idleCap(4096L << 20, "aarch64"), "large 64-bit heap");
        assertEquals(48L << 20, DirectPool.idleCap(788L << 20, "aarch64"), "1/16 of 788 MB, capped at 48");
        assertEquals(32L << 20, DirectPool.idleCap(512L << 20, "aarch64"), "1/16 of the heap");
        assertEquals(16L << 20, DirectPool.idleCap(788L << 20, "arm"), "32-bit process");
        assertEquals(8L << 20, DirectPool.idleCap(64L << 20, "aarch64"), "floor");
        DirectPool.trim();
        final ByteBuffer a = DirectPool.acquire(100_000);
        assertEquals(131_072, a.capacity());
        assertEquals(100_000, a.limit());
        assertEquals(0, a.position());
        assertTrue(a.isDirect());
        assertEquals(ByteOrder.nativeOrder(), a.order());
        DirectPool.release(a);
        assertEquals(131_072L, DirectPool.idleBytes());
        final ByteBuffer b = DirectPool.acquire(70_000);
        assertTrue(a == b, "the released buffer is reused");
        assertEquals(70_000, b.limit());
        DirectPool.release(ByteBuffer.allocateDirect(100_000));
        DirectPool.release(ByteBuffer.allocate(65_536));
        assertEquals(0L, DirectPool.idleBytes(), "odd sizes and heap buffers are not pooled");
        final ByteBuffer huge = DirectPool.acquire(20 << 20);
        assertEquals(20 << 20, huge.capacity(), "beyond the largest bucket: exact allocation");
        final ByteBuffer src = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder());
        src.putLong(0, 0x0102030405060708L);
        final ByteBuffer copy = DirectPool.copyOf(src);
        assertEquals(8, copy.remaining());
        assertEquals(src.getLong(0), copy.getLong(0));
        DirectPool.trim();
        assertEquals(0L, DirectPool.idleBytes());
    }

    private static ByteBuffer upQuads(final int quads, final boolean partial) {
        final ByteBuffer data = ByteBuffer.allocateDirect(quads * 128 + 32).order(ByteOrder.nativeOrder());
        final float[][] up = {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}};
        for (int q = 0; q < quads; q++) {
            for (int v = 0; v < 4; v++) {
                data.putFloat(up[v][0] + q).putFloat(up[v][1]).putFloat(up[v][2]);
                data.put((byte) 200).put((byte) 150).put((byte) 100).put((byte) 255);
                data.putFloat(0.25f).putFloat(0.5f);
                data.putShort((short) 240).putShort((short) 128);
                data.put((byte) 0).put((byte) 127).put((byte) 0).put((byte) 0);
            }
        }
        if (partial) {
            data.putLong(1L).putLong(2L).putLong(3L).putLong(4L);
        }
        data.flip();
        return data;
    }

    @Test
    @DisplayName("prepared uploads: workers convert meshes, the render thread takes them by builder")
    void preparedUploads() {
        PreparedUploads.clear();
        final PreparedUploads.Prepared shader = PreparedUploads.prepare(upQuads(3, false), true);
        assertEquals(1, shader.format);
        assertEquals(3, shader.quads);
        assertEquals(3 * CompactTerrain.QUAD_BYTES, shader.data.remaining());
        assertNotNull(shader.layout);
        assertEquals(3, shader.layout.quadsIn(CompactTerrain.UP));
        assertNull(shader.error);

        final PreparedUploads.Prepared compat = PreparedUploads.prepare(upQuads(2, false), false);
        assertEquals(2, compat.format);
        assertEquals(2 * 128, compat.data.remaining());

        final ByteBuffer odd = upQuads(1, true);
        final PreparedUploads.Prepared raw = PreparedUploads.prepare(odd, true);
        assertEquals(0, raw.format, "a partial quad keeps vanilla's bytes");
        assertEquals(odd.remaining(), raw.data.remaining());
        assertEquals(0, odd.position(), "the source buffer is not consumed");

        // A conversion that fails outright still yields a record (no data, an error), so the
        // upload never goes back to the builder the mesh was already taken from.
        final PreparedUploads.Prepared lost = PreparedUploads.prepare(null, true);
        assertNull(lost.data);
        assertNotNull(lost.error);
        PreparedUploads.done(lost);

        final Object builderA = new Object();
        final Object builderB = new Object();
        PreparedUploads.put(builderA, shader);
        PreparedUploads.put(builderB, compat);
        assertEquals(2, PreparedUploads.pending());
        assertNull(PreparedUploads.take(new Object()));
        assertTrue(PreparedUploads.take(builderB) == compat);
        assertNull(PreparedUploads.take(builderB), "taken once");
        PreparedUploads.done(compat);
        // Several builds through one builder before their uploads run (the render thread's shared
        // builders): every mesh is kept and handed out in pop order, none is replaced.
        PreparedUploads.put(builderA, raw);
        assertEquals(2, PreparedUploads.pending(), "a second mesh of the same builder is kept");
        assertTrue(PreparedUploads.take(builderA) == shader, "oldest first");
        assertEquals(1, PreparedUploads.pending());
        assertTrue(PreparedUploads.take(builderA) == raw, "then the newer one");
        assertNull(PreparedUploads.take(builderA));
        assertEquals(0, PreparedUploads.pending());
        PreparedUploads.put(builderA, shader);
        PreparedUploads.clear();
        assertEquals(0, PreparedUploads.pending());
        DirectPool.trim();
    }

    @Test
    @DisplayName("a render-thread rebuild makes the next upload drain run the whole queue, once")
    void drainAllAfterRenderThreadBuild() {
        final java.util.concurrent.ConcurrentLinkedQueue<Runnable> queue = new java.util.concurrent.ConcurrentLinkedQueue<Runnable>();
        final int[] ran = new int[1];
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                ran[0]++;
            }
        };
        for (int i = 0; i < 100; i++) {
            queue.add(task);
        }
        final long[] now = new long[1];
        final UploadBudget.Clock clock = new UploadBudget.Clock() {
            @Override
            public long nanoTime() {
                return now[0] += 1_000_000L; // every upload "takes" 1 ms
            }
        };
        assertEquals(UploadBudget.MIN_PER_FRAME, UploadBudget.drain(queue, clock, 1_000_000L), "budgeted");
        UploadBudget.requestDrainAll();
        assertEquals(100 - UploadBudget.MIN_PER_FRAME, UploadBudget.drain(queue, clock, 1_000_000L), "everything, in order");
        assertEquals(100, ran[0]);
        for (int i = 0; i < 100; i++) {
            queue.add(task);
        }
        assertEquals(UploadBudget.MIN_PER_FRAME, UploadBudget.drain(queue, clock, 1_000_000L), "only once");
        queue.clear();
    }

    @Test
    @DisplayName("upload budget adapts to the frame time within 1.5-4 ms")
    void adaptiveUploadBudget() {
        assertEquals(UploadBudget.MIN_BUDGET_NANOS, UploadBudget.budgetFor(5_000_000.0));
        assertEquals(2_000_000L, UploadBudget.budgetFor(16_666_667.0), 1_000L);
        assertEquals(UploadBudget.MAX_BUDGET_NANOS, UploadBudget.budgetFor(100_000_000.0));
        UploadBudget.adapt(33_333_333.0);
        assertEquals(3_999_999L, UploadBudget.currentBudgetNanos(), 1_000L);
        UploadBudget.adapt(16_666_667.0);
    }

    @Test
    @DisplayName("stutter log: only long frames well above the average count as spikes")
    void stutterSpikes() {
        final double avg = 16_666_667.0;
        assertFalse(StutterRecorder.isSpike(30_000_000L, avg), "under the 45 ms floor");
        assertTrue(StutterRecorder.isSpike(60_000_000L, avg));
        assertFalse(StutterRecorder.isSpike(60_000_000L, 40_000_000.0), "a slow device's normal frame");
        assertTrue(StutterRecorder.isSpike(120_000_000L, 40_000_000.0));
        final double after = StutterRecorder.nextAverage(avg, 200_000_000L, true);
        assertTrue(after < avg * 1.2, "one spike barely moves the average");
        assertTrue(StutterRecorder.nextAverage(avg, 33_000_000L, false) > avg);
    }

    @Test
    @DisplayName("threads: own mesh threads shrink the Android world-generation pool; mesh pool sizing")
    void threadSizing() {
        assertEquals(3, WorkerThreads.decide(0, 8, true, true));
        assertEquals(2, WorkerThreads.decide(0, 6, true, true));
        assertEquals(2, WorkerThreads.decide(0, 4, true, true));
        assertEquals(4, WorkerThreads.decide(0, 16, true, true));
        assertEquals(0, WorkerThreads.decide(0, 2, true, true));
        assertEquals(0, WorkerThreads.decide(0, 16, false, true), "desktop auto stays vanilla");
        assertEquals(6, WorkerThreads.decide(6, 8, true, true), "explicit value wins");
        assertEquals(5, WorkerThreads.decide(0, 8, true), "without own mesh threads: unchanged");
        assertEquals(3, ChunkBuildExecutor.threadsFor(8, true));
        assertEquals(2, ChunkBuildExecutor.threadsFor(4, true));
        assertEquals(2, ChunkBuildExecutor.threadsFor(4, false));
        assertEquals(4, ChunkBuildExecutor.threadsFor(12, false));
        assertEquals(6, ChunkBuildExecutor.threadsFor(64, false));
    }
}
