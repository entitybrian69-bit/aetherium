package com.aetherium.perf;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Turns the experimental terrain pipeline off after a session that ended while it was drawing.
 *
 * <p>A crash inside a GL driver or translator (GL4ES, MobileGlues, LTW ...) kills the process
 * natively: no Java exception, no crash report, nothing the pipeline's own fallback can catch. So
 * the pipeline writes a small marker file when it starts drawing and removes it when it stops or
 * the game exits normally. If the marker is still there at the next start, the pipeline is switched
 * off in the config and the Backend tab says why.</p>
 *
 * <p>Closing the app from the task switcher also skips the normal exit, so it can trip the guard.
 * Turning the option back on clears it.</p>
 */
public final class PipelineCrashGuard {

    static final String MARKER_NAME = "aetherium-pipeline.running";

    private static final AetheriumLog LOG = AetheriumLog.of(PipelineCrashGuard.class);

    private static volatile Path marker;
    private static volatile boolean armed;
    private static volatile boolean tripped;

    private PipelineCrashGuard() {
    }

    /**
     * At startup, after the config was loaded. Returns true when the previous session ended while the
     * pipeline was drawing; the option is then switched off in {@code config} (the caller saves it).
     */
    public static boolean checkAtStartup(final Path configDirectory, final AetheriumConfig config) {
        marker = configDirectory.resolve(MARKER_NAME);
        armed = false;
        tripped = false;
        if (!Files.exists(marker)) {
            return false;
        }
        deleteQuietly(marker);
        if (!config.experimentalChunkRenderer.get().booleanValue()) {
            return false;
        }
        config.experimentalChunkRenderer.set(Boolean.FALSE);
        tripped = true;
        LOG.warn("The last session ended while the Aetherium pipeline was drawing (most likely a crash in the "
                + "graphics driver or GL translator). The pipeline has been turned off; turn it back on in "
                + "Video Settings > Backend to try again.");
        return true;
    }

    /** The pipeline is about to draw: remember that until it stops or the game exits normally. */
    public static void arm() {
        final Path file = marker;
        if (armed || file == null) {
            return;
        }
        armed = true;
        tripped = false;
        try {
            Files.write(file, ("pipeline drawing since " + System.currentTimeMillis() + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (final IOException | RuntimeException error) {
            LOG.warn("Could not write the pipeline crash marker {}: {}", file, error.toString());
        }
    }

    /** The pipeline stopped (switched off, failed) or the game is exiting normally. */
    public static void disarm() {
        final Path file = marker;
        if (!armed || file == null) {
            return;
        }
        armed = false;
        deleteQuietly(file);
    }

    public static boolean isArmed() {
        return armed;
    }

    /** True when this session started with the pipeline switched off by the guard. */
    public static boolean wasTripped() {
        return tripped;
    }

    private static void deleteQuietly(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException | RuntimeException error) {
            LOG.warn("Could not delete the pipeline crash marker {}: {}", file, error.toString());
        }
    }
}
