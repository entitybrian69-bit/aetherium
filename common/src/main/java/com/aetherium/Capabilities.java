package com.aetherium;

/**
 * Compile-time feature matrix for the Minecraft version this jar was built for.
 *
 * <p>The constants are selected by the era engine ({@code tools/eras.py}) when a
 * port is generated, so every jar carries exactly the truth for its own version.
 * They replace the old runtime version-range gating in the mixin plugin, which
 * never worked: the version system property was only set after the Minecraft
 * classes had already been transformed.</p>
 *
 * <p>Rules: a flag is {@code false} only when the vanilla API the feature hooks
 * no longer exists on that version. The GUI then shows the row as
 * "Not available on this version" instead of offering a dead switch.</p>
 */
public final class Capabilities {

    /** Block/sky light lookup can be hooked, so dynamic lights work. */
    // @era:light-hook-begin color|brightness|coords
    public static final boolean DYNAMIC_LIGHTS = true;
    // @era:light-hook-else none
    //~ public static final boolean DYNAMIC_LIGHTS = false;
    // @era:light-hook-end

    /** The vignette pass can be cancelled. */
    // @era:vignette-begin render|extract
    public static final boolean VIGNETTE_TOGGLE = true;
    // @era:vignette-else none
    //~ public static final boolean VIGNETTE_TOGGLE = false;
    // @era:vignette-end

    /** The in-game HUD can host the frame-time overlay. */
    // @era:hud-begin stack|graphics-float|graphics-delta|extractor
    public static final boolean HUD_OVERLAY = true;
    // @era:hud-else none
    //~ public static final boolean HUD_OVERLAY = false;
    // @era:hud-end

    /** Gamma lives in an {@code OptionInstance}, so fullbright is a read override, not a write. */
    // @era:options-begin instances
    public static final boolean GAMMA_INSTANCE = true;
    // @era:options-else fields
    //~ public static final boolean GAMMA_INSTANCE = false;
    // @era:options-end

    /** Vanilla has a separate simulation distance (1.18+). */
    // @era:simdist-begin sim
    public static final boolean SIMULATION_DISTANCE = true;
    // @era:simdist-else none
    //~ public static final boolean SIMULATION_DISTANCE = false;
    // @era:simdist-end

    /** Finished chunk meshes can be spread over several frames (upload queue is hookable). */
    // @era:chunk-upload-begin crdbool|crd|srd
    public static final boolean SMOOTH_CHUNK_UPLOADS = true;
    // @era:chunk-upload-else none
    //~ public static final boolean SMOOTH_CHUNK_UPLOADS = false;
    // @era:chunk-upload-end

    /** Atlas texture animation can be paused ({@code TextureAtlas.tick} exists on every version). */
    public static final boolean TEXTURE_ANIMATION_TOGGLE = true;

    /** Block entities can be culled by distance before they are drawn (render or extract path). */
    public static final boolean BLOCK_ENTITY_CULL = true;

    /** The worker pool is sized by the {@code max.bg.threads} property (1.18+) rather than the Util mixin. */
    // @era:bg-threads-begin property
    public static final boolean THREADS_BY_PROPERTY = true;
    // @era:bg-threads-else clamp
    //~ public static final boolean THREADS_BY_PROPERTY = false;
    // @era:bg-threads-end

    /** Weather can be hidden on every supported version. */
    public static final boolean WEATHER = true;

    private Capabilities() {
    }
}
