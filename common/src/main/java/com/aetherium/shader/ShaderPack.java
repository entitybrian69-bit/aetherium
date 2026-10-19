package com.aetherium.shader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A loaded OptiFine-format shader pack: options, properties, id maps and, per world folder, the
 * resolved program set. Pure Java (no Minecraft or GL); {@code ShaderRuntime} compiles it.
 */
public final class ShaderPack {
    /** gbuffers program to the program used when it is missing (OptiFine's table). */
    public static final Map<String, String> FALLBACK = new LinkedHashMap<>();

    static {
        FALLBACK.put("gbuffers_basic", null);
        FALLBACK.put("gbuffers_line", "gbuffers_basic");
        FALLBACK.put("gbuffers_textured", "gbuffers_basic");
        FALLBACK.put("gbuffers_textured_lit", "gbuffers_textured");
        FALLBACK.put("gbuffers_skybasic", "gbuffers_basic");
        FALLBACK.put("gbuffers_skytextured", "gbuffers_textured");
        FALLBACK.put("gbuffers_clouds", "gbuffers_textured");
        FALLBACK.put("gbuffers_terrain", "gbuffers_textured_lit");
        FALLBACK.put("gbuffers_damagedblock", "gbuffers_terrain");
        FALLBACK.put("gbuffers_block", "gbuffers_terrain");
        FALLBACK.put("gbuffers_beaconbeam", "gbuffers_textured");
        FALLBACK.put("gbuffers_item", "gbuffers_textured_lit");
        FALLBACK.put("gbuffers_entities", "gbuffers_textured_lit");
        FALLBACK.put("gbuffers_entities_glowing", "gbuffers_entities");
        FALLBACK.put("gbuffers_armor_glint", "gbuffers_textured");
        FALLBACK.put("gbuffers_spidereyes", "gbuffers_textured");
        FALLBACK.put("gbuffers_hand", "gbuffers_textured_lit");
        FALLBACK.put("gbuffers_weather", "gbuffers_textured_lit");
        FALLBACK.put("gbuffers_water", "gbuffers_terrain");
        FALLBACK.put("gbuffers_hand_water", "gbuffers_hand");
        FALLBACK.put("shadow", null);
    }

    /** Full-screen pass families, in run order inside the frame. */
    public static final String[] PASS_FAMILIES = {"shadowcomp", "prepare", "deferred", "composite"};

    public final String name;
    public final PackSource source;
    public final ShaderOptions options;
    /** Properties preprocessed with option macros. */
    public PackProperties properties;
    /** Properties preprocessed without option macros (screens, profiles, sliders). */
    public PackProperties layout;
    public IdMap blocks;
    public IdMap items;
    public final Map<String, String> standardMacros;
    public final Set<String> profileDisabled = new HashSet<>();
    /** Language entries from shaders/lang/en_us.lang (option.X, value.X.v, screen.X, profile.X). */
    public final Map<String, String> lang = new HashMap<>();
    private final Map<String, ProgramSet> sets = new HashMap<>();
    private final Map<String, String> fileCache = new HashMap<>();

    private ShaderPack(final String name, final PackSource source, final ShaderOptions options,
                       final Map<String, String> standardMacros) {
        this.name = name;
        this.source = source;
        this.options = options;
        this.standardMacros = standardMacros;
    }

