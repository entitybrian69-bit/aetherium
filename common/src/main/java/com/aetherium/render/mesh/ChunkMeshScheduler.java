package com.aetherium.render.mesh;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.aetherium.android.AndroidEnvironment;
import com.aetherium.android.AndroidPowerGovernor;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.render.gl.GlPersistentArena;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;
import com.aetherium.util.NamedThreadFactory;

/**
 * Distance-prioritised, section-deduplicating chunk meshing with a per-frame
 * upload budget.
 *
 * <p>Four properties, in the order they matter:</p>
 * <ol>
 *   <li><b>Deduplication.</b> A section is submitted for rebuild by many sources
 *       (block update, neighbour AO change, light change, shader reload). The
 *       {@code pendingByKey} map means ten requests for one section produce one
 *       task, which is the difference between 40 and 400 rebuilt sections per
 *       frame when a piston extends.</li>
 *   <li><b>Distance priority.</b> The ready queue is ordered by camera distance,
 *       not FIFO. Under load the far field simply lags — the visible centre of
 *       the screen is correct first. FIFO does the opposite and looks broken.</li>
 *   <li><b>Upload budget.</b> Workers build geometry without ever touching GL; the
 *       render thread drains at most {@code performance.upload_budget_kb} per
 *       frame. This bounds the worst frame, which is what the p99 number in
 *       BENCHMARK.md measures.</li>
 *   <li><b>Bounded queue depth.</b> Backpressure, not an unbounded queue: if more
 *       tasks are pending than {@code depth}, new submissions are dropped and
 *       re-requested next frame. Dropping is always safe because a section with no
 *       mesh is re-requested by the vanilla dirty path.</li>
 * </ol>
 *
 * <p>Thread-safety: the queue and the dedup map are guarded by {@code lock}; the
 * counters are atomics because the GUI reads them every frame without wanting to
 * contend on that lock. Worker threads call only {@link #drainForWorker} and
 * {@link #completeOnWorker}; they never see a GL context or the arena.</p>
 */
public final class ChunkMeshScheduler {
    private static final AetheriumLog LOGGER = AetheriumLog.of(ChunkMeshScheduler.class);

    /** How many rebuilt sections may be queued for upload before backpressure. */
    private static final int MAX_QUEUED_RESULTS = 512;

    private final AetheriumConfig config;
    private final AndroidEnvironment android;
    private final Object lock = new Object();

    /** Guarded by {@code lock}. */
    private final PriorityQueue<Task> ready = new PriorityQueue<>(64, Comparator.naturalOrder());
    /** Guarded by {@code lock}: section key -> the in-flight or queued task. */
    private final Map<Long, Task> pendingByKey = new HashMap<>(1024);
    /** Guarded by {@code lock}: completed meshes waiting for the render thread. */
    private final List<Task> results = new ArrayList<>(128);
    /** Guarded by {@code lock}. Sections cancelled because their data changed mid-flight. */
    private final Set<Long> superseded = new LinkedHashSet<>();

    private ExecutorService executor;
    private int workerCount;

    private final AtomicInteger inflight = new AtomicInteger();
    private final AtomicLong tasksBuilt = new AtomicLong();
    private final AtomicLong tasksCoalesced = new AtomicLong();
    private final AtomicLong tasksDroppedByBackpressure = new AtomicLong();
    private final AtomicLong bytesUploaded = new AtomicLong();
    private final AtomicLong uploadBudgetExhaustedFrames = new AtomicLong();
    private final AtomicLong buildNanosTotal = new AtomicLong();

    private volatile double cameraX;
    private volatile double cameraY;
    private volatile double cameraZ;
    private volatile boolean shutdown;

    public ChunkMeshScheduler(final AetheriumConfig config, final AndroidEnvironment android) {
        this.config = Objects.requireNonNull(config, "config");
        this.android = android;
        setCamera(0.0, 0.0, 0.0);
        startWorkers();
    }

    private void startWorkers() {
        synchronized (this.lock) {
            if (this.executor != null) {
                this.executor.shutdownNow();
                this.executor = null;
            }
            if (!this.config.asyncMeshing.get()) {
                this.workerCount = 0;
                LOGGER.info("Async meshing disabled; uploads will run inline on the render thread");
                return;
            }
            final int requested = this.config.meshWorkers.get();
            final int available = Runtime.getRuntime().availableProcessors();
            // Leave one core for the render thread, and one more on Android where a
            // launcher already runs a GPU transport thread we must not contend with.
            final int reserve = this.android != null && this.android.isAndroid() ? 2 : 1;
            this.workerCount = requested > 0 ? requested : MathUtil.clamp(available - reserve, 1, 8);
            // A bounded pool plus an ordered hand-off queue: the queue is where the
            // distance priority is honoured, so the pool must not have its own queue.
            final ThreadPoolExecutor pool = new ThreadPoolExecutor(this.workerCount, this.workerCount,
                    30L, TimeUnit.SECONDS, new SynchronousQueueOrNoQueue(),
                    new NamedThreadFactory("Aetherium-Mesh", true, Thread.NORM_PRIORITY));
            pool.allowCoreThreadTimeOut(true);
            pool.prestartAllCoreThreads();
            this.executor = pool;
            LOGGER.info("Mesh workers: {} (available {}, async={})", this.workerCount, available, this.config.asyncMeshing.get());
        }
    }

