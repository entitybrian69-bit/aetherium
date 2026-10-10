package com.aetherium.android;

import java.util.Locale;
import java.util.Objects;

/**
 * The GPU path an Android launcher actually put the JVM on.
 *
 * <p>Everything in {@link #parse} is keyed off the launcher's own env contract,
 * read from FCL's {@code FCLauncher.java} and Pojav's {@code JREUtils} on
 * 2026-10-06 — these are the strings real launchers write, not guesses:</p>
 * <pre>
 *   POJAV_RENDERER=opengles2                        -> gl4es  (GL 2.1 compat)
 *   POJAV_RENDERER=opengles2_vgpu                   -> gl4es + vGPU
 *   POJAV_RENDERER=opengles3                        -> NGGL4ES (native GLES3)
 *   POJAV_RENDERER=gallium_virgl                    -> VirGL (Mesa, guest/server split)
 *   POJAV_RENDERER=gallium_freedreno                -> Mesa native (freedreno)
 *   POJAV_RENDERER=opengles3_desktopgl_zink_kopper  -> Zink (GL-on-Vulkan)
 *   MESA_GL_VERSION_OVERRIDE=4.6 / 4.3               -> reported GL level
 *   MESA_GLSL_VERSION_OVERRIDE=460 / 430             -> reported GLSL level
 * </pre>
 *
 * <p>The routing decision each renderer forces is in {@link #getPreferredBackend()}:
 * Zink and MobileGlues sit on a native Vulkan driver, so going to
 * {@code VULKAN_13} removes a whole translation layer from the frame; LTW exposes
 * GL 3.3 core; gl4es is ES-2.0 semantics only, where persistent mapping and
 * compute do not exist, so Aetherium keeps vanilla geometry and runs utilities.</p>
 */
public enum AndroidRenderer {
    /** Not on Android, or a native desktop driver. */
    NONE("none", "Native desktop driver", BackendHint.GL46),
    /** gl4es: GLES 2.0/3.0 translated to desktop GL by a shim. */
    GL4ES("gl4es", "gl4es (GLES 2.0 shim)", BackendHint.GL_LEGACY),
    /** NGGL4ES: the maintained gl4es fork exposing GLES 3.x semantics. */
    NGGL4ES("nggl4es", "NGGL4ES (GLES 3.x)", BackendHint.GL_LEGACY),
    /** Zink: Mesa's GL-on-Vulkan implementation. */
    ZINK("zink", "Zink (GL over Vulkan)", BackendHint.NATIVE_VULKAN),
    /** Little Whiteen / LTW: desktop GL 3.3 core translator. */
    LTW("ltw", "LTW (GL 3.3 core translator)", BackendHint.GL_CORE),
    /** MobileGlues: Android-side GL implementation over native Vulkan. */
    MOBILEGLUES("mobileglues", "MobileGlues (Vulkan-backed GL)", BackendHint.NATIVE_VULKAN),
    /** VirGL: guest GL commands over a host GPU channel; high latency. */
    VIRGL("virgl", "VirGL (host GPU over socket)", BackendHint.GL_CORE),
    /** ANGLE (Vulkan or GLES backend behind GLFW). */
    ANGLE("angle", "ANGLE", BackendHint.GL_CORE),
    /** Native Vulkan swapchain provided by the launcher itself. */
    NATIVE_VULKAN("vulkan", "Native Vulkan", BackendHint.NATIVE_VULKAN),
    /** Detected Android, could not tell which renderer. */
    UNKNOWN("unknown", "Unrecognised Android renderer", BackendHint.CONSERVATIVE);

    /** What the presence of this renderer implies for backend selection. */
    public enum BackendHint {
        GL46,
        GL_CORE,
        GL_LEGACY,
        NATIVE_VULKAN,
        CONSERVATIVE
    }

    private final String id;
    private final String displayName;
    private final BackendHint hint;

