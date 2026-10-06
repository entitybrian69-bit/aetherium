package com.aetherium.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The recorder is off unless a system property says otherwise, and the whole point of that
 * is a player never gets a surprise file - so the tests are mostly about the two states of
 * that switch and about what a run leaves behind.
 *
 * <p>The interval is never crossed by sleeping: {@code stop()} flushes the tail row, which
 * gives a deterministic file to assert on.</p>
 */
final class BenchmarkRecorderTest {
    @TempDir
    Path directory;

    @BeforeEach
    void clearProperties() {
        System.clearProperty(BenchmarkRecorder.PROPERTY);
        System.clearProperty(BenchmarkRecorder.INTERVAL_PROPERTY);
        System.clearProperty(BenchmarkRecorder.LABEL_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        BenchmarkRecorder.stop();
        clearProperties();
    }

    @Test
    @DisplayName("no property means no file handle and no work in the frame loop")
    void inertWithoutTheProperty() {
        BenchmarkRecorder.start();
        assertFalse(BenchmarkRecorder.isRecording(), "a normal launch must not be recording");
        assertNull(BenchmarkRecorder.getFile());
        final FrameStats stats = new FrameStats();
        stats.record(16_000_000L);
        BenchmarkRecorder.onFrame(stats);
        BenchmarkRecorder.stop();
        try (var listing = Files.list(this.directory)) {
            assertTrue(listing.findAny().isEmpty(), "nothing may be written when recording was never enabled");
        } catch (final IOException error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    @DisplayName("the tail row is flushed by stop(), so a short run still produces data")
    void stopFlushesThePartialInterval() throws IOException {
        System.setProperty(BenchmarkRecorder.PROPERTY, this.directory.toString());
        System.setProperty(BenchmarkRecorder.LABEL_PROPERTY, "unit-test");
        BenchmarkRecorder.start();
        assertTrue(BenchmarkRecorder.isRecording());
        assertNotNull(BenchmarkRecorder.getFile());

        final FrameStats stats = new FrameStats();
        for (int i = 0; i < 40; i++) {
            stats.record(16_000_000L);
        }
        stats.record(120_000_000L);
        assertFalse(Files.exists(BenchmarkRecorder.getFile()),
                "the interval has not elapsed, so no row may have been written yet");

        BenchmarkRecorder.onFrame(stats);
        BenchmarkRecorder.stop();

        final Path file = this.directory.resolve("frames.md");
        assertTrue(Files.exists(file), "stop() must flush the interval it has accumulated");
        final String content = Files.readString(file);
        assertTrue(content.startsWith("# Aetherium frame records"), content.substring(0, Math.min(80, content.length())));
        assertTrue(content.contains("| measurement | fps |"), "the header row is missing:\n" + content);
        assertTrue(content.contains("unit-test"), "the label from the property must appear: " + content);
        final long dataRows = content.lines().filter(line -> line.startsWith("| unit-test")).count();
        assertEquals(1L, dataRows, "exactly one interval row, and it must not be duplicated by stop()\n" + content);
        // The row carries the spike count, which is the number a benchmark reader wants.
        assertTrue(content.contains("1 |"), "the 120 ms frame should appear as one spike:\n" + content);
    }

    @Test
    @DisplayName("a second start() re-arms a recorder that a previous stop() latched off")
    void restartWorksAfterStop() throws IOException {
        System.setProperty(BenchmarkRecorder.PROPERTY, this.directory.toString());
        BenchmarkRecorder.start();
        BenchmarkRecorder.onFrame(new FrameStats());
        BenchmarkRecorder.stop();
        assertFalse(BenchmarkRecorder.isRecording());

        BenchmarkRecorder.start();
        assertTrue(BenchmarkRecorder.isRecording(),
                "tools/benchmark.sh runs several passes in one JVM; the second must still record");
        BenchmarkRecorder.onFrame(new FrameStats());
        BenchmarkRecorder.stop();
        // Appending, never truncating: pass 1's rows are the comparison for pass 2.
        final String content = Files.readString(this.directory.resolve("frames.md"));
        assertEquals(1L, content.lines().filter(line -> line.startsWith("# Aetherium")).count(),
                "the header must not be written twice:\n" + content);
    }

    @Test
    @DisplayName("a directory that cannot be created disables recording instead of throwing")
    void unwritableTargetDisablesRecording() {
        final Path blocked = this.directory.resolve("frames.md/not-a-directory");
        try {
            Files.writeString(this.directory.resolve("frames.md"), "occupied");
        } catch (final IOException error) {
            throw new IllegalStateException(error);
        }
        System.setProperty(BenchmarkRecorder.PROPERTY, blocked.toString());
        BenchmarkRecorder.start();
        assertFalse(BenchmarkRecorder.isRecording(),
                "the frame loop must not carry a broken benchmark target: " + blocked);
        final FrameStats stats = new FrameStats();
        stats.record(16_000_000L);
        BenchmarkRecorder.onFrame(stats);
        BenchmarkRecorder.stop();
        assertEquals("occupied", readFirstLine(), "the existing file must be left alone");
    }

    private String readFirstLine() {
        try {
            return Files.readString(this.directory.resolve("frames.md")).trim();
        } catch (final IOException error) {
            throw new IllegalStateException(error);
        }
    }

    @Test
    @DisplayName("an interval below one second is refused, because it would write per frame")
    void sillyIntervalIsRejected() {
        System.setProperty(BenchmarkRecorder.PROPERTY, this.directory.toString());
        System.setProperty(BenchmarkRecorder.INTERVAL_PROPERTY, "0.001");
        BenchmarkRecorder.start();
        assertFalse(BenchmarkRecorder.isRecording(), "a 1 ms interval would produce 1000 rows a second");
        assertNull(BenchmarkRecorder.getFile());
    }
}
