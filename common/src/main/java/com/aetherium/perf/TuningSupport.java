package com.aetherium.perf;

/**
 * Which of the tuning features (1.10.0 to 1.12.0, first written for Minecraft 1.16.5) exist on this
 * build. Each flag follows the era of the file that implements it, so a feature that has not been
 * ported to a version, or whose vanilla code is gone there, reports itself as missing: its mixin is
 * not applied ({@link #mixin}) and its settings row says "not on this version" ({@link #option}).
 */
public final class TuningSupport {
    private TuningSupport() {
    }

    // @era:t-ao-cache-begin a215|v16
    public static final boolean AO_CACHE = true;
    // @era:t-ao-cache-else none
    //~ public static final boolean AO_CACHE = false;
    // @era:t-ao-cache-end

    // @era:t-autosave-begin s18|s214|v16
    public static final boolean AUTOSAVE = true;
    // @era:t-autosave-else none
    //~ public static final boolean AUTOSAVE = false;
    // @era:t-autosave-end

    // @era:t-beam-begin b2111|v16
    public static final boolean BEAM = true;
    // @era:t-beam-else none
    //~ public static final boolean BEAM = false;
    // @era:t-beam-end

    // @era:t-block-entity-begin b17|b219|v16
    public static final boolean BLOCK_ENTITY = true;
    // @era:t-block-entity-else none
    //~ public static final boolean BLOCK_ENTITY = false;
    // @era:t-block-entity-end

    // @era:t-build-order-begin none
    public static final boolean BUILD_ORDER = false;
    // @era:t-build-order-else v16
    //~ public static final boolean BUILD_ORDER = true;
    // @era:t-build-order-end

    // @era:t-chunk-save-begin c21|c214|v16
    public static final boolean CHUNK_SAVE = true;
    // @era:t-chunk-save-else none
    //~ public static final boolean CHUNK_SAVE = false;
    // @era:t-chunk-save-end

    // @era:t-client-begin v16|v19|v262
    public static final boolean CLIENT = true;
    // @era:t-client-else none
    //~ public static final boolean CLIENT = false;
    // @era:t-client-end

    // @era:t-color-filter-begin r17|v16
    public static final boolean COLOR_FILTER = true;
    // @era:t-color-filter-else none
    //~ public static final boolean COLOR_FILTER = false;
    // @era:t-color-filter-end

    // @era:t-cull-begin none
    public static final boolean CULL = false;
    // @era:t-cull-else v16
    //~ public static final boolean CULL = true;
    // @era:t-cull-end

    // @era:t-explosion-begin v16|x212
    public static final boolean EXPLOSION = true;
    // @era:t-explosion-else none
    //~ public static final boolean EXPLOSION = false;
    // @era:t-explosion-end

    // @era:t-far-terrain-begin none
    public static final boolean FAR_TERRAIN = false;
    // @era:t-far-terrain-else v16
    //~ public static final boolean FAR_TERRAIN = true;
    // @era:t-far-terrain-end

    // @era:t-flight-preload-begin f20|f215|f216|v16
    public static final boolean FLIGHT_PRELOAD = true;
    // @era:t-flight-preload-else none
    //~ public static final boolean FLIGHT_PRELOAD = false;
    // @era:t-flight-preload-end

    // @era:t-frame-item-begin f17|f212|v16
    public static final boolean FRAME_ITEM = true;
    // @era:t-frame-item-else none
    //~ public static final boolean FRAME_ITEM = false;
    // @era:t-frame-item-end

    // @era:t-gl-state-begin none
    public static final boolean GL_STATE = false;
    // @era:t-gl-state-else v16
    //~ public static final boolean GL_STATE = true;
    // @era:t-gl-state-end

    // @era:t-glint-begin g21|g212|g219|v16
    public static final boolean GLINT = true;
    // @era:t-glint-else none
    //~ public static final boolean GLINT = false;
    // @era:t-glint-end

    // @era:t-grass-begin g19|g215|v16
    public static final boolean GRASS = true;
    // @era:t-grass-else none
    //~ public static final boolean GRASS = false;
    // @era:t-grass-end

