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
 *   follow, then {@code #line 1} so driver errors count lines of the expanded source.</li>
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
    private static final Pattern TEXMAT1 = Pattern.compile("\\bgl_TextureMatrix\\s*\\[\\s*1\\s*\\]");

    private GlslPatcher() {
    }

    /**
     * @param compat GL translator on GLES (gl4es, Krypton): no profile suffix and no {@code #line}
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
        if (stage != FRAGMENT) {
            if (body.indexOf("gl_MultiTexCoord1") >= 0) {
                body = MULTITEX1.matcher(body).replaceAll("gl_MultiTexCoord2");
            }
        }
        if (body.indexOf("gl_TextureMatrix") >= 0) {
            body = TEXMAT1.matcher(body).replaceAll("gl_TextureMatrix[2]");
        }
        final StringBuilder out = new StringBuilder(body.length() + 2048);
        out.append("#version ").append(version);
        if (version >= 150 && !compat && version != 300) {
            out.append(profile.isEmpty() || "core".equals(profile) ? " compatibility" : " " + profile);
        } else if (!profile.isEmpty() && version >= 150) {
            out.append(' ').append(profile);
        }
        out.append('\n');
        for (final Map.Entry<String, String> e : macros.entrySet()) {
            out.append("#define ").append(e.getKey());
            if (!e.getValue().isEmpty()) {
                out.append(' ').append(e.getValue());
            }
            out.append('\n');
        }
        if (!compat) {
            out.append("#line 1\n");
        }
        out.append(body);
        return out.toString();
    }

    /** Number of lines {@link #patch} puts before the source's line 1 when {@code #line} is not used. */
    public static int headerLines(final Map<String, String> macros) {
        return 1 + macros.size();
    }
}
