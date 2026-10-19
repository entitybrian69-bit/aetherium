package com.aetherium;

/**
 * Which Aetherium features exist on this build: Minecraft 1.12.2 (Aetherium Backport).
 *
 * <p>The main mod generates this class per Minecraft version; the backport has exactly one. A
 * feature that is false here has no hook on 1.12.2 and its row is not shown on the settings
 * screen.</p>
 */
public final class Capabilities {
    public static final boolean DYNAMIC_LIGHTS = true;
    public static final boolean VIGNETTE_TOGGLE = true;
    public static final boolean HUD_OVERLAY = true;
    public static final boolean GAMMA_INSTANCE = false;
    public static final boolean SIMULATION_DISTANCE = false;
    public static final boolean SMOOTH_CHUNK_UPLOADS = true;
    public static final boolean TEXTURE_ANIMATION_TOGGLE = true;
    public static final boolean BLOCK_ENTITY_CULL = true;
    public static final boolean THREADS_BY_PROPERTY = false;
    /** 1.12.2 already draws only the entities of visible chunk sections. */
    public static final boolean ENTITY_OCCLUSION = false;
    public static final boolean CHUNK_INFO_LIST = false;
    /** The 1.12.2 terrain renderer: face-sorted chunk meshes with camera-facing draw ranges. */
    public static final boolean AETHERIUM_RENDERER = true;
    public static final boolean SECTION_CULL = false;
    public static final boolean SECTION_CULL_MODERN = false;
    public static final boolean SECTION_CULL_SRD = false;
    public static final boolean SECTION_CULL_MESH = false;
    public static final boolean ANY_RENDERER = true;
    public static final boolean ZOOM = true;
    public static final boolean THREADED_NEAR_BUILDS = false;
    public static final boolean STREAMING = false;
    public static final boolean PACKET_BUDGET = false;
    public static final boolean STUTTER_LOG = true;
    public static final boolean MESH_THREADS = false;
    public static final boolean MESH_ALLOC = false;
    public static final boolean COALESCE = false;
    public static final boolean FAST_PARTICLES = false;
    public static final boolean FRAME_FRUSTUM = false;
    /** Chunks entirely inside the distance fog are not drawn. */
    public static final boolean FOG_CULL = true;
    public static final boolean FOG_CULL_INFO = false;
    public static final boolean FOG_CULL_SECTION = false;
    public static final boolean SEARCH_THROTTLE = false;
    public static final boolean VISIBLE_ANIMATIONS = false;
    public static final boolean VISIBLE_ANIMATION_PASS = false;
    public static final boolean FAST_MODELS = false;
    public static final boolean FAST_MODELS_MOJANG = false;
    public static final boolean FAST_MODELS_TRUSTED = false;
    public static final boolean SCENERY_CONTROLS = true;
    public static final boolean FREELOOK = true;
    public static final boolean FRAME_PACING = true;
    public static final boolean WEATHER = true;

    private Capabilities() {
    }

    // Flags read by shared main-mod code (Phase 33 sync).
    public static final boolean COALESCE_EXTRACT = false;
    public static final boolean FOG_CULL_EXTRACT = false;
    /** Render scale (1.14.0): the 1.12.2 framebuffer path, see client/RenderScaleHooks. */
    public static final boolean RENDER_SCALE = false;
    /** The tuning options (perf.Tuning); which ones exist on 1.12.2 is in perf.TuningSupport. */
    public static final boolean TUNING = true;
}