    // @era:t-grass-quads-begin v16
    public static final boolean GRASS_QUADS = true;
    // @era:t-grass-quads-else none
    //~ public static final boolean GRASS_QUADS = false;
    // @era:t-grass-quads-end

    // @era:t-http-textureacc-begin v16
    public static final boolean HTTP_TEXTUREACC = true;
    // @era:t-http-textureacc-else none
    //~ public static final boolean HTTP_TEXTUREACC = false;
    // @era:t-http-textureacc-end

    // @era:t-layers-begin l212|l219|v16
    public static final boolean LAYERS = true;
    // @era:t-layers-else none
    //~ public static final boolean LAYERS = false;
    // @era:t-layers-end

    // @era:t-lazy-data-fixers-begin none
    public static final boolean LAZY_DATA_FIXERS = false;
    // @era:t-lazy-data-fixers-else v16
    //~ public static final boolean LAZY_DATA_FIXERS = true;
    // @era:t-lazy-data-fixers-end

    // @era:t-leaf-cull-begin f17|f212|f215|v16
    public static final boolean LEAF_CULL = true;
    // @era:t-leaf-cull-else none
    //~ public static final boolean LEAF_CULL = false;
    // @era:t-leaf-cull-end

    // @era:t-leaves-begin l2111|v16
    public static final boolean LEAVES = true;
    // @era:t-leaves-else none
    //~ public static final boolean LEAVES = false;
    // @era:t-leaves-end

    // @era:t-level-begin none
    public static final boolean LEVEL = false;
    // @era:t-level-else v16
    //~ public static final boolean LEVEL = true;
    // @era:t-level-end

    // @era:t-levelacc-begin none
    public static final boolean LEVELACC = false;
    // @era:t-levelacc-else v16
    //~ public static final boolean LEVELACC = true;
    // @era:t-levelacc-end

    // @era:t-light-begin v16
    public static final boolean LIGHT = true;
    // @era:t-light-else none
    //~ public static final boolean LIGHT = false;
    // @era:t-light-end

    // @era:t-lightmap-begin v16
    public static final boolean LIGHTMAP = true;
    // @era:t-lightmap-else none
    //~ public static final boolean LIGHTMAP = false;
    // @era:t-lightmap-end

    // @era:t-loading-begin d194|v16
    public static final boolean LOADING = true;
    // @era:t-loading-else none
    //~ public static final boolean LOADING = false;
    // @era:t-loading-end

    // @era:t-map-begin m212|v16
    public static final boolean MAP = true;
    // @era:t-map-else none
    //~ public static final boolean MAP = false;
    // @era:t-map-end

    // @era:t-minecart-begin m212|m219|v16
    public static final boolean MINECART = true;
    // @era:t-minecart-else none
    //~ public static final boolean MINECART = false;
    // @era:t-minecart-end

    // @era:t-name-tag-begin n17|n212|v16
    public static final boolean NAME_TAG = true;
    // @era:t-name-tag-else none
    //~ public static final boolean NAME_TAG = false;
    // @era:t-name-tag-end

    // @era:t-ocean-floor-begin none
    public static final boolean OCEAN_FLOOR = false;
    // @era:t-ocean-floor-else v16
    //~ public static final boolean OCEAN_FLOOR = true;
    // @era:t-ocean-floor-end

    // @era:t-portal-begin p21|p215|v16
    public static final boolean PORTAL = true;
    // @era:t-portal-else none
    //~ public static final boolean PORTAL = false;
    // @era:t-portal-end

    // @era:t-server-idle-begin v16
    public static final boolean SERVER_IDLE = true;
    // @era:t-server-idle-else none
    //~ public static final boolean SERVER_IDLE = false;
    // @era:t-server-idle-end

    // @era:t-server-light-begin none
    public static final boolean SERVER_LIGHT = false;
    // @era:t-server-light-else v16
    //~ public static final boolean SERVER_LIGHT = true;
    // @era:t-server-light-end

    // @era:t-shadow-begin s17|s212|s219|v16
    public static final boolean SHADOW = true;
    // @era:t-shadow-else none
    //~ public static final boolean SHADOW = false;
    // @era:t-shadow-end

    // @era:t-sign-begin s193|s20|s214|s219|v16
    public static final boolean SIGN = true;
    // @era:t-sign-else none
    //~ public static final boolean SIGN = false;
    // @era:t-sign-end

