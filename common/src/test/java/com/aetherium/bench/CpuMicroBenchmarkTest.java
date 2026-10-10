package com.aetherium.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigStore;
import com.aetherium.hud.FrameStats;
import com.aetherium.lighting.LightField;
import com.aetherium.perf.RenderToggles;
import com.aetherium.util.MathUtil;

/**
 * The CPU micro-benchmark {@code tools/benchmark.sh} runs with {@code -Daetherium.bench=true}.
 *
 * <p>It is off by default and disabled in a normal {@code ./gradlew build}: a timing test
 * that fails on a busy CI box teaches nobody anything, so the pass criterion here is "the
 * measurement completed and the numbers are physically plausible", not "the code is fast".
 * The report it writes is the deliverable; the numbers in it are only ever compared against
 * the same harness on the same machine.</p>
 */
@EnabledIfSystemProperty(named = "aetherium.bench", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
final class CpuMicroBenchmarkTest {
    private static final BenchmarkHarness HARNESS = new BenchmarkHarness("Aetherium CPU micro-benchmarks", 20_000);
    private static final List<BenchmarkHarness.Result> MEASURED = new ArrayList<>();

    @TempDir
    static Path temporaryDirectory;

    private static void note(final BenchmarkHarness.Result result) {
        MEASURED.add(result);
        // A measurement that ran zero operations or produced no work is a harness bug,
        // and it is the only sanity check a timing test can honestly make.
        assertTrue(result.millis() > 0.0, "the clock did not move for " + result.name());
        assertTrue(result.iterations() > 0);
        assertTrue(result.nanosPerOperation() > 0.0, "sub-nanosecond means the work was optimised away");
    }

    @Test
    @DisplayName("RGB pixel packing helpers")
    void pixelPacking() {
        final BenchmarkHarness.Result pack = HARNESS.measure("packRgb", 200_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                final float level = (i & 15) / 15.0f;
                checksum += MathUtil.packRgb(level, level * 0.9f, level * 0.8f);
            }
            return checksum;
        });
        final BenchmarkHarness.Result unpack = HARNESS.measure("channelUnpack", 200_000, iterations -> {
            long checksum = 0L;
            int pixel = 0x0040_80C0;
            for (int i = 0; i < iterations; i++) {
                pixel = MathUtil.packRgb(MathUtil.channelRed(pixel) + 0.001f,
                        MathUtil.channelGreen(pixel), MathUtil.channelBlue(pixel));
                checksum += pixel & 0xFF;
            }
            return checksum;
        });
        note(pack);
        note(unpack);
        assertTrue(pack.nanosPerOperation() < 200.0, "packing is unexpectedly slow: " + pack);
        assertTrue(unpack.nanosPerOperation() < 200.0, "unpacking is unexpectedly slow: " + unpack);
    }

    @Test
    @DisplayName("config serialisation and parsing, the whole file twice")
    void configJsonRoundTrip() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final Map<String, Object> document;
        try (ConfigStore store = new ConfigStore(temporaryDirectory.resolve("bench.json"), config)) {
            document = store.serialize();
        }
        final String text = com.aetherium.config.Json.write(document);
        final int keys = countLeaves(document);
        assertTrue(keys > 30, "the config document got smaller than the option count: " + keys);

        final BenchmarkHarness.Result write = HARNESS.measure("json.write", 2_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                checksum += com.aetherium.config.Json.write(document).length();
            }
            return checksum;
        });
        final BenchmarkHarness.Result read = HARNESS.measure("json.parse", 2_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                checksum += com.aetherium.config.Json.parseObject(text).size();
            }
            return checksum;
        });
        note(write);
        note(read);
        // Config IO happens a handful of times per session; two seconds of it would be a
        // startup regression, and that is the only threshold worth asserting.
        assertTrue(write.millis() < 2_000.0, "writing the config took " + write.millis() + " ms");
        assertTrue(read.millis() < 2_000.0, "parsing the config took " + read.millis() + " ms");
    }

    @Test
    @DisplayName("frame recording, which runs once per rendered frame forever")
    void frameRecording() {
        final FrameStats stats = new FrameStats();
        final BenchmarkHarness.Result record = HARNESS.measure("FrameStats.record", 200_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                stats.record(16_000_000L + (i % 7) * 1_000L);
                checksum += stats.getFramesTotal();
            }
            return checksum;
        });
        note(record);
        // The harness warms up twice (20k each) and then measures once: every recorded
        // frame counts, because record() has no notion of a warm-up. Pinning the exact
        // total is also what fails if someone changes the warm-up policy by surprise.
        assertEquals(200_000L + 2 * 20_000L, stats.getFramesTotal());
        assertTrue(record.nanosPerOperation() < 500.0,
                "per-frame bookkeeping must stay far below a millisecond: " + record);
        final BenchmarkHarness.Result percentile = HARNESS.measure("FrameStats.percentile", 2_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                checksum += (long) (stats.percentile(0.99) * 1000.0);
            }
            return checksum;
        });
        note(percentile);
        assertFalse(Double.isNaN(stats.getP99Ms()));
    }

    @Test
    @DisplayName("dynamic-light lookup, once per block light read during chunk meshing")
    void lightFieldLookup() {
        final LightField.Source[] sources = new LightField.Source[8];
        for (int i = 0; i < sources.length; i++) {
            sources[i] = new LightField.Source(i * 6.0, 64.0, i * 3.0, 14);
        }
        LightField.publish(sources);
        try {
            final BenchmarkHarness.Result lit = HARNESS.measure("LightField.adjustPacked (8 sources)", 200_000, iterations -> {
                long checksum = 0L;
                for (int i = 0; i < iterations; i++) {
                    checksum += LightField.adjustPacked(0x00F0_0000, i & 63, 60 + (i & 7), (i >> 6) & 31);
                }
                return checksum;
            });
            note(lit);
            // Meshing one section reads light ~4k times; above ~250 ns/op the hook alone
            // would add a millisecond per section rebuild.
            assertTrue(lit.nanosPerOperation() < 1_000.0, "light lookup is too slow for the mesh path: " + lit);
        } finally {
            LightField.clear();
        }
        final BenchmarkHarness.Result empty = HARNESS.measure("LightField.isEmpty (no sources)", 200_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                checksum += LightField.isEmpty() ? i & 1 : 2;
            }
            return checksum;
        });
        note(empty);
    }

    @Test
    @DisplayName("particle decimation, once per spawned particle")
    void particleDecimation() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.particleDensity.set(30);
        RenderToggles.refresh(config, true);
        try {
            final BenchmarkHarness.Result drop = HARNESS.measure("RenderToggles.dropParticle", 200_000, iterations -> {
                long kept = 0L;
                for (int i = 0; i < iterations; i++) {
                    if (!RenderToggles.dropParticle()) {
                        kept++;
                    }
                }
                return kept;
            });
            note(drop);
            final double keptShare = drop.checksum() / (double) drop.iterations();
            assertEquals(0.30, keptShare, 0.01, "30 % density must keep 30 % of particles");
        } finally {
            RenderToggles.refresh(AetheriumConfig.createDefaults(), false);
        }
    }

    @Test
    @DisplayName("the report has one row per measurement and says what it is not")
    void reportIsComplete() {
        final String markdown = HARNESS.toMarkdown();
        assertTrue(markdown.startsWith("### "), markdown.substring(0, 40));
        assertTrue(markdown.contains("| measurement | iterations | ms | ns/op | checksum |"), markdown);
        for (final BenchmarkHarness.Result result : HARNESS.results()) {
            assertTrue(markdown.contains("| " + result.name() + " |"), "missing row for " + result.name());
        }
        assertTrue(markdown.contains("not frame rates"), "the disclaimer is the point of this file");
        assertTrue(markdown.contains("+/-20%"), "the variance has to be stated next to the numbers");
    }

    @AfterAll
    static void writeReport() throws IOException {
        final Path target = BenchmarkHarness.reportPathOrDefault(temporaryDirectory.resolve("cpu.md"));
        HARNESS.writeReport(target);
        final String written = Files.readString(target, StandardCharsets.UTF_8);
        assertEquals(HARNESS.results().size(), MEASURED.size(), "a measurement was never checked");
        assertTrue(written.contains("FrameStats.record") || HARNESS.results().isEmpty(),
                "the file on disk must match what was measured: " + written);
    }

    private static int countLeaves(final Map<String, Object> document) {
        int count = 0;
        for (final Object value : document.values()) {
            if (value instanceof Map) {
                count += countLeaves(cast(value));
            } else {
                count++;
            }
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(final Object value) {
        return (Map<String, Object>) value;
    }
}