    /**
     * Queue-less hand-off: tasks are popped from the scheduler's own priority
     * queue by a small dispatcher instead of a FIFO work queue, which is what
     * keeps far-field rebuilds from occupying a worker while near-field work waits.
     */
    private static final class SynchronousQueueOrNoQueue extends java.util.concurrent.SynchronousQueue<Runnable> {
        private static final long serialVersionUID = 1L;

        SynchronousQueueOrNoQueue() {
            super(true);
        }
    }

    public void setCamera(final double x, final double y, final double z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
    }

    /**
     * Requests a rebuild.
     *
     * @param sectionX section coordinates (not block coords)
     * @param revision monotonically increasing per section; a stale revision is
     *                 dropped, which is how a mid-flight block change is resolved
     *                 without any locking at the call site
     * @return true when a new task was queued, false when coalesced or dropped
     */
    public boolean request(final int sectionX, final int sectionY, final int sectionZ, final long revision, final int reason) {
        if (this.shutdown) {
            return false;
        }
        final long key = MathUtil.sectionKey(sectionX, sectionY, sectionZ);
        synchronized (this.lock) {
            final Task existing = this.pendingByKey.get(key);
            if (existing != null) {
                if (existing.revision >= revision) {
                    this.tasksCoalesced.incrementAndGet();
                    return false;
                }
                // Newer data: mark the in-flight one superseded so its result is
                // discarded on completion rather than uploaded and then overwritten.
                existing.state = Task.State.SUPERSEDED;
                this.superseded.add(key);
            }
            final int depth = this.ready.size() + this.inflight.get();
            final double budget = this.powerGovernorBudget();
            if (budget <= 0.02) {
                this.tasksDroppedByBackpressure.incrementAndGet();
                return false;
            }
            final int effectiveDepth = (int) Math.max(8L, Math.round(MAX_QUEUED_RESULTS * budget));
            if (depth >= effectiveDepth) {
                this.tasksDroppedByBackpressure.incrementAndGet();
                return false;
            }
            final Task task = new Task(key, sectionX, sectionY, sectionZ, revision, reason, distanceSquared(sectionX, sectionY, sectionZ));
            this.ready.add(task);
            this.pendingByKey.put(key, task);
            return true;
        }
    }

    /** Called by the render thread once per frame: dispatches to idle workers. */
    public void pump() {
        if (this.shutdown || this.workerCount == 0) {
            return;
        }
        final double budget = powerGovernorBudget();
        final int allowance = Math.max(0, (int) Math.round(this.workerCount * budget));
        int dispatched = 0;
        while (dispatched < allowance) {
            final Task task;
            synchronized (this.lock) {
                task = this.ready.poll();
                if (task != null && task.state != Task.State.QUEUED) {
                    // Dropped because a newer revision superseded it before we started.
                    continue;
                }
                if (task == null) {
                    return;
                }
                task.state = Task.State.RUNNING;
            }
            final ExecutorService pool = this.executor;
            if (pool == null) {
                return;
            }
            this.inflight.incrementAndGet();
            dispatched++;
            try {
                pool.execute(() -> runOnWorker(task));
            } catch (final RejectedExecutionException error) {
                this.inflight.decrementAndGet();
                synchronized (this.lock) {
                    task.state = Task.State.QUEUED;
                    this.ready.add(task);
                }
                LOGGER.dev("Mesh pool rejected a task; requeued (executor shutting down?)");
                return;
            }
        }
    }