    // @era:t-skin-begin v16
    public static final boolean SKIN = true;
    // @era:t-skin-else none
    //~ public static final boolean SKIN = false;
    // @era:t-skin-end

    // @era:t-skull-begin none
    public static final boolean SKULL = false;
    // @era:t-skull-else v16
    //~ public static final boolean SKULL = true;
    // @era:t-skull-end

    // @era:t-skull-lookup-begin none
    public static final boolean SKULL_LOOKUP = false;
    // @era:t-skull-lookup-else v16
    //~ public static final boolean SKULL_LOOKUP = true;
    // @era:t-skull-lookup-end

    // @era:t-sky-begin e17|e21|v16
    public static final boolean SKY = true;
    // @era:t-sky-else none
    //~ public static final boolean SKY = false;
    // @era:t-sky-end

    // @era:t-view-areaacc-begin none
    public static final boolean VIEW_AREAACC = false;
    // @era:t-view-areaacc-else v16
    //~ public static final boolean VIEW_AREAACC = true;
    // @era:t-view-areaacc-end

    // @era:t-world-begin v16|w182|w206|w21
    public static final boolean WORLD = true;
    // @era:t-world-else none
    //~ public static final boolean WORLD = false;
    // @era:t-world-end

    // @era:t-world-cache-begin r17|v16
    public static final boolean WORLD_CACHE = true;
    // @era:t-world-cache-else none
    //~ public static final boolean WORLD_CACHE = false;
    // @era:t-world-cache-end

    /** Whether the {@code Tuning*} mixin (or tuning accessor) with this simple name applies here. */
    public static boolean mixin(final String simpleName) {
        switch (simpleName) {
            case "LazyDataFixersMixin":
                return LAZY_DATA_FIXERS;
            case "TuningAoCacheMixin":
                return AO_CACHE;
            case "TuningAutosaveMixin":
                return AUTOSAVE;
            case "TuningBeamMixin":
                return BEAM;
            case "TuningBlockEntityMixin":
                return BLOCK_ENTITY;
            case "TuningBuildOrderMixin":
                return BUILD_ORDER;
            case "TuningChunkSaveMixin":
                return CHUNK_SAVE;
            case "TuningCullMixin":
                return CULL;
            case "TuningExplosionMixin":
                return EXPLOSION;
            case "TuningFrameItemMixin":
                return FRAME_ITEM;
            case "TuningGlStateMixin":
                return GL_STATE;
            case "TuningGlintMixin":
                return GLINT;
            case "TuningGrassMixin":
                return GRASS;
            case "TuningHttpTextureAccessor":
                return HTTP_TEXTUREACC;
            case "TuningLayersMixin":
                return LAYERS;
            case "TuningLeafCullMixin":
                return LEAF_CULL;
            case "TuningLeavesMixin":
                return LEAVES;
            case "TuningLevelAccessor":
                return LEVELACC;
            case "TuningLevelMixin":
                return LEVEL;
            case "TuningLightMixin":
                return LIGHT;
            case "TuningLightmapMixin":
                return LIGHTMAP;
            case "TuningLoadingMixin":
                return LOADING;
            case "TuningMapMixin":
                return MAP;
            case "TuningMinecartMixin":
                return MINECART;
            case "TuningNameTagMixin":
                return NAME_TAG;
            case "TuningPortalMixin":
                return PORTAL;
            case "TuningServerIdleMixin":
                return SERVER_IDLE;
            case "TuningServerLightMixin":
                return SERVER_LIGHT;
            case "TuningShadowMixin":
                return SHADOW;
            case "TuningSignMixin":
                return SIGN;
            case "TuningSkinMixin":
                return SKIN;
            case "TuningSkullMixin":
                return SKULL;
            case "TuningSkyMixin":
                return SKY;
            case "TuningViewAreaAccessor":
                return VIEW_AREAACC;
            case "TuningWorldMixin":
                return WORLD;
            default:
                return false;
        }
    }

