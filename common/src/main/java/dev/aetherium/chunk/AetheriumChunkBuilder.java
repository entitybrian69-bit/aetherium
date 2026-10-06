package dev.aetherium.chunk;

import dev.aetherium.Aetherium;
import dev.aetherium.android.AndroidLauncherCompat;
import dev.aetherium.config.AetheriumConfig;

import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Priority-driven asynchronous section build scheduler. Replaces the executor vanilla hands to
 * SectionRenderDispatcher so meshing work is ordered by distance to the camera, stale jobs for the same section are
 * superseded instead of queued twice, and thread count follows the config / mobile memory mode / thermal state.
 * Completed results are drained on the render thread in {@link #drainCompleted()} with a per-frame time budget.
 */
public final class AetheriumChunkBuilder implements Executor {
    private static final AetheriumChunkBuilder INSTANCE = new AetheriumChunkBuilder();
    private static final long DRAIN_BUDGET_NANOS = 2_000_000L; // 2 ms per frame for uploads

    private final AtomicLong sequence = new AtomicLong();
    private final ConcurrentHashMap<Long, Job> latestForSection = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Runnable> completed = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private volatile ThreadPoolExecutor pool;
    private volatile int configuredThreads;
    private volatile double camX, camY, camZ;
    private volatile boolean asyncEnabled = true;

    private static final class Job implements Runnable, Comparable<Job> {
        final Runnable task;
        final long sectionKey;
        final long seq;
        final double distanceSq;
        volatile boolean cancelled;

        Job(Runnable task, long sectionKey, long seq, double distanceSq) {
            this.task = task; this.sectionKey = sectionKey; this.seq = seq; this.distanceSq = distanceSq;
        }

        @Override public void run() {
            if (cancelled) return;
            task.run();
        }

        @Override public int compareTo(Job o) {
            int c = Double.compare(distanceSq, o.distanceSq);
            return c != 0 ? c : Long.compare(seq, o.seq);
        }
    }

    private AetheriumChunkBuilder() {}

    public static AetheriumChunkBuilder get() { return INSTANCE; }

    public static int autoThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        int t = Math.max(1, cores - 1);
        if (AndroidLauncherCompat.isAndroid()) t = Math.max(1, Math.min(t, cores / 2)); // big.LITTLE: avoid efficiency cores
        return Math.min(t, 16);
    }

    public synchronized void applyConfig(AetheriumConfig config) {
        asyncEnabled = config.performance.asyncChunkMeshing;
        int threads = config.performance.chunkBuilderThreads > 0 ? config.performance.chunkBuilderThreads : autoThreads();
        if (config.android.mobileMemoryMode) threads = Math.max(1, threads / 2);
        if (AndroidLauncherCompat.isThermallyThrottled(config)) threads = Math.max(1, threads / 2);
        if (threads == configuredThreads && pool != null) return;
        configuredThreads = threads;
        if (pool == null) {
            pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
                    new PriorityBlockingQueue<>(256, Comparator.naturalOrder()), factory());
            pool.allowCoreThreadTimeOut(true);
        } else {
            pool.setCorePoolSize(threads);
            pool.setMaximumPoolSize(threads);
        }
        Aetherium.LOGGER.info("[Aetherium] Chunk builder threads = {}", threads);
    }

    private static ThreadFactory factory() {
        AtomicInteger n = new AtomicInteger();
        return r -> {
            Thread t = new Thread(r, "Aetherium-ChunkBuilder-" + n.incrementAndGet());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };
    }

    public void setCameraPosition(double x, double y, double z) { camX = x; camY = y; camZ = z; }

    /** Plain Executor contract (what vanilla's SectionRenderDispatcher calls); priority is by submission order only. */
    @Override public void execute(Runnable command) {
        submit(command, Long.MIN_VALUE, 0, 0, 0);
    }

    /** Preferred entry: distance-prioritized and de-duplicated per section. */
    public void submit(Runnable task, long sectionKey, int sectionX, int sectionY, int sectionZ) {
        ThreadPoolExecutor p = pool;
        if (p == null || !asyncEnabled) { task.run(); return; }
        double dx = (sectionX * 16 + 8) - camX, dy = (sectionY * 16 + 8) - camY, dz = (sectionZ * 16 + 8) - camZ;
        double distSq = sectionKey == Long.MIN_VALUE ? 0 : dx * dx + dy * dy + dz * dz;
        Job job = new Job(() -> {
            try { task.run(); } finally { inFlight.decrementAndGet(); }
        }, sectionKey, sequence.incrementAndGet(), distSq);
        if (sectionKey != Long.MIN_VALUE) {
            Job prev = latestForSection.put(sectionKey, job);
            if (prev != null) prev.cancelled = true;
        }
        inFlight.incrementAndGet();
        p.execute(job);
    }

    /** Worker threads enqueue GPU-upload closures here; the render thread runs them inside the frame budget. */
    public void completeOnRenderThread(Runnable upload) { completed.add(upload); }

    public void drainCompleted() {
        long start = System.nanoTime();
        Runnable r;
        while ((r = completed.poll()) != null) {
            r.run();
            if (System.nanoTime() - start > DRAIN_BUDGET_NANOS) break;
        }
    }

    public static long sectionKey(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }

    public int threads() { return configuredThreads; }
    public int queued() { ThreadPoolExecutor p = pool; return p == null ? 0 : p.getQueue().size(); }
    public int inFlight() { return inFlight.get(); }
    public int pendingUploads() { return completed.size(); }

    public synchronized void shutdown() {
        ThreadPoolExecutor p = pool;
        pool = null;
        if (p != null) p.shutdownNow();
        latestForSection.clear();
        completed.clear();
    }
}
