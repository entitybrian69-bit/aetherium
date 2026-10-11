package com.aetherium.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Every Aetherium option, in declaration order. Declaration order is load-bearing:
 * it drives {@code CONFIG_SCHEMA.json}, the GUI tab ordering and the file layout
 * of {@code aetherium.json}, so a delta patch that only adds an option still
 * produces a deterministic GUI.
 *
 * <p>Keys are {@code group.name}; {@link #byGroup(String)} powers the sidebar
 * tabs. Instances are created once in {@link #createDefaults()} and shared —
 * {@code Aetherium.config()} hands out the one live instance, matching how
 * Sodium's {@code GameOptions} singleton works. Never construct a second copy
 * at runtime: listeners bound by the GUI would be orphaned.</p>
 */
public final class AetheriumConfig {
    /** Schema version; bumped by a migration when the key set changes shape. */
    public static final int CURRENT_VERSION = 5;

    // --------------------------------------------------------------- general
    public final ConfigValue<Boolean> enabled = ConfigValue.bool(
            "general.enabled", true,
            "Master switch. true = OpenGL 3.0 backend (Aetherium hooks active), false = Vanilla. Live, no restart.");
    public final ConfigValue<Preset> preset = ConfigValue.enumerated(
            "general.preset", Preset.CUSTOM, Preset.class, false,
            "Last applied performance preset; any manual change turns it back into CUSTOM.");
    public final ConfigValue<Boolean> showFrameHud = ConfigValue.bool("general.hud.fps", false, "Corner FPS overlay.");
    public final ConfigValue<String> hudCorner = ConfigValue.string(
            "general.hud.corner", "top-left", "Overlay anchor: top-left, top-right, bottom-left, bottom-right.");
    public final ConfigValue<Boolean> notifyConflicts = ConfigValue.bool("general.notify_conflicts", true, "Log and show a notice when a conflicting mod is present.");
    public final ConfigValue<Boolean> darkMode = ConfigValue.bool("general.ui.dark_mode", false, "Dark theme for the Aetherium settings screen.");
    public final ConfigValue<Boolean> uiSounds = ConfigValue.bool("general.ui.sounds", true, "Interface sounds in the Aetherium settings screen.");

    // ------------------------------------------------------------ performance
    public final ConfigValue<Boolean> entityCulling = ConfigValue.bool("performance.entity_culling", true, "Skip rendering entities beyond the cull distance (bosses, vehicles and the camera entity are exempt).");
    public final ConfigValue<Boolean> occlusionCulling = ConfigValue.bool(
            "performance.occlusion_culling", true, "Skip entities hidden behind terrain, using the chunk visibility vanilla already computes for blocks. Players, named and glowing entities are never hidden.");
    public final ConfigValue<Integer> entityCullDistance = ConfigValue.intRange(
            "performance.entity_cull_distance", 64, 16, 256, false, "Entities farther than this many blocks are not rendered.");
    public final ConfigValue<Integer> particleDensity = ConfigValue.intRange(
            "performance.particle_density", 100, 0, 100, false, "Percentage of spawned particles that are kept.");
    public final ConfigValue<Boolean> adaptiveDistance = ConfigValue.bool(
            "performance.adaptive_distance", false, "Lower the render distance by one chunk after FPS stays under the target for 10 seconds, raise it after 30 seconds of headroom. Every change reloads all chunks, so it waits 30 seconds between changes.");
    public final ConfigValue<Integer> adaptiveTargetFps = ConfigValue.intRange(
            "performance.adaptive_target_fps", 60, 20, 240, false, "FPS the adaptive render distance tries to hold.");
    public final ConfigValue<Boolean> animatedTextures = ConfigValue.bool(
            "performance.animated_textures", true, "Animate water, lava, fire, portals and other animated block textures. Off skips every animation texture upload, a large saving on mobile GL layers.");
    public final ConfigValue<Integer> blockEntityDistance = ConfigValue.intRange(
            "performance.block_entity_distance", 64, 16, 64, false, "Chests, signs, banners, heads and other block entities farther than this many blocks are not drawn (64 = vanilla; beacon beams are exempt).");
    public final ConfigValue<Boolean> smoothChunkLoading = ConfigValue.bool(
            "performance.smooth_chunk_loading", true, "Spread finished chunk meshes over several frames instead of uploading them all in one frame, which removes the stutter while chunks load or generate.");
    public final ConfigValue<Boolean> threadedChunkBuilder = ConfigValue.bool(
            "performance.threaded_chunk_builder", true, "Rebuild changed chunks near you on the worker threads instead of freezing the frame (vanilla 1.16.5 rebuilds everything within 28 blocks on the render thread). Blocks you place or break still update instantly. Also spreads client light updates over frames. The biggest fix for stutter while chunks load and while walking with dynamic lights.");
    public final ConfigValue<Boolean> smoothStreaming = ConfigValue.bool(
            "performance.smooth_streaming", true, "Spread the work of arriving chunks over several frames: incoming chunk, light and entity packets get a time budget per frame instead of all being processed in one frame, and chunk meshes are converted for the GPU on the worker threads. Removes the big frame drops when walking or flying into unloaded terrain.");
    public final ConfigValue<Boolean> coalesceRebuilds = ConfigValue.bool(
            "performance.coalesce_rebuilds", true, "While terrain loads, every chunk section is marked for rebuilding again each time a neighbouring chunk or its light arrives. Wait until a far or new section has been quiet for a moment, then build it once instead of up to ten times. Sections you edit, and built sections within 28 blocks, are never delayed.");
    public final ConfigValue<Integer> workerThreads = ConfigValue.intRange(
            "performance.worker_threads", 0, 0, 16, false, "Threads for world generation. 0 = automatic (vanilla on desktop; on Android half the cores minus one, between 2 and 4, so the render and server threads are not starved). Chunk meshes are built on Aetherium's own threads. Applies after a restart.");

    // ---------------------------------------------------------------- quality
    public final ConfigValue<Boolean> weather = ConfigValue.bool("quality.weather", true, "Render rain and snow. Off saves a lot of fill rate on mobile GPUs.");
    public final ConfigValue<Boolean> vignette = ConfigValue.bool("quality.vignette", true, "Draw the darkened screen edges.");

    public final ConfigValue<Boolean> distanceFog = ConfigValue.bool(
            "quality.fog.distance", true, "Fog that hides the edge of the render distance. Off shows terrain all the way to the edge (also removes the thick Nether fog). Fog under water, in lava and from blindness is unaffected.");
    public final ConfigValue<Integer> fogStart = ConfigValue.intRange(
            "quality.fog.start", 75, 0, 100, false, "Where the distance fog begins, in percent of the render distance. 75 = vanilla; higher keeps more of the world clear.");
    public final ConfigValue<Integer> waterFog = ConfigValue.intRange(
            "quality.fog.water", 100, 0, 100, false, "Density of the fog under water, in percent. 100 = vanilla, 0 = clear water.");
    public final ConfigValue<Boolean> clearLava = ConfigValue.bool(
            "quality.fog.clear_lava", false, "See a few blocks further while inside lava.");
    public final ConfigValue<Boolean> sky = ConfigValue.bool(
            "quality.sky.sky", true, "Draw the sky dome, sunrise colours, sun, moon and stars. Off leaves a flat sky colour (cheapest).");
    public final ConfigValue<Boolean> sunMoon = ConfigValue.bool(
            "quality.sky.sun_moon", true, "Draw the sun, the moon and the sunrise and sunset glow.");
    public final ConfigValue<Boolean> stars = ConfigValue.bool("quality.sky.stars", true, "Draw the stars at night.");
    public final ConfigValue<Integer> cloudHeight = ConfigValue.intRange(
            "quality.sky.cloud_height", 0, 0, 320, false, "Height of the cloud layer in blocks. 0 = the dimension's own height (128 in the Overworld).");
    public final ConfigValue<Boolean> particlesExplosions = ConfigValue.bool(
            "quality.particles.explosions", true, "Explosion, firework and flash particles.");
    public final ConfigValue<Boolean> particlesWeather = ConfigValue.bool(
            "quality.particles.weather", true, "Raindrop splashes on the ground and in water.");
    public final ConfigValue<Boolean> particlesSmoke = ConfigValue.bool(
            "quality.particles.smoke", true, "Smoke, campfire smoke, clouds and poof particles.");
    public final ConfigValue<Boolean> particlesPotions = ConfigValue.bool(
            "quality.particles.potions", true, "Potion swirls and splash effects, beacon and conduit effects.");
    public final ConfigValue<Boolean> particlesCombat = ConfigValue.bool(
            "quality.particles.combat", true, "Critical and enchanted hit stars, sweep attacks and damage hearts.");
    public final ConfigValue<Boolean> particlesFire = ConfigValue.bool(
            "quality.particles.fire", true, "Torch and fire flames, soul flames and lava pops.");
    public final ConfigValue<Boolean> particlesDrips = ConfigValue.bool(
            "quality.particles.drips", true, "Dripping water, lava, honey and tears; bubbles and underwater particles.");
    public final ConfigValue<Boolean> particlesPortals = ConfigValue.bool(
            "quality.particles.portals", true, "Nether portal, ender pearl and ender chest particles.");
    public final ConfigValue<Boolean> particlesAmbient = ConfigValue.bool(
            "quality.particles.ambient", true, "Floating ambient particles: Nether ash and spores, mycelium and the underwater haze.");
    public final ConfigValue<Boolean> particlesTotem = ConfigValue.bool(
            "quality.particles.totem", true, "The burst from a Totem of Undying.");
    public final ConfigValue<Boolean> particlesBlockBreak = ConfigValue.bool(
            "quality.particles.block_break", true, "Fragments when blocks are broken, and the chips while mining one.");

    // ---------------------------------------------------------------- effects
    public final ConfigValue<LightMode> dynamicLights = ConfigValue.enumerated(
            "effects.dynamic_lights", LightMode.OFF, LightMode.class, false,
            "Light emitted by held items and entities: hold a torch and the blocks around you light up. FAST updates 4x per second with a shorter radius; FANCY updates every tick. Takes effect immediately, no restart; Off costs nothing.");
    public final ConfigValue<Boolean> dynamicLightsHeld = ConfigValue.bool("effects.dynamic_lights.held", true, "Torches and other luminous items held by players and mobs emit light.");
    public final ConfigValue<Boolean> dynamicLightsEntities = ConfigValue.bool("effects.dynamic_lights.entities", true, "Dropped luminous items, burning entities and blazes emit light.");
    public final ConfigValue<Boolean> fullbright = ConfigValue.bool("effects.fullbright", false, "Override the brightness so caves and nights are fully lit.");
    public final ConfigValue<Integer> fullbrightStrength = ConfigValue.intRange(
            "effects.fullbright.strength", 100, 10, 100, false, "Fullbright strength in percent.");
    public final ConfigValue<Integer> zoomDefault = ConfigValue.intRange(
            "effects.zoom.default", 50, 0, 100, false, "How far the Zoom key zooms in when pressed, in percent. 50 is about four times, like OptiFine. Change it while zooming with the scroll wheel or the Zoom In / Zoom Out keys (rebind them in Options > Controls).");
    public final ConfigValue<Boolean> zoomToggle = ConfigValue.bool(
            "effects.zoom.toggle", false, "First person: press Zoom once to zoom and again to stop, instead of holding it.");
    public final ConfigValue<Boolean> zoomCinematic = ConfigValue.bool(
            "effects.zoom.cinematic", true, "Use the smooth cinematic camera while zoomed, for steady aiming at long range.");
    public final ConfigValue<Boolean> zoomScrolling = ConfigValue.bool(
            "effects.zoom.scrolling", true, "While zooming, the scroll wheel zooms in and out instead of changing the hotbar slot.");
    public final ConfigValue<Integer> zoomSpeed = ConfigValue.intRange(
            "effects.zoom.speed", 20, 1, 100, false, "How fast the scroll wheel and the Zoom In / Zoom Out keys change the zoom.");
    public final ConfigValue<Integer> zoomSmoothnessMs = ConfigValue.intRange(
            "effects.zoom.smoothness_ms", 150, 0, 500, false, "Zoom changes are animated over this many milliseconds. 0 = instant.");
    public final ConfigValue<Integer> zoomSensitivityFloor = ConfigValue.intRange(
            "effects.zoom.sensitivity_floor", 40, 0, 100, false, "Mouse sensitivity is lowered while zoomed, but never below this percentage of normal. 100 = unchanged.");
    public final ConfigValue<Integer> zoomMinFov = ConfigValue.intRange(
            "effects.zoom.min_fov", 1, 1, 30, false, "Field of view at full zoom, in degrees. Lower = stronger maximum zoom.");
    public final ConfigValue<Integer> zoomThirdPersonDistance = ConfigValue.intRange(
            "effects.zoom.third_person_distance", 15, 0, 32, false, "Third person: the zoom keys move the camera between 0.5 blocks and this distance instead of zooming. 0 = zoom the field of view in third person too.");
    public final ConfigValue<Boolean> zoomThirdPersonToggle = ConfigValue.bool(
            "effects.zoom.third_person_toggle", true, "Third person: the Zoom key toggles the camera distance instead of being held.");
    public final ConfigValue<Boolean> freelookToggle = ConfigValue.bool(
            "effects.freelook.toggle", false, "Press Freelook once to look around and again to stop, instead of holding it.");
    public final ConfigValue<Boolean> freelookThirdPerson = ConfigValue.bool(
            "effects.freelook.third_person", true, "Switch to the third-person camera while freelooking from first person, so you can see around your player. Off keeps the first-person view.");

    // ----------------------------------------------------------------- android
    public final ConfigValue<Boolean> androidSupport = ConfigValue.bool("android.enabled", true, "Enable launcher/renderer probing. No-op on desktop.");
    public final ConfigValue<AndroidRendererChoice> androidForceRenderer = ConfigValue.enumerated(
            "android.force_renderer", AndroidRendererChoice.AUTO, AndroidRendererChoice.class, true,
            "Override the detected renderer; use when the launcher env is unreadable.");
    public final ConfigValue<Boolean> readCustomEnv = ConfigValue.bool("android.read_custom_env", true, "Parse the launcher's custom_env.txt for renderer hints.");
    public final ConfigValue<Integer> memoryBudgetMb = ConfigValue.intRange("android.memory_budget_mb", 1024, 256, 8192, false, "Heap budget reported for the device; auto-detected from cgroup limits.");
    public final ConfigValue<Boolean> batterySaver = ConfigValue.bool("android.battery_saver", false, "Cap the frame rate to save battery.");
    public final ConfigValue<Integer> batteryFpsCap = ConfigValue.intRange("android.battery_fps_cap", 30, 15, 60, false, "Frame-rate cap used by battery saver.");
    public final ConfigValue<Boolean> thermalGuard = ConfigValue.bool("android.thermal_guard", false, "Android only, opt-in: cap the frame rate at 30 while the phone is above the thermal ceiling. The temperature is read on a background thread.");
    public final ConfigValue<Integer> thermalCeilingC = ConfigValue.intRange("android.thermal_ceiling_c", 75, 40, 95, false, "Hottest CPU/GPU temperature at which the thermal guard engages.");
    public final ConfigValue<Boolean> touchMode = ConfigValue.bool("android.touch_mode", false, "Taller rows and larger hit targets in the Aetherium screen.");

    // ---------------------------------------------------------------- advanced
    public final ConfigValue<Boolean> debugLogging = ConfigValue.bool("advanced.debug_logging", false, "Enable dev-level logging (hot-path cost: near zero when off).");
    public final ConfigValue<Boolean> stutterLog = ConfigValue.bool(
            "advanced.stutter_log", true, "Write every frame that takes far longer than usual to logs/aetherium-stutter.log, with what happened in that frame (packets, chunk uploads, builds, garbage collection). Written on a background thread; at most 1 MB.");
    public final ConfigValue<Boolean> experimentalChunkRenderer = ConfigValue.bool(
            "advanced.experimental_chunk_renderer", false, "Experimental, Minecraft 1.16.5 only: Aetherium's own terrain pipeline. Solid and cutout chunks are stored in a compact vertex format (a quarter smaller than vanilla's), grouped by face direction so faces pointing away from you are never sent to the GPU, and drawn with Aetherium's own shader. Translucent blocks stay vanilla. Switching it rebuilds the loaded chunks once; it turns itself off and falls back to vanilla if anything fails.");
    public final ConfigValue<Boolean> pipelineShader = ConfigValue.bool(
            "advanced.pipeline_shader", true, "How the Aetherium pipeline draws. On: Aetherium's own shader with compact vertices (fastest). Off: compatible mode, which keeps vanilla's vertex format and vanilla's own draw calls and only skips the faces pointing away from you, for graphics drivers and GL translators the shader crashes on. Switched off automatically if shader mode crashed the game.");
    public final ConfigValue<Boolean> conflictAutoDelegate = ConfigValue.bool("advanced.conflict_auto_delegate", true, "Hand features back to a conflicting mod instead of double-applying them.");

    // ------------------------------------------------------------------- lookup
    /** Declaration-order index, keyed by {@code group.name}. */
    private final Map<String, ConfigValue<?>> options = new LinkedHashMap<>();

    private int fileVersion = CURRENT_VERSION;

    public AetheriumConfig() {
        for (final java.lang.reflect.Field field : this.getClass().getFields()) {
            if (!ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                final ConfigValue<?> value = (ConfigValue<?>) field.get(this);
                this.options.put(value.getKey(), value);
            } catch (final IllegalAccessException error) {
                // Public fields cannot throw here; surfacing it beats hiding it.
                throw new IllegalStateException("Failed to index config field " + field.getName(), error);
            }
        }
    }

    public static AetheriumConfig createDefaults() {
        return new AetheriumConfig();
    }

    public Map<String, ConfigValue<?>> all() {
        return this.options;
    }

    public Collection<ConfigValue<?>> values() {
        return this.options.values();
    }

    public ConfigValue<?> byKey(final String key) {
        return this.options.get(Objects.requireNonNull(key, "key"));
    }

    public Map<String, ConfigValue<?>> byGroup(final String group) {
        final Map<String, ConfigValue<?>> out = new LinkedHashMap<>();
        for (final Map.Entry<String, ConfigValue<?>> entry : this.options.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(group + ".")) {
                out.put(key, entry.getValue());
            }
        }
        return out;
    }

    public int getFileVersion() {
        return this.fileVersion;
    }

    public void setFileVersion(final int version) {
        this.fileVersion = version;
    }

    public boolean anyDirty() {
        for (final ConfigValue<?> value : this.options.values()) {
            if (value.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public void clearDirty() {
        for (final ConfigValue<?> value : this.options.values()) {
            // Direct flag reset: set(get()) never clears the flag, so a write that
            // "succeeded" would leave every value dirty and the shutdown hook would
            // rewrite the file for no reason.
            value.clearDirtyFlag();
        }
    }

    /** Performance presets offered on the General tab. */
    public enum Preset {
        CUSTOM, MAX_FPS, BALANCED, QUALITY
    }

    /** Dynamic light update modes. */
    public enum LightMode {
        OFF, FAST, FANCY
    }

    /** Manual override for the Android renderer when env probing is not enough. */
    public enum AndroidRendererChoice {
        AUTO,
        GL4ES,
        ZINK,
        LTW,
        MOBILEGLUES,
        VIRGL,
        ANGLE,
        NATIVE_VULKAN
    }

    @Override
    public String toString() {
        return "AetheriumConfig{version=" + this.fileVersion + ", options=" + this.options.size() + '}';
    }
}
