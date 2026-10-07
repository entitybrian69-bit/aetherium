package com.aetherium.hud;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import com.aetherium.Aetherium;
import com.aetherium.util.AetheriumLog;

import net.minecraft.client.Minecraft;

/**
 * The in-game half of {@code tools/benchmark.sh --mc}.
 *
 * <p>Enabled only by a system property - {@code -Daetherium.benchmark=<directory>} - so a
 * normal player never pays for it and never gets a surprise file. While enabled it appends
 * one Markdown row per interval to {@code <directory>/frames.md}. That is the entire
 * feature: it measures, it does not judge, and it never prints a ratio.</p>
 *
 * <p>Thread-safety: every method here runs on the render thread from
 * {@code ClientHooks.endFrame}, so the mutable fields below need no guard. The one
 * exception is {@link #stop()}, which {@code Aetherium.shutdown} also calls on the main
 * thread; {@code disabled} is volatile so a torn overlap can only ever lose a row, and
 * {@code Files.write} with APPEND on a single line is atomic enough for a log at this
 * rate (one write per fifteen seconds).</p>
 */
public final class BenchmarkRecorder {
    /** Property the script passes; a directory, not a file. */
    public static final String PROPERTY = "aetherium.benchmark";
    public static final String INTERVAL_PROPERTY = "aetherium.benchmark.seconds";
    public static final String LABEL_PROPERTY = "aetherium.benchmark.label";

    private static final AetheriumLog LOGGER = AetheriumLog.of(BenchmarkRecorder.class);

    /** Volatile because {@link #stop()} can arrive from the main thread during shutdown. */
    private static volatile boolean disabled;
    private static Path file;
    private static long intervalNanos = 15_000_000_000L;
    private static long intervalStartNanos;
    private static long framesInInterval;
    private static String label = "aetherium";
    private static boolean headerWritten;
    private static boolean warnedOnce;

    private BenchmarkRecorder() {
    }

    /**
     * Reads the system property. Called once from {@code Aetherium.initialize}; a no-op
     * (and no file handle held) when the property is absent.
     */
    public static void start() {
        final String configured = System.getProperty(PROPERTY);
        if (configured == null || configured.isBlank()) {
            return;
        }
        final Path directory = Path.of(configured);
        // A previous stop() latches the recorder off; starting a new run must clear that,
        // or `benchmark.sh --mc` twice in one JVM session would silently record nothing.
        disabled = false;
        warnedOnce = false;
        headerWritten = false;
        framesInInterval = 0L;
        try {
            Files.createDirectories(directory);
            file = directory.resolve("frames.md");
            // The property name matches what tools/benchmark.sh passes with --seconds.
            final String interval = System.getProperty(INTERVAL_PROPERTY, "15");
            final double seconds = Double.parseDouble(interval);
            if (seconds < 1.0d) {
                throw new IllegalArgumentException("interval must be at least 1 second, got " + seconds);
            }
            intervalNanos = (long) (seconds * 1_000_000_000.0d);
            final String configuredLabel = System.getProperty(LABEL_PROPERTY);
            label = configuredLabel == null || configuredLabel.isBlank() ? "aetherium" : configuredLabel;
            intervalStartNanos = System.nanoTime();
            LOGGER.warn("Benchmark recording to {} every {} s - this is not a play session",
                    file, String.format(Locale.ROOT, "%.1f", seconds));
        } catch (final IOException | RuntimeException error) {
            // A benchmark run that silently records nothing is worse than one that refuses
            // to start, so this is a hard "disabled", reported at warn level.
            LOGGER.warn("Could not start benchmark recording into " + directory
                    + "; no frames will be written", error);
            file = null;
            disabled = true;
        }
    }

    /** Called once per frame from the client hook, after the frame has been recorded. */
    public static void onFrame(final FrameStats stats) {
        if (disabled || file == null || stats == null) {
            return;
        }
        framesInInterval++;
        final long now = System.nanoTime();
        if (now - intervalStartNanos < intervalNanos) {
            return;
        }
        intervalStartNanos = now;
        final long frames = framesInInterval;
        framesInInterval = 0L;
        try {
            writeRow(stats, frames);
            // A fresh window per interval: the row must describe the interval, not the session.
            stats.resetWindow();
        } catch (final RuntimeException error) {
            // A benchmark must never be the reason a frame throws. Report once, then stop.
            if (!warnedOnce) {
                warnedOnce = true;
                LOGGER.error("Benchmark recording failed and was disabled", error);
            }
            disabled = true;
        }
    }

