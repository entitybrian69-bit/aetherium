package com.aetherium.render.backend;

import java.util.Locale;
import java.util.Objects;

/**
 * The renderer backends Aetherium can drive, ordered by preference.
 *
 * <p>{@link #COMPATIBILITY} is not a backend in the GPU sense: it is "let
 * vanilla render, keep Aetherium's utilities". It exists so the switch between
 * Aetherium and vanilla is a value like any other — one field, live, no
 * restart — instead of a code path sprinkled through every mixin.</p>
 */
public enum RenderBackend {
    /**
     * OpenGL 4.6 with direct state access, buffer storage, persistent mapping,
     * compute and indirect draws. Preferred: it is the only configuration where
     * the whole upload path can run without binding state.
     */
    GL46_DSA("gl46_dsa", "OpenGL 4.6 (DSA)", ApiType.OPENGL, 4, 6, true, true),

    /** Vulkan 1.3 device. Secondary because driver variance dominates mobile. */
    VULKAN_13("vulkan_13", "Vulkan 1.3", ApiType.VULKAN, 1, 3, false, false),

    /**
     * OpenGL 3.3 core: no DSA, no persistent mapping (GL 4.4+), so uploads go
     * through buffer-sub-data with explicit binds. This is the LTW target on
     * Android, where 4.x core is not offered.
     */
    GL_CORE("gl_core", "OpenGL 3.3 Core", ApiType.OPENGL, 3, 3, false, false),

    /**
     * GL ES 2.1-compatible path for gl4es. Only the utilities run; the custom
     * render passes are disabled because the driver translstates rather than
     * implements 3.3 semantics.
     */
    GL_LEGACY("gl_legacy", "GL ES 2.0 compat (gl4es)", ApiType.OPENGL, 2, 1, false, false),

    /** Vanilla path, Aetherium hooks measure only. */
    COMPATIBILITY("compatibility", "Compatibility (vanilla)", ApiType.NONE, 0, 0, false, false);

    /** API families, used by capability probing and by the Android router. */
    public enum ApiType {
        OPENGL,
        VULKAN,
        NONE
    }

    private final String id;
    private final String displayName;
    private final ApiType apiType;
    private final int majorVersion;
    private final int minorVersion;
    private final boolean supportsPersistentBuffers;
    private final boolean supportsIndirectDraw;

    RenderBackend(final String id, final String displayName, final ApiType apiType, final int majorVersion,
                  final int minorVersion, final boolean supportsPersistentBuffers, final boolean supportsIndirectDraw) {
        this.id = id;
        this.displayName = displayName;
        this.apiType = apiType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.supportsPersistentBuffers = supportsPersistentBuffers;
        this.supportsIndirectDraw = supportsIndirectDraw;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public ApiType getApiType() {
        return this.apiType;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }

    public int getMinorVersion() {
        return this.minorVersion;
    }

    public boolean supportsPersistentBuffers() {
        return this.supportsPersistentBuffers;
    }

    public boolean supportsIndirectDraw() {
        return this.supportsIndirectDraw;
    }

    public boolean isOpenGL() {
        return this.apiType == ApiType.OPENGL;
    }

    public boolean isVulkan() {
        return this.apiType == ApiType.VULKAN;
    }

    /** Whether this backend may issue geometry draws of its own. */
    public boolean isCustomPassesAllowed() {
        return this != COMPATIBILITY && this != GL_LEGACY;
    }

    /** Minimum GL version this backend needs, as a comparable {@code major*100+minor}. */
    public int requiredGlLevel() {
        return this.majorVersion * 100 + this.minorVersion;
    }

    public static RenderBackend fromId(final String text) {
        Objects.requireNonNull(text, "text");
        final String normalized = text.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (final RenderBackend backend : values()) {
            if (backend.id.equals(normalized) || backend.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return backend;
            }
        }
        return COMPATIBILITY;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
