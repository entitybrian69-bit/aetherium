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

    /**
     * The 3D world can be drawn into a smaller framebuffer and scaled up (1.9.0 on 1.16.5; every
     * version since 1.14.0): the main render target is swapped around {@code GameRenderer.renderLevel}.
     * Upscaled by a linear glBlitFramebuffer up to 26.1 and by a vanilla render pass on 26.2+.
     */
    // @era:render-scale-begin target|r17|r212|r215|g262|g263
    public static final boolean RENDER_SCALE = true;
    // @era:render-scale-else none
    //~ public static final boolean RENDER_SCALE = false;
    // @era:render-scale-end

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

    /**
     * The OptiFine-format shader pack engine (com.aetherium.shader, Shader*Mixin): it drives the
     * fixed-function GL 2.1 renderer, which exists until 1.16.5.
     */
    // @era:shader-begin none
    public static final boolean SHADERS = false;
    // @era:shader-else v16
    //~ public static final boolean SHADERS = true;
    // @era:shader-end

    /** Aetherium's own terrain renderer exists for this version. */
    // @era:chunk-renderer-begin none
    public static final boolean AETHERIUM_RENDERER = false;
    // @era:chunk-renderer-else gl16
    //~ public static final boolean AETHERIUM_RENDERER = true;
    // @era:chunk-renderer-end

    /**
     * The Aetherium renderer on 1.17..1.21.4: vanilla's section draws, with solid and cutout meshes
     * sorted by face direction and drawn as the one index range that can face the camera.
     */
    // @era:section-cull-begin pop17|rendered19|mesh21
    public static final boolean SECTION_CULL = true;
    // @era:section-cull-else v16|none
    //~ public static final boolean SECTION_CULL = false;
    // @era:section-cull-end

    /**
     * The Aetherium renderer on 1.21.5+: meshes sorted by face direction while they are built, and
     * each section's draw record narrowed to the faces that can point at the camera.
     */
    // @era:cull-new-begin old
    public static final boolean SECTION_CULL_MODERN = false;
    // @era:cull-new-else srd5|csm6|uber26|uber262|pearl
    //~ public static final boolean SECTION_CULL_MODERN = true;
    // @era:cull-new-end

    /** 1.21.5 flavour of {@link #SECTION_CULL_MODERN}: layouts handed to each layer's GPU buffers. */
    // @era:cull-new-begin old|csm6|uber26|uber262|pearl
    public static final boolean SECTION_CULL_SRD = false;
    // @era:cull-new-else srd5
    //~ public static final boolean SECTION_CULL_SRD = true;
    // @era:cull-new-end

    /** 1.21.6+ flavour of {@link #SECTION_CULL_MODERN}: layouts kept by the compiled section mesh. */
    // @era:cull-new-begin old|srd5
    public static final boolean SECTION_CULL_MESH = false;
    // @era:cull-new-else csm6|uber26|uber262|pearl
    //~ public static final boolean SECTION_CULL_MESH = true;
    // @era:cull-new-end

    /** Some version of the Aetherium renderer exists for this version. */
    public static final boolean ANY_RENDERER = AETHERIUM_RENDERER || SECTION_CULL || SECTION_CULL_MODERN;

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

    /**
     * The 1.10.0 tuning hooks ({@code Tuning*Mixin}: dynamic FPS, entity detail distance, crowd
     * culling, leaves, weather, explosions, light merging, smooth autosave and more). On every
     * version since 1.13.0; which individual features exist is in {@code perf.TuningSupport}.
     */
    public static final boolean TUNING = true;

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

    /** 26.2+: the dirty flags moved to SectionUpdateTracker (CoalesceDirtyStateMixin / CoalesceExtractMixin). */
    // @era:level-extract-begin none
    public static final boolean COALESCE_EXTRACT = false;
    // @era:level-extract-else x262
    //~ public static final boolean COALESCE_EXTRACT = true;
    // @era:level-extract-end

    /** Rebuild coalescing (per-section dirty timestamps), every version. */
    // @era:section-api-begin v16|crd|srd|srd-ro|mesh
    public static final boolean COALESCE = true;
    // @era:section-api-else none
    //~ public static final boolean COALESCE = COALESCE_EXTRACT;
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

    /** 26.2+: the frustum pass moved to LevelExtractor, so the fog cull filters there (FogCullExtractMixin). */
    // @era:fog-cull-x-begin none
    public static final boolean FOG_CULL_EXTRACT = false;
    // @era:fog-cull-x-else extract
    //~ public static final boolean FOG_CULL_EXTRACT = true;
    // @era:fog-cull-x-end

    /** Visible sections wholly inside the fog are skipped (1.17+; 1.16.5 culls in STREAMING). */
    // @era:fog-cull-begin bfs|info|section
    public static final boolean FOG_CULL = true;
    // @era:fog-cull-else none
    //~ public static final boolean FOG_CULL = FOG_CULL_EXTRACT;
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
    //~ public static final boolean FOG_CULL_SECTION = FOG_CULL_EXTRACT;
    // @era:fog-cull-end

    /** Visible-section search limited while terrain streams in (1.17; 1.16.5 in STREAMING). */
    // @era:search-throttle-begin v16|none
    public static final boolean SEARCH_THROTTLE = false;
    // @era:search-throttle-else bfs
    //~ public static final boolean SEARCH_THROTTLE = true;
    // @era:search-throttle-end

    /** Visible-only animated textures (1.16.5..1.21.4; 1.21.5 reworks tickers, quads and sections). */
    // @era:anim-begin on
    public static final boolean VISIBLE_ANIMATIONS = true;
    /** The visible-section pass in VisibleSpritesMixin (1.16.5 marks them in StreamingLevelMixin). */
    public static final boolean VISIBLE_ANIMATION_PASS = true;
    // @era:anim-else v16
    //~ public static final boolean VISIBLE_ANIMATIONS = true;
    //~ public static final boolean VISIBLE_ANIMATION_PASS = false;
    // @era:anim-else none
    //~ public static final boolean VISIBLE_ANIMATIONS = false;
    //~ public static final boolean VISIBLE_ANIMATION_PASS = false;
    // @era:anim-end

    /** Fast mob/block-entity models (1.16.5..1.20.6, while BufferBuilder writes a plain ByteBuffer). */
    // @era:model-fast-begin none
    public static final boolean FAST_MODELS = false;
    public static final boolean FAST_MODELS_MOJANG = false;
    public static final boolean FAST_MODELS_TRUSTED = false;
    // @era:model-fast-else joml
    //~ public static final boolean FAST_MODELS = true;
    //~ /** com.mojang.math.Matrix3f normal matrix (..1.19.2): Matrix3fAccessor. */
    //~ public static final boolean FAST_MODELS_MOJANG = false;
    //~ /** 1.20.5..1.20.6: the pose's trustedNormals flag (PoseNormalsAccessor). */
    //~ public static final boolean FAST_MODELS_TRUSTED = false;
    // @era:model-fast-else v16|mojang
    //~ public static final boolean FAST_MODELS = true;
    //~ public static final boolean FAST_MODELS_MOJANG = true;
    //~ public static final boolean FAST_MODELS_TRUSTED = false;
    // @era:model-fast-else trusted
    //~ public static final boolean FAST_MODELS = true;
    //~ public static final boolean FAST_MODELS_MOJANG = false;
    //~ public static final boolean FAST_MODELS_TRUSTED = true;
    // @era:model-fast-end

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
