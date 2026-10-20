package com.aetherium.shader;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

/**
 * What the GL context can do for shader packs, read once on the render thread. Detects the GL
 * translators Android launchers use (gl4es and its forks such as Krypton Wrapper / NG-GL4ES,
 * MobileGlues, ANGLE) for "Android compatibility mode": draw buffers capped to what the
 * translator reports, no geometry shaders unless available, blit-free depth copies, and
 * GLES-safe texture formats.
 */
public final class GlCaps {
    public final String vendor;
    public final String renderer;
    public final String version;
    public final int glVersion;
    public final int glslVersion;
    public final boolean translator;
    public final String translatorName;
    public final int maxDrawBuffers;
    public final int maxColorAttachments;
    public final int maxTextureUnits;
    public final boolean floatTextures;
    public final boolean geometryShaders;
    public final boolean blit;
    public final boolean fragDataLocation;
    public final Set<String> extensions;

    private GlCaps(final String vendor, final String renderer, final String version, final int glVersion,
                   final int glslVersion, final String translatorName, final int maxDrawBuffers,
                   final int maxColorAttachments, final int maxTextureUnits, final boolean floatTextures,
                   final boolean geometryShaders, final boolean blit, final boolean fragDataLocation,
                   final Set<String> extensions) {
        this.vendor = vendor;
        this.renderer = renderer;
        this.version = version;
        this.glVersion = glVersion;
        this.glslVersion = glslVersion;
        this.translatorName = translatorName;
        this.translator = translatorName != null;
        this.maxDrawBuffers = maxDrawBuffers;
        this.maxColorAttachments = maxColorAttachments;
        this.maxTextureUnits = maxTextureUnits;
        this.floatTextures = floatTextures;
        this.geometryShaders = geometryShaders;
        this.blit = blit;
        this.fragDataLocation = fragDataLocation;
        this.extensions = extensions;
    }

    /** Reads the current context. {@code forceCompat}: treat as a translator (testing on desktop). */
    public static GlCaps read(final boolean forceCompat) {
        return read(forceCompat, false);
    }

