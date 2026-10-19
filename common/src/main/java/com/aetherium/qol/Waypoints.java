package com.aetherium.qol;

import com.aetherium.config.Json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Waypoints and the death point (Quality of life), Minecraft-free: the list for each world or
 * server, the colours, the per-dimension coordinate scale and the file they are kept in
 * ({@code aetherium-waypoints.json} next to the Aetherium config).
 *
 * <p>Waypoints belong to a world key (the server address, or {@code local:<folder>} for a
 * singleplayer world) and a dimension id ({@code minecraft:overworld}, ...). Everything is
 * render-thread only; the file is written whenever the list changes, which is rare (a key press
 * or an edit in the waypoint screen), through a temporary file so a crash never leaves half a file.</p>
 */
public final class Waypoints {

    /** Colours offered for waypoints, in the order the screen cycles through them. */
    public static final int[] COLORS = {
        0xFF5B8CFF, 0xFFFF5B5B, 0xFF4ADE80, 0xFFFACC15, 0xFFC084FC, 0xFF22D3EE, 0xFFFB923C, 0xFFF472B6, 0xFFFFFFFF,
    };
    public static final String[] COLOR_NAMES = {
        "Blue", "Red", "Green", "Yellow", "Purple", "Cyan", "Orange", "Pink", "White",
    };
    public static final String DEATH_NAME = "Death";
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    /** Longest name the screen lets you type. */
    public static final int MAX_NAME = 24;
    private static final int MAX_PER_WORLD = 256;

    /** One waypoint. Mutable: the screen edits it in place, then calls {@link #changed()}. */
    public static final class Waypoint {
        public String name;
        public int x;
        public int y;
        public int z;
        public String dimension;
        public int color;
        public boolean visible = true;
        public boolean death;

        public Waypoint(final String name, final int x, final int y, final int z, final String dimension, final int color) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
            this.color = color;
        }

        public int argb() {
            return COLORS[Math.floorMod(this.color, COLORS.length)];
        }

        Map<String, Object> toJson() {
            final Map<String, Object> out = new LinkedHashMap<String, Object>();
            out.put("name", this.name);
            out.put("x", Integer.valueOf(this.x));
            out.put("y", Integer.valueOf(this.y));
            out.put("z", Integer.valueOf(this.z));
            out.put("dimension", this.dimension);
            out.put("color", Integer.valueOf(this.color));
            out.put("visible", Boolean.valueOf(this.visible));
            if (this.death) {
                out.put("death", Boolean.TRUE);
            }
            return out;
        }