    /**
     * Loads a pack. {@code optionsFile} ({@code shaderpacks/<pack>.txt}) may be null (bundled
     * packs keep their values in Aetherium's config instead).
     */
    public static ShaderPack load(final String name, final PackSource source, final Map<String, String> standardMacros,
                                  final Path optionsFile, final Map<String, String> presetValues) {
        final Map<String, String> texts = new LinkedHashMap<>();
        for (final String f : source.files()) {
            final String lower = f.toLowerCase();
            if (lower.endsWith(".vsh") || lower.endsWith(".fsh") || lower.endsWith(".gsh") || lower.endsWith(".glsl")
                    || lower.endsWith(".inc") || lower.endsWith(".h") || lower.endsWith(".csh")) {
                final String t = source.text(f);
                if (t != null) {
                    texts.put(f, t);
                }
            }
        }
        final ShaderOptions options = ShaderOptions.detect(texts);
        final ShaderPack pack = new ShaderPack(name, source, options, standardMacros);
        pack.layout = PackProperties.parse(pack.preprocessProperties("shaders.properties", false));
        if (optionsFile != null) {
            options.load(optionsFile);
        }
        if (presetValues != null) {
            for (final Map.Entry<String, String> e : presetValues.entrySet()) {
                options.set(e.getKey(), e.getValue());
            }
        }
        // Programs a profile disables are not stored as option values: restore them from the
        // profile the saved values still match (applying it again leaves the values unchanged).
        final String current = options.currentProfile(pack.layout.profiles);
        if (current != null) {
            options.applyProfile(current, pack.layout.profiles, pack.profileDisabled, 0);
        }
        pack.loadLang();
        pack.reloadDerived();
        return pack;
    }

    /** Re-reads everything that depends on option values (after the user changes options). */
    public void reloadDerived() {
        this.fileCache.clear();
        this.sets.clear();
        this.properties = PackProperties.parse(this.preprocessProperties("shaders.properties", true));
        this.blocks = IdMap.parse(this.preprocessProperties("block.properties", true), "block");
        this.items = IdMap.parse(this.preprocessProperties("item.properties", true), "item");
    }

