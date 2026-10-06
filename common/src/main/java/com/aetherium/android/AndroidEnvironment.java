package com.aetherium.android;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;

/**
 * Everything Aetherium knows about an Android host, computed once at boot.
 *
 * <p>This class answers three questions that the rest of the engine refuses to
 * guess at:</p>
 * <ol>
 *   <li><b>Who launched us?</b> {@link AndroidLauncher} — Pojav, Zalith, FCL,
 *       Amethyst, DroidBridge. Determines where the env file lives and which
 *       heap-size conventions apply.</li>
 *   <li><b>What is the GPU path?</b> {@link AndroidRenderer} — gl4es, Zink, LTW,
 *       MobileGlues, VirGL. Determines which backends are even offered.</li>
 *   <li><b>How much memory/thermal headroom is there?</b> {@code Runtime}, the
 *       cgroup limit and {@code /sys/class/thermal}, which set the arena sizes
 *       and the throttle targets.</li>
 * </ol>
 *
 * <p>No method here may throw: the probe runs before the first frame, and an I/O
 * failure on {@code /sys} (SELinux denies it on many stock ROMs) must degrade to
 * "no throttling", never to a crash.</p>
 */
public final class AndroidEnvironment {
    private static final AetheriumLog LOGGER = AetheriumLog.of(AndroidEnvironment.class);

    private final boolean android;
    private final AndroidLauncher launcher;
    private final AndroidRenderer renderer;
    private final AetheriumConfig.AndroidRendererChoice forcedChoice;
    private final int declaredGlLevel;
    private final long memoryBudgetMb;
    private final long maxHeapBytes;
    private final boolean sixFourBit;
    private final boolean arm64;
    private final boolean sveAvailable;
    private final boolean neonAvailable;
    private final List<String> cpuFeatures;
    private final Path envFile;
    private final Map<String, String> envOverlay;
    private final String osVersion;

    private volatile int thermalMilliCelsius = -1;
    private volatile boolean thermalSensorUsable;

    private AndroidEnvironment(final boolean android, final AndroidLauncher launcher, final AndroidRenderer renderer,
                               final AetheriumConfig.AndroidRendererChoice forcedChoice, final int declaredGlLevel,
                               final long memoryBudgetMb, final long maxHeapBytes, final boolean sixFourBit,
                               final boolean arm64, final boolean neonAvailable, final boolean sveAvailable,
                               final List<String> cpuFeatures, final Path envFile, final Map<String, String> envOverlay,
                               final String osVersion) {
        this.android = android;
        this.launcher = launcher;
        this.renderer = renderer;
        this.forcedChoice = forcedChoice;
        this.declaredGlLevel = declaredGlLevel;
        this.memoryBudgetMb = memoryBudgetMb;
        this.maxHeapBytes = maxHeapBytes;
        this.sixFourBit = sixFourBit;
        this.arm64 = arm64;
        this.neonAvailable = neonAvailable;
        this.sveAvailable = sveAvailable;
        this.cpuFeatures = cpuFeatures;
        this.envFile = envFile;
        this.envOverlay = envOverlay;
        this.osVersion = osVersion;
    }

