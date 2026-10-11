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
 * <p>The marker records which mode was drawing. Shader mode was removed in 1.5.5 (it kept crashing
 * GL4ES), so a shader-mode marker (or one without a mode, from 1.3.1/1.3.2) left by an older version
 * is ignored; a crash in compatible mode turns the pipeline off. A crash while drawing batched
 * terrain (1.6.0, {@link #MODE_REGIONS}) first turns only batching off.</p>
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
    private static volatile boolean batchingOff;
    private static volatile String armedMode = "";

    /** Marker modes. */
    public static final String MODE_SHADER = "shader";
    public static final String MODE_COMPATIBLE = "compatible";
    /** Compatible mode with batched terrain (TerrainRegions). */
    public static final String MODE_REGIONS = "regions";

    private PipelineCrashGuard() {
    }

    /**
     * At startup, after the config was loaded. Returns true when the previous session ended while the
     * pipeline was drawing; the option is then switched off in {@code config} (the caller saves it).
     */
    public static boolean checkAtStartup(final Path configDirectory, final AetheriumConfig config) {
        marker = configDirectory.resolve(MARKER_NAME);
        armed = false;
        armedMode = "";
        tripped = false;
        batchingOff = false;
        if (!Files.exists(marker)) {
            return false;
        }
        final String content = readQuietly(marker);
        deleteQuietly(marker);
        if (!config.aetheriumRenderer.get().booleanValue()) {
            return false;
        }
        if (content.contains("mode=" + MODE_REGIONS) && config.regionBatching.get().booleanValue()) {
            config.regionBatching.set(Boolean.FALSE);
            batchingOff = true;
            LOG.warn("The last session ended while the Aetherium pipeline was drawing batched terrain (most likely a "
                    + "crash in the graphics driver or GL translator). Batched terrain has been turned off; the "
                    + "pipeline stays on. Turn it back on in Video Settings > Backend to try again.");
            return true;
        }
        if (content.contains("mode=" + MODE_REGIONS)) {
            // Batching already off (the user turned it off since): treat as a compatible-mode crash.
            config.aetheriumRenderer.set(Boolean.FALSE);
            tripped = true;
            LOG.warn("The last session ended while the Aetherium pipeline was drawing. The pipeline has been turned "
                    + "off; turn it back on in Video Settings > Backend to try again.");
            return true;
        }
        if (!content.contains("mode=" + MODE_COMPATIBLE)) {
            // Written by a version that still had shader mode (removed in 1.5.5): the pipeline now
            // only draws in compatible mode, so that crash says nothing about it.
            LOG.warn("The last session ended while the Aetherium pipeline was drawing in shader mode. Shader mode "
                    + "has been removed; the pipeline stays on in compatible mode.");
            return false;
        }
        config.aetheriumRenderer.set(Boolean.FALSE);
        tripped = true;
        LOG.warn("The last session ended while the Aetherium pipeline was drawing (most likely a crash in the "
                + "graphics driver or GL translator). The pipeline has been turned off; turn it back on in "
                + "Video Settings > Backend to try again.");
        return true;
    }

    /**
     * The pipeline is about to draw in {@code mode} ({@link #MODE_SHADER} / {@link #MODE_COMPATIBLE}):
     * remember that until it stops or the game exits normally.
     */
    public static void arm(final String mode) {
        final Path file = marker;
        if (file == null || armed && mode.equals(armedMode)) {
            return;
        }
        armed = true;
        armedMode = mode;
        tripped = false;
        if (MODE_REGIONS.equals(mode)) {
            batchingOff = false;
        }
        try {
            Files.write(file, ("mode=" + mode + "\npipeline drawing since " + System.currentTimeMillis() + "\n")
                    .getBytes(StandardCharsets.UTF_8));
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
        armedMode = "";
        deleteQuietly(file);
    }

    public static boolean isArmed() {
        return armed;
    }

    /** True when this session started with the pipeline switched off by the guard. */
    public static boolean wasTripped() {
        return tripped;
    }

    /** True when this session started with batched terrain switched off by the guard. */
    public static boolean wasBatchingTurnedOff() {
        return batchingOff;
    }

    private static String readQuietly(final Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (final IOException | RuntimeException error) {
            return "";
        }
    }

    private static void deleteQuietly(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException | RuntimeException error) {
            LOG.warn("Could not delete the pipeline crash marker {}: {}", file, error.toString());
        }
    }
}
