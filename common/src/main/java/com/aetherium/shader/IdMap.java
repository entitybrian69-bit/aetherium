package com.aetherium.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code block.properties} / {@code item.properties} / {@code entity.properties}:
 * {@code block.<id>=<name> <name>:<prop>=<v1>,<v2> ...}. Names without a namespace are
 * {@code minecraft:}. Numeric (pre-1.13) ids are ignored. {@code layer.*} keys are kept apart.
 */
public final class IdMap {
    /** One entry: the required property values (all must match), or none. */
    public static final class Entry {
        public final int id;
        public final Map<String, List<String>> properties;

        Entry(final int id, final Map<String, List<String>> properties) {
            this.id = id;
            this.properties = properties;
        }

        public boolean matches(final Map<String, String> state) {
            for (final Map.Entry<String, List<String>> p : this.properties.entrySet()) {
                final String v = state.get(p.getKey());
                if (v == null || !p.getValue().contains(v)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Full name ("minecraft:oak_leaves") to entries, most specific first. */
    public final Map<String, List<Entry>> byName = new HashMap<>();
    /** layer.solid / layer.cutout / layer.translucent to block names. */
    public final Map<String, List<String>> layers = new HashMap<>();

    public static IdMap parse(final String text, final String prefix) {
        final IdMap map = new IdMap();
        if (text == null) {
            return map;
        }
        final String keyPrefix = prefix + ".";
        for (final String[] kv : PackProperties.entries(text)) {
            final String key = kv[0];
            if (key.startsWith("layer.")) {
                final List<String> names = new ArrayList<>();
                for (final String n : kv[1].split("\\s+")) {
                    if (!n.isEmpty()) {
                        names.add(fullName(n.split(":").length > 2 ? n.substring(0, n.lastIndexOf(':')) : n));
                    }
                }
                map.layers.put(key.substring(6), names);
                continue;
            }
            if (!key.startsWith(keyPrefix)) {
                continue;
            }
            final int id;
            try {
                id = Integer.parseInt(key.substring(keyPrefix.length()).trim());
            } catch (final NumberFormatException e) {
                continue;
            }
            for (final String token : kv[1].split("\\s+")) {
                map.addToken(token, id);
            }
        }
        for (final List<Entry> l : map.byName.values()) {
            l.sort((a, b) -> Integer.compare(b.properties.size(), a.properties.size()));
        }
        return map;
    }

    private void addToken(final String token, final int id) {
        if (token.isEmpty() || Character.isDigit(token.charAt(0))) {
            return; // legacy numeric id
        }
        final String[] parts = token.split(":");
        String name;
        int first;
        if (parts.length >= 2 && parts[1].indexOf('=') < 0) {
            name = parts[0] + ":" + parts[1];
            first = 2;
        } else {
            name = "minecraft:" + parts[0];
            first = 1;
        }
        final Map<String, List<String>> props = new HashMap<>();
        for (int i = first; i < parts.length; i++) {
            final int eq = parts[i].indexOf('=');
            if (eq <= 0) {
                continue;
            }
            final List<String> values = new ArrayList<>();
            for (final String v : parts[i].substring(eq + 1).split(",")) {
                if (!v.isEmpty()) {
                    values.add(v);
                }
            }
            props.put(parts[i].substring(0, eq), values);
        }
        this.byName.computeIfAbsent(name, k -> new ArrayList<>()).add(new Entry(id, props));
    }

    static String fullName(final String n) {
        return n.indexOf(':') >= 0 ? n : "minecraft:" + n;
    }

    /** The id for a name and its state properties, or {@code missing}. */
    /**
     * Packed-id mode (GL translators): the distinct ids, lowest first, by index (0 unused, at most
     * 127 ids; the vertex colour's alpha carries the index).
     */
    public float[] packedTable() {
        final float[] table = new float[128];
        final java.util.TreeSet<Integer> ids = new java.util.TreeSet<>();
        for (final List<Entry> l : this.byName.values()) {
            for (final Entry e : l) {
                ids.add(e.id);
            }
        }
        int i = 1;
        for (final int id : ids) {
            if (i >= 128) {
                break;
            }
            table[i++] = id;
        }
        return table;
    }

    public int lookup(final String name, final Map<String, String> state, final int missing) {
        final List<Entry> l = this.byName.get(name);
        if (l == null) {
            return missing;
        }
        for (final Entry e : l) {
            if (e.matches(state)) {
                return e.id;
            }
        }
        return missing;
    }
}
