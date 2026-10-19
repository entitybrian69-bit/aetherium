package com.aetherium.shader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One program of a pack (vertex + fragment [+ geometry]) after includes and option values. */
public final class ProgramSource {
    /** Expanded source with the origin of every line, for readable compile errors. */
    public static final class Expanded {
        public final String text;
        final List<String> files;
        final int[] lines;

        Expanded(final String text, final List<String> files, final int[] lines) {
            this.text = text;
            this.files = files;
            this.lines = lines;
        }

        /** "lib/common.glsl:42" for a 1-based line of {@link #text}. */
        public String origin(final int line) {
            if (line < 1 || line > this.lines.length) {
                return "line " + line;
            }
            return this.files.get(line - 1) + ":" + this.lines[line - 1];
        }
    }

    public final String name;
    public final Expanded vertex;
    public final Expanded fragment;
    public final Expanded geometry;
    /** Color buffers written, in output order ({@code DRAWBUFFERS}/{@code RENDERTARGETS}, or guessed). */
    public int[] drawBuffers;
    public boolean explicitDrawBuffers;
    /** {@code const} declarations in active code of both stages: name to value text. */
    public final Map<String, String> consts = new LinkedHashMap<>();
    /** Fragment outputs needing glBindFragDataLocation: name to output index. */
    public final Map<String, Integer> fragOutputs = new LinkedHashMap<>();
    public boolean usesEntity;
    public boolean usesMidTexCoord;
    public boolean usesTangent;
    public int glslVersion;

    ProgramSource(final String name, final Expanded vertex, final Expanded fragment, final Expanded geometry) {
        this.name = name;
        this.vertex = vertex;
        this.fragment = fragment;
        this.geometry = geometry;
    }

    private static final Pattern DRAWBUFFERS = Pattern.compile("/\\*\\s*DRAWBUFFERS\\s*:\\s*([0-9A-Fa-f]+)\\s*\\*/");
    private static final Pattern RENDERTARGETS = Pattern.compile("/\\*\\s*RENDERTARGETS\\s*:\\s*([0-9,\\s]+)\\*/");
    private static final Pattern CONST = Pattern.compile(
            "\\bconst\\s+(?:int|float|bool|vec4|ivec2|vec2|vec3)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*([^;]+);");
    private static final Pattern FRAG_DATA = Pattern.compile("gl_FragData\\s*\\[\\s*(\\d+)\\s*\\]");
    private static final Pattern OUT_DECL = Pattern.compile(
            "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*)?\\bout\\s+(?:(?:highp|mediump|lowp)\\s+)?"
                    + "(?:vec[234]|float|ivec[234]|uvec[234]|int|uint)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*;");
    private static final Pattern VERSION = Pattern.compile("^\\s*#\\s*version\\s+(\\d+)");

    /** Reads directives from the active lines (preprocessed with {@code macros}). */
    void scan(final Map<String, String> macros) {
        this.glslVersion = version(this.fragment.text);
        final List<String> fsh = new Preprocessor(macros).activeLines(this.fragment.text);
        final List<String> vsh = new Preprocessor(macros).activeLines(this.vertex.text);
        int[] db = null;
        final List<Integer> fragData = new ArrayList<>();
        boolean fragColor = false;
        final List<Integer> outLoc = new ArrayList<>();
        int implicitIndex = 0;
        for (final String line : fsh) {
            if (line.indexOf("DRAWBUFFERS") >= 0) {
                final Matcher m = DRAWBUFFERS.matcher(line);
                if (m.find()) {
                    final String s = m.group(1);
                    db = new int[s.length()];
                    for (int i = 0; i < s.length(); i++) {
                        db[i] = Character.digit(s.charAt(i), 16);
                    }
                }
            } else if (line.indexOf("RENDERTARGETS") >= 0) {
                final Matcher m = RENDERTARGETS.matcher(line);
                if (m.find()) {
                    final List<Integer> l = new ArrayList<>();
                    for (final String part : m.group(1).split(",")) {
                        final String t = part.trim();
                        if (!t.isEmpty()) {
                            l.add(Integer.parseInt(t));
                        }
                    }
                    db = toArray(l);
                }
            }
            if (line.indexOf("gl_FragData") >= 0) {
                final Matcher m = FRAG_DATA.matcher(line);
                while (m.find()) {
                    fragData.add(Integer.parseInt(m.group(1)));
                }
            }
            if (line.indexOf("gl_FragColor") >= 0) {
                fragColor = true;
            }
            if (this.glslVersion >= 130 && line.indexOf("out") >= 0) {
                final String code = Preprocessor.stripComment(line);
                final Matcher m = OUT_DECL.matcher(code);
                while (m.find()) {
                    final String name = m.group(2);
                    int index;
                    if (m.group(1) != null) {
                        index = Integer.parseInt(m.group(1));
                    } else {
                        if (name.startsWith("outColor")) {
                            try {
                                index = Integer.parseInt(name.substring(8));
                            } catch (final NumberFormatException e) {
                                index = implicitIndex;
                            }
                        } else {
                            index = implicitIndex;
                        }
                        this.fragOutputs.put(name, index);
                    }
                    implicitIndex++;
                    outLoc.add(index);
                }
            }
        }
        if (db != null) {
            this.drawBuffers = db;
            this.explicitDrawBuffers = true;
        } else if (!fragData.isEmpty()) {
            int max = 0;
            for (final int i : fragData) {
                max = Math.max(max, i);
            }
            this.drawBuffers = range(Math.min(max, 7) + 1);
        } else if (!outLoc.isEmpty()) {
            int max = 0;
            for (final int i : outLoc) {
                max = Math.max(max, i);
            }
            this.drawBuffers = range(Math.min(max, 7) + 1);
        } else {
            this.drawBuffers = range(1);
        }
        if (fragColor && db == null && fragData.isEmpty()) {
            this.drawBuffers = range(1);
        }
        scanConsts(vsh, this.consts);
        scanConsts(fsh, this.consts);
        for (final String line : vsh) {
            if (line.indexOf("mc_Entity") >= 0) {
                this.usesEntity = true;
            }
            if (line.indexOf("mc_midTexCoord") >= 0) {
                this.usesMidTexCoord = true;
            }
            if (line.indexOf("at_tangent") >= 0) {
                this.usesTangent = true;
            }
        }
    }

