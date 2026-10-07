package com.aetherium.util;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Names Aetherium's threads so they are identifiable in a profiler capture or
 * {@code jstack}, which is how the mesh-upload contention measurements in
 * BENCHMARK.md get attributed.
 *
 * <p>Mesh workers are created as platform (non-daemon) threads with an explicit
 * priority: a render-thread starved by a lower-priority GC helper is a common
 * Android failure mode, so priorities are set rather than inherited.</p>
 */
public final class NamedThreadFactory implements ThreadFactory {
    /** Written by the constructing thread only; read by factory users. */
    private final String prefix;
    private final boolean daemon;
    private final int priority;
    private final Runnable onUncaughtContext;

    /** Monotonic per-factory counter. Volatile-safe via AtomicInteger (no lock). */
    private final AtomicInteger counter = new AtomicInteger();

    public NamedThreadFactory(final String prefix, final boolean daemon, final int priority) {
        this(prefix, daemon, priority, null);
    }

    public NamedThreadFactory(final String prefix, final boolean daemon, final int priority, final Runnable uncaughtLogger) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.daemon = daemon;
        this.priority = MathUtil.clamp(priority, Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);
        this.onUncaughtContext = uncaughtLogger;
    }

    @Override
    public Thread newThread(final Runnable task) {
        Objects.requireNonNull(task, "task");
        final Thread thread = new Thread(task, this.prefix + "-" + this.counter.incrementAndGet());
        thread.setDaemon(this.daemon);
        thread.setPriority(this.priority);
        // A worker that dies silently turns a 200 fps renderer into a stall, so the
        // handler is always installed even when the caller does not pass a logger.
        thread.setUncaughtExceptionHandler((t, error) -> {
            System.err.println("[Aetherium] uncaught error on " + t.getName());
            error.printStackTrace();
            if (this.onUncaughtContext != null) {
                this.onUncaughtContext.run();
            }
        });
        return thread;
    }
}
