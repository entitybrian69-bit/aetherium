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
        config.aetheriumRenderer.set(Boolean.TRUE);

        // Session 1: starts clean, draws, then dies natively (no disarm).
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        assertTrue(Files.exists(marker));
        assertTrue(PipelineCrashGuard.isArmed());

        // Session 2: the marker is found, the option goes off, the marker is gone.
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.aetheriumRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
        assertFalse(Files.exists(marker));

        // The user turns it back on and it draws: the "tripped" note clears; a clean exit removes the marker.
        config.aetheriumRenderer.set(Boolean.TRUE);
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        assertFalse(PipelineCrashGuard.wasTripped());
        PipelineCrashGuard.disarm();
        assertFalse(Files.exists(marker));

        // Session 3: clean start, nothing changes.
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.TRUE, config.aetheriumRenderer.get());
    }

    @Test
    @DisplayName("pipeline crash guard: shader-mode markers from older versions are ignored; compatible mode crashes turn it off")
    void shaderMarkersIgnored() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        final Path marker = dir.resolve(PipelineCrashGuard.MARKER_NAME);
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.aetheriumRenderer.set(Boolean.TRUE);

        // Markers from 1.3.1/1.3.2 (no mode) and from shader mode (removed in 1.5.5): nothing changes.
        for (final String old : new String[] {"pipeline drawing since 1\n", "mode=shader\npipeline drawing since 1\n",
                "mode=shader\nshaderfix=2\npipeline drawing since 1\n"}) {
            Files.write(marker, old.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(PipelineCrashGuard.checkAtStartup(dir, config), old);
            assertFalse(PipelineCrashGuard.wasTripped());
            assertEquals(Boolean.TRUE, config.aetheriumRenderer.get());
            assertFalse(Files.exists(marker));
        }

        // Compatible mode crashes: the pipeline goes off.
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_SHADER);
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE); // a mode switch rewrites the marker
        assertTrue(new String(Files.readAllBytes(marker), java.nio.charset.StandardCharsets.UTF_8).contains("mode=compatible"));
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.aetheriumRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
    }

    @Test
    @DisplayName("pipeline crash guard: a crash while drawing batched terrain turns only batching off, then the pipeline")
    void regionsCrashTurnsBatchingOff() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        final Path marker = dir.resolve(PipelineCrashGuard.MARKER_NAME);
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.aetheriumRenderer.set(Boolean.TRUE);
        assertEquals(Boolean.TRUE, config.regionBatching.get(), "batching is the default");
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));

        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_REGIONS);
        assertTrue(new String(Files.readAllBytes(marker), java.nio.charset.StandardCharsets.UTF_8).contains("mode=regions"));
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.regionBatching.get());
        assertEquals(Boolean.TRUE, config.aetheriumRenderer.get(), "the pipeline stays on");
        assertTrue(PipelineCrashGuard.wasBatchingTurnedOff());
        assertFalse(PipelineCrashGuard.wasTripped());

        // The pipeline then crashes in plain compatible mode: off.
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.aetheriumRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
        assertFalse(PipelineCrashGuard.wasBatchingTurnedOff());
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
