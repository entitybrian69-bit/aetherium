package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Worker-pool sizing, the chunk-upload budget and block-entity distance culling. */
final class PerfLogicTest {

    @Test
    @DisplayName("worker threads: explicit value wins, desktop auto stays vanilla, Android auto leaves headroom")
    void workerThreadDecision() {
        assertEquals(6, WorkerThreads.decide(6, 8, false));
        assertEquals(255, WorkerThreads.decide(4000, 8, true));
        assertEquals(0, WorkerThreads.decide(0, 16, false), "desktop auto must not touch vanilla");
        assertEquals(5, WorkerThreads.decide(0, 8, true));
        assertEquals(3, WorkerThreads.decide(0, 6, true));
        assertEquals(2, WorkerThreads.decide(0, 4, true));
        assertEquals(0, WorkerThreads.decide(0, 2, true), "two cores: vanilla already uses one worker");
        for (int cores = 3; cores <= 64; cores++) {
            final int n = WorkerThreads.decide(0, cores, true);
            assertTrue(n >= 2 && n <= cores - 1, "never below 2 or above vanilla's cores-1 (cores=" + cores + ")");
        }
    }

    @Test
    @DisplayName("worker threads: the early reader finds the key in the grouped config file and tolerates junk")
    void workerThreadConfigParsing() {
        final String file = "{ // comment\n \"version\": 4, \"aetherium\": { \"performance\": { \"worker_threads\": 3 } } }";
        assertEquals(3, WorkerThreads.parseConfigured(file));
        assertEquals(0, WorkerThreads.parseConfigured("{\"version\":4,\"aetherium\":{}}"));
        assertEquals(0, WorkerThreads.parseConfigured("not json at all"));
        assertEquals(0, WorkerThreads.parseConfigured("{\"aetherium\":{\"performance\":{\"worker_threads\":-5}}}"));
        assertEquals(0, WorkerThreads.readConfigured(Paths.get("/definitely/missing/aetherium.json")));
    }

    @Test
    @DisplayName("worker threads: game directory comes from --gameDir, including paths with spaces")
    void gameDirectoryFromCommand() {
        assertEquals(Paths.get("/games/mc"), WorkerThreads.gameDirectory("net.fabricmc.Main --username x --gameDir /games/mc --assetsDir /a", "/cwd"));
        assertEquals(Paths.get("/storage/My Games/.minecraft"),
                WorkerThreads.gameDirectory("Main --gameDir /storage/My Games/.minecraft --width 854", "/cwd"));
        assertEquals(Paths.get("/last"), WorkerThreads.gameDirectory("Main --gameDir /last", "/cwd"));
        assertEquals(Paths.get("/cwd"), WorkerThreads.gameDirectory("Main --version 1.21", "/cwd"));
        assertEquals(Paths.get("/cwd"), WorkerThreads.gameDirectory(null, "/cwd"));
    }

    /** Fake clock: every task "takes" {@code step} nanoseconds. */
    private static final class StepClock implements UploadBudget.Clock {
        long now;

        @Override
        public long nanoTime() {
            return this.now;
        }
    }

    private static Queue<Runnable> tasks(final int count, final StepClock clock, final long cost, final int[] ran) {
        final Queue<Runnable> queue = new ArrayDeque<Runnable>();
        for (int i = 0; i < count; i++) {
            queue.add(() -> {
                clock.now += cost;
                ran[0]++;
            });
        }
        return queue;
    }

    @Test
    @DisplayName("upload budget: small queues drain fully, bursts stop at the time budget but never below the minimum")
    void uploadBudget() {
        final StepClock clock = new StepClock();
        final int[] ran = {0};
        Queue<Runnable> queue = tasks(UploadBudget.DRAIN_ALL_BELOW - 1, clock, 10_000_000L, ran);
        assertEquals(UploadBudget.DRAIN_ALL_BELOW - 1, UploadBudget.drain(queue, clock, 3_000_000L));
        assertTrue(queue.isEmpty(), "a short queue is always drained, however slow");

        ran[0] = 0;
        queue = tasks(500, clock, 1_000_000L, ran); // 1 ms per upload
        final int first = UploadBudget.drain(queue, clock, 3_000_000L);
        assertEquals(UploadBudget.MIN_PER_FRAME, first, "slow uploads: exactly the guaranteed minimum");
        assertEquals(500 - first, queue.size(), "the rest waits for the next frame");

        ran[0] = 0;
        queue = tasks(500, clock, 50_000L, ran); // 0.05 ms per upload
        final int fast = UploadBudget.drain(queue, clock, 3_000_000L);
        assertEquals(60, fast, "fast uploads continue until the 3 ms budget is spent");

        int frames = 1;
        while (!queue.isEmpty()) {
            UploadBudget.drain(queue, clock, 3_000_000L);
            frames++;
            assertTrue(frames < 100, "the queue must always make progress");
        }
        assertEquals(500, ran[0], "every task runs exactly once");
    }

    @Test
    @DisplayName("block entities: vanilla limit costs nothing, a shorter limit culls by distance to the block centre")
    void blockEntityCull() {
        BlockEntityCull.setDistance(64);
        assertFalse(BlockEntityCull.active());
        BlockEntityCull.setView(0, 0, 0);
        assertFalse(BlockEntityCull.beyond(10_000, 0, 0));

        BlockEntityCull.setDistance(32);
        assertTrue(BlockEntityCull.active());
        BlockEntityCull.setView(0.5, 64.5, 0.5);
        assertFalse(BlockEntityCull.beyond(32, 64, 0), "exactly 32 blocks away is still drawn");
        assertTrue(BlockEntityCull.beyond(33, 64, 0));
        assertTrue(BlockEntityCull.beyond(0, 64 + 40, 0), "vertical distance counts too");
        assertFalse(BlockEntityCull.beyond(-20, 60, 20));

        BlockEntityCull.setDistance(0);
        assertFalse(BlockEntityCull.active(), "0 or less means vanilla, never cull everything");
        BlockEntityCull.setDistance(64);
    }
}
