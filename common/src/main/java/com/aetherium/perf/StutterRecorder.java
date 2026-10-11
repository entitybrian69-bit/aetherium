package com.aetherium.perf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Logs frames that take far longer than usual, with what happened during them, to
 * {@code logs/aetherium-stutter.log}. Answers "what made that frame slow?" on devices where
 * nobody can attach a profiler.
 *
 * <p>Render thread: {@link #frame} once per frame, a handful of counter reads and one
 * comparison. Lines go through a bounded queue to a daemon writer thread, so the render thread
 * never touches the disk. At most {@link #MAX_LINES_PER_SECOND} lines per second, and the file
 * stops growing at {@link #MAX_BYTES}.</p>
 */
public final class StutterRecorder {
    /** A frame is a spike when it is this many times the running average... */
    static final double SPIKE_FACTOR = 2.5;
    /** ...and at least this long. */
    static final long MIN_SPIKE_NANOS = 45_000_000L;
    static final int MAX_LINES_PER_SECOND = 5;
    static final long MAX_BYTES = 1L << 20;

    // Per-frame counters, written by the render thread (and by worker threads for builds).
    public static int packetTasks;
    public static int packetBacklog;
    public static int uploads;
    public static int preparedUploads;
    public static int lightUpdates;
    public static volatile int buildsFinished;

    private static volatile boolean enabled;
    private static double averageNanos = 16_666_667.0;
    private static long lastFrame;
    private static long windowStart;
    private static int linesInWindow;
    private static long gcCount;
    private static long gcMillis;
    private static List<GarbageCollectorMXBean> collectors;

    private static BlockingQueue<String> queue;
    private static Thread writer;
    private static File file;

    private StutterRecorder() {
    }

    /** Starts (or stops) recording; the file is created on the first spike. */
    public static synchronized void configure(final boolean on, final File gameDir) {
        enabled = on;
        if (!on || writer != null) {
            return;
        }
        file = new File(new File(gameDir, "logs"), "aetherium-stutter.log");
        queue = new ArrayBlockingQueue<String>(64);
        try {
            collectors = ManagementFactory.getGarbageCollectorMXBeans();
        } catch (final Throwable error) {
            collectors = null;
        }
        final Thread t = new Thread(StutterRecorder::writeLoop, "Aetherium Stutter Log");
        t.setDaemon(true);
        t.start();
        writer = t;
    }

    public static boolean enabled() {
        return enabled;
    }

    /** Whether a frame of {@code frameNanos} counts as a spike against the running average. */
    static boolean isSpike(final long frameNanos, final double average) {
        return frameNanos >= MIN_SPIKE_NANOS && frameNanos >= average * SPIKE_FACTOR;
    }

    /** Next running average; spikes are blended in slowly so one hitch does not hide the next. */
    static double nextAverage(final double average, final long frameNanos, final boolean spike) {
        final double weight = spike ? 0.01 : 0.05;
        return average + (frameNanos - average) * weight;
    }

    /**
     * Render thread, once per frame (start of the frame).
     *
     * @param now   {@code System.nanoTime()}
     * @param extra a short description of other per-frame state (may be null)
     */
    public static void frame(final long now, final String extra) {
        final long previous = lastFrame;
        lastFrame = now;
        if (previous == 0L) {
            resetCounters();
            return;
        }
        final long frameNanos = now - previous;
        final boolean spike = isSpike(frameNanos, averageNanos);
        if (!enabled) {
            averageNanos = nextAverage(averageNanos, Math.min(frameNanos, 1_000_000_000L), spike);
            RebuildCoalescer.takeDeferred();
            resetCounters();
            return;
        }
        long gcDelta = 0L;
        long gcTime = 0L;
        final List<GarbageCollectorMXBean> beans = collectors;
        if (beans != null) {
            long count = 0L;
            long millis = 0L;
            for (int i = 0; i < beans.size(); i++) {
                final GarbageCollectorMXBean bean = beans.get(i);
                count += Math.max(0L, bean.getCollectionCount());
                millis += Math.max(0L, bean.getCollectionTime());
            }
            gcDelta = count - gcCount;
            gcTime = millis - gcMillis;
            gcCount = count;
            gcMillis = millis;
        }
        if (spike && frameNanos < 10_000_000_000L) {
            if (now - windowStart > 1_000_000_000L) {
                windowStart = now;
                linesInWindow = 0;
            }
            if (linesInWindow++ < MAX_LINES_PER_SECOND) {
                final StringBuilder line = new StringBuilder(160);
                line.append(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()))
                        .append(" frame=").append(frameNanos / 100_000L / 10.0).append("ms")
                        .append(" avg=").append(Math.round(averageNanos / 100_000.0) / 10.0).append("ms")
                        .append(" packets=").append(packetTasks).append(" backlog=").append(packetBacklog)
                        .append(" uploads=").append(uploads).append(" prepared=").append(preparedUploads)
                        .append(" builds=").append(buildsFinished)
                        .append(" deferred=").append(RebuildCoalescer.takeDeferred())
                        .append(" gc=").append(gcDelta).append('/').append(gcTime).append("ms");
                if (extra != null) {
                    line.append(' ').append(extra);
                }
                final BlockingQueue<String> q = queue;
                if (q != null) {
                    q.offer(line.toString());
                }
            }
        } else {
            RebuildCoalescer.takeDeferred();
        }
        averageNanos = nextAverage(averageNanos, Math.min(frameNanos, 1_000_000_000L), spike);
        resetCounters();
    }

    /** Running average frame time (kept even while logging is off; drives the upload budget). */
    public static double averageFrameNanos() {
        return averageNanos;
    }

    private static void resetCounters() {
        packetTasks = 0;
        packetBacklog = 0;
        uploads = 0;
        preparedUploads = 0;
        lightUpdates = 0;
        buildsFinished = 0;
    }

    private static void writeLoop() {
        boolean headerWritten = false;
        while (true) {
            final String line;
            try {
                line = queue.take();
            } catch (final InterruptedException stop) {
                return;
            }
            try {
                final File f = file;
                if (f.length() > MAX_BYTES) {
                    continue;
                }
                final File dir = f.getParentFile();
                if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
                    continue;
                }
                final OutputStream out = new FileOutputStream(f, true);
                try {
                    if (!headerWritten) {
                        headerWritten = true;
                        final String header = "# Aetherium stutter log " + new Date() + " cores="
                                + Runtime.getRuntime().availableProcessors() + " heapMB="
                                + (Runtime.getRuntime().maxMemory() >> 20) + " java=" + System.getProperty("java.version")
                                + "\n# frame = this frame, avg = running average; packets = queued network tasks run this frame"
                                + " (backlog = queue length); uploads = chunk meshes sent to the GPU (prepared = already"
                                + " converted on a worker); builds = chunk meshes finished; deferred = sections whose rebuild"
                                + " was merged; gc = collections/time since the previous frame\n";
                        out.write(header.getBytes(StandardCharsets.UTF_8));
                    }
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                } finally {
                    out.close();
                }
            } catch (final IOException | RuntimeException ignored) {
                // Diagnostics only: never let the log disturb the game.
            }
        }
    }
}