    private static final String[] OPTION_KEYS = {
            "perf.low_end_mode",
            "perf.hitch_guard",
            "perf.redstone_particle_limit",
            "perf.lazy_far_rebuilds",
            "perf.dynamic_fps",
            "perf.menu_fps",
            "perf.loading_screen_gc",
            "perf.entity_detail_distance",
            "perf.crowd_culling",
            "perf.dimension_culling",
            "perf.translucent_sort_throttle",
            "quality.light_weather",
            "perf.explosion_particles",
            "perf.light_updates",
            "perf.map_updates",
            "perf.async_skulls",
            "perf.smooth_autosave",
            "perf.flight_preload",
            "perf.block_entity_limit",
            "perf.model_lod",
            "perf.menu_world_cache",
            "perf.server_lighting",
            "perf.directional_culling",
            "perf.view_first_building",
            "perf.portal_preload",
            "perf.fast_loading",
            "perf.server_idle",
            "perf.ocean_floor_culling",
            "perf.end_optimizations",
            "perf.gl_state_cache",
            "perf.cheap_damage_tint",
            "perf.lightmap_rate",
            "perf.ao_cache",
            "perf.async_skins",
            "quality.far_terrain",
            "quality.shader_preset",
            "quality.cloud_distance",
            "quality.fast_grass",
            "quality.leaves",
            "quality.leaf_culling",
            "quality.enchant_glint",
            "quality.end_crystal_beams"
    };

    /** Every tuning option's settings key. */
    public static String[] optionKeys() {
        return OPTION_KEYS.clone();
    }

    /** Whether the tuning option with this settings key does anything on this version. */
    public static boolean option(final String key) {
        switch (key) {
            case "perf.low_end_mode":
            case "perf.hitch_guard":
            case "perf.redstone_particle_limit":
            case "perf.lazy_far_rebuilds":
                return true;
            case "perf.dynamic_fps":
            case "perf.menu_fps":
            case "perf.loading_screen_gc":
                return CLIENT;
            case "perf.entity_detail_distance":
                return FRAME_ITEM || LAYERS || MINECART || SIGN;
            case "perf.crowd_culling":
            case "perf.dimension_culling":
            case "perf.translucent_sort_throttle":
            case "quality.light_weather":
                return LEVEL;
            case "perf.explosion_particles":
                return EXPLOSION;
            case "perf.light_updates":
                return LIGHT && CLIENT;
            case "perf.map_updates":
                return MAP;
            case "perf.async_skulls":
                return SKULL && SKULL_LOOKUP;
            case "perf.smooth_autosave":
                return CHUNK_SAVE;
            case "perf.flight_preload":
                return AUTOSAVE && FLIGHT_PRELOAD;
            case "perf.block_entity_limit":
                return BLOCK_ENTITY;
            case "perf.model_lod":
                return LAYERS || com.aetherium.Capabilities.FAST_MODELS;
            case "perf.menu_world_cache":
                return WORLD && WORLD_CACHE;
            case "perf.server_lighting":
                return SERVER_LIGHT;
            case "perf.directional_culling":
                return CULL;
            case "perf.view_first_building":
                return BUILD_ORDER;
            case "perf.portal_preload":
                return PORTAL;
            case "perf.fast_loading":
                return LOADING || WORLD;
            case "perf.server_idle":
                return SERVER_IDLE;
            case "perf.ocean_floor_culling":
                return OCEAN_FLOOR && CULL && LIGHT;
            case "perf.end_optimizations":
                return SKY || LEAF_CULL;
            case "perf.gl_state_cache":
            case "perf.cheap_damage_tint":
                return GL_STATE;
            case "perf.lightmap_rate":
                return LIGHTMAP;
            case "perf.ao_cache":
                return AO_CACHE;
            case "perf.async_skins":
                return SKIN;
            case "quality.far_terrain":
                return FAR_TERRAIN && LEVEL && LIGHT;
            case "quality.shader_preset":
                return COLOR_FILTER && WORLD;
            case "quality.cloud_distance":
                return SKY;
            case "quality.fast_grass":
                return GRASS && GRASS_QUADS;
            case "quality.leaves":
                return LEAVES;
            case "quality.leaf_culling":
                return LEAF_CULL;
            case "quality.enchant_glint":
                return GLINT;
            case "quality.end_crystal_beams":
                return BEAM;
            default:
                return false;
        }
    }
}
