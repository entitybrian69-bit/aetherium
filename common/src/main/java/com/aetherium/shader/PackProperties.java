package com.aetherium.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code shaders/shaders.properties}, read after preprocessing with the standard and option macros.
 * Keeps declaration order (custom variables may refer to earlier ones).
 */
public final class PackProperties {
    /** Every key in file order (later duplicates replace the value but keep the first position). */
    public final Map<String, String> raw = new LinkedHashMap<>();

    public final Map<String, String> profiles = new LinkedHashMap<>();
    public final Map<String, String> screens = new LinkedHashMap<>();
    public final List<String> sliders = new ArrayList<>();
    public final Map<String, String> programEnabled = new HashMap<>();
    public final Map<String, String> alphaTest = new HashMap<>();
    public final Map<String, String> blend = new HashMap<>();
    public final Map<String, String> scale = new HashMap<>();
    public final Map<String, String> flip = new HashMap<>();
    public final Map<String, String> bufferSize = new HashMap<>();
    /** texture.&lt;stage&gt;.&lt;name&gt; to path (stage gbuffers, deferred, composite, prepare, shadow). */
    public final Map<String, String> textures = new LinkedHashMap<>();
    public String noiseTexture;

    /** Custom uniforms and variables in order: {kind ("uniform"/"variable"), type, name, expression}. */
    public final List<String[]> customs = new ArrayList<>();

    public static PackProperties parse(final String text) {
        final PackProperties p = new PackProperties();
        if (text == null) {
            return p;
        }
        for (final String[] kv : entries(text)) {
            p.put(kv[0], kv[1]);
        }
        return p;
    }

    /** key/value pairs of a .properties text (continuations, '#'/'!' comments, '=' or ':' separators). */
    public static List<String[]> entries(final String text) {
        final List<String[]> out = new ArrayList<>();
        final String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            while (line.endsWith("\\") && !line.endsWith("\\\\") && i + 1 < lines.length) {
                line = line.substring(0, line.length() - 1) + " " + lines[++i].trim();
            }
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '!') {
                continue;
            }
            int sep = -1;
            for (int k = 0; k < line.length(); k++) {
                final char c = line.charAt(k);
                if (c == '=' || c == ':') {
                    sep = k;
                    break;
                }
            }
            if (sep <= 0) {
                continue;
            }
            out.add(new String[] {line.substring(0, sep).trim(), line.substring(sep + 1).trim()});
        }
        return out;
    }

    private void put(final String key, final String value) {
        this.raw.put(key, value);
        if (key.startsWith("profile.")) {
            this.profiles.put(key.substring(8), value);
        } else if (key.equals("screen") || key.startsWith("screen.") && !key.endsWith(".columns")) {
            this.screens.put(key, value);
        } else if (key.equals("sliders")) {
            for (final String s : value.split("\\s+")) {
                if (!s.isEmpty()) {
                    this.sliders.add(s);
                }
            }
        } else if (key.startsWith("program.") && key.endsWith(".enabled")) {
            this.programEnabled.put(key.substring(8, key.length() - 8), value);
        } else if (key.startsWith("alphaTest.")) {
            this.alphaTest.put(key.substring(10), value);
        } else if (key.startsWith("blend.")) {
            this.blend.put(key.substring(6), value);
        } else if (key.startsWith("scale.")) {
            this.scale.put(key.substring(6), value);
        } else if (key.startsWith("flip.")) {
            this.flip.put(key.substring(5), value);
        } else if (key.startsWith("size.buffer.")) {
            this.bufferSize.put(key.substring(12), value);
        } else if (key.equals("texture.noise")) {
            this.noiseTexture = value;
        } else if (key.startsWith("texture.")) {
            this.textures.put(key.substring(8), value);
        } else if (key.startsWith("uniform.") || key.startsWith("variable.")) {
            final String[] parts = key.split("\\.", 3);
            if (parts.length == 3) {
                this.customs.add(new String[] {parts[0], parts[1], parts[2], value});
            }
        }
    }

    public String get(final String key, final String def) {
        final String v = this.raw.get(key);
        return v == null ? def : v;
    }

    /** "true"/"false" keys such as sun, moon, vignette, shadowTerrain; {@code def} when absent. */
    public boolean flag(final String key, final boolean def) {
        final String v = this.raw.get(key);
        if (v == null) {
            return def;
        }
        return "true".equalsIgnoreCase(v.trim()) || "on".equalsIgnoreCase(v.trim());
    }

    /**
     * Evaluates {@code program.<name>.enabled} (a boolean expression of option switches). Programs
     * without an entry are enabled.
     */
    public boolean programOn(final String name, final ShaderOptions options) {
        final String expr = this.programEnabled.get(name);
        if (expr == null) {
            return true;
        }
        try {
            final Map<String, String> m = options.snapshot();
            return Expr.parse(expr).evalScalar(new Expr.Env() {
                @Override
                public double scalar(final String n) {
                    final String v = m.get(n);
                    if (v == null) {
                        return 0;
                    }
                    if ("true".equals(v)) {
                        return 1;
                    }
                    if ("false".equals(v)) {
                        return 0;
                    }
                    final double d = Preprocessor.parseNumber(v);
                    return Double.isNaN(d) ? 0 : d;
                }

                @Override
                public double[] vector(final String n) {
                    return null;
                }

                @Override
                public double time() {
                    return 0;
                }
            }) != 0;
        } catch (final RuntimeException e) {
            return true;
        }
    }
}