    /** {@code disableCompat}: treat a detected translator like a desktop driver (compat mode "off"). */
    public static GlCaps read(final boolean forceCompat, final boolean disableCompat) {
        final String vendor = str(GL11.glGetString(GL11.GL_VENDOR));
        final String renderer = str(GL11.glGetString(GL11.GL_RENDERER));
        final String version = str(GL11.glGetString(GL11.GL_VERSION));
        String glsl = "";
        try {
            glsl = str(GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));
        } catch (final RuntimeException ignored) {
            // GL 1.x
        }
        final GLCapabilities caps = GL.getCapabilities();
        final String all = (vendor + " " + renderer + " " + version).toLowerCase(Locale.ROOT);
        String translator = null;
        if (all.contains("krypton") || all.contains("ng-gl4es") || all.contains("ng_gl4es")) {
            translator = "Krypton Wrapper (NG-GL4ES)";
        } else if (all.contains("gl4es")) {
            translator = "GL4ES";
        } else if (all.contains("mobileglues") || all.contains("mobile glues")) {
            translator = "MobileGlues";
        } else if (all.contains("angle")) {
            translator = "ANGLE";
        }
        if (disableCompat) {
            translator = null;
        }
        if (translator == null && forceCompat) {
            translator = "forced";
        }
        final int glVersion = parseVersion(version);
        final int glslVersion = parseVersion(glsl);
        final boolean gl30 = caps.OpenGL30;
        int drawBuffers = 1;
        int attachments = 1;
        try {
            drawBuffers = Math.max(1, GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS));
            attachments = Math.max(1, GL11.glGetInteger(GL30.GL_MAX_COLOR_ATTACHMENTS));
        } catch (final RuntimeException ignored) {
            // not reported: keep one
        }
        int units = 8;
        try {
            units = Math.max(2, GL11.glGetInteger(GL20.GL_MAX_TEXTURE_IMAGE_UNITS));
        } catch (final RuntimeException ignored) {
            // keep 8
        }
        units = usableUnits(units, translator != null);
        drawBuffers = Math.min(Math.min(drawBuffers, attachments), 8);
        final boolean floats = gl30 || caps.GL_ARB_texture_float;
        final boolean geometry = (caps.OpenGL32 || caps.GL_ARB_geometry_shader4) && translator == null;
        // translators: only when they report GL 3.0 (GLES3 backends); ES2 ones get the shader copy
        final boolean blit = gl30 || (translator == null && caps.GL_EXT_framebuffer_blit);
        final Set<String> ext = new HashSet<>();
        try {
            final int n = GL11.glGetInteger(GL30.GL_NUM_EXTENSIONS);
            for (int i = 0; i < n && gl30; i++) {
                ext.add(GL30.glGetStringi(GL11.GL_EXTENSIONS, i));
            }
        } catch (final RuntimeException ignored) {
            // legacy context
        }
        if (ext.isEmpty()) {
            try {
                for (final String e : str(GL11.glGetString(GL11.GL_EXTENSIONS)).split(" ")) {
                    if (!e.isEmpty()) {
                        ext.add(e);
                    }
                }
            } catch (final RuntimeException ignored) {
                // none
            }
        }
        return new GlCaps(vendor, renderer, version, glVersion, glslVersion, translator, drawBuffers, attachments,
                units, floats, geometry, blit, gl30, ext);
    }

    private static String str(final String s) {
        return s == null ? "" : s;
    }

    /** "4.6.0 NVIDIA 535" to 460, "2.1 gl4es wrapper" to 210, "OpenGL ES 3.2" to 320. */
    /**
     * Texture units glActiveTexture really accepts. GL translators (gl4es, Krypton) report the GLES
     * image unit count but reject units above their own fixed-function limit (often 8): a bind there
     * lands on whatever unit is active, i.e. on Minecraft's atlas, font or lightmap. Probed by
     * activating the top unit and reading it back.
     */
    private static int usableUnits(final int reported, final boolean translator) {
        int units = Math.min(reported, 32);
        try {
            if (translator) {
                final int fixed = GL11.glGetInteger(0x84E2); // GL_MAX_TEXTURE_UNITS (fixed function)
                if (fixed >= 4) {
                    units = Math.min(units, fixed);
                }
            }
            final int previous = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            while (units > 4) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + units - 1);
                if (GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) == GL13.GL_TEXTURE0 + units - 1) {
                    break;
                }
                units--;
            }
            GL13.glActiveTexture(previous);
            while (GL11.glGetError() != 0) { // GL_NO_ERROR
                // drop the probe's GL_INVALID_ENUMs so nobody else reports them
            }
        } catch (final RuntimeException ignored) {
            // keep what was reported
        }
        return units;
    }

    static int parseVersion(final String s) {
        final java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)").matcher(s);
        if (!m.find()) {
            return 0;
        }
        final int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        if (minor >= 10) {
            minor = minor / 10; // "1.20" GLSL
            return major * 100 + minor * 10;
        }
        return major * 100 + minor * 10;
    }

    /** OptiFine's standard macros for this context (MC_VERSION and the hand depth are added by the caller). */
    public Map<String, String> standardMacros() {
        final Map<String, String> m = new TreeMap<>();
        m.put("MC_GL_VERSION", Integer.toString(Math.max(this.glVersion, 210)));
        m.put("MC_GLSL_VERSION", Integer.toString(Math.max(this.glslVersion, 120)));
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        m.put(os.contains("win") ? "MC_OS_WINDOWS" : os.contains("mac") ? "MC_OS_MAC"
                : os.contains("linux") && !this.translator ? "MC_OS_LINUX" : "MC_OS_OTHER", "");
        final String v = this.vendor.toLowerCase(Locale.ROOT);
        final String r = this.renderer.toLowerCase(Locale.ROOT);
        m.put(v.contains("nvidia") ? "MC_GL_VENDOR_NVIDIA" : v.contains("ati") || v.contains("amd") ? "MC_GL_VENDOR_ATI"
                : v.contains("intel") ? "MC_GL_VENDOR_INTEL" : v.contains("mesa") ? "MC_GL_VENDOR_MESA"
                : v.contains("x.org") ? "MC_GL_VENDOR_XORG" : "MC_GL_VENDOR_OTHER", "");
        m.put(r.contains("geforce") ? "MC_GL_RENDERER_GEFORCE" : r.contains("quadro") ? "MC_GL_RENDERER_QUADRO"
                : r.contains("radeon") ? "MC_GL_RENDERER_RADEON" : r.contains("intel") ? "MC_GL_RENDERER_INTEL"
                : r.contains("gallium") || r.contains("llvmpipe") ? "MC_GL_RENDERER_GALLIUM"
                : r.contains("mesa") ? "MC_GL_RENDERER_MESA" : "MC_GL_RENDERER_OTHER", "");
        for (final String e : this.extensions) {
            if (e.startsWith("GL_ARB_") || e.startsWith("GL_EXT_")) {
                m.put("MC_" + e, "");
            }
        }
        final String[] stages = {"NONE", "SKY", "SUNSET", "CUSTOM_SKY", "SUN", "MOON", "STARS", "VOID",
                "TERRAIN_SOLID", "TERRAIN_CUTOUT_MIPPED", "TERRAIN_CUTOUT", "ENTITIES", "BLOCK_ENTITIES", "DESTROY",
                "OUTLINE", "DEBUG", "HAND_SOLID", "TERRAIN_TRANSLUCENT", "TRIPWIRE", "PARTICLES", "CLOUDS",
                "RAIN_SNOW", "WORLD_BORDER", "HAND_TRANSLUCENT"};
        for (int i = 0; i < stages.length; i++) {
            m.put("MC_RENDER_STAGE_" + stages[i], Integer.toString(i));
        }
        if (this.translator) {
            m.put("AETHERIUM_COMPAT", "");
        }
        return m;
    }

    public String describe() {
        return this.renderer + " / " + this.version + (this.translator ? " [" + this.translatorName + "]" : "")
                + ", draw buffers " + this.maxDrawBuffers + ", texture units " + this.maxTextureUnits
                + ", GLSL " + this.glslVersion;
    }
}
