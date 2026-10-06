package com.aetherium.android;

import java.util.Objects;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;

/**
 * Closed-loop work shedder for phones and tablets.
 *
 * <p>Why this exists as its own class: thermal throttling on mobile is not a
 * frame-rate problem, it is a *sustained* problem. A renderer that ignores package
 * temperature gets to 90 fps for forty seconds and then the SoC clocks down and
 * it sits at 35 for ten minutes. Shedding meshing work and upload volume at ~68°C
 * keeps the sustained number higher than chasing peak fps. Measured, not claimed:
 * see BENCHMARK.md §"thermal soak".</p>
 *
 * <p>Thread-safety: polled once per frame from the render thread, so the state
 * fields are plain fields written and read by the same thread. The two values the
 * mesh workers read ({@code intensity}, {@code workerBudget}) are volatile because
 * the scheduler samples them when deciding whether to accept new work.</p>
 */
public final class AndroidPowerGovernor {
    private static final AetheriumLog LOGGER = AetheriumLog.of(AndroidPowerGovernor.class);

    /** Hysteresis band, in milli-degrees C, so we do not oscillate at the ceiling. */
    private static final int HYSTERESIS_MILLIC = 3_000;

    private final AndroidEnvironment environment;
    private final AetheriumConfig config;

    /** Render-thread only. */
    private long lastPollNanos;
    /** Render-thread only: smoothed temperature, avoids a one-frame spike reaction. */
    private float smoothedMilliC;
    /** Render-thread only. */
    private int level;

    /** Read by worker threads. */
    private volatile double intensity;
    /** Read by the mesh scheduler: 1.0 = full queue depth, 0 = stop accepting. */
    private volatile double workerBudget = 1.0;

    private int observations;
    private boolean lowMemoryObserved;

    public AndroidPowerGovernor(final AndroidEnvironment environment, final AetheriumConfig config) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * One frame of governance. No-op off Android and when both features are off,
     * so desktop pays a single boolean test.
     *
     * @param frameNanos wall-clock duration of the completed frame
     */
    public void onFrame(final long frameNanos) {
        if (!this.environment.isAndroid()) {
            return;
        }
        final boolean thermalActive = this.config.thermalThrottle.get();
        final boolean batteryActive = this.config.batterySaver.get();
        if (!thermalActive && !batteryActive && !this.config.mobileMemoryMode.get()) {
            return;
        }

        final long now = System.nanoTime();
        // 2 Hz poll: /sys/class/thermal reads are a syscall each, and 140 sensors
        // per frame on a mid-range SoC is measurable in the frame graph.
        if (now - this.lastPollNanos < 500_000_000L) {
            return;
        }
        this.lastPollNanos = now;

        double requested = 0.0;
        int requestedLevel = 0;

        if (thermalActive && this.environment.isThermalSensorUsable()) {
            final int milliC = this.environment.readThermalMilliCelsius();
            if (milliC >= 0) {
                if (this.observations++ == 0) {
                    this.smoothedMilliC = milliC;
                } else {
                    // 1.5 s half-life: fast enough to react before the hard limit,
                    // slow enough that a single hot frame changes nothing.
                    this.smoothedMilliC = MathUtil.smoothDamp(this.smoothedMilliC, milliC, 1.5f, 0.5f);
                }
                final int ceiling = config.thermalCeilingC.get() * 1000;
                final float over = (this.smoothedMilliC - (ceiling - HYSTERESIS_MILLIC)) / (float) Math.max(1, HYSTERESIS_MILLIC * 4);
                if (over > 0.0f) {
                    requested = Math.max(requested, MathUtil.clamp(over, 0.0, 1.0));
                    requestedLevel = Math.max(requestedLevel, over > 0.66f ? 2 : 1);
                }
            }
        }

        if (batteryActive) {
            final double drain = estimateBatteryPressure();
            requested = Math.max(requested, 0.35 + 0.5 * drain);
            requestedLevel = Math.max(requestedLevel, 1);
        }

        if (this.config.mobileMemoryMode.get()) {
            final double heap = this.environment.heapUsageFraction();
            if (heap > 0.9) {
                requested = Math.max(requested, 0.9);
                requestedLevel = Math.max(requestedLevel, 2);
                if (!this.lowMemoryObserved) {
                    this.lowMemoryObserved = true;
                    LOGGER.warn("Heap at {}% of max — stalling mesh uploads to avoid a GC stall inside the frame", (int) (heap * 100.0));
                }
            } else if (heap < 0.75) {
                this.lowMemoryObserved = false;
            }
        }

        // Frame-time feedback: if we are already inside the frame budget there is
        // nothing to save, so throttle pressure is scaled by how far we missed it.
        final double targetMs = this.config.targetFps.get() > 0 ? 1000.0 / this.config.targetFps.get() : 16.666;
        final double overrun = MathUtil.clamp((frameNanos / 1_000_000.0) / targetMs - 1.0, 0.0, 1.0);
        requested = Math.min(1.0, requested + 0.25 * overrun * (requestedLevel > 0 ? 1.0 : 0.0));

        this.intensity = requested;
        this.level = requestedLevel;
        this.workerBudget = 1.0 - MathUtil.clamp(requested, 0.0, 0.95);
    }

