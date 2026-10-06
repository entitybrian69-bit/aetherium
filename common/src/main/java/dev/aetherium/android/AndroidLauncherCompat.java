package dev.aetherium.android;

import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime detection of Android Java launchers (PojavLauncher, Zalith, Fold Craft, Amethyst, DroidBridge) and their GL
 * translation layers (GL4ES, Zink, LTW, MobileGlues), plus mobile heuristics: heap-aware memory mode, battery
 * saver and thermal throttle protection sourced from Linux sysfs.
 */
public final class AndroidLauncherCompat {
    public enum Launcher { NONE, POJAV, ZALITH, FOLD_CRAFT, AMETHYST, DROIDBRIDGE, UNKNOWN_POJAV_FORK }
    public enum Renderer { UNKNOWN, GL4ES, ZINK, LTW, MOBILEGLUES, VIRGL, VULKAN_ZINK, KOPPER_ZINK, DESKTOP }

    private static volatile boolean android;
    private static volatile Launcher launcher = Launcher.NONE;
    private static volatile Renderer renderer = Renderer.DESKTOP;
    private static volatile String rendererRaw = "";
    private static volatile boolean arm64;
    private static volatile boolean detected;
    private static final Map<String, String> customEnv = new HashMap<>();

    private static final List<Path> CUSTOM_ENV_PATHS = List.of(
            Path.of("/storage/emulated/0/Android/data/net.kdt.pojavlaunch/files/custom_env.txt"),
            Path.of("/sdcard/Android/data/net.kdt.pojavlaunch/files/custom_env.txt"),
            Path.of("/storage/emulated/0/Android/data/com.movtery.zalithlauncher/files/custom_env.txt"),
            Path.of("/storage/emulated/0/Android/data/com.tungsten.fcl/files/custom_env.txt")
    );

    private AndroidLauncherCompat() {}

