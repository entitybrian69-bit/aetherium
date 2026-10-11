package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.config.AetheriumConfig;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A native crash while the pipeline draws must not become a crash loop. */
final class PipelineCrashGuardTest {

    @Test
    @DisplayName("pipeline crash guard: a session that died while drawing turns the pipeline off once")
    void tripsAfterUncleanExit() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        final Path marker = dir.resolve(PipelineCrashGuard.MARKER_NAME);
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.experimentalChunkRenderer.set(Boolean.TRUE);

        // Session 1: starts clean, draws, then dies natively (no disarm).
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        PipelineCrashGuard.arm();
        assertTrue(Files.exists(marker));
        assertTrue(PipelineCrashGuard.isArmed());

        // Session 2: the marker is found, the option goes off, the marker is gone.
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.experimentalChunkRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
        assertFalse(Files.exists(marker));

        // The user turns it back on and it draws: the "tripped" note clears; a clean exit removes the marker.
        config.experimentalChunkRenderer.set(Boolean.TRUE);
        PipelineCrashGuard.arm();
        assertFalse(PipelineCrashGuard.wasTripped());
        PipelineCrashGuard.disarm();
        assertFalse(Files.exists(marker));

        // Session 3: clean start, nothing changes.
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.TRUE, config.experimentalChunkRenderer.get());
    }

    @Test
    @DisplayName("pipeline crash guard: a stale marker with the option already off changes nothing")
    void staleMarkerWithOptionOff() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        Files.write(dir.resolve(PipelineCrashGuard.MARKER_NAME), new byte[] {1});
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        assertFalse(PipelineCrashGuard.wasTripped());
        assertFalse(Files.exists(dir.resolve(PipelineCrashGuard.MARKER_NAME)));
    }
}
