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
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        assertTrue(Files.exists(marker));
        assertTrue(PipelineCrashGuard.isArmed());

        // Session 2: the marker is found, the option goes off, the marker is gone.
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.experimentalChunkRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
        assertFalse(Files.exists(marker));

        // The user turns it back on and it draws: the "tripped" note clears; a clean exit removes the marker.
        config.experimentalChunkRenderer.set(Boolean.TRUE);
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE);
        assertFalse(PipelineCrashGuard.wasTripped());
        PipelineCrashGuard.disarm();
        assertFalse(Files.exists(marker));

        // Session 3: clean start, nothing changes.
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.TRUE, config.experimentalChunkRenderer.get());
    }

    @Test
    @DisplayName("pipeline crash guard: pre-fix shader markers retry shader mode once; a shader crash switches to compatible mode, then off")
    void shaderCrashDowngradesFirst() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        final Path marker = dir.resolve(PipelineCrashGuard.MARKER_NAME);
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.experimentalChunkRenderer.set(Boolean.TRUE);
        assertEquals(Boolean.TRUE, config.pipelineShader.get(), "shader mode is the default");

        // Markers from 1.3.1/1.3.2 (no mode) and 1.3.3 (mode, no fix level) are the GL4ES attribute crash
        // fixed in 1.3.4: shader mode gets one more try, nothing changes in the config.
        for (final String old : new String[] {"pipeline drawing since 1\n", "mode=shader\npipeline drawing since 1\n"}) {
            Files.write(marker, old.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertFalse(PipelineCrashGuard.checkAtStartup(dir, config), old);
            assertTrue(PipelineCrashGuard.wasRetriedAfterFix());
            assertEquals(Boolean.TRUE, config.pipelineShader.get());
            assertEquals(Boolean.TRUE, config.experimentalChunkRenderer.get());
            assertFalse(Files.exists(marker));
        }

        // A shader-mode crash with the fix: compatible mode.
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_SHADER);
        assertTrue(new String(Files.readAllBytes(marker), java.nio.charset.StandardCharsets.UTF_8)
                .contains("shaderfix=" + PipelineCrashGuard.SHADER_FIX));
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertFalse(PipelineCrashGuard.wasRetriedAfterFix());
        assertEquals(Boolean.TRUE, config.experimentalChunkRenderer.get(), "the pipeline stays on");
        assertEquals(Boolean.FALSE, config.pipelineShader.get(), "in compatible mode");
        assertTrue(PipelineCrashGuard.wasDowngraded());
        assertFalse(PipelineCrashGuard.wasTripped());

        // Shader mode again (user re-enabled it) and it crashes: compatible mode again.
        config.pipelineShader.set(Boolean.TRUE);
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_SHADER);
        assertTrue(new String(Files.readAllBytes(marker), java.nio.charset.StandardCharsets.UTF_8).contains("mode=shader"));
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.pipelineShader.get());
        assertEquals(Boolean.TRUE, config.experimentalChunkRenderer.get());

        // Compatible mode crashes too: now the pipeline goes off.
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_SHADER);
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_COMPATIBLE); // a mode switch rewrites the marker
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.experimentalChunkRenderer.get());
        assertTrue(PipelineCrashGuard.wasTripped());
        assertFalse(PipelineCrashGuard.wasDowngraded());
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
