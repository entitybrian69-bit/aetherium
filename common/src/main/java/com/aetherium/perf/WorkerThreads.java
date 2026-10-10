package com.aetherium.perf;

import com.aetherium.android.AndroidLauncher;
import com.aetherium.config.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sizes Minecraft's shared background pool (world generation, chunk meshing, resource loading).
 *
 * <p>Vanilla uses {@code cores - 1} threads. On a phone that oversubscribes the CPU: the render
 * thread and the integrated-server thread then fight seven busy workers for two big cores, which
 * is what turns world generation into frame drops. Vanilla reads the size from the
 * {@code max.bg.threads} system property once, when {@code Util} initializes, so the value has to
 * be in place before any Minecraft class loads; the mixin plugin calls {@link #applyEarly} from
 * {@code onLoad} for exactly that reason. Config cannot be loaded normally that early, so the
 * single key is read straight from the file.</p>
 */
public final class WorkerThreads {
    public static final String PROPERTY = "max.bg.threads";
    private static final String KEY = "aetherium.performance.worker_threads";

    private static volatile int applied;

    private WorkerThreads() {
    }

    /**
     * @param configured value of {@code performance.worker_threads} (0 = automatic)
     * @return the pool size to request, or 0 to leave vanilla's choice alone
     */
    public static int decide(final int configured, final int cores, final boolean android) {
        if (configured > 0) {
            return Math.min(configured, 255);
        }
        if (!android || cores <= 2) {
            return 0;
        }
        // Leave the render thread, the server thread and one core for the launcher/GPU driver.
        return Math.max(2, Math.min(cores - 1, cores - 3));
    }

    /** Pool size this launch asked for, or 0 when vanilla decided. */
    public static int applied() {
        return applied;
    }

    /** Sets {@link #PROPERTY} unless the user already passed it as a JVM flag. Never throws. */
    public static void applyEarly(final Path configFile) {
        try {
            if (System.getProperty(PROPERTY) != null) {
                return;
            }
            final boolean android = AndroidLauncher.detect(System::getenv, System.getProperty("user.home"),
                    System.getProperty("sun.java.command")).isAndroid();
            final int threads = decide(readConfigured(configFile), Runtime.getRuntime().availableProcessors(), android);
            if (threads > 0) {
                System.setProperty(PROPERTY, Integer.toString(threads));
                applied = threads;
            }
        } catch (final RuntimeException | LinkageError error) {
            // Too early for logging; vanilla's default stays in effect.
        }
    }

    private static final Pattern GAME_DIR = Pattern.compile("--gameDir\\s+(.+?)(?=\\s+--|$)");

    /**
     * {@code <gameDir>/config/aetherium.json}. Loader APIs are not ready when the mixin plugin
     * loads, so the game directory comes from the launch arguments (every launcher passes
     * {@code --gameDir}) with the working directory as the fallback.
     */
    public static Path defaultConfigFile() {
        return gameDirectory(System.getProperty("sun.java.command"), System.getProperty("user.dir", "."))
                .resolve("config").resolve("aetherium.json");
    }

    static Path gameDirectory(final String command, final String workingDir) {
        if (command != null) {
            final Matcher matcher = GAME_DIR.matcher(command);
            if (matcher.find()) {
                try {
                    return Paths.get(matcher.group(1).trim());
                } catch (final RuntimeException error) {
                    // Unparseable path: fall through.
                }
            }
        }
        return Paths.get(workingDir == null ? "." : workingDir);
    }

    static int readConfigured(final Path configFile) {
        if (configFile == null || !Files.isRegularFile(configFile)) {
            return 0;
        }
        try {
            final String text = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
            return parseConfigured(text);
        } catch (final IOException | RuntimeException error) {
            return 0;
        }
    }

    static int parseConfigured(final String text) {
        try {
            final Map<String, Object> root = Json.parseObject(text);
            final Object value = root == null ? null : Json.path(root, KEY);
            if (value instanceof Number) {
                final long n = ((Number) value).longValue();
                return n < 0 ? 0 : (int) Math.min(n, 255L);
            }
        } catch (final RuntimeException error) {
            // Malformed file: the normal loader will report it later.
        }
        return 0;
    }
}