    /** Full probe. Never throws; every sub-step degrades to a safe default. */
    public static AndroidEnvironment probe(final AetheriumConfig config) {
        Objects.requireNonNull(config, "config");
        final AndroidEnvProvider env = System::getenv;

        final AndroidLauncher launcher = AndroidLauncher.detect(env, System.getProperty("user.home"), System.getProperty("sun.java.command"));
        final boolean isAndroid = launcher.isAndroid();
        final AetheriumConfig.AndroidRendererChoice forced = config.androidForceRenderer.get();

        final String rawRenderer = firstNonNull(env.get("POJAV_RENDERER"), env.get("ZALITH_RENDERER"),
                env.get("FCL_RENDERER"), env.get("AMETHYST_RENDERER"), env.get("DROIDBRIDGE_RENDERER"));
        AndroidRenderer renderer = AndroidRenderer.NONE;
        if (isAndroid) {
            renderer = AndroidRenderer.parse(rawRenderer, env.get("MESA_GL_VERSION_OVERRIDE"));
            if (renderer == AndroidRenderer.UNKNOWN && config.readCustomEnv().get()) {
                // Launcher UIs sometimes only persist the choice in custom_env.txt.
                final File file = AndroidLauncher.findCustomEnvFile(env);
                final Map<String, String> overlay = file == null ? Map.of() : CustomEnvFile.parse(toPath(file));
                final String fromFile = overlay.get("POJAV_RENDERER");
                if (fromFile != null) {
                    renderer = AndroidRenderer.parse(fromFile, overlay.get("MESA_GL_VERSION_OVERRIDE"));
                }
            }
        }
        if (forced != AetheriumConfig.AndroidRendererChoice.AUTO) {
            final AndroidRenderer override = fromChoice(forced);
            LOGGER.info("Renderer override from config: {} (detected {})", override.getDisplayName(), renderer.getDisplayName());
            renderer = override;
        }

        final long maxHeap = Runtime.getRuntime().maxMemory();
        final long cgroupLimitMb = readCgroupMemoryLimitMb();
        final int configuredBudget = config.memoryBudgetMb.get();
        long budgetMb = configuredBudget > 0 ? configuredBudget : MathUtil.clamp(maxHeap / (1024L * 1024L) / 2L, 256L, 4096L);
        if (cgroupLimitMb > 0) {
            budgetMb = Math.min(budgetMb, Math.max(256L, cgroupLimitMb * 3L / 4L));
        }

        final String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        final boolean is64 = arch.contains("64") || arch.contains("aarch64");
        final boolean isArm = arch.contains("arm") || arch.contains("aarch64");
        final List<String> features = isArm ? readCpuFeatures() : List.of();
        final boolean neon = features.contains("asimd") || features.contains("neon") || arch.contains("aarch64");
        final boolean sve = features.contains("sve") || features.contains("sve2");

        final File envFileHandle = isAndroid && config.readCustomEnv().get() ? AndroidLauncher.findCustomEnvFile(env) : null;
        final Map<String, String> overlay = envFileHandle == null ? Map.of() : CustomEnvFile.parse(toPath(envFileHandle));

        LOGGER.info("Android probe: launcher={} renderer={} android={} glLevelDeclared={} budget={}MB heap={}MB cgroup={}MB arch={} neon={} sve={} envFile={}",
                launcher.getDisplayName(), renderer.getDisplayName(), isAndroid,
                renderer == AndroidRenderer.NONE ? "n/a" : String.valueOf(declaredLevel(rawRenderer, env)),
                budgetMb, maxHeap / (1024 * 1024), cgroupLimitMb, arch, neon, sve, envFileHandle);

        return new AndroidEnvironment(isAndroid, launcher, renderer, forced,
                AndroidRenderer.declaredGlLevel(env.get("MESA_GL_VERSION_OVERRIDE")), budgetMb, maxHeap, is64, isArm && is64,
                neon, sve, features, envFileHandle == null ? null : toPath(envFileHandle), overlay,
                System.getProperty("os.version", "unknown"));
    }

    private static Path toPath(final File file) {
        return file.toPath();
    }

