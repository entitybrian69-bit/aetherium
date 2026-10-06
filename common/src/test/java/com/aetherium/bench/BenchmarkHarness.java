package com.aetherium.bench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A deliberately small micro-benchmark harness.
 *
 * <p>Why not JMH: JMH wants to fork a JVM per benchmark and generate a jar, which is
 * exactly what a Minecraft client test run cannot do reliably (the game's own class loader
 * and Mixin transformations make forked-JVM launch scripts a support burden for every
 * contributor). What this harness gives instead is honest measurement of <em>CPU work in
 * our own code</em> - the parts that do not involve a driver, a swapchain or a GPU - plus
 * a printed warning that these numbers are not frame rates.</p>
 *
 * <p>Measurement method: warm up, then {@code System.nanoTime()} around a loop whose result
 * is accumulated into {@link Result#checksum()} so the JIT cannot delete the work. Timings
 * from a single JVM inside a container are noisy to roughly &plusmn;20%; the harness reports
 * that as a range rather than pretending to nanosecond precision.</p>
 */
public final class BenchmarkHarness {
    /** @return a checksum of everything measured, which the caller must consume */
    public interface Step {
        long run(int iterations);
    }

    /** One measurement. */
    public record Result(String name, int iterations, double millis, double nanosPerOperation, long checksum) {
        public double operationsPerSecond() {
            return this.millis <= 0.0 ? 0.0 : this.iterations / (this.millis / 1_000.0);
        }
    }

    private final List<Result> results = new ArrayList<>();
    private final String label;
    private final int warmupIterations;

    public BenchmarkHarness(final String label, final int warmupIterations) {
        this.label = Objects.requireNonNull(label, "label");
        this.warmupIterations = Math.max(0, warmupIterations);
    }

    /**
     * Runs one measurement.
     *
     * @param name       row label in the report
     * @param iterations how many operations to time
     * @param step       the work; its return value is kept, so nothing is optimised away
     */
    public Result measure(final String name, final int iterations, final Step step) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(step, "step");
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive, got " + iterations);
        }
        if (this.warmupIterations > 0) {
            step.run(this.warmupIterations);
            step.run(this.warmupIterations);
        }
        final long start = System.nanoTime();
        final long checksum = step.run(iterations);
        final long elapsed = Math.max(1L, System.nanoTime() - start);
        final double millis = elapsed / 1_000_000.0;
        final Result result = new Result(name, iterations, millis, (double) elapsed / iterations, checksum);
        this.results.add(result);
        System.out.printf(Locale.ROOT, "[bench] %-28s %8d ops  %10.2f ms  %8.1f ns/op%n",
                result.name(), result.iterations(), result.millis(), result.nanosPerOperation());
        return result;
    }

    public List<Result> results() {
        return List.copyOf(this.results);
    }

    /** @return the Markdown body for {@code BENCHMARK.md} (CPU-only section) */
    public String toMarkdown() {
        final StringBuilder builder = new StringBuilder(512);
        builder.append("### ").append(this.label).append('\n').append('\n');
        builder.append("Measured in-JVM on the CPU only. These are not frame rates and must not be");
        builder.append(" quoted as FPS: no driver, no GPU and no swapchain are involved, and");
        builder.append(" each row is one function rather than a rendered scene.\n\n");
        builder.append("| measurement | iterations | ms | ns/op | checksum |\n");
        builder.append("| --- | ---: | ---: | ---: | ---: |\n");
        for (final Result result : this.results) {
            builder.append(String.format(Locale.ROOT, "| %s | %d | %.2f | %.1f | %d |%n",
                    result.name(), result.iterations(), result.millis(), result.nanosPerOperation(),
                    result.checksum()));
        }
        builder.append("\nRepeat variance on a container is roughly +/-20%; a claim of a smaller");
        builder.append(" difference than that is not supportable from these numbers.\n");
        return builder.toString();
    }

    /** Writes the report, creating parent directories; returns the path for logging. */
    public Path writeReport(final Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        final Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, toMarkdown().getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * Resolves where the report goes. {@code tools/benchmark.sh} passes a <em>directory</em>
     * ({@code -Daetherium.bench.out=benchmark-out}), so a value without a {@code .md} suffix
     * - or one that already exists as a directory - is treated as a directory and
     * {@code cpu.md} is placed inside it.
     */
    public static Path reportPathOrDefault(final Path fallback) {
        final String configured = System.getProperty("aetherium.bench.out");
        if (configured == null || configured.isBlank()) {
            return fallback;
        }
        final Path base = Path.of(configured);
        if (Files.isDirectory(base) || !configured.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return base.resolve("cpu.md");
        }
        return base;
    }
}
