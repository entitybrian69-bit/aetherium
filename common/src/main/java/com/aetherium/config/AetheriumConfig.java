package com.aetherium.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
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
    public static final int CURRENT_VERSION = 3;

    // --------------------------------------------------------------- general
    public final ConfigValue<Boolean> enabled = ConfigValue.bool(
            "general.enabled", true,
            "Master switch. false = Compatibility mode (vanilla renderer), live, no restart.");
    public final ConfigValue<Boolean> showFrameHud = ConfigValue.bool("general.hud.fps", false, "Corner FPS + 99th-percentile overlay.");
    public final ConfigValue<Boolean> showFrameGraph = ConfigValue.bool("general.hud.frame_graph", false, "Spike graph of the last 240 frames.");
    public final ConfigValue<Boolean> showBackendTag = ConfigValue.bool("general.hud.backend_tag", true, "Append [Aetherium/GL46] to the F3 header line.");
    public final ConfigValue<String> hudCorner = ConfigValue.string(
            "general.hud.corner", "top-left", "Overlay anchor: top-left, top-right, bottom-left, bottom-right.");
    public final ConfigValue<Boolean> notifyConflicts = ConfigValue.bool("general.notify_conflicts", true, "Show a notice on screen (top-right, under the backend tag) when a conflicting renderer mod is present.");

    // ------------------------------------------------------------ performance
    public final ConfigValue<BackendChoice> backend = ConfigValue.enumerated(
            "performance.backend", BackendChoice.AUTO, BackendChoice.class, true,
            "Backend selection. AUTO probes the GPU and picks the best available.");
    public final ConfigValue<Boolean> asyncMeshing = ConfigValue.bool("performance.async_meshing", true, "Mesh sections off-thread with distance priority.");
    public final ConfigValue<Integer> meshWorkers = ConfigValue.intRange(
            "performance.mesh_workers", 0, 0, 32, false,
            "0 = auto (min(availableProcessors()-1, 8)). Applies on next world load.");
    public final ConfigValue<Integer> uploadBudgetKb = ConfigValue.intRange(
            "performance.upload_budget_kb", 24576, 512, 262144, false,
            "Per-frame ceiling on mapped-buffer uploads; keeps upload spikes out of the frame graph.");
    public final ConfigValue<Boolean> indirectDraw = ConfigValue.bool("performance.indirect_draw", true, "glMultiDrawElementsIndirectCount batching (needs ARB_indirect_parameters / GL 4.6).");
    public final ConfigValue<Boolean> persistentBuffers = ConfigValue.bool("performance.persistent_buffers", true, "Persistent mapped buffers with triple buffering + fences.");
    public final ConfigValue<Boolean> hzb = ConfigValue.bool("performance.hzb", true, "Hierarchical Z-buffer occlusion culling.");
    public final ConfigValue<Integer> hzbDepth = ConfigValue.intRange("performance.hzb.levels", 6, 1, 12, false, "Pyramid depth; larger = cheaper tests, coarser rejection.");
    public final ConfigValue<Integer> indirectBatchCapacity = ConfigValue.intRange(
            "performance.indirect_batch_capacity", 4096, 256, 65536, false,
            "Draw-command slots allocated up front; the GPU-visible count decides how many are used.");
    public final ConfigValue<Integer> targetFps = ConfigValue.intRange("performance.target_fps", 0, 0, 1000, false, "0 = uncapped. Soft cap applied after the swap; never blocks the render thread.");
    public final ConfigValue<Boolean> programBinaryCache = ConfigValue.bool("performance.program_cache", true, "Disk-cache GL program binaries (skips recompiles across launches).");
    public final ConfigValue<Boolean> asyncShaderCompile = ConfigValue.bool("performance.async_shaders", true, "Use ARB_parallel_shader_compile where available.");

    // ---------------------------------------------------------------- quality
    public final ConfigValue<Ternary> fogQuality = ConfigValue.enumerated("quality.fog", Ternary.FANCY, Ternary.class, false, "Fog rendering quality.");
    public final ConfigValue<Ternary> cloudQuality = ConfigValue.enumerated("quality.clouds", Ternary.FANCY, Ternary.class, false, "Cloud rendering quality.");
    public final ConfigValue<Ternary> weatherQuality = ConfigValue.enumerated("quality.weather", Ternary.FANCY, Ternary.class, false, "Rain/snow quality; FAST also silences weather particle churn.");
    public final ConfigValue<Boolean> smoothLighting = ConfigValue.bool("quality.smooth_lighting", true, "Ambient-occlusion-smoothed vertex lighting.");
    public final ConfigValue<Boolean> entityCulling = ConfigValue.bool("quality.entity_culling", true, "Frustum + occlusion culling of entity render dispatches.");
    public final ConfigValue<Boolean> biomeBlend = ConfigValue.bool("quality.biome_blend", true, "Per-vertex biome blending.");
    public final ConfigValue<Integer> maxDirectLight = ConfigValue.intRange("quality.max_direct_light", 15, 0, 15, true, "Maximum direct block light written to the lightmap.");

    // ---------------------------------------------------------------- shaders
    public final ConfigValue<Boolean> irisIntegration = ConfigValue.bool("shaders.iris_integration", true, "Bind the Iris/Oculus v0 API by reflection when present.");
    public final ConfigValue<Boolean> pauseDynamicLights = ConfigValue.bool("shaders.pause_dynamic_lights_with_shaders", true, "Disable dynamic lightmap writes while a shader pack owns the lightmap.");
    public final ConfigValue<Boolean> reloadShadersOnWorldChange = ConfigValue.bool("shaders.reload_on_world_change", false, "Re-apply pack options on dimension/world change.");

    // -------------------------------------------------------------- utilities
    public final ConfigValue<Boolean> gammaEnabled = ConfigValue.bool("utilities.gamma.enabled", false, "Override the vanilla lightmap gamma response.");
    public final ConfigValue<Double> gammaAmount = ConfigValue.doubleRange("utilities.gamma.amount", 1.0, 0.0, 20.0, "Full-brightness slider; 20.0 is the cave-vision ceiling.");
    public final ConfigValue<Boolean> caveVision = ConfigValue.bool("utilities.gamma.cave_vision", false, "Raise the block-light floor only while the camera is under skylight 0.");
    public final ConfigValue<Double> caveVisionFloor = ConfigValue.doubleRange("utilities.gamma.cave_vision_floor", 8.0, 0.0, 15.0, "Effective block light guaranteed by cave vision.");
    public final ConfigValue<Boolean> nightVisionBoost = ConfigValue.bool("utilities.gamma.night_vision", false, "Brighten the sky-light channel at night without changing block light.");
    public final ConfigValue<Double> nightVisionLevel = ConfigValue.doubleRange("utilities.gamma.night_vision_level", 4.0, 0.0, 15.0, "Sky-light floor applied by night vision boost.");
    public final ConfigValue<Boolean> timeBasedGamma = ConfigValue.bool("utilities.gamma.time_based", false, "Blend gamma between a day and a night target using world time.");
    public final ConfigValue<Double> gammaNightTarget = ConfigValue.doubleRange("utilities.gamma.time_based_night", 1.8, 0.0, 20.0, "Gamma applied at midnight when time-based gamma is on.");
    public final ConfigValue<String> gammaCurve = ConfigValue.string(
            "utilities.gamma.curve", "0:0,0.15:0.32,0.4:0.62,0.7:0.85,1:1",
            "Monotone control points 'x:y,...' — see gamma.GammaCurve; blank disables the curve editor.");

    public final ConfigValue<Boolean> dynamicLights = ConfigValue.bool("utilities.dynamic_lights.enabled", false, "Item/entity light sources written into the lightmap.");
    public final ConfigValue<Integer> dynamicLightsQuality = ConfigValue.intRange("utilities.dynamic_lights.quality", 2, 0, 3, false, "0 off, 1 sources only, 2 + attenuation smoothing, 3 + occlusion test.");
    public final ConfigValue<Integer> dynamicLightsRange = ConfigValue.intRange("utilities.dynamic_lights.range", 14, 4, 15, false, "Propagation radius in blocks.");
    public final ConfigValue<Boolean> dynamicLightsColored = ConfigValue.bool("utilities.dynamic_lights.colored", true, "Use per-source RGB instead of a white falloff.");
    public final ConfigValue<Boolean> dynamicLightsEntities = ConfigValue.bool("utilities.dynamic_lights.entities", true, "Light from entities holding a light source.");
    public final ConfigValue<Double> dynamicLightsIntensity = ConfigValue.doubleRange("utilities.dynamic_lights.intensity", 1.0, 0.1, 2.0, "Multiplier on emitted light level.");

    // ----------------------------------------------------------------- android
    public final ConfigValue<Boolean> androidSupport = ConfigValue.bool("android.enabled", true, "Enable launcher/renderer probing. No-op on desktop.");
    public final ConfigValue<AndroidRendererChoice> androidForceRenderer = ConfigValue.enumerated(
            "android.force_renderer", AndroidRendererChoice.AUTO, AndroidRendererChoice.class, true,
            "Override the detected renderer; use when the launcher env is unreadable.");
    public final ConfigValue<Boolean> readCustomEnv = ConfigValue.bool("android.read_custom_env", true, "Parse the launcher's custom_env.txt for renderer hints.");
    public final ConfigValue<Boolean> mobileMemoryMode = ConfigValue.bool("android.mobile_memory", true, "Shrink arenas, mesh cache and texture mips to the heap budget.");
    public final ConfigValue<Integer> memoryBudgetMb = ConfigValue.intRange("android.memory_budget_mb", 1024, 256, 8192, false, "Soft ceiling for native arenas; auto-detected from cgroup limits when 0.");
    public final ConfigValue<Boolean> batterySaver = ConfigValue.bool("android.battery_saver", false, "Cap FPS, halve upload budget, stop background meshing on battery.");
    public final ConfigValue<Boolean> thermalThrottle = ConfigValue.bool("android.thermal_throttle", true, "Read thermal zones and back off before the SoC clocks down.");
    public final ConfigValue<Integer> thermalCeilingC = ConfigValue.intRange("android.thermal_ceiling_c", 68, 40, 95, false, "Package temperature at which Aetherium starts shedding work.");
    public final ConfigValue<Boolean> touchMode = ConfigValue.bool("android.touch_mode", true, "48dp hit targets, swipe scroll, long-press tooltips.");

    // ---------------------------------------------------------------- advanced
    public final ConfigValue<Boolean> debugLogging = ConfigValue.bool("advanced.debug_logging", false, "Enable dev-level logging (hot-path cost: near zero when off).");
    public final ConfigValue<Boolean> glErrors = ConfigValue.bool("advanced.gl_errors", true, "Check glGetError at pass boundaries in debug builds.");
    public final ConfigValue<Boolean> failFastUnsupportedGpu = ConfigValue.bool("advanced.fail_fast_gpu", false, "Crash with a report instead of falling back to Compatibility mode.");
    public final ConfigValue<Boolean> experimentalFullRenderer = ConfigValue.bool("advanced.experimental_full_renderer", false, "NOT SHIPPED. Setting this true is refused at startup: an error is logged and the engine runs in shadow (Compatibility) mode. See docs/ARCHITECTURE.md, \"Honest status\".");
    public final ConfigValue<Boolean> strictMixins = ConfigValue.bool("advanced.strict_mixins", false, "require=1 on every injection, so a version delta that no longer matches fails the launch instead of silently skipping.");
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
            // Field-level reset is not exposed on ConfigValue; the store calls
            // this after a successful write and dirty is only read, never
            // required to survive the write.
            clearOne(value);
        }
    }

    /**
     * Re-setting to the current value under the value's own type parameter. `ConfigValue<?>`
     * cannot be `set(value.get())` inline - the compiler only knows the capture, not that both
     * sides share it (javac: "Object cannot be converted to CAP#1") - so the capture is named here.
     */
    private static <T> void clearOne(final ConfigValue<T> value) {
        if (value.isDirty()) {
            value.set(value.get());
        }
    }

    /** Enumerations are declared here so the schema exporter can describe them. */
    public enum Ternary {
        OFF, FAST, FANCY;

        public boolean isFancy() {
            return this == FANCY;
        }

        public boolean isFastOrFancy() {
            return this != OFF;
        }
    }

    /** Backend selection as exposed to users; AUTO resolves via BackendProber. */
    public enum BackendChoice {
        AUTO,
        GL46_DSA,
        VULKAN_13,
        GL_CORE,
        GL_LEGACY,
        COMPATIBILITY;

        public static BackendChoice from(final String text) {
            final String normalized = text.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            for (final BackendChoice choice : values()) {
                if (choice.name().equals(normalized)) {
                    return choice;
                }
            }
            return AUTO;
        }
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
