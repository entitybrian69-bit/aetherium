package com.aetherium.shader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gives the vertex and fragment shader of a program the same varyings in the same order (GL
 * translators only).
 *
 * <p>Krypton / NG-GL4ES compiles every shader on its own through glslang with automatic
 * locations, and glslang numbers varyings in declaration order (iomapper.cpp: {@code TOrderByPriority}
 * falls back to the symbol id). The stages are then linked by those numbers, not by name. A pack
 * whose fragment shader declares its varyings in another order than the vertex shader, or declares
 * one the other stage lacks, gets them crossed: texture coordinates read the fog distance and so on
 * (flat untextured blocks, everything in fog). Desktop drivers match by name, so it only shows on
 * phones.
 *
 * <p>Here every varying declaration of both stages (including ones inside inactive #if branches,
 * which are harmless when declared) is removed, line breaks kept, and the union is declared at the
 * end of the Aetherium header, sorted by name, identically in both stages. The built-in varyings
 * (gl_TexCoord, gl_FrontColor / gl_Color, gl_FrontSecondaryColor / gl_SecondaryColor,
 * gl_FogFragCoord) become ordinary varyings first, because the translator declares those only in
 * the stage that uses them, which shifts everything after them. When a program does something this
 * cannot rewrite safely the sources are returned unchanged.
 */
public final class VaryingAligner {
    private VaryingAligner() {
    }

    private static final Pattern DECL = Pattern.compile(
            "(^|[;{}])([ \\t]*)((?:(?:flat|smooth|noperspective|centroid|invariant|highp|mediump|lowp)\\s+)*)"
                    + "(varying|in|out)\\s+((?:(?:flat|smooth|noperspective|centroid|highp|mediump|lowp)\\s+)*)"
                    + "([A-Za-z_]\\w*)\\s+([^;{}()=]+);",
            Pattern.MULTILINE);
    private static final Pattern DECLARATOR = Pattern.compile("\\s*([A-Za-z_]\\w*)\\s*(?:\\[\\s*(\\d+)\\s*\\])?\\s*");
    private static final Pattern TYPES = Pattern.compile(
            "float|vec[234]|mat[234]|mat[234]x[234]|int|ivec[234]|uint|uvec[234]");
    private static final Pattern VERSION = Pattern.compile("^#version\\s+(\\d+)");
    private static final Pattern VARYING_WORD = Pattern.compile("\\bvarying\\b");

    private static final Pattern GL_TEXCOORD = Pattern.compile("\\bgl_TexCoord\\s*\\[\\s*([^\\]]*?)\\s*\\]");
    private static final Pattern GL_TEXCOORD_ANY = Pattern.compile("\\bgl_TexCoord\\b");

    static final class Varying {
        final String name;
        final String type;
        final int array; // 0 = not an array
        final String interpolation; // "", "flat", "smooth", "noperspective"
        final boolean centroid;

        Varying(final String name, final String type, final int array, final String interpolation, final boolean centroid) {
            this.name = name;
            this.type = type;
            this.array = array;
            this.interpolation = interpolation;
            this.centroid = centroid;
        }

        boolean sameShape(final Varying o) {
            return this.type.equals(o.type) && this.array == o.array;
        }
    }

    /** Thrown inside: this program is left as it is. */
    private static final class Unsupported extends Exception {
        private static final long serialVersionUID = 1L;

        Unsupported(final String why) {
            super(why, null, false, false);
        }
    }

    /** The two stages after aligning, and why nothing was done (null when aligned). */
    public static final class Result {
        public final String vertex;
        public final String fragment;
        public final String skipped;

        Result(final String vertex, final String fragment, final String skipped) {
            this.vertex = vertex;
            this.fragment = fragment;
            this.skipped = skipped;
        }
    }

    /** {@code vs} and {@code fs} are GlslPatcher output (with the Aetherium header). */
    public static Result align(final String vs, final String fs) {
        try {
            return alignOrThrow(vs, fs);
        } catch (final Unsupported e) {
            return new Result(vs, fs, e.getMessage());
        }
    }

