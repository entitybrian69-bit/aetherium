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
    /** Bundled test pack for the CI smoke test only (not listed): block ids, normals, shadow map. */
    public static final String DIAGNOSTIC = "Aetherium Diagnostic";

    /** The jar folder of a bundled pack, or null for packs on disk. */
    public static String bundledRoot(final String name) {
        if (LITE.equals(name)) {
            return "/assets/aetherium/shaderpacks/lite/";
        }
        if (DIAGNOSTIC.equals(name)) {
            return "/assets/aetherium/shaderpacks/diagnostic/";
        }
        return null;
    }
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

    /** Opens a pack: bundled ones from the jar (through their files.txt index), others from disk. */
    public static PackSource openSource(final String name) throws IOException {
        final String root = bundledRoot(name);
        if (root == null) {
            final Path path = packPath(name);
            return path == null ? null : PackSource.open(path);
        }
        final byte[] index = resource(root + "files.txt");
        if (index == null) {
            return null;
        }
        final Map<String, byte[]> files = new java.util.HashMap<>();
        for (final String line : new String(index, java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
            final String f = line.trim();
            if (!f.isEmpty()) {
                final byte[] data = resource(root + "shaders/" + f);
                if (data != null) {
                    files.put(f, data);
                }
            }
        }
        return new PackSource.Memory(files);
    }

    private static byte[] resource(final String path) throws IOException {
        try (java.io.InputStream in = ShaderBridge.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            final byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static volatile ShaderPack preview;
    private static volatile String previewKey = "";

    /**
     * The pack whose options the settings page shows: the loaded one, or (selected but not loaded
     * yet, e.g. on the title screen) one read from its files without touching the GPU.
     */
    public static ShaderPack optionsPack(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        final ShaderPack loaded = pack;
        if (loaded != null && loaded.name.equals(name)) {
            return loaded;
        }
        final Path file = bundledRoot(name) == null ? packPath(name) : null;
        long stamp = 0;
        try {
            stamp = file == null ? 0 : Files.getLastModifiedTime(file).toMillis();
        } catch (final IOException ignored) {
            // read below anyway
        }
        final String key = name + "|" + stamp;
        if (preview != null && key.equals(previewKey)) {
            return preview;
        }
        try (PackSource source = openSource(name)) {
            if (source == null) {
                return null;
            }
            final Map<String, String> macros = new java.util.TreeMap<>();
            macros.put("MC_VERSION", "11605");
            macros.put("MC_GL_VERSION", "300");
            macros.put("MC_GLSL_VERSION", "130");
            macros.put("MC_RENDER_QUALITY", "1.0");
            macros.put("MC_SHADOW_QUALITY", "1.0");
            macros.put("MC_HAND_DEPTH", "0.125");
            macros.put("AETHERIUM", "1");
            final ShaderPack p = ShaderPack.load(name, source, macros, optionsFile(name), null);
            preview = p;
            previewKey = key;
            return p;
        } catch (final Throwable t) {
            return null;
        }
    }

    /** What the settings page shows for an option: the staged edit, else the pack's value. */
    public static String optionValue(final String option, final String fallback) {
        synchronized (PENDING) {
            final String v = PENDING.get(option);
            if (v != null) {
                return v;
            }
        }
        final ShaderPack p = pack;
        if (p != null && p.name.equals(selected)) {
            final ShaderOptions.Option o = p.options.get(option);
            if (o != null) {
                return o.value();
            }
        }
        return fallback;
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

    /**
     * Folders searched for packs: the game folder's shaderpacks first (created when missing), then
     * shaderpacks folders above it up to .minecraft. Android launchers often give each version its
     * own game folder (.minecraft/versions/1.16.5-fabric) while packs sit in .minecraft/shaderpacks.
     */
    public static List<Path> packDirs() {
        final List<Path> out = new ArrayList<>();
        final Path own = shaderpacksDir();
        if (own == null) {
            return out;
        }
        try {
            Files.createDirectories(own);
        } catch (final IOException | RuntimeException ignored) {
            // read-only storage: still list what exists
        }
        out.add(own);
        Path up = gameDir.toAbsolutePath().normalize().getParent();
        for (int i = 0; up != null && i < 4; i++, up = up.getParent()) {
            final Path dir = up.resolve("shaderpacks");
            if (Files.isDirectory(dir) && !out.contains(dir)) {
                out.add(dir);
            }
            final Path name = up.getFileName();
            if (name != null && name.toString().equals(".minecraft")) {
                break;
            }
        }
        return out;
    }

    /** The pack file or folder for a listed name (first folder of {@link #packDirs} that has it). */
    public static Path packPath(final String name) {
        for (final Path dir : packDirs()) {
            final Path p = dir.resolve(name);
            if (Files.exists(p)) {
                return p;
            }
        }
        final Path own = shaderpacksDir();
        return own == null ? null : own.resolve(name);
    }

    /** Pack names for the settings page: the bundled one, then folders and zips in the pack folders. */
    public static List<String> available() {
        final List<String> out = new ArrayList<>();
        out.add(LITE);
        out.addAll(externalPacks());
        return out;
    }

    /** Downloaded packs only (folders and zips in the pack folders), sorted: the External Shaders page. */
    public static List<String> externalPacks() {
        final List<String> found = new ArrayList<>();
        for (final Path dir : packDirs()) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (final Path p : stream) {
                    final String n = p.getFileName().toString();
                    final boolean pack = Files.isDirectory(p) ? PackSource.shadersFolder(p) != null
                            : n.toLowerCase(Locale.ROOT).endsWith(".zip");
                    if (pack && !found.contains(n)) {
                        found.add(n);
                    }
                }
            } catch (final IOException | RuntimeException ignored) {
                // unreadable folder: skip it
            }
        }
        Collections.sort(found, String.CASE_INSENSITIVE_ORDER);
        return found;
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
        if (DIAGNOSTIC.equals(name)) {
            return gameDir.resolve("config").resolve("aetherium-diagnostic-shader.txt");
        }
        final Path pack = packPath(name);
        return (pack == null || pack.getParent() == null ? dir : pack.getParent()).resolve(name + ".txt");
    }

    /** Whether compat (GL translator) mode applies for the detected translator. */
    public static boolean forceCompat() {
        return "on".equals(compat);
    }

    public static boolean disableCompat() {
        return "off".equals(compat);
    }
}
