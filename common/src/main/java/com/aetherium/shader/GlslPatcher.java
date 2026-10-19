package com.aetherium.shader;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns an expanded pack source into the GLSL handed to the driver.
 *
 * <ul>
 *   <li>{@code #version} goes first (a pack without one gets {@code 120}); from 150 up, desktop GL
 *   gets the {@code compatibility} profile, since OptiFine-format packs for 1.16.5 use the
 *   fixed-function built-ins ({@code gl_Vertex}, {@code gl_ModelViewMatrix}, {@code gl_Fog}).</li>
 *   <li>The standard macros ({@code MC_VERSION}, {@code MC_GL_*}, {@code MC_RENDER_STAGE_*}, ...)
 *   follow, then a marker comment; compile errors subtract the header length (no {@code #line}).</li>
 *   <li>{@code #extension} lines are hoisted under {@code #version}, and a {@code #version 120}
 *   shader that uses GLSL 1.30 syntax is compiled as 130 where the context allows it.</li>
 *   <li>Lightmap: OptiFine moves vanilla's lightmap to texture unit 1. Minecraft 1.16.5 keeps it on
 *   unit 2 (unit 1 is the entity overlay), so {@code gl_MultiTexCoord1}/{@code gl_TextureMatrix[1]}
 *   become unit 2, and the {@code lightmap} sampler is set to unit 2.</li>
 * </ul>
 */
public final class GlslPatcher {
    public static final int VERTEX = 0;
    public static final int FRAGMENT = 1;
    public static final int GEOMETRY = 2;

    private static final Pattern VERSION = Pattern.compile("^\\s*#\\s*version\\s+(\\d+)\\s*([a-z]*)\\s*$",
            Pattern.MULTILINE);
    private static final Pattern MULTITEX1 = Pattern.compile("\\bgl_MultiTexCoord1\\b");
    /** Pack vertex attributes, carried in texture coordinate sets 3-5 of the extended terrain format. */
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "^\\s*(?:attribute|in)\\s+(?:(?:highp|mediump|lowp)\\s+)?(float|vec[234]|ivec[234])\\s+"
                    + "(mc_Entity|mc_midTexCoord|at_tangent|at_velocity|at_midBlock)\\s*;.*$", Pattern.MULTILINE);
    /** #extension lines anywhere in the expanded source (includes put them mid-file). */
    private static final Pattern EXTENSION = Pattern.compile("^[ \\t]*#[ \\t]*extension[ \\t]+([A-Za-z0-9_]+)[ \\t]*:[ \\t]*([a-z]+)[^\\n]*$",
            Pattern.MULTILINE);
    private static final Pattern TEXMAT1 = Pattern.compile("\\bgl_TextureMatrix\\s*\\[\\s*1\\s*\\]");

    static final String HEADER_END = "// end of Aetherium header";

    private GlslPatcher() {
    }

    /**
     * @param compat GL translator on GLES (gl4es, Krypton): no profile suffix
     *               (some translators mishandle it); version capped to what it accepts
     * @param maxGlsl highest GLSL version the context supports (e.g. 120, 330, 460)
     */
    public static String patch(final String source, final int stage, final Map<String, String> macros,
                               final boolean compat, final int maxGlsl) {
        String body = source;
        int version = 120;
        String profile = "";
        final Matcher vm = VERSION.matcher(body);
        if (vm.find()) {
            version = Integer.parseInt(vm.group(1));
            profile = vm.group(2);
            body = body.substring(0, vm.start()) + body.substring(vm.end()); // keep the line count: leaves ""
        }
        if (stage == GEOMETRY && version < 150) {
            version = 150; // layout qualifiers need 150; OptiFine packs declare it anyway
        }
        if (version > maxGlsl && maxGlsl >= 120) {
            version = Math.max(maxGlsl, 120); // try anyway: many "#version 130" packs only use 120 syntax
        }
        if (version < 130 && maxGlsl >= 130 && needs130(body)) {
            version = 130; // GLSL 1.30 syntax under "#version 120" (lenient drivers accept it, Mesa and GLES do not)
        }
        if (stage != FRAGMENT) {
            if (body.indexOf("gl_MultiTexCoord1") >= 0) {
                body = MULTITEX1.matcher(body).replaceAll("gl_MultiTexCoord2");
            }
        }
        final StringBuilder attributeDefines = new StringBuilder();
        if (stage == VERTEX && (body.indexOf("mc_") >= 0 || body.indexOf("at_") >= 0)) {
            final Matcher am = ATTRIBUTE.matcher(body);
            final StringBuffer sb = new StringBuffer();
            while (am.find()) {
                attributeDefines.append("#define ").append(am.group(2)).append(' ')
                        .append(attributeSource(am.group(2), am.group(1))).append('\n');
                am.appendReplacement(sb, ""); // the line stays (empty) so line numbers hold
            }
            am.appendTail(sb);
            body = sb.toString();
        }
        if (body.indexOf("gl_TextureMatrix") >= 0) {
            body = TEXMAT1.matcher(body).replaceAll("gl_TextureMatrix[2]");
        }
        // GLSL wants #extension before any code; drivers like Mesa reject it later. Hoisted under
        // #version (the line stays, empty); "require" becomes "enable" so an unused one cannot fail.
        final StringBuilder extensions = new StringBuilder();
        if (body.indexOf("extension") >= 0) {
            final Matcher em = EXTENSION.matcher(body);
            final StringBuffer sb = new StringBuffer();
            final java.util.Set<String> seen = new java.util.HashSet<>();
            while (em.find()) {
                final String behaviour = "require".equals(em.group(2)) ? "enable" : em.group(2);
                if (seen.add(em.group(1))) {
                    extensions.append("#extension ").append(em.group(1)).append(" : ").append(behaviour).append('\n');
                }
                em.appendReplacement(sb, "");
            }
            em.appendTail(sb);
            body = sb.toString();
        }
        final StringBuilder out = new StringBuilder(body.length() + 2048);
        out.append("#version ").append(version);
        if (version >= 150 && !compat && version != 300) {
            out.append(profile.isEmpty() || "core".equals(profile) ? " compatibility" : " " + profile);
        } else if (!profile.isEmpty() && version >= 150) {
            out.append(' ').append(profile);
        }
        out.append('\n');
        out.append(extensions);
        for (final Map.Entry<String, String> e : macros.entrySet()) {
            out.append("#define ").append(e.getKey());
            if (!e.getValue().isEmpty()) {
                out.append(' ').append(e.getValue());
            }
            out.append('\n');
        }
        out.append(attributeDefines);
        // no #line: drivers disagree on whether "#line 1" numbers the next line 1 or 2 (GLSL < 3.30),
        // and some translators mishandle it; errors subtract the header length instead
        out.append(HEADER_END).append('\n');
        out.append(body);
        return out.toString();
    }

    private static final Pattern NEEDS_130 = Pattern.compile(
            "\\bflat\\s+(?:varying|in|out)\\b|\\b(?:texture|texelFetch|textureLod|textureSize|textureGrad)\\s*\\("
                    + "|\\bgl_VertexID\\b|\\bgl_InstanceID\\b|\\b(?:uint|uvec[234])\\b");
    private static final Pattern TEXTURE_VARIABLE = Pattern.compile(
            "\\b(?:sampler2D|sampler2DShadow|float|vec[234]|int)\\s+texture\\s*[;=,\\[]");

    /** 1.30-only syntax in a pre-1.30 shader, and nothing named "texture" that 1.30 would clash with. */
    static boolean needs130(final String body) {
        return NEEDS_130.matcher(body).find() && !TEXTURE_VARIABLE.matcher(body).find();
    }

    /**
     * The built-in an attribute maps to. 1.16.5 only supports extra vertex elements as texture
     * coordinate sets (its VertexFormatElement turns other indexed elements into UVs), so the
     * extended terrain format stores mc_Entity in set 3, mc_midTexCoord in set 4 and at_tangent in
     * set 5. at_velocity / at_midBlock (Iris, 1.17+) read as zero.
     */
    static String attributeSource(final String name, final String type) {
        final String src;
        switch (name) {
            case "mc_Entity": src = "gl_MultiTexCoord3"; break;
            case "mc_midTexCoord": src = "gl_MultiTexCoord4"; break;
            case "at_tangent": src = "gl_MultiTexCoord5"; break;
            default: return type.startsWith("i") ? type + "(0)" : type + "(0.0)";
        }
        switch (type) {
            case "float": return "(" + src + ".x)";
            case "vec2": return "(" + src + ".xy)";
            case "vec3": return "(" + src + ".xyz)";
            case "vec4": return src;
            case "ivec2": return "ivec2(" + src + ".xy)";
            case "ivec3": return "ivec3(" + src + ".xyz)";
            default: return "ivec4(" + src + ")";
        }
    }

    /** Number of lines {@link #patch} puts before the source's line 1. */
    public static int headerLines(final String patched) {
        final int end = patched.indexOf(HEADER_END);
        if (end < 0) {
            return 0;
        }
        int n = 1;
        for (int i = 0; i < end; i++) {
            if (patched.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }
}