    private static Result alignOrThrow(final String vs, final String fs) throws Unsupported {
        final int vEnd = headerEnd(vs);
        final int fEnd = headerEnd(fs);
        final int vVersion = version(vs);
        final int fVersion = version(fs);
        String vBody = vs.substring(vEnd);
        String fBody = fs.substring(fEnd);

        final Map<String, Varying> all = new TreeMap<String, Varying>();

        // built-in varyings first: they become ordinary ones
        final int texCoords = Math.max(texCoordCount(vBody), texCoordCount(fBody));
        if (texCoords > 0) {
            vBody = GL_TEXCOORD.matcher(vBody).replaceAll("aetherium_TexCoord[$1]");
            fBody = GL_TEXCOORD.matcher(fBody).replaceAll("aetherium_TexCoord[$1]");
            add(all, new Varying("aetherium_TexCoord", "vec4", texCoords, "", false));
        }
        if (contains(vBody, "gl_FrontColor") || contains(fBody, "gl_Color")) {
            vBody = replace(vBody, "gl_FrontColor", "aetherium_Color");
            fBody = replace(fBody, "gl_Color", "aetherium_Color");
            add(all, new Varying("aetherium_Color", "vec4", 0, "", false));
        }
        if (contains(vBody, "gl_FrontSecondaryColor") || contains(fBody, "gl_SecondaryColor")) {
            vBody = replace(vBody, "gl_FrontSecondaryColor", "aetherium_SecondaryColor");
            fBody = replace(fBody, "gl_SecondaryColor", "aetherium_SecondaryColor");
            add(all, new Varying("aetherium_SecondaryColor", "vec4", 0, "", false));
        }
        if (contains(vBody, "gl_FogFragCoord") || contains(fBody, "gl_FogFragCoord")) {
            vBody = replace(vBody, "gl_FogFragCoord", "aetherium_FogFragCoord");
            fBody = replace(fBody, "gl_FogFragCoord", "aetherium_FogFragCoord");
            add(all, new Varying("aetherium_FogFragCoord", "float", 0, "", false));
        }
        if (contains(vBody, "gl_BackColor") || contains(vBody, "gl_BackSecondaryColor")) {
            throw new Unsupported("two-sided colors");
        }

        // only what the preprocessor leaves active: packs keep both stages' declarations in one
        // file behind #ifdef VERTEX_SHADER / FRAGMENT_SHADER, and alternatives behind options
        final boolean[] vMask = mask(vs, vVersion);
        final boolean[] fMask = mask(fs, fVersion);
        final int vOffset = lines(vs, vEnd);
        final int fOffset = lines(fs, fEnd);
        final String vActive = take(masked(vBody, vMask, vOffset), true, all);
        final String fActive = take(masked(fBody, fMask, fOffset), false, all);
        vBody = merge(vBody, vActive, vMask, vOffset);
        fBody = merge(fBody, fActive, fMask, fOffset);
        if (all.isEmpty()) {
            return new Result(vs, fs, "no varyings");
        }
        // a varying declared only under some #if, and something else of that name under another
        // (Complementary: "vec3 binormal;" in an #else) cannot be hoisted
        final java.util.Set<String> globals = globalNames(vActive);
        globals.addAll(globalNames(fActive));
        for (final Varying v : all.values()) {
            if (globals.contains(v.name)) {
                throw new Unsupported(v.name + " is also declared as something else");
            }
            final Pattern defined = Pattern.compile("#\\s*(?:define|undef)\\s+" + v.name + "\\b");
            if (defined.matcher(vBody).find() || defined.matcher(fBody).find()) {
                throw new Unsupported("varying " + v.name + " is also a macro");
            }
            if (!v.interpolation.isEmpty() && (vVersion < 130 || fVersion < 130)) {
                throw new Unsupported(v.interpolation + " varying in a GLSL 1.20 stage");
            }
        }
        if (VARYING_WORD.matcher(vActive).find() || VARYING_WORD.matcher(fActive).find()) {
            throw new Unsupported("a varying declaration this cannot read");
        }
        return new Result(vs.substring(0, vEnd - lineLength(vs, vEnd)) + block(all, true, vVersion)
                + vs.substring(vEnd - lineLength(vs, vEnd), vEnd) + vBody,
                fs.substring(0, fEnd - lineLength(fs, fEnd)) + block(all, false, fVersion)
                        + fs.substring(fEnd - lineLength(fs, fEnd), fEnd) + fBody,
                null);
    }