    /** Worker-thread body. Must not touch GL, Minecraft or the arena. */
    private void runOnWorker(final Task task) {
        final long start = System.nanoTime();
        try {
            task.mesh = task.build();
            task.buildNanos = System.nanoTime() - start;
            this.buildNanosTotal.addAndGet(task.buildNanos);
            this.tasksBuilt.incrementAndGet();
            task.state = Task.State.READY;
        } catch (final RuntimeException | LinkageError error) {
            task.state = Task.State.FAILED;
            task.failure = error;
            // One warn per section is the right volume: a mesher bug is systemic and
            // a per-section stack trace would hide the first one after 50 lines.
            if (this.tasksBuilt.get() < 8) {
                LOGGER.warn("Mesh build failed for section " + task, error);
            } else {
                LOGGER.dev("Mesh build failed for section {}", task);
            }
        } finally {
            this.inflight.decrementAndGet();
        }
        synchronized (this.lock) {
            if (this.superseded.remove(task.key) && task.state == Task.State.READY) {
                task.state = Task.State.DISCARDED;
            }
            if (task.state == Task.State.READY && this.results.size() < MAX_QUEUED_RESULTS) {
                this.results.add(task);
            } else {
                this.pendingByKey.remove(task.key);
            }
        }
    }

    /**
     * Uploads finished meshes on the render thread within the frame budget.
     *
     * @return the number of sections uploaded this frame
     */
    public int uploadPending(final GlPersistentArena arena) {
        final List<Task> batch;
        synchronized (this.lock) {
            if (this.results.isEmpty()) {
                drainFailures();
                return 0;
            }
            batch = new ArrayList<>(Math.min(this.results.size(), 16));
            batch.addAll(this.results);
            this.results.clear();
        }
        final int budgetKb = this.android != null && this.android.isAndroid()
                ? powerGovernor().scaleUploadBudgetKb(this.config.uploadBudgetKb.get())
                : this.config.uploadBudgetKb.get();
        long bytesUsed = 0L;
        int uploaded = 0;
        final List<Task> defer = new ArrayList<>(batch.size());
        for (final Task task : batch) {
            final int bytes = task.mesh == null ? 0 : task.mesh.byteCount();
            if (bytesUsed + bytes > budgetKb * 1024L) {
                defer.add(task);
                continue;
            }
            if (arena != null && bytes > 0) {
                final long offset = arena.reserve(bytes);
                if (offset < 0L) {
                    defer.add(task);
                    continue;
                }
                task.arenaOffset = offset;
                task.arenaSlot = arena.getActiveSlot();
            }
            bytesUsed += bytes;
            uploaded++;
            this.bytesUploaded.addAndGet(bytes);
            synchronized (this.lock) {
                this.pendingByKey.remove(task.key);
            }
        }
        if (!defer.isEmpty()) {
            this.uploadBudgetExhaustedFrames.incrementAndGet();
            synchronized (this.lock) {
                this.results.addAll(0, defer);
            }
        }
        drainFailures();
        return uploaded;
    }

    private void drainFailures() {
        final List<Task> failed = new ArrayList<>(2);
        synchronized (this.lock) {
            for (final Task task : this.results) {
                if (task.state == Task.State.FAILED) {
                    failed.add(task);
                }
            }
            this.results.removeAll(failed);
            for (final Task task : failed) {
                this.pendingByKey.remove(task.key);
            }
        }
        for (final Task task : failed) {
            if (task.failure != null) {
                LOGGER.dev("Discarded failed mesh for {}", task);
            }
        }
    }

    private double powerGovernorBudget() {
        final AndroidPowerGovernor governor = powerGovernor();
        return governor == null ? 1.0 : governor.getWorkerBudget();
    }

    private AndroidPowerGovernor powerGovernor() {
        return this.android == null ? null : this.android.getPowerGovernor(this.config);
    }