    static void scanConsts(final List<String> lines, final Map<String, String> out) {
        for (final String line : lines) {
            if (line.indexOf("const") < 0) {
                continue;
            }
            final Matcher m = CONST.matcher(line);
            while (m.find()) {
                out.put(m.group(1), m.group(2).trim());
            }
        }
    }

    static int version(final String text) {
        for (final String line : text.split("\n", 64)) {
            final Matcher m = VERSION.matcher(line);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return 110;
    }

    private static int[] range(final int n) {
        final int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = i;
        }
        return r;
    }

    private static int[] toArray(final List<Integer> l) {
        final int[] r = new int[l.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = l.get(i);
        }
        return r;
    }

    // ------------------------------------------------------------------ includes

    private static final Pattern INCLUDE = Pattern.compile("^\\s*#\\s*include\\s+[\"<]([^\">]+)[\">]");

    /** Expands {@code #include} (relative to the file, or absolute from the shaders folder). */
    static Expanded expand(final PackSource pack, final String path, final ShaderOptions options,
                           final Map<String, String> cache) {
        final StringBuilder sb = new StringBuilder();
        final List<String> files = new ArrayList<>();
        final List<Integer> lines = new ArrayList<>();
        expandInto(pack, path, options, cache, sb, files, lines, 0);
        final int[] arr = new int[lines.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = lines.get(i);
        }
        return new Expanded(sb.toString(), files, arr);
    }

    private static void expandInto(final PackSource pack, final String path, final ShaderOptions options,
                                   final Map<String, String> cache, final StringBuilder sb, final List<String> files,
                                   final List<Integer> lines, final int depth) {
        String text = cache.get(path);
        if (text == null) {
            final String raw = pack.text(path);
            if (raw == null) {
                sb.append("// aetherium: missing include ").append(path).append('\n');
                files.add(path);
                lines.add(0);
                return;
            }
            text = Preprocessor.joinContinuations(options.apply(raw));
            cache.put(path, text);
        }
        final String dir = path.lastIndexOf('/') >= 0 ? path.substring(0, path.lastIndexOf('/') + 1) : "";
        final String[] split = text.split("\n", -1);
        for (int i = 0; i < split.length; i++) {
            final String line = split[i];
            if (line.indexOf("include") >= 0 && depth < 10) {
                final Matcher m = INCLUDE.matcher(line);
                if (m.find()) {
                    final String target = m.group(1).startsWith("/") ? PackSource.normalize(m.group(1))
                            : PackSource.normalize(dir + m.group(1));
                    expandInto(pack, target, options, cache, sb, files, lines, depth + 1);
                    continue;
                }
            }
            if (i == split.length - 1 && line.isEmpty()) {
                break;
            }
            sb.append(line).append('\n');
            files.add(path);
            lines.add(i + 1);
        }
    }

    /** Builds a program, or null when its vertex or fragment file is missing. */
    static ProgramSource load(final PackSource pack, final String folder, final String name,
                              final ShaderOptions options, final Map<String, String> macros,
                              final Map<String, String> cache) {
        final String base = folder + name;
        if (!pack.exists(base + ".vsh") || !pack.exists(base + ".fsh")) {
            return null;
        }
        final Expanded v = expand(pack, base + ".vsh", options, cache);
        final Expanded f = expand(pack, base + ".fsh", options, cache);
        final Expanded g = pack.exists(base + ".gsh") ? expand(pack, base + ".gsh", options, cache) : null;
        final ProgramSource p = new ProgramSource(name, v, f, g);
        p.scan(macros);
        return p;
    }

    static Map<String, String> copy(final Map<String, String> m) {
        return new HashMap<>(m);
    }
}
