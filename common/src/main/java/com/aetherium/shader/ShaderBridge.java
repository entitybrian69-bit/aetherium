package com.aetherium.shader;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Version-independent face of the shader pack engine: what the settings page, the config and
 * the other renderer features talk to. The version-specific runtime ({@code ShaderRuntime},
 * 1.16.5) polls {@link #takeReload()} at frame start on the render thread and reports back
 * through {@link #loaded} / {@link #failed}.
 */
public final class ShaderBridge {
    /** Name of the pack bundled in the jar (assets/aetherium/shaderpacks/lite). */
    public static final String LITE = "Aetherium Lite";
    public static final String OFF = "";
    public static final String[] COMPAT_MODES = {"auto", "on", "off"};

    private static volatile boolean supported;
    private static volatile String selected = OFF;
    private static volatile String compat = "auto";
    private static volatile boolean reload = true;
    /** True while a pack is loaded and drawing (render thread writes). */
    private static volatile boolean running;
    private static volatile ShaderPack pack;
    private static volatile ShaderPipeline pipeline;
    private static volatile String status = "Off";
    private static volatile List<String> problems = Collections.emptyList();
    private static volatile String device = "";
    private static final Map<String, String> PENDING = new LinkedHashMap<>();
    private static volatile String pendingProfile;
    private static volatile Path gameDir;

    private ShaderBridge() {
    }

    /** From RenderToggles.refresh: what the config asks for. Changes trigger a reload. */
    public static void configure(final boolean available, final String packName, final String compatMode) {
        final String name = available && packName != null ? packName.trim() : OFF;
        final String mode = compatMode == null ? "auto" : compatMode.trim().toLowerCase(Locale.ROOT);
        if (!name.equals(selected) || !mode.equals(compat) || available != supported) {
            selected = name;
            compat = mode;
            supported = available;
            reload = true;
        }
    }

    /** A pack is selected (it may still be loading or have failed): other renderers step aside. */
    public static boolean wantsPack() {
        return supported && !selected.isEmpty();
    }

    /** A pack is loaded and drawing this session. */
    public static boolean packActive() {
        return running;
    }

    public static boolean supported() {
        return supported;
    }

    public static String selected() {
        return selected;
    }

    public static String compatMode() {
        return compat;
    }

    public static ShaderPack pack() {
        return pack;
    }

    public static ShaderPipeline pipeline() {
        return pipeline;
    }

    public static String status() {
        return status;
    }

    public static List<String> problems() {
        return problems;
    }

    public static String device() {
        return device;
    }

    public static void setGameDir(final Path dir) {
        gameDir = dir;
    }

    public static Path shaderpacksDir() {
        return gameDir == null ? null : gameDir.resolve("shaderpacks");
    }

    /** Settings page: an option value staged for the next reload. */
    public static void stageOption(final String name, final String value) {
        synchronized (PENDING) {
            PENDING.put(name, value);
        }
        reload = true;
    }

    public static void stageProfile(final String profile) {
        pendingProfile = profile;
        reload = true;
    }

    public static void stageResetOptions() {
        pendingProfile = "\u0000reset";
        reload = true;
    }

    public static void requestReload() {
        reload = true;
    }

    /** Render thread: true once per requested reload. */
    public static boolean takeReload() {
        if (!reload) {
            return false;
        }
        reload = false;
        return true;
    }

    /** Applies staged option edits to a pack; returns true when anything changed. */
    public static boolean applyPending(final ShaderPack target) {
        boolean changed = false;
        final String profile = pendingProfile;
        pendingProfile = null;
        if (profile != null && target != null) {
            if (profile.equals("\u0000reset")) {
                target.options.resetAll();
                target.profileDisabled.clear();
                target.reloadDerived();
            } else {
                target.applyProfile(profile);
            }
            changed = true;
        }
        final Map<String, String> edits;
        synchronized (PENDING) {
            edits = new LinkedHashMap<>(PENDING);
            PENDING.clear();
        }
        if (target != null && !edits.isEmpty()) {
            for (final Map.Entry<String, String> e : edits.entrySet()) {
                target.options.set(e.getKey(), e.getValue());
            }
            target.profileDisabled.clear();
            final String now = target.options.currentProfile(target.layout.profiles);
            if (now != null) {
                target.options.applyProfile(now, target.layout.profiles, target.profileDisabled, 0);
            }
            target.reloadDerived();
            changed = true;
        }
        return changed;
    }

    public static void loaded(final ShaderPack p, final ShaderPipeline pl, final String deviceInfo) {
        pack = p;
        pipeline = pl;
        device = deviceInfo;
        final List<String> all = new ArrayList<>();
        all.addAll(pl.errors);
        all.addAll(pl.warnings);
        problems = all;
        status = p.name + ": " + pl.summary();
        running = true;
    }

    public static void failed(final String message, final List<String> errors) {
        running = false;
        pack = null;
        pipeline = null;
        status = message;
        problems = errors == null ? Collections.<String>emptyList() : new ArrayList<>(errors);
    }

    public static void off() {
        running = false;
        pack = null;
        pipeline = null;
        status = "Off";
        problems = Collections.emptyList();
    }

    /** Runtime error while drawing: the pack is switched off until the user reloads. */
    public static void broken(final String message) {
        running = false;
        pipeline = null;
        status = "Stopped: " + message;
    }

    /** Pack names for the settings page: the bundled one, then folders and zips in shaderpacks/. */
    public static List<String> available() {
        final List<String> out = new ArrayList<>();
        out.add(LITE);
        final Path dir = shaderpacksDir();
        if (dir == null || !Files.isDirectory(dir)) {
            return out;
        }
        final List<String> found = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (final Path p : stream) {
                final String n = p.getFileName().toString();
                if (Files.isDirectory(p) || n.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                    found.add(n);
                }
            }
        } catch (final IOException ignored) {
            // unreadable folder: only the bundled pack
        }
        Collections.sort(found, String.CASE_INSENSITIVE_ORDER);
        out.addAll(found);
        return out;
    }

    /** Where a pack's option values are saved (OptiFine's shaderpacks/&lt;pack&gt;.txt). */
    public static Path optionsFile(final String name) {
        final Path dir = shaderpacksDir();
        if (dir == null) {
            return null;
        }
        if (LITE.equals(name)) {
            return gameDir.resolve("config").resolve("aetherium-lite-shader.txt");
        }
        return dir.resolve(name + ".txt");
    }

    /** Whether compat (GL translator) mode applies for the detected translator. */
    public static boolean forceCompat() {
        return "on".equals(compat);
    }

    public static boolean disableCompat() {
        return "off".equals(compat);
    }
}
