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
import com.aetherium.gamma.GammaApplier;
import com.aetherium.hud.FrameStats;
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
    @DisplayName("lightmap pixel packing, the innermost loop of the gamma pass")
    void pixelPacking() {
        final int size = GammaApplier.LIGHTMAP_SIZE;
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
        // This loop runs 256 times per lightmap frame; at 240 fps that is 61k packs per
        // second, so anything above ~20 ns/op would be a regression worth noticing.
        assertTrue(pack.nanosPerOperation() < 200.0, "packing is unexpectedly slow: " + pack);
        assertTrue(unpack.nanosPerOperation() < 200.0, "unpacking is unexpectedly slow: " + unpack);
        assertEquals(size, GammaApplier.LIGHTMAP_SIZE);
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
        assertTrue(keys > 50, "the config document got smaller than the option count: " + keys);

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
    @DisplayName("gamma curve evaluation, once per channel per lightmap pixel")
    void gammaCurveEvaluation() {
        final GammaApplier.GammaCurve curve = GammaApplier.GammaCurve.parse("0:0,0.2:0.55,0.5:0.8,0.8:0.95,1:1");
        final BenchmarkHarness.Result evaluate = HARNESS.measure("GammaCurve.evaluate", 200_000, iterations -> {
            long checksum = 0L;
            for (int i = 0; i < iterations; i++) {
                checksum += (long) (curve.evaluate((i % 1000) / 1000.0f) * 1_000_000.0);
            }
            return checksum;
        });
        note(evaluate);
        assertTrue(evaluate.checksum() > 0L, "every sample evaluated to zero, which means the curve is broken");
        // The lightmap pass is 768 evaluations per frame; above ~130 ns/op the pass alone
        // would cost a tenth of a 60 fps frame budget.
        assertTrue(evaluate.nanosPerOperation() < 400.0, "curve evaluation is too slow to run per pixel: " + evaluate);
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