    private static String firstNonNull(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static Object declaredLevel(final String rawRenderer, final AndroidEnvProvider env) {
        final int level = AndroidRenderer.declaredGlLevel(env.get("MESA_GL_VERSION_OVERRIDE"));
        return level < 0 ? (rawRenderer == null ? "unknown" : "implied-by-" + rawRenderer) : level / 100 + "." + (level % 100);
    }

    private static AndroidRenderer fromChoice(final AetheriumConfig.AndroidRendererChoice choice) {
        switch (choice) {
            case GL4ES:
                return AndroidRenderer.GL4ES;
            case ZINK:
                return AndroidRenderer.ZINK;
            case LTW:
                return AndroidRenderer.LTW;
            case MOBILEGLUES:
                return AndroidRenderer.MOBILEGLUES;
            case VIRGL:
                return AndroidRenderer.VIRGL;
            case ANGLE:
                return AndroidRenderer.ANGLE;
            case NATIVE_VULKAN:
                return AndroidRenderer.NATIVE_VULKAN;
            case AUTO:
            default:
                return AndroidRenderer.NONE;
        }
    }

    /** cgroup v2 limit for the app's own slice; Android uses this to cap heap. */
    private static long readCgroupMemoryLimitMb() {
        final Path v2 = Path.of("/sys/fs/cgroup/memory.max");
        try {
            if (Files.isReadable(v2)) {
                final String text = Files.readString(v2, StandardCharsets.UTF_8).trim();
                if (!text.equals("max")) {
                    return Long.parseLong(text) / (1024L * 1024L);
                }
            }
            final Path v1 = Path.of("/sys/fs/cgroup/memory/memory.limit_in_bytes");
            if (Files.isReadable(v1)) {
                final long bytes = Long.parseLong(Files.readString(v1, StandardCharsets.UTF_8).trim());
                // cgroup v1 uses a huge sentinel instead of "max".
                return bytes > 0 && bytes < (1L << 40) ? bytes / (1024L * 1024L) : -1L;
            }
        } catch (final IOException | NumberFormatException | RuntimeException error) {
            LOGGER.dev("Could not read cgroup memory limit ({}); using heap-derived budget", error.getClass().getSimpleName());
        }
        return -1L;
    }

    /** CPU feature flags from {@code /proc/cpuinfo}; empty when unreadable. */
    private static List<String> readCpuFeatures() {
        final List<String> out = new ArrayList<>(16);
        try {
            for (final String line : Files.readAllLines(Path.of("/proc/cpuinfo"), StandardCharsets.UTF_8)) {
                if (line.startsWith("Features") || line.startsWith("features")) {
                    final int colon = line.indexOf(':');
                    if (colon > 0) {
                        for (final String token : line.substring(colon + 1).trim().split("\\s+")) {
                            out.add(token.toLowerCase(Locale.ROOT));
                        }
                    }
                    break;
                }
            }
        } catch (final IOException error) {
            LOGGER.dev("/proc/cpuinfo unreadable: {}", error.getMessage());
        }
        return List.copyOf(out);
    }

    public boolean isAndroid() {
        return this.android;
    }

    public AndroidLauncher getLauncher() {
        return this.launcher;
    }

    public AndroidRenderer getRenderer() {
        return this.renderer;
    }

    public AetheriumConfig.AndroidRendererChoice getForcedChoice() {
        return this.forcedChoice;
    }

    public int getDeclaredGlLevel() {
        return this.declaredGlLevel;
    }

    public long getMemoryBudgetMb() {
        return this.memoryBudgetMb;
    }

    public long getMaxHeapBytes() {
        return this.maxHeapBytes;
    }

    public boolean is64Bit() {
        return this.sixFourBit;
    }

    /**
     * ARM64 only. {@code armeabi-v7a} is deliberately excluded from native builds:
     * 32-bit processes cannot address the arena sizes the persistent-mapping path
     * needs, and every device that runs LTW/MobileGlues is 64-bit capable.
     */
    public boolean isArm64() {
        return this.arm64;
    }

    public boolean hasNeon() {
        return this.neonAvailable;
    }

    public boolean hasSve() {
        return this.sveAvailable;
    }

    public List<String> getCpuFeatures() {
        return this.cpuFeatures;
    }

    public String getOsVersion() {
        return this.osVersion;
    }

    public Path getEnvFile() {
        return this.envFile;
    }

    /** Keys read from the launcher's env file that the process env did not have. */
    public Map<String, String> getEnvOverlay() {
        return this.envOverlay;
    }

    public boolean supportsNativeVulkan() {
        return this.renderer.isVulkanBacked();
    }

    /**
     * Applies the Android-specific config mutations before the first frame:
     * mobile memory mode shrinks arenas, battery saver caps the frame rate, and
     * thermal protection engages the sensor reader. Called once, from boot.
     */
    public void applyRuntimeHints(final AetheriumConfig config) {
        Objects.requireNonNull(config, "config");
        if (!this.android) {
            return;
        }
        if (config.mobileMemoryMode.get()) {
            final int budgetMb = (int) MathUtil.clamp(this.memoryBudgetMb, 256L, 8192L);
            // Upload budget tracks the heap: a 1 GB device cannot absorb a 24 MB
            // per-frame arena spike without a GC pause inside the frame.
            final int scaled = MathUtil.clamp(budgetMb / 16, 2, 24) * 1024;
            if (scaled < config.uploadBudgetKb.get()) {
                LOGGER.info("Mobile memory mode: upload budget {} KB -> {} KB (heap {} MB)", config.uploadBudgetKb.get(), scaled, budgetMb);
                config.uploadBudgetKb.set(scaled);
            }
            if (this.maxHeapBytes < 1_500_000_000L) {
                config.asyncShaderCompile.set(false);
                config.programBinaryCache.set(true);
                LOGGER.info("Mobile memory mode: async compilation off, program-binary cache on (small heap)");
            }
        }
        if (config.batterySaver.get()) {
            config.targetFps.set(Math.min(config.targetFps.get() <= 0 ? 60 : config.targetFps.get(), 60));
            config.hzb.set(false);
            LOGGER.info("Battery saver engaged: target fps <= 60, HZB off");
        }
        if (config.thermalThrottle.get()) {
            this.thermalSensorUsable = readThermalMilliCelsius() >= 0;
            if (!this.thermalSensorUsable) {
                LOGGER.warn("Thermal throttle requested but no thermal zone was readable; disabling automatic throttling");
                config.thermalThrottle.set(false);
            }
        }
    }

    /**
     * Polls the hottest package thermal zone.
     *
     * @return temperature in milli-degrees C, or -1 when no sensor is readable.
     *         Guarded by nothing: a stale value only delays a throttle decision by
     *         one frame, and the write is a single int (atomic in practice, and
     *         {@code volatile} keeps it visible).
     */
    public int readThermalMilliCelsius() {
        int hottest = -1;
        final File root = new File("/sys/class/thermal");
        final File[] zones = root.listFiles((dir, name) -> name.startsWith("thermal_zone"));
        if (zones != null) {
            for (final File zone : zones) {
                final File type = new File(zone, "type");
                final File temp = new File(zone, "temp");
                if (!type.isFile() || !temp.isFile()) {
                    continue;
                }
                try {
                    final String kind = Files.readString(type.toPath(), StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
                    if (!(kind.contains("cpu") || kind.contains("gpu") || kind.contains("package") || kind.contains("soc") || kind.contains("skill"))) {
                        continue;
                    }
                    final long value = Long.parseLong(Files.readString(temp.toPath(), StandardCharsets.UTF_8).trim());
                    hottest = Math.max(hottest, (int) value);
                } catch (final IOException | NumberFormatException error) {
                    LOGGER.dev("Thermal zone {} unreadable: {}", zone.getName(), error.getClass().getSimpleName());
                }
            }
        }
        this.thermalMilliCelsius = hottest;
        return hottest;
    }

    public boolean isThermalSensorUsable() {
        return this.thermalSensorUsable;
    }

    /** @return milli-degrees C from the last poll (no syscall) */
    public int getLastThermalMilliCelsius() {
        return this.thermalMilliCelsius;
    }

    /**
     * Fraction of heap actually in use, used by {@code AndroidPowerGovernor} to
     * decide whether to drop mesh workers. {@code ManagementFactory} is used
     * instead of {@code Runtime} arithmetic because some Android ART builds report
     * a max heap that includes the native allocation arena.
     */
    public double heapUsageFraction() {
        final Runtime runtime = Runtime.getRuntime();
        final long max = this.maxHeapBytes <= 0 ? runtime.maxMemory() : this.maxHeapBytes;
        if (max <= 0) {
            return 0.0;
        }
        final long used = runtime.totalMemory() - runtime.freeMemory();
        return MathUtil.clamp((double) used / (double) max, 0.0, 1.0);
    }

    /** Diagnostic string for the F3 line and the Android tab. */
    public String describe() {
        if (!this.android) {
            return "desktop (" + this.osVersion + ')';
        }
        return this.launcher.getDisplayName() + " / " + this.renderer.getDisplayName()
                + String.format(Locale.ROOT, ", %.0f MB heap, %d%% used", this.maxHeapBytes / (1024.0 * 1024.0), this.heapUsageFraction() * 100.0)
                + (this.thermalMilliCelsius >= 0 ? String.format(Locale.ROOT, ", %.1f°C", this.thermalMilliCelsius / 1000.0) : "");
    }

    /**
     * The thermal/battery governor, created on first use.
     *
     * <p>Lazily because constructing it touches {@code /sys/class/thermal}, which is
     * a file read on a device where every millisecond of world-load matters, and
     * because a desktop run must never pay it. Guarded by {@code this} rather than a
     * dedicated lock: the field is written at most twice in practice (render thread
     * first, worker later), and a duplicate construction is harmless — the governor
     * is stateless apart from its cached sensor handle.</p>
     */
    public synchronized AndroidPowerGovernor getPowerGovernor(final AetheriumConfig config) {
        if (this.powerGovernor == null) {
            this.powerGovernor = new AndroidPowerGovernor(this, config);
        }
        return this.powerGovernor;
    }

    /** Non-creating accessor for diagnostics. */
    public AndroidPowerGovernor peekPowerGovernor() {
        return this.powerGovernor;
    }

    /** JVM uptime in ms, for the boot-timing log line. */
    public static long jvmUptimeMs() {
        try {
            return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        } catch (final RuntimeException error) {
            return -1L;
        }
    }
}