    public static synchronized void detect() {
        if (detected) return;
        detected = true;

        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        arm64 = osArch.contains("aarch64") || osArch.contains("arm64");
        String javaVendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT);
        String vmName = System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT);

        boolean androidHints = env("POJAV_RENDERER") != null || env("ZALITH_RENDERER") != null || env("FCL_RENDERER") != null
                || env("POJAV_NATIVEDIR") != null || env("ANDROID_ROOT") != null || env("ANDROID_DATA") != null
                || Files.isDirectory(Path.of("/system/app")) || javaVendor.contains("android") || vmName.contains("dalvik");
        android = androidHints;
        if (!android) { renderer = Renderer.DESKTOP; return; }

        loadCustomEnv();

        // ---- launcher ----
        String nativeDir = nz(env("POJAV_NATIVEDIR")).toLowerCase(Locale.ROOT);
        String home = nz(env("HOME")).toLowerCase(Locale.ROOT);
        String ext = nz(env("EXTERNAL_STORAGE")).toLowerCase(Locale.ROOT);
        String all = nativeDir + " " + home + " " + ext + " " + nz(System.getProperty("java.library.path")).toLowerCase(Locale.ROOT);
        if (env("FCL_RENDERER") != null || all.contains("com.tungsten.fcl")) launcher = Launcher.FOLD_CRAFT;
        else if (env("ZALITH_RENDERER") != null || all.contains("zalith")) launcher = Launcher.ZALITH;
        else if (all.contains("amethyst")) launcher = Launcher.AMETHYST;
        else if (all.contains("droidbridge")) launcher = Launcher.DROIDBRIDGE;
        else if (env("POJAV_RENDERER") != null || all.contains("net.kdt.pojavlaunch")) launcher = Launcher.POJAV;
        else launcher = Launcher.UNKNOWN_POJAV_FORK;

        // ---- renderer / translation layer ----
        String r = firstNonNull(env("FCL_RENDERER"), env("ZALITH_RENDERER"), env("POJAV_RENDERER"), customEnv.get("POJAV_RENDERER"), "");
        rendererRaw = r;
        String rl = r.toLowerCase(Locale.ROOT);
        boolean mesaOverride = env("MESA_GL_VERSION_OVERRIDE") != null || env("MESA_GLSL_VERSION_OVERRIDE") != null
                || customEnv.containsKey("MESA_GL_VERSION_OVERRIDE");
        String libgl = nz(env("LIBGL_ES")) + nz(env("LIBGL_GL")) + nz(env("LIBGL_NAME"));
        String galliumDriver = nz(firstNonNull(env("GALLIUM_DRIVER"), customEnv.get("GALLIUM_DRIVER"))).toLowerCase(Locale.ROOT);

        if (rl.contains("ltw") || rl.contains("opengles3_ltw")) renderer = Renderer.LTW;
        else if (rl.contains("mobileglues") || rl.contains("mg")) renderer = Renderer.MOBILEGLUES;
        else if (rl.contains("vulkan_zink") || rl.contains("vulkanzink")) renderer = Renderer.VULKAN_ZINK;
        else if (rl.contains("kopper")) renderer = Renderer.KOPPER_ZINK;
        else if (rl.contains("zink") || galliumDriver.contains("zink") || (mesaOverride && rl.isEmpty())) renderer = Renderer.ZINK;
        else if (rl.contains("virgl")) renderer = Renderer.VIRGL;
        else if (rl.contains("gl4es") || rl.contains("opengles2") || rl.contains("holy") || !libgl.isEmpty()) renderer = Renderer.GL4ES;
        else renderer = Renderer.UNKNOWN;

        Aetherium.LOGGER.info("[Aetherium] Android detected: launcher={} renderer={} ({}) arm64={} heap={}MiB",
                launcher, renderer, rendererRaw, arm64, Runtime.getRuntime().maxMemory() >> 20);
    }

    private static void loadCustomEnv() {
        for (Path p : CUSTOM_ENV_PATHS) {
            if (!Files.isRegularFile(p)) continue;
            try {
                for (String line : Files.readAllLines(p)) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    int eq = line.indexOf('=');
                    if (eq > 0) customEnv.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
                Aetherium.LOGGER.info("[Aetherium] Loaded {} overrides from {}", customEnv.size(), p);
            } catch (IOException e) {
                Aetherium.LOGGER.debug("[Aetherium] Could not read {}", p, e);
            }
        }
    }

    /** Called once after config load: turn on mobile defaults the first time we see a phone. */
    public static void applyEnvironmentOverrides(AetheriumConfig cfg) {
        if (!android) return;
        long heap = Runtime.getRuntime().maxMemory();
        if (heap < 3L * 1024 * 1024 * 1024 && !cfg.android.mobileMemoryMode) {
            cfg.android.mobileMemoryMode = true;
            Aetherium.LOGGER.info("[Aetherium] Heap {} MiB < 3 GiB: Mobile Memory Mode enabled", heap >> 20);
        }
        if (renderer == Renderer.GL4ES) {
            cfg.performance.hzbOcclusionCulling = false;
            cfg.performance.gpuDrivenRendering = false;
            cfg.performance.multiDrawIndirect = false;
            cfg.performance.persistentMappedBuffers = false;
        }
        cfg.save();
    }

    /** Resolve the user's Android backend choice against what is actually running. */
    public static AetheriumConfig.AndroidBackend resolveBackend(AetheriumConfig cfg) {
        if (cfg.android.backend != AetheriumConfig.AndroidBackend.AUTO) return cfg.android.backend;
        return switch (renderer) {
            case ZINK, VULKAN_ZINK, KOPPER_ZINK, MOBILEGLUES -> AetheriumConfig.AndroidBackend.VULKAN_NATIVE;
            case LTW -> AetheriumConfig.AndroidBackend.LTW;
            case GL4ES, VIRGL, UNKNOWN -> AetheriumConfig.AndroidBackend.GL4ES;
            case DESKTOP -> AetheriumConfig.AndroidBackend.AUTO;
        };
    }

    // ---------------------------------------------------------------- power & thermal

    /** Battery percentage 0..100, or -1 if unknown. Reads sysfs; no Android framework needed. */
    public static int batteryPercent() {
        for (String p : new String[]{"/sys/class/power_supply/battery/capacity", "/sys/class/power_supply/BAT0/capacity"}) {
            try {
                Path path = Path.of(p);
                if (Files.isRegularFile(path)) return Integer.parseInt(Files.readString(path).trim());
            } catch (IOException | NumberFormatException ignored) {}
        }
        return -1;
    }

    public static boolean isCharging() {
        try {
            Path p = Path.of("/sys/class/power_supply/battery/status");
            if (Files.isRegularFile(p)) {
                String s = Files.readString(p).trim().toLowerCase(Locale.ROOT);
                return s.contains("charging") && !s.contains("discharging");
            }
        } catch (IOException ignored) {}
        return false;
    }

    /** Hottest reported thermal zone in °C, or -1 if unavailable. */
    public static int deviceTemperatureC() {
        int max = -1;
        for (int i = 0; i < 32; i++) {
            Path p = Path.of("/sys/class/thermal/thermal_zone" + i + "/temp");
            if (!Files.isRegularFile(p)) { if (i > 4 && max >= 0) break; continue; }
            try {
                int raw = Integer.parseInt(Files.readString(p).trim());
                int c = raw > 1000 ? raw / 1000 : raw;
                if (c > 0 && c < 150) max = Math.max(max, c);
            } catch (IOException | NumberFormatException ignored) {}
        }
        return max;
    }

    public static boolean isThermallyThrottled(AetheriumConfig cfg) {
        if (!android || !cfg.android.thermalThrottleProtection) return false;
        int t = deviceTemperatureC();
        return t >= cfg.android.thermalLimitCelsius;
    }

    /** Render distance the battery saver wants right now (the configured one when off / charging / unknown). */
    public static int batterySaverRenderDistance(AetheriumConfig cfg) {
        int configured = cfg.general.renderDistance;
        if (!android || !cfg.android.batterySaver || isCharging()) return configured;
        int pct = batteryPercent();
        if (pct < 0) return configured;
        int min = cfg.android.batterySaverMinRenderDistance;
        if (pct > 50) return configured;
        if (pct > 20) return Math.max(min, configured * 2 / 3);
        return min;
    }

    // ---------------------------------------------------------------- queries

    public static boolean isAndroid() { return android; }
    public static boolean isArm64() { return arm64; }
    public static Launcher launcher() { return launcher; }
    public static Renderer renderer() { return renderer; }
    public static String rendererRaw() { return rendererRaw; }
    public static long heapMiB() { return Runtime.getRuntime().maxMemory() >> 20; }
    public static int touchMinHitPx(int guiScale) { return Math.max(20, (int) Math.ceil(48.0 / Math.max(1, guiScale))); }

    public static String launcherDisplayName() {
        return switch (launcher) {
            case POJAV -> "PojavLauncher";
            case ZALITH -> "Zalith Launcher";
            case FOLD_CRAFT -> "Fold Craft Launcher";
            case AMETHYST -> "Amethyst";
            case DROIDBRIDGE -> "DroidBridge";
            case UNKNOWN_POJAV_FORK -> "Pojav-based launcher";
            case NONE -> "Desktop";
        };
    }

    private static String env(String k) { return System.getenv(k); }
    private static String nz(String s) { return s == null ? "" : s; }
    private static String firstNonNull(String... vals) { for (String v : vals) if (v != null) return v; return null; }
}
