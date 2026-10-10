package com.aetherium.render.gl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.aetherium.util.AetheriumLog;

/**
 * Drains GL program compilation without ever blocking the render thread.
 *
 * <p>Mechanism: {@code ARB_parallel_shader_compile} makes the driver compile on a
 * worker thread and expose progress as a single boolean,
 * {@code GL_COMPLETION_STATUS}. That turns "async compilation" from a lie
 * (a thread calling {@code glLinkProgram} still blocks, because the context is
 * current on one thread) into something real: submit the link, poll the status
 * once per frame, and only touch the program when it is ready.</p>
 *
 * <p>Poll budget: at most {@code MAX_POLLS_PER_FRAME} programs per frame. Each
 * {@code glGetProgrami} on this path can be a driver-side sync point on some
 * stacks, so unbounded polling measurably costs frame time on Intel — the cap
 * keeps the worst case at a few microseconds while still clearing a full queue of
 * 40 pipelines in under a second at 240 fps.</p>
 *
 * <p>Thread-safety: render-thread only. Mesh workers never link programs; they
 * only produce source. The queue is an {@link ArrayDeque} with no lock because
 * there is exactly one accessor.</p>
 */
public final class GlAsyncShaderCompiler implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(GlAsyncShaderCompiler.class);
    private static final int MAX_POLLS_PER_FRAME = 8;
    /**
     * Compiler threads we ask the driver for. Above ~4 the driver clamps to its own
     * internal maximum anyway (the parameter is a request, not an allocation), and a
     * mobile GPU with one shader core gains nothing from a larger ask - but it also
     * loses nothing, which is why this is a constant and not a config key.
     */
    private static final int MAX_COMPILER_THREADS = 4;

    /** A program submitted for linking, waiting on the driver. */
    public static final class Pending {
        private final int program;
        private final String key;
        private final String label;
        private final long submittedNanos;
        private int polls;

        Pending(final int program, final String key, final String label) {
            this.program = program;
            this.key = key;
            this.label = label;
            this.submittedNanos = System.nanoTime();
        }

        public int getProgram() {
            return this.program;
        }

        public String getLabel() {
            return this.label;
        }

        public int getPolls() {
            return this.polls;
        }
    }

    private final ArrayDeque<Pending> queue = new ArrayDeque<>(32);
    private final List<Pending> ready = new ArrayList<>(8);
    private final GlProgramCache cache;

    /** Guarded by nothing (single-threaded); read for the GUI's compile-stall readout. */
    private long totalWaitNanos;
    private long longestWaitNanos;
    private int submitted;
    private int completed;
    private int cacheHits;
    private boolean asyncUsable = true;
    /** Set once maxShaderCompilerThreads has been attempted; see submit(). */
    private boolean capLifted;

    public GlAsyncShaderCompiler(final GlProgramCache cache) {
        this.cache = cache;
    }

    /**
     * Submits a program for asynchronous completion.
     *
     * @param key cache key from {@link GlProgramCache#keyFor}, may be null to skip caching
     * @return true when the program was already usable (binary hit) and the caller
     *         may bind it this frame instead of waiting
     */
    public boolean submit(final int program, final String key, final String label) {
        Objects.requireNonNull(label, "label");
        if (program == 0) {
            throw new IllegalArgumentException("program must be a valid object name");
        }
        if (key != null && this.cache != null && this.cache.tryLoad(program, key)) {
            this.cacheHits++;
            LOGGER.dev("Program '{}' served from the binary cache", label);
            return true;
        }
        if (!this.asyncUsable) {
            return true;
        }
        if (!this.capLifted) {
            // First use: raise the driver's compiler-thread cap. Without this call the
            // extension is allowed to default to one compiler thread, and "async"
            // compiles queue behind each other - the exact stall this class exists to
            // avoid. Idempotent in practice (the value is a driver setting), and done
            // once here rather than at construction so a device that reports the
            // extension and then refuses the call is caught by the same guard that
            // handles a completion-status poll failing.
            this.capLifted = true;
            this.asyncUsable = GlProcs.maxShaderCompilerThreads(MAX_COMPILER_THREADS);
            if (!this.asyncUsable) {
                LOGGER.dev("parallel_shader_compile absent; compiling synchronously on the caller thread");
                return true;
            }
        }
        final Pending pending = new Pending(program, key, label);
        this.queue.addLast(pending);
        this.submitted++;
        return false;
    }

    /** Moves finished programs into {@link #drainReady()}. Called once per frame. */
    public void poll() {
        if (this.queue.isEmpty()) {
            return;
        }
        final List<Pending> deferred = new ArrayList<>(this.queue.size());
        int polls = 0;
        while (!this.queue.isEmpty() && polls < MAX_POLLS_PER_FRAME) {
            final Pending pending = this.queue.pollFirst();
            polls++;
            final int status;
            try {
                status = GlProcs.getProgrami(pending.program, GlProcs.GL_COMPLETION_STATUS);
            } catch (final RuntimeException | LinkageError error) {
                // No completion status on this driver: stop pretending to be
                // asynchronous and let callers link synchronously.
                LOGGER.warn("GL_COMPLETION_STATUS unsupported; async compilation disabled", error);
                this.asyncUsable = false;
                this.queue.clear();
                this.ready.add(pending);
                this.completed++;
                return;
            }
            if (status != GlProcs.GL_FALSE) {
                final long waited = System.nanoTime() - pending.submittedNanos;
                this.totalWaitNanos += waited;
                this.longestWaitNanos = Math.max(this.longestWaitNanos, waited);
                pending.polls = polls;
                this.ready.add(pending);
                this.completed++;
                if (this.cache != null && pending.key != null) {
                    this.cache.store(pending.program, pending.key);
                }
            } else {
                deferred.add(pending);
            }
        }
        // Re-queue in FIFO order so a program cannot be starved by a burst.
        for (final Pending pending : deferred) {
            this.queue.addLast(pending);
        }
    }

    /** @return programs finished since the last drain; the list is cleared by this call */
    public List<Pending> drainReady() {
        if (this.ready.isEmpty()) {
            return Collections.emptyList();
        }
        final List<Pending> out = new ArrayList<>(this.ready);
        this.ready.clear();
        return out;
    }

    /** Blocks until the queue is empty. Only for the "Reload shaders" GUI action. */
    public void flushBlocking(final long timeoutNanos) {
        final long deadline = System.nanoTime() + timeoutNanos;
        while (!this.queue.isEmpty() && System.nanoTime() < deadline) {
            poll();
            if (this.queue.isEmpty()) {
                break;
            }
            final String errors = GlProcs.drainErrors();
            if (errors != null) {
                LOGGER.dev("GL error while flushing shader queue: {}", errors);
            }
            GlProcs.flush();
            Thread.onSpinWait();
        }
        if (!this.queue.isEmpty()) {
            LOGGER.warn("Shader compile flush timed out with {} programs pending", this.queue.size());
        }
    }

    public int getPendingCount() {
        return this.queue.size();
    }

    public boolean isAsyncUsable() {
        return this.asyncUsable;
    }

    public String describe() {
        final double averageMs = this.completed == 0 ? 0.0 : this.totalWaitNanos / 1_000_000.0 / this.completed;
        return String.format(java.util.Locale.ROOT, "shader compile: %d submitted, %d done, avg %.2f ms, worst %.2f ms, %d binary-cache hits%s",
                this.submitted, this.completed, averageMs, this.longestWaitNanos / 1_000_000.0, this.cacheHits,
                this.asyncUsable ? "" : " (driver has no parallel compile)");
    }

    /** Average link latency, ms — reported by the benchmark harness. */
    public double getAverageWaitMs() {
        return this.completed == 0 ? 0.0 : this.totalWaitNanos / 1_000_000.0 / this.completed;
    }

    public void close() {
        LOGGER.info("{}", describe());
    }
}
