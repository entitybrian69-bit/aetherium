package com.aetherium.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@code custom_env.txt} is written by a launcher, on Android, by someone following a
 * Discord guide - so the parser is tested against the messy real thing rather than a
 * clean two-line sample.
 */
final class CustomEnvFileTest {
    @TempDir
    Path directory;

    private Path write(final String content) {
        try {
            final Path file = this.directory.resolve("custom_env.txt");
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (final IOException error) {
            throw new IllegalStateException("could not stage the env file", error);
        }
    }

    @Test
    @DisplayName("key=value lines, shell exports, comments and junk all parse")
    void tolerantParse() {
        final Path file = write("""
                # Pojav launcher environment
                POJAV_RENDERER=opengles2
                export ZALITH_RENDERER="virgl driver"
                   FCL_RENDERER = gallium_virgl
                PATH=/system/bin:$PATH
                this line has no equals sign and is ignored
                """);
        final Map<String, String> parsed = CustomEnvFile.parse(file);
        assertEquals("opengles2", parsed.get("POJAV_RENDERER"));
        assertEquals("virgl driver", parsed.get("ZALITH_RENDERER"), "quotes must be stripped");
        assertEquals("gallium_virgl", parsed.get("FCL_RENDERER"), "whitespace around = must be trimmed");
        assertEquals("/system/bin:$PATH", parsed.get("PATH"));
        assertEquals(4, parsed.size(), "the junk line must be skipped, not turned into a key: " + parsed);
    }

    @Test
    @DisplayName("keys are upper-cased because Android's env is not case-consistent")
    void keysAreNormalised() {
        final Map<String, String> parsed = CustomEnvFile.parse(
                write("pojav_renderer=angle\nPojav_Renderer=opengles3\n"));
        assertEquals("opengles3", parsed.get("POJAV_RENDERER"), "the later line wins, matching a shell's behaviour");
        assertEquals(1, parsed.size());
    }

    @Test
    @DisplayName("only the renderer keys the engine reads can influence it")
    void rendererKeyExtraction() {
        final Map<String, String> parsed = CustomEnvFile.parse(write("""
                POJAV_RENDERER=opengles3
                MESA_GL_VERSION_OVERRIDE=4.6
                MESA_GLSL_VERSION_OVERRIDE=460
                LD_PRELOAD=/evil.so
                PATH=/bin
                GALLIUM_HUD=fps
                AMETHYST_RENDERER=angle
                DROIDBRIDGE_RENDERER=zink
                """));
        final Map<String, String> relevant = CustomEnvFile.extractRendererKeys(parsed);
        assertEquals("opengles3", relevant.get("POJAV_RENDERER"));
        assertEquals("4.6", relevant.get("MESA_GL_VERSION_OVERRIDE"));
        assertEquals("angle", relevant.get("AMETHYST_RENDERER"));
        assertEquals("zink", relevant.get("DROIDBRIDGE_RENDERER"));
        assertFalse(relevant.containsKey("LD_PRELOAD"), "an arbitrary env key must never reach the backend router");
        assertFalse(relevant.containsKey("PATH"));
        assertTrue(relevant.containsKey("GALLIUM_HUD"), "the HUD key is used to detect a debug overlay costing frames");
        assertEquals(6, relevant.size(), "unexpected extraction result: " + relevant.keySet());
        // The input map is unmodifiable (it is shared between the probe and the GUI);
        // the extracted map is a fresh one the caller may keep editing.
        assertThrows(UnsupportedOperationException.class, () -> parsed.put("X", "y"));
        relevant.put("X", "y");
        assertEquals(7, relevant.size());
    }

    @Test
    @DisplayName("an empty value is treated as unset")
    void emptyValuesIgnored() {
        final Map<String, String> parsed = CustomEnvFile.parse(write("POJAV_RENDERER=\nZALITH_RENDERER=\n"));
        assertEquals(2, parsed.size(), "the raw map keeps both keys");
        assertTrue(CustomEnvFile.extractRendererKeys(parsed).isEmpty(),
                "but neither may be reported as a renderer choice");
    }

    @Test
    @DisplayName("a missing or unreadable file yields an empty map, never an exception")
    void missingFileIsNotAnError() {
        assertTrue(CustomEnvFile.parse(this.directory.resolve("does-not-exist.txt")).isEmpty());
        final Path directory = this.directory.resolve("subdir");
        assertTrue(CustomEnvFile.parse(directory).isEmpty(), "a directory is not an env file");
    }

    @Test
    @DisplayName("a null path is a programming error and says so")
    void nullPathRejected() {
        assertThrows(NullPointerException.class, () -> CustomEnvFile.parse(null));
    }

    @Test
    @DisplayName("an oversized file is refused instead of exhausting the heap")
    void oversizedFileRefused() throws IOException {
        final Path file = this.directory.resolve("huge_env.txt");
        try (var stream = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            final String filler = "# a very long comment line to make this file big enough to matter\n";
            for (int i = 0; i < 6000; i++) {
                stream.write(filler);
            }
            stream.write("POJAV_RENDERER=opengles2\n");
        }
        assertTrue(Files.size(file) > 256L * 1024L, "the fixture must actually exceed the cap");
        assertTrue(CustomEnvFile.parse(file).isEmpty(), "a 300 KB env file is a mistake or an attack, and we read neither");
    }

    @Test
    @DisplayName("values keep inner equals signs and dollar-brace syntax intact")
    void valuePreservesStructure() {
        final Map<String, String> parsed = CustomEnvFile.parse(write(
                "JAVA_ARGS=-Xmx${MEM}m -Dfoo=bar\nMESA_EXTENSION_OVERRIDE=+GL_KHR_no_error\n"));
        assertEquals("-Xmx${MEM}m -Dfoo=bar", parsed.get("JAVA_ARGS"));
        assertEquals("+GL_KHR_no_error", parsed.get("MESA_EXTENSION_OVERRIDE"));
    }
}