    /**
     * Battery pressure without a Android framework dependency: we cannot read
     * BatteryManager from a launcher-hosted JVM (no Activity context), so the
     * proxy is "how hard is the CPU being throttled", derived from load average
     * where available, else a constant.
     */
    private double estimateBatteryPressure() {
        final String load = System.getProperty("aetherium.fake.load", System.getenv("AETHERIUM_FAKE_LOAD"));
        if (load != null) {
            try {
                return MathUtil.clamp(Double.parseDouble(load) / Runtime.getRuntime().availableProcessors(), 0.0, 1.0);
            } catch (final NumberFormatException error) {
                LOGGER.dev("AETHERIUM_FAKE_LOAD='{}' is not a number; ignoring", load);
            }
        }
        try {
            final java.lang.management.OperatingSystemMXBean bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            final double average = bean.getSystemLoadAverage();
            if (average >= 0.0) {
                return MathUtil.clamp(average / Math.max(1, bean.getAvailableProcessors()), 0.0, 1.0);
            }
        } catch (final RuntimeException error) {
            LOGGER.dev("SystemLoadAverage unavailable: {}", error.getClass().getSimpleName());
        }
        return 0.0;
    }

    /** 0 = no pressure, 1 = shed as much as possible without dropping frames. */
    public double getIntensity() {
        return this.intensity;
    }

    /** Fraction of the mesh queue depth the scheduler may keep in flight. */
    public double getWorkerBudget() {
        return this.workerBudget;
    }

    public int getLevel() {
        return this.level;
    }

    /** Multiplier applied to the per-frame upload budget. */
    public int scaleUploadBudgetKb(final int configuredKb) {
        final double factor = 1.0 - 0.75 * this.intensity;
        return Math.max(256, (int) (configuredKb * factor));
    }

    /** Multiplier applied to indirect batch size; small batches hurt less when hot. */
    public int scaleIndirectBatch(final int configured) {
        final double factor = 1.0 - 0.5 * this.intensity;
        return Math.max(16, (int) (configured * factor));
    }

    /** Diagnostic string for the Android GUI tab. */
    public String describe() {
        if (!this.environment.isAndroid()) {
            return "not applicable (desktop)";
        }
        return String.format(java.util.Locale.ROOT, "level %d, intensity %.2f, budget %.0f%%, heap %.0f%%, %s",
                this.level, this.intensity, this.workerBudget * 100.0, this.environment.heapUsageFraction() * 100.0,
                this.environment.isThermalSensorUsable()
                        ? String.format(java.util.Locale.ROOT, "%.1f C", this.environment.getLastThermalMilliCelsius() / 1000.0)
                        : "no thermal sensor");
    }
}
