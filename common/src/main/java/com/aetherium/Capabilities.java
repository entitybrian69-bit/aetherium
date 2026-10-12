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

    /**
     * Entities hidden behind terrain can be skipped using vanilla's visible-section list (every
     * version; vanilla itself only frustum-tests entities, and 1.21.11+ adds a fade-in check).
     */
    public static final boolean ENTITY_OCCLUSION = true;

    /** The visible list holds {@code LevelRenderer$RenderChunkInfo} (1.16.5-1.20.1) rather than sections. */
    // @era:section-vis-begin sections|render-origin
    public static final boolean CHUNK_INFO_LIST = false;
    // @era:section-vis-else list16|list17|list18
    //~ public static final boolean CHUNK_INFO_LIST = true;
    // @era:section-vis-end

    /** Aetherium's own terrain renderer exists for this version. */
    // @era:chunk-renderer-begin none
    public static final boolean AETHERIUM_RENDERER = false;
    // @era:chunk-renderer-else gl16
    //~ public static final boolean AETHERIUM_RENDERER = true;
    // @era:chunk-renderer-end

    /** Zume-style zoom with rebindable keys (every version). */
    // @era:zoom-begin on
    public static final boolean ZOOM = true;
    // @era:zoom-else none
    //~ public static final boolean ZOOM = false;
    // @era:zoom-end

    /**
     * Dirty sections near the camera are rebuilt on worker threads instead of the render thread,
     * and client light updates get a per-frame time budget (1.16.5's "build near" path).
     */
    // @era:near-build-begin none
    public static final boolean THREADED_NEAR_BUILDS = false;
    // @era:near-build-else v16
    //~ public static final boolean THREADED_NEAR_BUILDS = true;
    // @era:near-build-end

    /**
     * Terrain streaming: a per-frame budget for queued packet tasks, coalesced rebuilds of
     * sections dirtied repeatedly while chunks load, and dedicated chunk-mesh threads (1.16.5 first).
     */
    // @era:qol16-begin none
    public static final boolean STREAMING = false;
    // @era:qol16-else v16
    //~ public static final boolean STREAMING = true;
    // @era:qol16-end

    /** Per-frame budget for queued packet tasks ({@code Minecraft.runTick}); every version. */
    public static final boolean PACKET_BUDGET = true;

    /** The stutter log (frame-time spikes with their streaming counters); every version. */
    public static final boolean STUTTER_LOG = true;

    /** Chunk meshes built on Aetherium's own threads (the dispatcher's executor); every version. */
    public static final boolean MESH_THREADS = true;

    /** Allocation-free block meshing (reused BitSet / AO position); vanilla stopped allocating in 1.21.5. */
    // @era:mesh-alloc-begin v16
    public static final boolean MESH_ALLOC = true;
    // @era:mesh-alloc-else none
    //~ public static final boolean MESH_ALLOC = false;
    // @era:mesh-alloc-end

    /** Rebuild coalescing (per-section dirty timestamps); 26.2 dropped the section dirty flag. */
    // @era:section-api-begin v16|crd|srd|srd-ro|mesh
    public static final boolean COALESCE = true;
    // @era:section-api-else none
    //~ public static final boolean COALESCE = false;
    // @era:section-api-end

    /** Allocation-free, frustum-culled camera-facing particles (1.16.5..1.21.8). */
    // @era:particle-fast-begin v16|quat|joml|facing|inner
    public static final boolean FAST_PARTICLES = true;
    // @era:particle-fast-else none
    //~ public static final boolean FAST_PARTICLES = false;
    // @era:particle-fast-end

    /** Per-frame camera frustum from vanilla's own matrices (1.16.5 via StreamingLevelMixin). */
    // @era:frustum-cap-begin mojang|joml|joml-ret
    public static final boolean FRAME_FRUSTUM = true;
    // @era:frustum-cap-else none
    //~ public static final boolean FRAME_FRUSTUM = false;
    // @era:frustum-cap-end

    /** Visible sections wholly inside the fog are skipped (1.17..26.1; 1.16.5 culls in STREAMING). */
    // @era:fog-cull-begin bfs|info|section
    public static final boolean FOG_CULL = true;
    // @era:fog-cull-else none
    //~ public static final boolean FOG_CULL = false;
    // @era:fog-cull-end

    /** Which list the fog cull filters: RenderChunkInfo (1.17..1.20.1) or RenderSection (1.20.2..26.1). */
    // @era:fog-cull-begin section
    public static final boolean FOG_CULL_INFO = false;
    public static final boolean FOG_CULL_SECTION = true;
    // @era:fog-cull-else bfs|info
    //~ public static final boolean FOG_CULL_INFO = true;
    //~ public static final boolean FOG_CULL_SECTION = false;
    // @era:fog-cull-else none
    //~ public static final boolean FOG_CULL_INFO = false;
    //~ public static final boolean FOG_CULL_SECTION = false;
    // @era:fog-cull-end

    /** Visible-section search limited while terrain streams in (1.17; 1.16.5 in STREAMING). */
    // @era:search-throttle-begin v16|none
    public static final boolean SEARCH_THROTTLE = false;
    // @era:search-throttle-else bfs
    //~ public static final boolean SEARCH_THROTTLE = true;
    // @era:search-throttle-end

    /** Fog controls, sky toggles and per-type particle toggles (every version since 1.8.0). */
    public static final boolean SCENERY_CONTROLS = true;

    /** Freelook: look around without turning the player (every version). */
    public static final boolean FREELOOK = true;

    /** Even frame pacing replaces vanilla's {@code limitDisplayFPS} wait (every version). */
    public static final boolean FRAME_PACING = true;

    /** Weather can be hidden on every supported version. */
    public static final boolean WEATHER = true;

    private Capabilities() {
    }
}