    private static void writeRow(final FrameStats stats, final long frames) {
        final StringBuilder builder = new StringBuilder(256);
        builder.append(stats.formatMarkdownRow(describeLabel())).append('\n');
        try {
            if (!headerWritten) {
                ensureHeader();
            }
            Files.write(file, builder.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException error) {
            if (!warnedOnce) {
                warnedOnce = true;
                LOGGER.error("Benchmark row could not be written to " + file + "; recording stops", error);
            }
            disabled = true;
        }
        LOGGER.dev("Benchmark row: {} frames in the interval", frames);
    }

    private static void ensureHeader() throws IOException {
        if (Files.exists(file)) {
            // Appending to a file someone already started: their header stays.
            headerWritten = true;
            return;
        }
        final String header = """
                # Aetherium frame records

                Rows are written by `BenchmarkRecorder` while the game runs under
                `-Daetherium.benchmark=<dir>`. Each row covers one interval (default 15 s)
                and is measured on this machine only. Backend, render distance and seed are
                in the label column because a row without them is not reproducible.

                Append the hardware by hand before quoting any of this: CPU, GPU, driver,
                Java version, and the mod list. A number that cannot be re-run is not a
                benchmark, and this file deliberately contains no comparison to any other
                renderer.

                | measurement | fps | p50 ms | p99 ms | p99.9 ms | longest ms | spikes > 100 ms |
                | --- | ---: | ---: | ---: | ---: | ---: | ---: |
                """;
        Files.write(file, header.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        headerWritten = true;
    }

    /** Backend + render distance + seed: enough to re-run the row, nothing invented. */
    private static String describeLabel() {
        final StringBuilder builder = new StringBuilder(64);
        builder.append(label).append(' ');
        final com.aetherium.render.gl.GlDevice device = com.aetherium.client.ClientHooks.device();
        builder.append(device == null ? "compat" : device.getBackend().getDisplayName());
        builder.append(vanillaSettings());
        return builder.toString();
    }

    // [UNVERIFIED: GameOptions#renderDistance()/#simulationDistance() returning an
    // Option<Integer> holder is read straight from 1.21.1 (the 1.21.1 compile agrees: those two
    // lines are the ones CI did not reject). A rename on another version degrades the label to
    // "vanilla=unavailable" - the whole block is inside a catch, because a frame-loop label must
    // never be the reason a benchmark run crashes on port 15.]
    private static String vanillaSettings() {
        final Minecraft client = peekClient();
        if (client == null) {
            return "";
        }
        try {
            final StringBuilder builder = new StringBuilder(48);
            builder.append(" rd=").append(client.options.renderDistance().get());
            builder.append(" sim=").append(client.options.simulationDistance().get());
            // The seed is handed in by tools/benchmark.sh (-Daetherium.benchmark.seed) instead of
            // read off the level: ClientLevel has no getSeed() on 1.21.1 (javac: cannot find symbol),
            // and a row that quietly lost its seed would look reproducible without being one. An
            // unseeded manual run therefore says "unrecorded" rather than guessing.
            builder.append(" seed=").append(System.getProperty(PROPERTY + ".seed", "unrecorded"));
            return builder.toString();
        } catch (final RuntimeException | LinkageError error) {
            if (!warnedOnce) {
                LOGGER.dev("Benchmark label cannot read vanilla options: {}", error.toString());
            }
            return " vanilla=unavailable";
        }
    }

    /** Never throws: this runs inside the frame loop, where a missing client is normal. */
    private static Minecraft peekClient() {
        try {
            return Minecraft.getInstance();
        } catch (final RuntimeException | LinkageError error) {
            // LinkageError is not paranoia: the engine classes are unit-tested without a
            // Minecraft jar on the classpath, and a label must degrade rather than fail.
            if (!warnedOnce) {
                LOGGER.dev("No client handle for the benchmark label: {}", error.toString());
            }
            return null;
        }
    }

    /** Flushes the last partial interval, then stops. Safe to call twice. */
    public static void stop() {
        if (file == null || disabled) {
            return;
        }
        final FrameStats stats = Aetherium.subsystemsOrNull() == null ? null : Aetherium.frameStats();
        if (stats != null && framesInInterval > 0L) {
            writeRow(stats, framesInInterval);
            framesInInterval = 0L;
        }
        file = null;
        disabled = true;
        LOGGER.info("Benchmark recording stopped");
    }

    /** True while rows are being written; read by the GUI's Advanced tab. */
    public static boolean isRecording() {
        return file != null && !disabled;
    }

    /** Where the rows go, or null when recording is off. */
    public static Path getFile() {
        return file;
    }
}