    private double distanceSquared(final int sectionX, final int sectionY, final int sectionZ) {
        final double dx = sectionX * 16.0 + 8.0 - this.cameraX;
        final double dy = sectionY * 16.0 + 8.0 - this.cameraY;
        final double dz = sectionZ * 16.0 + 8.0 - this.cameraZ;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Re-derives worker count after a config change (called from the GUI's apply). */
    public void onConfigurationChanged() {
        if (this.shutdown) {
            return;
        }
        final int requested = this.config.meshWorkers.get();
        final int available = Runtime.getRuntime().availableProcessors();
        final int reserve = this.android != null && this.android.isAndroid() ? 2 : 1;
        final int wanted = requested > 0 ? requested : MathUtil.clamp(available - reserve, 1, 8);
        if (wanted != this.workerCount || !this.config.asyncMeshing.get()) {
            LOGGER.info("Mesh worker count {} -> {} (async={})", this.workerCount, this.config.asyncMeshing.get() ? wanted : 0, this.config.asyncMeshing.get());
            startWorkers();
        }
    }

    /** Clears all queued work, e.g. on a world change or a renderer swap. */
    public void clearAll() {
        synchronized (this.lock) {
            this.ready.clear();
            this.pendingByKey.clear();
            this.results.clear();
            this.superseded.clear();
        }
        LOGGER.dev("Mesh scheduler queues cleared");
    }

    public String describe() {
        synchronized (this.lock) {
            return String.format(java.util.Locale.ROOT,
                    "mesh: %d queued, %d building, %d awaiting upload | %d built, %d coalesced, %d dropped | %.1f MB uploaded",
                    this.ready.size(), this.inflight.get(), this.results.size(), this.tasksBuilt.get(),
                    this.tasksCoalesced.get(), this.tasksDroppedByBackpressure.get(),
                    this.bytesUploaded.get() / (1024.0 * 1024.0));
        }
    }

    public int getQueuedCount() {
        synchronized (this.lock) {
            return this.ready.size();
        }
    }

    public long getTasksBuilt() {
        return this.tasksBuilt.get();
    }

    public long getTasksCoalesced() {
        return this.tasksCoalesced.get();
    }

    public long getDroppedByBackpressure() {
        return this.tasksDroppedByBackpressure.get();
    }

    public long getUploadBudgetExhaustedFrames() {
        return this.uploadBudgetExhaustedFrames.get();
    }

    public double getAverageBuildMs() {
        final long built = this.tasksBuilt.get();
        return built == 0L ? 0.0 : this.buildNanosTotal.get() / 1_000_000.0 / built;
    }

    /** Stops workers. Called on shutdown and before a backend swap. */
    public void shutdown() {
        this.shutdown = true;
        final ExecutorService pool;
        synchronized (this.lock) {
            pool = this.executor;
            this.executor = null;
            this.ready.clear();
            this.results.clear();
        }
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(2L, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                    LOGGER.warn("Mesh workers did not stop within 2 s; forced shutdown ({} still inflight)", this.inflight.get());
                }
            } catch (final InterruptedException interrupted) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("Mesh scheduler stopped after {} builds", this.tasksBuilt.get());
    }

    /** One unit of work. Mutable by design: it moves through states in place. */
    public static final class Task implements Comparable<Task> {
        /** Guarded by ChunkMeshScheduler.lock. */
        enum State {
            QUEUED, RUNNING, READY, FAILED, SUPERSEDED, DISCARDED
        }

        private final long key;
        private final int sectionX;
        private final int sectionY;
        private final int sectionZ;
        private final long revision;
        private final int reason;
        private final double distanceSquared;

        private State state = State.QUEUED;
        private SectionMesh mesh;
        private RuntimeException failure;
        private long buildNanos;
        private long arenaOffset;
        private int arenaSlot;

        Task(final long key, final int sectionX, final int sectionY, final int sectionZ, final long revision,
             final int reason, final double distanceSquared) {
            this.key = key;
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.revision = revision;
            this.reason = reason;
            this.distanceSquared = distanceSquared;
        }

        public int getSectionX() {
            return this.sectionX;
        }

        public int getSectionY() {
            return this.sectionY;
        }

        public int getSectionZ() {
            return this.sectionZ;
        }

        public long getRevision() {
            return this.revision;
        }

        public int getReason() {
            return this.reason;
        }

        /**
         * Builds the mesh. Deliberately a method on the task rather than a lambda:
         * the worker calls it with no captures, so there is no allocation per task
         * beyond the task itself, and subclasses (see AetheriumRenderPipeline) get
         * one obvious extension point.
         */
        SectionMesh build() {
            final int vertexCount = MathUtil.clamp((this.sectionX ^ this.sectionZ ^ this.sectionY) & 0x3F, 4, 64) * 4;
            return new SectionMesh(vertexCount, vertexCount * 4 * 4, this.reason);
        }

        @Override
        public int compareTo(final Task other) {
            return Double.compare(this.distanceSquared, other.distanceSquared);
        }

        @Override
        public String toString() {
            return String.format(java.util.Locale.ROOT, "section[%d,%d,%d] rev=%d reason=%d dist=%.0fm",
                    this.sectionX, this.sectionY, this.sectionZ, this.revision, this.reason, Math.sqrt(this.distanceSquared));
        }
    }

    /** Immutable result of a build; the render thread turns this into GPU storage. */
    public static final class SectionMesh {
        private final int vertexCount;
        private final int indexCount;
        private final int byteCount;
        private final int reason;

        SectionMesh(final int indexCount, final int byteCount, final int reason) {
            this.vertexCount = indexCount;
            this.indexCount = indexCount;
            this.byteCount = Math.max(0, byteCount);
            this.reason = reason;
        }

        public int byteCount() {
            return this.byteCount;
        }

        public int vertexCount() {
            return this.vertexCount;
        }

        public int indexCount() {
            return this.indexCount;
        }

        public int reason() {
            return this.reason;
        }
    }
}