    private static boolean[] mask(final String src, final int version) {
        final Map<String, String> initial = new java.util.HashMap<String, String>();
        initial.put("__VERSION__", Integer.toString(version));
        return new Preprocessor(initial).activeMask(src);
    }

    private static int lines(final String src, final int end) {
        int n = 0;
        for (int i = 0; i < end; i++) {
            if (src.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    /** The body with inactive lines emptied (line count kept). */
    private static String masked(final String body, final boolean[] mask, final int offset) {
        final String[] lines = body.split("\n", -1);
        final StringBuilder b = new StringBuilder(body.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            final int at = offset + i;
            if (at < mask.length && mask[at]) {
                b.append(lines[i]);
            }
        }
        return b.toString();
    }

    /** Active lines from {@code active}, the others as they were. */
    private static String merge(final String body, final String active, final boolean[] mask, final int offset) {
        final String[] lines = body.split("\n", -1);
        final String[] taken = active.split("\n", -1);
        final StringBuilder b = new StringBuilder(body.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                b.append('\n');
            }
            final int at = offset + i;
            b.append(at < mask.length && mask[at] && i < taken.length ? taken[i] : lines[i]);
        }
        return b.toString();
    }

    /** Index just after the header end line (the body starts there). */
    private static int headerEnd(final String src) throws Unsupported {
        final int at = src.indexOf(GlslPatcher.HEADER_END);
        if (at < 0) {
            throw new Unsupported("no Aetherium header");
        }
        final int nl = src.indexOf('\n', at);
        return nl < 0 ? src.length() : nl + 1;
    }

    /** Length of the header end line including its newline (the block goes in front of it). */
    private static int lineLength(final String src, final int end) {
        final int start = src.lastIndexOf('\n', end - 2);
        return end - (start + 1);
    }

    private static int version(final String src) {
        final Matcher m = VERSION.matcher(src);
        return m.find() ? Integer.parseInt(m.group(1)) : 110;
    }

    private static boolean contains(final String body, final String word) {
        return body.indexOf(word) >= 0 && Pattern.compile("\\b" + word + "\\b").matcher(body).find();
    }

    private static String replace(final String body, final String word, final String with) {
        return body.indexOf(word) < 0 ? body : body.replaceAll("\\b" + word + "\\b", with);
    }

    private static int texCoordCount(final String body) throws Unsupported {
        if (body.indexOf("gl_TexCoord") < 0) {
            return 0;
        }
        int max = -1;
        int uses = 0;
        final Matcher m = GL_TEXCOORD.matcher(body);
        while (m.find()) {
            uses++;
            try {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            } catch (final NumberFormatException e) {
                throw new Unsupported("gl_TexCoord with a computed index");
            }
        }
        int any = 0;
        final Matcher a = GL_TEXCOORD_ANY.matcher(body);
        while (a.find()) {
            any++;
        }
        if (any != uses) {
            throw new Unsupported("gl_TexCoord used without an index");
        }
        return max + 1;
    }

    private static void add(final Map<String, Varying> all, final Varying v) throws Unsupported {
        final Varying had = all.get(v.name);
        if (had == null) {
            all.put(v.name, v);
            return;
        }
        if (!had.sameShape(v)) {
            throw new Unsupported("varying " + v.name + " has two types");
        }
        if (had.interpolation.isEmpty() && !v.interpolation.isEmpty() || !had.centroid && v.centroid) {
            all.put(v.name, new Varying(v.name, v.type, v.array,
                    had.interpolation.isEmpty() ? v.interpolation : had.interpolation, had.centroid || v.centroid));
        }
    }

    /** Removes the stage's varying declarations (newlines kept) and records them. */
    private static String take(final String body, final boolean vertex, final Map<String, Varying> all) throws Unsupported {
        final Matcher m = DECL.matcher(body);
        final StringBuffer out = new StringBuffer(body.length());
        while (m.find()) {
            final String storage = m.group(4);
            final boolean varying = "varying".equals(storage) || (vertex ? "out".equals(storage) : "in".equals(storage));
            if (!varying) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                continue;
            }
            final String quals = m.group(3) + " " + m.group(5);
            final String type = m.group(6);
            if (!TYPES.matcher(type).matches()) {
                throw new Unsupported("varying of type " + type);
            }
            String interpolation = "";
            for (final String q : new String[] {"flat", "smooth", "noperspective"}) {
                if (Pattern.compile("\\b" + q + "\\b").matcher(quals).find()) {
                    interpolation = q;
                }
            }
            final boolean centroid = quals.contains("centroid");
            final List<Varying> found = new ArrayList<Varying>();
            for (final String d : m.group(7).split(",")) {
                final Matcher dm = DECLARATOR.matcher(d);
                if (!dm.matches()) {
                    throw new Unsupported("varying declarator '" + d.trim() + "'");
                }
                found.add(new Varying(dm.group(1), type, dm.group(2) == null ? 0 : Integer.parseInt(dm.group(2)),
                        interpolation, centroid));
            }
            for (final Varying v : found) {
                if (v.name.startsWith("gl_")) {
                    throw new Unsupported("redeclared built-in " + v.name);
                }
                add(all, v);
            }
            // keep the boundary character and every newline of the declaration
            final StringBuilder keep = new StringBuilder(m.group(1));
            final String decl = m.group(0).substring(m.group(1).length());
            for (int i = 0; i < decl.length(); i++) {
                if (decl.charAt(i) == '\n') {
                    keep.append('\n');
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(keep.toString()));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static final Pattern GLOBAL_DECL = Pattern.compile(
            "^\\s*(?:(?:const|uniform|in|out|inout|attribute|varying|buffer|shared|flat|smooth|noperspective|centroid|"
                    + "invariant|precise|highp|mediump|lowp|layout\\s*\\([^)]*\\))\\s+)*[A-Za-z_]\\w*\\s*(?:\\[[^\\]]*\\]\\s*)?"
                    + "([A-Za-z_][\\s\\S]*)$");
    private static final Pattern LEADING_NAME = Pattern.compile("^\\s*([A-Za-z_]\\w*)");

    /** Names declared at global scope (variables, functions) in what is left of a stage. */
    static java.util.Set<String> globalNames(final String body) {
        final java.util.Set<String> names = new java.util.HashSet<String>();
        int depth = 0;
        final StringBuilder statement = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            final char c = body.charAt(i);
            if (c == '#' && (i == 0 || body.charAt(i - 1) == '\n')) {
                final int nl = body.indexOf('\n', i);
                i = nl < 0 ? body.length() : nl; // preprocessor line
                continue;
            }
            if (depth == 0 && (c == ';' || c == '{' || c == '}')) {
                declared(statement.toString(), names);
                statement.setLength(0);
            } else if (depth == 0) {
                statement.append(c);
            }
            if (c == '{') {
                depth++;
            } else if (c == '}' && depth > 0) {
                depth--;
            }
        }
        return names;
    }

    private static void declared(final String statement, final java.util.Set<String> names) {
        final Matcher m = GLOBAL_DECL.matcher(statement);
        if (!m.matches()) {
            return;
        }
        int parens = 0;
        int start = 0;
        final String decls = m.group(1);
        for (int i = 0; i <= decls.length(); i++) {
            final char c = i < decls.length() ? decls.charAt(i) : ',';
            if (c == '(' || c == '[') {
                parens++;
            } else if (c == ')' || c == ']') {
                parens--;
            } else if (c == ',' && parens == 0) {
                final Matcher n = LEADING_NAME.matcher(decls.substring(start, i));
                if (n.find()) {
                    names.add(n.group(1));
                }
                start = i + 1;
            }
        }
    }

    private static String block(final Map<String, Varying> all, final boolean vertex, final int version) {
        final StringBuilder b = new StringBuilder();
        final String storage = version >= 130 ? (vertex ? "out" : "in") : "varying";
        for (final Varying v : all.values()) {
            if (!v.interpolation.isEmpty()) {
                b.append(v.interpolation).append(' ');
            } else if (version >= 130 && (v.type.startsWith("i") || v.type.startsWith("u"))) {
                b.append("flat "); // integer varyings must be flat
            }
            if (v.centroid) {
                b.append("centroid ");
            }
            b.append(storage).append(' ').append(v.type).append(' ').append(v.name);
            if (v.array > 0) {
                b.append('[').append(v.array).append(']');
            }
            b.append(";\n");
        }
        return b.toString();
    }
}
