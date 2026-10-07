package com.aetherium.android;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

/**
 * Which Android Java launcher is hosting the JVM.
 *
 * <p>Detection order matters: a launcher that wraps another one (FCL shipping a
 * Pojav-compatible {@code custom_env.txt}) must report the wrapper, because the
 * wrapper is what sets the renderer environment. Every signal below is a string
 * a launcher writes itself, verified against launcher sources on 2026-10-06:
 * {@code POJAV_RENDERER} (Pojav and its derivatives, including FCL, which reuses
 * Pojav's env contract), the {@code /data/user/N/<package>} library paths, and
 * the launcher package name from {@code java.command}.</p>
 */
public enum AndroidLauncher {
    DESKTOP("desktop", "Desktop"),
    POJAV("pojav", "PojavLauncher"),
    ZALITH("zalith", "Zalith Launcher"),
    FCL("fcl", "Fold Craft Launcher"),
    AMETHYST("amethyst", "Amethyst (AngelAura)"),
    DROIDBRIDGE("droidbridge", "DroidBridge"),
    UNKNOWN_ANDROID("unknown_android", "Android (unknown launcher)");

    /** Package-name fragments that identify each launcher's private data dir. */
    private final String id;
    private final String displayName;

    AndroidLauncher(final String id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isAndroid() {
        return this != DESKTOP;
    }

    /**
     * Identifies the launcher from process environment. Cheap and side-effect
     * free so it can be re-run after a config reload.
     *
     * @param env        process environment (never null; {@code System.getenv()} in production)
     * @param userDir    {@code user.home}/{@code user.dir} style path (may be null)
     * @param command    {@code java.command} system property (may be null)
     */
    public static AndroidLauncher detect(final AndroidEnvProvider env, final String userDir, final String command) {
        Objects.requireNonNull(env, "env");
        final String haystack = normalize(userDir) + ' ' + normalize(command) + ' ' + normalize(env.get("LD_LIBRARY_PATH"))
                + ' ' + normalize(env.get("java.library.path"));

        // Explicit markers win over path heuristics: they are set by the launcher.
        if (env.get("ZALITH_LAUNCHER") != null || haystack.contains("zalith")) {
            return ZALITH;
        }
        if (env.get("FCL_LAUNCHER") != null || haystack.contains("foldcraft") || haystack.contains("fcl")) {
            return FCL;
        }
        if (env.get("AMETHYST_LAUNCHER") != null || haystack.contains("angelaura") || haystack.contains("amethyst")) {
            return AMETHYST;
        }
        if (env.get("DROIDBRIDGE") != null || haystack.contains("droidbridge")) {
            return DROIDBRIDGE;
        }
        if (env.get("POJAV_RENDERER") != null || haystack.contains("pojav") || haystack.contains("kdt.pojavlaunch")) {
            return POJAV;
        }
        if (isAndroidPath(haystack)) {
            return UNKNOWN_ANDROID;
        }
        return DESKTOP;
    }

    /** True for the Android private-data layouts the launchers actually use. */
    static boolean isAndroidPath(final String haystack) {
        if (haystack == null || haystack.isEmpty()) {
            return false;
        }
        return haystack.matches("(?s).*/data/(user/\\d+/|data/)(com|net|io)\\.[A-Za-z0-9_.]+.*")
                || haystack.contains("/storage/emulated/0/games/")
                || haystack.contains("/data/data/");
    }

    /** Filesystem hint used to locate the launcher's env file; null when absent. */
    public static File findCustomEnvFile(final AndroidEnvProvider env) {
        final String[] candidates = {
                env.get("POJAV_CUSTOM_ENV"),
                env.get("FCL_CUSTOM_ENV"),
                env.get("ZALITH_CUSTOM_ENV"),
                new File(System.getProperty("user.dir", "."), "custom_env.txt").getPath(),
                new File(System.getProperty("user.home", "."), "custom_env.txt").getPath(),
                "/data/data/net.kdt.pojavlaunch/custom_env.txt",
                "/data/data/com.tungsten.fclauncher/custom_env.txt",
                "/data/data/com.zalithlauncher.app/custom_env.txt",
        };
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isEmpty()) {
                continue;
            }
            final File file = new File(candidate);
            if (file.isFile() && file.canRead()) {
                return file;
            }
        }
        return null;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
