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
    public final ConfigValue<Integer> workerThreads = ConfigValue.intRange(
            "performance.worker_threads", 0, 0, 16, false, "Threads for world generation and chunk building. 0 = automatic (vanilla on desktop; on Android all cores but three, at least 2, so the render and server threads are not starved). Applies after a restart.");

    // ---------------------------------------------------------------- quality
    public final ConfigValue<Boolean> weather = ConfigValue.bool("quality.weather", true, "Render rain and snow. Off saves a lot of fill rate on mobile GPUs.");
    public final ConfigValue<Boolean> vignette = ConfigValue.bool("quality.vignette", true, "Draw the darkened screen edges.");

    // ---------------------------------------------------------------- effects
    public final ConfigValue<LightMode> dynamicLights = ConfigValue.enumerated(
            "effects.dynamic_lights", LightMode.OFF, LightMode.class, false,
            "Light emitted by held items and entities. FAST updates 4x per second with a shorter radius; FANCY updates every tick. Off removes the light hooks entirely for full speed, so turning it on from Off takes effect after a restart.");
    public final ConfigValue<Boolean> dynamicLightsHeld = ConfigValue.bool("effects.dynamic_lights.held", true, "Torches and other luminous items held by players and mobs emit light.");
    public final ConfigValue<Boolean> dynamicLightsEntities = ConfigValue.bool("effects.dynamic_lights.entities", true, "Dropped luminous items, burning entities and blazes emit light.");
    public final ConfigValue<Boolean> fullbright = ConfigValue.bool("effects.fullbright", false, "Override the brightness so caves and nights are fully lit.");
    public final ConfigValue<Integer> fullbrightStrength = ConfigValue.intRange(
            "effects.fullbright.strength", 100, 10, 100, false, "Fullbright strength in percent.");

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
    public final ConfigValue<Boolean> experimentalChunkRenderer = ConfigValue.bool(
            "advanced.experimental_chunk_renderer", false, "Experimental, Minecraft 1.16.5 only: draw solid and cutout terrain with Aetherium's own shader and a shared index buffer instead of vanilla's fixed-function path. Translucent blocks stay vanilla. Turns itself off and falls back to vanilla if the shader fails.");
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
