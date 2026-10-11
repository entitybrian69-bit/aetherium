package com.aetherium.perf;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated threads for chunk meshing.
 *
 * <p>In 1.16.5 the chunk mesh builder shares Minecraft's background pool with world generation,
 * lighting and saving. While new terrain is generated, mesh builds queue behind generation tasks,
 * so loaded chunks stay invisible longer and finish in bursts. A small pool of its own keeps mesh
 * builds flowing at a steady rate, independent of world generation.</p>
 */
public final class ChunkBuildExecutor {
    private static volatile ThreadPoolExecutor pool;
    private static volatile int threads;

    private ChunkBuildExecutor() {
    }

    /** Mesh threads for this device. */
    public static int threadsFor(final int cores, final boolean android) {
        if (android) {
            return cores >= 8 ? 3 : 2;
        }
        return Math.max(2, Math.min(6, cores / 3));
    }

    /** The shared executor, created on first use. */
    public static synchronized Executor get(final int cores, final boolean android) {
        final int want = threadsFor(cores, android);
        if (pool == null || pool.isShutdown()) {
            final AtomicInteger n = new AtomicInteger();
            final ThreadFactory factory = new ThreadFactory() {
                @Override
                public Thread newThread(final Runnable r) {
                    final Thread t = new Thread(r, "Aetherium Chunk Builder " + n.incrementAndGet());
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
            };
            final ThreadPoolExecutor created = new ThreadPoolExecutor(want, want, 30L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(), factory);
            created.allowCoreThreadTimeOut(true);
            pool = created;
            threads = want;
        }
        return pool;
    }

    public static int threads() {
        return threads;
    }

    public static int queued() {
        final ThreadPoolExecutor p = pool;
        return p == null ? 0 : p.getQueue().size();
    }
}