        static Waypoint fromJson(final Object raw) {
            if (!(raw instanceof Map)) {
                return null;
            }
            final Map<?, ?> map = (Map<?, ?>) raw;
            final Object name = map.get("name");
            final Object dimension = map.get("dimension");
            if (!(name instanceof String) || !(dimension instanceof String)) {
                return null;
            }
            final Waypoint w = new Waypoint(clampName((String) name), intOf(map.get("x")), intOf(map.get("y")),
                    intOf(map.get("z")), (String) dimension, intOf(map.get("color")));
            w.visible = !Boolean.FALSE.equals(map.get("visible"));
            w.death = Boolean.TRUE.equals(map.get("death"));
            return w;
        }
    }

    private final Path file;
    private final Map<String, List<Waypoint>> worlds = new LinkedHashMap<String, List<Waypoint>>();
    private String world = "";
    private List<Waypoint> current = new ArrayList<Waypoint>();
    private int version;

    public Waypoints(final Path file) {
        this.file = file;
        load();
    }

    // ------------------------------------------------------------------ world selection

    /** Switches to the list of {@code worldKey} (joining a world or server). */
    public void selectWorld(final String worldKey) {
        final String key = worldKey == null ? "" : worldKey;
        if (key.equals(this.world)) {
            return;
        }
        this.world = key;
        List<Waypoint> list = this.worlds.get(key);
        if (list == null) {
            list = new ArrayList<Waypoint>();
            if (!key.isEmpty()) {
                this.worlds.put(key, list);
            }
        }
        this.current = list;
        this.version++;
    }

    public String world() {
        return this.world;
    }

    /** The waypoints of the current world, every dimension; edit through the methods below. */
    public List<Waypoint> all() {
        return Collections.unmodifiableList(this.current);
    }

    /** Bumped on every change, so caches (the screen's rows) know when to rebuild. */
    public int version() {
        return this.version;
    }

    // ------------------------------------------------------------------ edits

    /** A new waypoint named "Waypoint N" with the next colour; null when the world is full. */
    public Waypoint add(final int x, final int y, final int z, final String dimension) {
        if (this.world.isEmpty() || this.current.size() >= MAX_PER_WORLD) {
            return null;
        }
        int number = 1;
        for (final Waypoint w : this.current) {
            if (!w.death) {
                number++;
            }
        }
        final Waypoint w = new Waypoint(uniqueName("Waypoint " + number), x, y, z, dimension,
                (number - 1) % COLORS.length);
        this.current.add(w);
        changed();
        return w;
    }

    /** Replaces the death point of this world (there is only ever one). */
    public Waypoint setDeath(final int x, final int y, final int z, final String dimension) {
        if (this.world.isEmpty()) {
            return null;
        }
        for (int i = this.current.size() - 1; i >= 0; i--) {
            if (this.current.get(i).death) {
                this.current.remove(i);
            }
        }
        final Waypoint w = new Waypoint(DEATH_NAME, x, y, z, dimension, 1);
        w.death = true;
        this.current.add(0, w);
        changed();
        return w;
    }

    public void remove(final Waypoint w) {
        if (this.current.remove(w)) {
            changed();
        }
    }

    /** Saves after an in-place edit (name, colour, visibility). */
    public void changed() {
        this.version++;
        save();
    }

    private String uniqueName(final String base) {
        String name = base;
        int suffix = 2;
        while (hasName(name)) {
            name = base + " (" + suffix++ + ")";
        }
        return name;
    }

    private boolean hasName(final String name) {
        for (final Waypoint w : this.current) {
            if (w.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ coordinates

    /** Horizontal scale of a dimension relative to the Overworld (the Nether is 8). */
    public static double scale(final String dimension) {
        return NETHER.equals(dimension) ? 8.0 : 1.0;
    }

    /**
     * Where a waypoint of {@code from} is when viewed from {@code to}: the same block for the same
     * dimension, the Overworld / Nether conversion between those two (x and z divided or multiplied
     * by 8), or NaN when the two dimensions do not map onto each other (the End).
     */
    public static double convert(final double coordinate, final String from, final String to) {
        if (from.equals(to)) {
            return coordinate;
        }
        if (!isLinked(from) || !isLinked(to)) {
            return Double.NaN;
        }
        return coordinate * scale(from) / scale(to);
    }

    /** The Overworld and the Nether are linked by portals; other dimensions are not. */
    public static boolean isLinked(final String dimension) {
        return OVERWORLD.equals(dimension) || NETHER.equals(dimension);
    }

    /** "Nether" / "Overworld" / "The End" / the path of a modded id. */
    public static String dimensionName(final String dimension) {
        if (OVERWORLD.equals(dimension)) {
            return "Overworld";
        }
        if (NETHER.equals(dimension)) {
            return "Nether";
        }
        if ("minecraft:the_end".equals(dimension)) {
            return "The End";
        }
        final int colon = dimension.indexOf(':');
        return colon >= 0 ? dimension.substring(colon + 1) : dimension;
    }

    // ------------------------------------------------------------------ file

    private void load() {
        if (this.file == null || !Files.isRegularFile(this.file)) {
            return;
        }
        try {
            final String text = new String(Files.readAllBytes(this.file), StandardCharsets.UTF_8);
            final Object worldsRaw = Json.parseObject(text).get("worlds");
            if (!(worldsRaw instanceof Map)) {
                return;
            }
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) worldsRaw).entrySet()) {
                if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof List)) {
                    continue;
                }
                final List<Waypoint> list = new ArrayList<Waypoint>();
                for (final Object raw : (List<?>) entry.getValue()) {
                    final Waypoint w = Waypoint.fromJson(raw);
                    if (w != null && list.size() < MAX_PER_WORLD) {
                        list.add(w);
                    }
                }
                this.worlds.put((String) entry.getKey(), list);
            }
        } catch (final IOException | RuntimeException error) {
            // A damaged file is kept aside and the waypoints start empty, like the config does.
            try {
                Files.move(this.file, this.file.resolveSibling(this.file.getFileName() + ".bad"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (final IOException ignored) {
                // Nothing more to do; the next save overwrites it.
            }
        }
    }

    private void save() {
        if (this.file == null) {
            return;
        }
        final Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("version", Integer.valueOf(1));
        final Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (final Map.Entry<String, List<Waypoint>> entry : this.worlds.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            final List<Object> list = new ArrayList<Object>();
            for (final Waypoint w : entry.getValue()) {
                list.add(w.toJson());
            }
            out.put(entry.getKey(), list);
        }
        root.put("worlds", out);
        try {
            final Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
            Files.write(temp, Json.write(root).getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final IOException atomicUnsupported) {
                Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException ignored) {
            // Waypoints stay in memory; the next change tries again.
        }
    }

    // ------------------------------------------------------------------ helpers

    public static String clampName(final String name) {
        final String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return "Waypoint";
        }
        return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
    }

    private static int intOf(final Object raw) {
        if (raw instanceof Number) {
            final double d = ((Number) raw).doubleValue();
            if (Double.isFinite(d)) {
                return (int) Math.max(-30_000_000, Math.min(30_000_000, Math.round(d)));
            }
        }
        return 0;
    }
}