    AndroidRenderer(final String id, final String displayName, final BackendHint hint) {
        this.id = id;
        this.displayName = displayName;
        this.hint = hint;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public BackendHint getHint() {
        return this.hint;
    }

    /**
     * Persistent mapped buffers are the biggest single win of the GL46 path and
     * the most common cause of corruption on translators, so they are off unless
     * the renderer is known to implement buffer storage honestly.
     */
    public boolean allowPersistentMapping() {
        switch (this) {
            case NONE:
            case NATIVE_VULKAN:
            case ANGLE:
                return true;
            case LTW:
            case VIRGL:
            case ZINK:
            case MOBILEGLUES:
            case NGGL4ES:
            default:
                return false;
            case GL4ES:
            case UNKNOWN:
                return false;
        }
    }

    /** Indirect draws are a per-call win; over a socket transport they are a loss. */
    public boolean allowIndirectDraw() {
        switch (this) {
            case NONE:
            case NATIVE_VULKAN:
            case ANGLE:
            case ZINK:
            case MOBILEGLUES:
            case LTW:
                return true;
            default:
                return false;
        }
    }

    /** Compute-shader work (HZB pyramid, GPU compaction) — needs real 4.3 semantics. */
    public boolean allowCompute() {
        switch (this) {
            case NONE:
            case NATIVE_VULKAN:
            case ZINK:
            case MOBILEGLUES:
            case LTW:
                return true;
            default:
                return false;
        }
    }

    public boolean isVulkanBacked() {
        return this == ZINK || this == MOBILEGLUES || this == NATIVE_VULKAN;
    }

    /**
     * Maps {@code POJAV_RENDERER} / {@code *_RENDERER} values to a renderer.
     *
     * @param rendererValue raw env value, e.g. {@code opengles3_desktopgl_zink_kopper}
     * @param mesaGlVersion {@code MESA_GL_VERSION_OVERRIDE}, may be null
     */
    public static AndroidRenderer parse(final String rendererValue, final String mesaGlVersion) {
        if (rendererValue == null || rendererValue.trim().isEmpty()) {
            return mesaGlVersion != null ? ZINK : UNKNOWN;
        }
        final String value = rendererValue.trim().toLowerCase(Locale.ROOT);
        if (value.contains("zink")) {
            return ZINK;
        }
        if (value.contains("virgl") || value.contains("gallium_virgl")) {
            return VIRGL;
        }
        if (value.contains("angle")) {
            return ANGLE;
        }
        if (value.contains("mobileglues") || value.contains("movile_glues") || value.contains("movileglues")) {
            return MOBILEGLUES;
        }
        if (value.contains("ltw") || value.contains("whiten")) {
            return LTW;
        }
        if (value.contains("vulkan") || value.equals("native_vulkan")) {
            return NATIVE_VULKAN;
        }
        // Pojav's naming: "opengles2" is gl4es, "opengles3" is the NGGL4ES path.
        if (value.startsWith("opengles3")) {
            return NGGL4ES;
        }
        if (value.startsWith("opengles2")) {
            return GL4ES;
        }
        if (value.contains("freedreno") || value.contains("asahi") || value.contains("turnip") || value.contains("v3d")) {
            return NATIVE_VULKAN;
        }
        return UNKNOWN;
    }

    /** Declared GL level implied by {@code MESA_GL_VERSION_OVERRIDE}, or -1. */
    public static int declaredGlLevel(final String mesaGlVersion) {
        if (mesaGlVersion == null || mesaGlVersion.trim().isEmpty()) {
            return -1;
        }
        final String[] parts = mesaGlVersion.trim().split("\\.");
        try {
            final int major = Integer.parseInt(parts[0]);
            final int minor = parts.length > 1 ? Integer.parseInt(parts[1].replaceAll("[^0-9].*$", "0")) : 0;
            return major * 100 + minor;
        } catch (final NumberFormatException error) {
            return -1;
        }
    }

    public static AndroidRenderer fromId(final String text) {
        Objects.requireNonNull(text, "text");
        final String normalized = text.trim().toLowerCase(Locale.ROOT);
        for (final AndroidRenderer renderer : values()) {
            if (renderer.id.equals(normalized) || renderer.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return renderer;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
