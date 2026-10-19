package com.aetherium.shader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A shader pack's user options, detected the way OptiFine does it:
 * <ul>
 *   <li>{@code #define NAME} / {@code //#define NAME}: an on/off switch (only when some file tests
 *   it with {@code #ifdef}, {@code #ifndef} or {@code defined});</li>
 *   <li>{@code #define NAME 1.0 // comment [0.5 1.0 2.0]}: a value option with allowed values;</li>
 *   <li>{@code const int shadowMapResolution = 2048; // [1024 2048 4096]}: the recognized
 *   {@code const} settings.</li>
 * </ul>
 * Chosen values are applied by rewriting those lines in every source file before compiling.
 * Values are stored next to the pack in {@code shaderpacks/<pack>.txt} ({@code NAME=value}), the
 * same file and format OptiFine uses, so settings carry over.
 */
public final class ShaderOptions {
    public static final class Option {
        public final String name;
        public final boolean toggle;
        public final boolean constant;
        public final String defaultValue;
        public final List<String> values;
        public final String comment;
        /** Shown in the options list: switches, and value options with an allowed-values list. */
        public final boolean listed;
        boolean ambiguous;
        String value;

        Option(final String name, final boolean toggle, final boolean constant, final String def,
               final List<String> values, final String comment, final boolean listed) {
            this.name = name;
            this.toggle = toggle;
            this.constant = constant;
            this.defaultValue = def;
            this.values = values;
            this.comment = comment;
            this.listed = listed;
            this.value = def;
        }

        public String value() {
            return this.value;
        }

        public boolean isOn() {
            return "true".equals(this.value);
        }

        public boolean changeable() {
            return !this.ambiguous && (this.toggle || this.values.size() > 1);
        }

        /** Next (step 1) or previous (step -1) allowed value; switches flip. */
        public String cycled(final int step) {
            if (this.toggle) {
                return this.isOn() ? "false" : "true";
            }
            final int n = this.values.size();
            if (n == 0) {
                return this.value;
            }
            int i = indexOfValue(this.values, this.value);
            i = i < 0 ? 0 : Math.floorMod(i + step, n);
            return this.values.get(i);
        }
    }

    /** Recognized const settings (OptiFine's list). */
    static final Set<String> CONSTS = new HashSet<>(Arrays.asList("shadowMapResolution", "shadowDistance",
            "shadowDistanceRenderMul", "shadowIntervalSize", "generateShadowMipmap", "generateShadowColorMipmap",
            "shadowHardwareFiltering", "shadowHardwareFiltering0", "shadowHardwareFiltering1", "shadowtex0Mipmap",
            "shadowtexMipmap", "shadowtex1Mipmap", "shadowcolor0Mipmap", "shadowColor0Mipmap", "shadowcolor1Mipmap",
            "shadowColor1Mipmap", "shadowtex0Nearest", "shadowtexNearest", "shadow0MinMagNearest", "shadowtex1Nearest",
            "shadow1MinMagNearest", "shadowcolor0Nearest", "shadowColor0Nearest", "shadowColor0MinMagNearest",
            "shadowcolor1Nearest", "shadowColor1Nearest", "shadowColor1MinMagNearest", "wetnessHalflife",
            "drynessHalflife", "eyeBrightnessHalflife", "centerDepthHalflife", "sunPathRotation",
            "ambientOcclusionLevel", "superSamplingLevel", "noiseTextureResolution"));

    static final Pattern TOGGLE = Pattern.compile("^\\s*(//+)?\\s*#define\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(//(.*))?$");
    static final Pattern VALUE = Pattern.compile(
            "^\\s*#define\\s+([A-Za-z_][A-Za-z0-9_]*)\\s+([^\\s/]+(?:\\s*/(?!/)[^\\s/]+)*)\\s*(//(.*))?$");
    static final Pattern CONST = Pattern.compile(
            "^\\s*const\\s+(int|float|bool)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*([^;\\s]+)\\s*;\\s*(//(.*))?$");
    private static final Pattern TESTED = Pattern.compile(
            "#\\s*(?:ifdef|ifndef)\\s+([A-Za-z_][A-Za-z0-9_]*)|defined\\s*\\(?\\s*([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern IDENT = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern ALLOWED = Pattern.compile("\\[([^\\]]*)\\]");

    private final Map<String, Option> options = new LinkedHashMap<>();

    public Map<String, Option> all() {
        return this.options;
    }

    public Option get(final String name) {
        return this.options.get(name);
    }

    /** Scans the pack's shader sources. {@code files} maps path to text. */
    public static ShaderOptions detect(final Map<String, String> files) {
        return detect(files, null);
    }

    /**
     * Scans the pack's shader sources; {@code properties} (shaders.properties, may be null) also
     * marks switches it names in screens, sliders and program.*.enabled as options: some packs
     * gate whole programs on a switch that no #ifdef tests.
     */
    public static ShaderOptions detect(final Map<String, String> files, final String properties) {
        final Set<String> tested = new HashSet<>();
        if (properties != null) {
            for (final String raw : properties.split("\n")) {
                final String line = raw.trim();
                final int eq = line.indexOf('=');
                if (eq < 0 || !(line.startsWith("screen") || line.startsWith("sliders")
                        || (line.startsWith("program.") && line.substring(0, eq).trim().endsWith(".enabled")))) {
                    continue;
                }
                final Matcher m = IDENT.matcher(line.substring(eq + 1));
                while (m.find()) {
                    tested.add(m.group());
                }
            }
        }
        for (final String text : files.values()) {
            if (text.indexOf("if") < 0 && text.indexOf("defined") < 0) {
                continue;
            }
            final Matcher m = TESTED.matcher(text);
            while (m.find()) {
                tested.add(m.group(1) != null ? m.group(1) : m.group(2));
            }
        }
        final ShaderOptions out = new ShaderOptions();
        for (final Map.Entry<String, String> file : files.entrySet()) {
            final String text = file.getValue();
            if (text.indexOf("#define") < 0 && text.indexOf("const") < 0) {
                continue;
            }
            for (final String line : text.split("\n")) {
                out.scanLine(line, tested);
            }
        }
        return out;
    }

    private void scanLine(final String line, final Set<String> tested) {
        if (line.indexOf("#define") >= 0) {
            Matcher m = TOGGLE.matcher(line);
            if (m.matches()) {
                final String name = m.group(2);
                if (!tested.contains(name) || name.startsWith("MC_") || name.startsWith("GL_")) {
                    return;
                }
                final String def = m.group(1) == null ? "true" : "false";
                this.add(new Option(name, true, false, def, Arrays.asList("true", "false"),
                        comment(m.group(4)), true));
                return;
            }
            m = VALUE.matcher(line);
            if (m.matches() && line.trim().startsWith("#")) {
                final String name = m.group(1);
                if (name.startsWith("MC_") || name.startsWith("GL_")) {
                    return;
                }
                final String def = m.group(2).trim();
                final String comment = m.group(4);
                final List<String> allowed = allowed(comment, def);
                this.add(new Option(name, false, false, def, allowed, comment(comment), comment != null
                        && ALLOWED.matcher(comment).find()));
            }
            return;
        }
        if (line.indexOf("const") >= 0) {
            final Matcher m = CONST.matcher(line);
            if (m.matches() && CONSTS.contains(m.group(2))) {
                final String def = m.group(3).trim();
                final String comment = m.group(5);
                if ("bool".equals(m.group(1))) {
                    this.add(new Option(m.group(2), true, true, def, Arrays.asList("true", "false"), comment(comment),
                            comment != null && ALLOWED.matcher(comment).find()));
                } else {
                    final List<String> allowed = allowed(comment, def);
                    this.add(new Option(m.group(2), false, true, def, allowed, comment(comment),
                            comment != null && ALLOWED.matcher(comment).find()));
                }
            }
        }
    }

    private void add(final Option o) {
        final Option prev = this.options.get(o.name);
        if (prev == null) {
            this.options.put(o.name, o);
            return;
        }
        if (!sameValue(prev.defaultValue, o.defaultValue) || prev.toggle != o.toggle) {
            prev.ambiguous = true; // different defaults in different files: locked, like OptiFine
        }
    }

    private static String comment(final String c) {
        if (c == null) {
            return "";
        }
        return ALLOWED.matcher(c).replaceAll("").trim();
    }

    static List<String> allowed(final String comment, final String def) {
        final List<String> out = new ArrayList<>();
        if (comment != null) {
            final Matcher m = ALLOWED.matcher(comment);
            if (m.find()) {
                for (final String v : m.group(1).trim().split("\\s+")) {
                    if (!v.isEmpty()) {
                        out.add(v);
                    }
                }
            }
        }
        if (indexOfValue(out, def) < 0) {
            out.add(def);
        }
        return out;
    }

    static int indexOfValue(final List<String> values, final String v) {
        for (int i = 0; i < values.size(); i++) {
            if (sameValue(values.get(i), v)) {
                return i;
            }
        }
        return -1;
    }

    static boolean sameValue(final String a, final String b) {
        if (a.equals(b)) {
            return true;
        }
        final double x = Preprocessor.parseNumber(a);
        final double y = Preprocessor.parseNumber(b);
        return !Double.isNaN(x) && x == y;
    }

    // ------------------------------------------------------------------ values

    public void set(final String name, final String value) {
        final Option o = this.options.get(name);
        if (o == null || o.ambiguous || value == null) {
            return;
        }
        if (o.toggle) {
            o.value = "true".equalsIgnoreCase(value.trim()) ? "true" : "false";
            return;
        }
        final int i = indexOfValue(o.values, value.trim());
        o.value = i >= 0 ? o.values.get(i) : (o.listed ? o.defaultValue : value.trim());
    }

    public void resetAll() {
        for (final Option o : this.options.values()) {
            o.value = o.defaultValue;
        }
    }

    /** Values that differ from the defaults, for saving and the pack's cache key. */
    public Map<String, String> changed() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Option o : this.options.values()) {
            if (!sameValue(o.value, o.defaultValue)) {
                m.put(o.name, o.value);
            }
        }
        return m;
    }

    public void load(final Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            for (final String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                final int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    this.set(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (final IOException ignored) {
            // unreadable: keep defaults
        }
    }

    public void save(final Path file) {
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, String> e : this.changed().entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        try {
            if (sb.length() == 0) {
                Files.deleteIfExists(file);
            } else {
                Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (final IOException ignored) {
            // read-only folder: settings last for this session
        }
    }

    /** Macros for preprocessing properties files: every switched-on option and every value option. */
    public Map<String, String> macros() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Option o : this.options.values()) {
            if (o.constant) {
                continue;
            }
            if (o.toggle) {
                if (o.isOn()) {
                    m.put(o.name, "");
                }
            } else {
                m.put(o.name, o.value);
            }
        }
        return m;
    }

    // ------------------------------------------------------------------ applying

    /** Rewrites option lines in one source file to the chosen values. */
    public String apply(final String text) {
        if (this.changed().isEmpty() || (text.indexOf("#define") < 0 && text.indexOf("const") < 0)) {
            return text;
        }
        final String[] lines = text.split("\n", -1);
        boolean any = false;
        for (int i = 0; i < lines.length; i++) {
            final String r = this.applyLine(lines[i]);
            if (r != null) {
                lines[i] = r;
                any = true;
            }
        }
        return any ? String.join("\n", lines) : text;
    }

    private String applyLine(final String line) {
        if (line.indexOf("#define") >= 0) {
            Matcher m = TOGGLE.matcher(line);
            if (m.matches()) {
                final Option o = this.options.get(m.group(2));
                if (o == null || !o.toggle || o.constant) {
                    return null;
                }
                final boolean on = m.group(1) == null;
                if (on == o.isOn()) {
                    return null;
                }
                return (o.isOn() ? "" : "//") + "#define " + o.name + (m.group(3) != null ? " " + m.group(3) : "");
            }
            m = VALUE.matcher(line);
            if (m.matches() && line.trim().startsWith("#")) {
                final Option o = this.options.get(m.group(1));
                if (o == null || o.toggle || o.constant || sameValue(o.value, m.group(2).trim())) {
                    return null;
                }
                return "#define " + o.name + " " + o.value + (m.group(3) != null ? " " + m.group(3) : "");
            }
            return null;
        }
        if (line.indexOf("const") >= 0) {
            final Matcher m = CONST.matcher(line);
            if (m.matches()) {
                final Option o = this.options.get(m.group(2));
                if (o == null || !o.constant || sameValue(o.value, m.group(3).trim())) {
                    return null;
                }
                return "const " + m.group(1) + " " + o.name + " = " + o.value + ";"
                        + (m.group(4) != null ? " " + m.group(4) : "");
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ profiles

    /**
     * Applies a profile line ("profile.HIGH=SHADOWS SSAO:true !BLOOM profile.MED SHADOW_RES=2048
     * !program.composite1"). Disabled programs are added to {@code disabledPrograms}.
     */
    public void applyProfile(final String name, final Map<String, String> profiles, final Set<String> disabledPrograms,
                             final int depth) {
        final String def = profiles.get(name);
        if (def == null || depth > 8) {
            return;
        }
        for (final String tok : def.trim().split("\\s+")) {
            if (tok.isEmpty()) {
                continue;
            }
            if (tok.startsWith("profile.")) {
                this.applyProfile(tok.substring(8), profiles, disabledPrograms, depth + 1);
            } else if (tok.startsWith("!program.")) {
                disabledPrograms.add(tok.substring(9));
            } else if (tok.startsWith("!")) {
                this.set(tok.substring(1), "false");
            } else {
                final int sep = indexOfSep(tok);
                if (sep > 0) {
                    this.set(tok.substring(0, sep), tok.substring(sep + 1));
                } else {
                    this.set(tok, "true");
                }
            }
        }
    }

    /** Which profile matches the current values exactly, or null ("Custom"). */
    public String currentProfile(final Map<String, String> profiles) {
        for (final String p : profiles.keySet()) {
            final ShaderOptions probe = this.copy();
            probe.resetAll();
            final Map<String, String> before = this.snapshot();
            probe.restore(before);
            probe.applyProfile(p, profiles, new HashSet<>(), 0);
            if (probe.snapshot().equals(before)) {
                return p;
            }
        }
        return null;
    }

    private static int indexOfSep(final String tok) {
        final int c = tok.indexOf(':');
        final int e = tok.indexOf('=');
        if (c < 0) {
            return e;
        }
        if (e < 0) {
            return c;
        }
        return Math.min(c, e);
    }

    public Map<String, String> snapshot() {
        final Map<String, String> m = new LinkedHashMap<>();
        for (final Option o : this.options.values()) {
            m.put(o.name, o.value);
        }
        return m;
    }

    public void restore(final Map<String, String> values) {
        for (final Map.Entry<String, String> e : values.entrySet()) {
            final Option o = this.options.get(e.getKey());
            if (o != null) {
                o.value = e.getValue();
            }
        }
    }

    private ShaderOptions copy() {
        final ShaderOptions c = new ShaderOptions();
        for (final Option o : this.options.values()) {
            final Option n = new Option(o.name, o.toggle, o.constant, o.defaultValue, o.values, o.comment, o.listed);
            n.ambiguous = o.ambiguous;
            n.value = o.value;
            c.options.put(n.name, n);
        }
        return c;
    }
}