    private void loadLang() {
        final String text = this.source.text("lang/en_us.lang") != null ? this.source.text("lang/en_us.lang")
                : this.source.text("lang/en_US.lang");
        if (text == null) {
            return;
        }
        for (final String line : text.split("\n")) {
            final int eq = line.indexOf('=');
            if (eq > 0 && !line.startsWith("#")) {
                this.lang.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        }
    }

    private String preprocessProperties(final String file, final boolean withOptions) {
        final String text = this.source.text(file);
        if (text == null) {
            return null;
        }
        final Map<String, String> m = new HashMap<>(this.standardMacros);
        if (withOptions) {
            m.putAll(this.options.macros());
        }
        return String.join("\n", new Preprocessor(m).activeLines(text));
    }

    /** Macros for directive scanning: standard ones plus option values. */
    public Map<String, String> scanMacros() {
        final Map<String, String> m = new HashMap<>(this.standardMacros);
        m.putAll(this.options.macros());
        return m;
    }

    /** The world folder to load from: "world-1" for the nether when the pack has it, else "". */
    public String folderFor(final String worldFolder) {
        if (worldFolder != null && !worldFolder.isEmpty() && this.source.hasDir(worldFolder)) {
            return worldFolder + "/";
        }
        return "";
    }

    /** The programs for a world folder ("world0", "world-1", "world1", or a mod dimension's). */
    public ProgramSet programs(final String worldFolder) {
        final String folder = this.folderFor(worldFolder);
        ProgramSet set = this.sets.get(folder);
        if (set == null) {
            set = new ProgramSet(this, folder);
            this.sets.put(folder, set);
        }
        return set;
    }

    boolean enabled(final String program, final String folder) {
        if (this.profileDisabled.contains(program) || (!folder.isEmpty()
                && this.profileDisabled.contains(folder + program))) {
            return false;
        }
        return this.properties.programOn(program, this.options)
                && (folder.isEmpty() || this.properties.programOn(folder + program, this.options));
    }

    /** A pack's programs for one world folder. */
    public static final class ProgramSet {
        public final String folder;
        /** Defined and enabled programs by name. */
        public final Map<String, ProgramSource> defined = new LinkedHashMap<>();
        /** Pass programs in run order, per family ("deferred" to [deferred, deferred1, ...]). */
        public final Map<String, List<ProgramSource>> passes = new HashMap<>();
        public ProgramSource finalPass;
        /** Merged const settings of every program (later programs win, like OptiFine). */
        public final Map<String, String> consts = new LinkedHashMap<>();

        ProgramSet(final ShaderPack pack, final String folder) {
            this.folder = folder;
            final Map<String, String> macros = pack.scanMacros();
            final List<String> names = new ArrayList<>(FALLBACK.keySet());
            for (final String fam : PASS_FAMILIES) {
                names.add(fam);
                for (int i = 1; i < 100; i++) {
                    names.add(fam + i);
                }
            }
            names.add("final");
            final Set<String> files = new HashSet<>(pack.source.files());
            for (final String n : names) {
                if (!files.contains(folder + n + ".fsh") || !files.contains(folder + n + ".vsh")) {
                    continue;
                }
                if (!pack.enabled(n, folder)) {
                    continue;
                }
                final ProgramSource p = ProgramSource.load(pack.source, folder, n, pack.options, macros,
                        pack.fileCache);
                if (p != null) {
                    this.defined.put(n, p);
                    this.consts.putAll(p.consts);
                }
            }
            for (final String fam : PASS_FAMILIES) {
                final List<ProgramSource> list = new ArrayList<>();
                final ProgramSource first = this.defined.get(fam);
                if (first != null) {
                    list.add(first);
                }
                for (int i = 1; i < 100; i++) {
                    final ProgramSource p = this.defined.get(fam + i);
                    if (p != null) {
                        list.add(p);
                    }
                }
                this.passes.put(fam, list);
            }
            this.finalPass = this.defined.get("final");
        }

        /** The program drawing {@code name}, following the fallback chain; null for fixed function. */
        public ProgramSource resolve(final String name) {
            String n = name;
            for (int guard = 0; n != null && guard < 8; guard++) {
                final ProgramSource p = this.defined.get(n);
                if (p != null) {
                    return p;
                }
                n = FALLBACK.get(n);
            }
            return null;
        }

        public String constValue(final String name, final String def) {
            final String v = this.consts.get(name);
            return v == null ? def : v;
        }

        public double constNumber(final String name, final double def) {
            final String v = this.consts.get(name);
            if (v == null) {
                return def;
            }
            final double d = Preprocessor.parseNumber(v.trim());
            return Double.isNaN(d) ? def : d;
        }

        public boolean constBool(final String name, final boolean def) {
            final String v = this.consts.get(name);
            return v == null ? def : "true".equals(v.trim());
        }

        /** True when any defined program uses the attribute (the terrain format needs extending). */
        public boolean needsExtendedVertex() {
            for (final ProgramSource p : this.defined.values()) {
                if (p.usesEntity || p.usesMidTexCoord || p.usesTangent) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasShadow() {
            return this.defined.containsKey("shadow");
        }
    }

    /** Names of the user-facing programs a profile may disable ("!program.composite1"). */
    public static final List<String> PROGRAM_NAMES = Arrays.asList("gbuffers_basic", "gbuffers_textured",
            "gbuffers_textured_lit", "gbuffers_skybasic", "gbuffers_skytextured", "gbuffers_clouds",
            "gbuffers_terrain", "gbuffers_damagedblock", "gbuffers_water", "gbuffers_block", "gbuffers_beaconbeam",
            "gbuffers_entities", "gbuffers_armor_glint", "gbuffers_spidereyes", "gbuffers_hand", "gbuffers_weather",
            "composite", "final", "shadow", "deferred", "gbuffers_hand_water");

    /** Applies a profile and records the programs it disables. */
    public void applyProfile(final String profile) {
        this.profileDisabled.clear();
        this.options.applyProfile(profile, this.layout.profiles, this.profileDisabled, 0);
        this.reloadDerived();
    }

    /** Readable label: lang file entry or the raw name. */
    public String label(final String key, final String fallback) {
        final String v = this.lang.get(key);
        return v == null || v.isEmpty() ? fallback : v;
    }
}
