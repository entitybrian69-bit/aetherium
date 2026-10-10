package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.Capabilities;
import com.aetherium.android.AndroidEnvironment;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigValue;
import com.aetherium.gui.AetheriumView;
import com.aetherium.gui.Page;
import com.aetherium.gui.PixelArt;
import com.aetherium.gui.Setting;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.shader.IrisBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

/**
 * The seven tabs of the Aetherium video settings screen and every row on them.
 *
 * <p>Rows read live values when the screen opens and write them only on Apply
 * (see {@link Setting}). Vanilla options go through {@link VanillaOptions};
 * Aetherium options go straight to the config, and {@link ClientHooks} picks
 * them up after Apply, so every change takes effect immediately without a
 * restart.</p>
 */
public final class AetheriumPages {

    /** What buttons on the pages need from the screen. */
    public interface Actions {
        void openShaderPacks();

        void resetDefaults();

        void openVanillaVideo();
    }

    static final String NOT_ON_THIS_VERSION = "Not available on this version";

    private static final String[] GRAPHICS = {"Fast", "Fancy", "Fabulous"};
    private static final String[] CLOUDS = {"Off", "Fast", "Fancy"};
    private static final String[] PARTICLES = {"All", "Decreased", "Minimal"};
    private static final String[] PRESETS = {"Max FPS", "Balanced", "Quality"};
    private static final String[] LIGHTS = {"Off", "Fast", "Fancy"};
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};
    private static final String[] CORNER_KEYS = {"top-left", "top-right", "bottom-left", "bottom-right"};

    // Preset table. Columns follow PRESET_ROWS; rows are MAX_FPS, BALANCED, QUALITY.
    private static final String[] PRESET_ROWS = {
        "perf.render_distance", "perf.simulation_distance", "quality.graphics", "quality.clouds",
        "quality.particles", "quality.smooth_lighting", "quality.biome_blend", "quality.shadows",
        "perf.cull_distance", "perf.particle_density", "quality.weather", "quality.vignette",
        "effects.dynamic_lights", "perf.entity_distance", "perf.adaptive",
    };
    private static final int[][] PRESET_VALUES = {
        {6, 5, 0, 0, 2, 0, 0, 0, 48, 30, 0, 0, 0, 75, 1},
        {10, 8, 1, 1, 1, 1, 2, 1, 64, 70, 1, 1, 1, 100, 0},
        {16, 12, 1, 2, 0, 1, 5, 1, 128, 100, 1, 1, 2, 150, 0},
    };

    private final AetheriumConfig config;
    private final IrisBridge iris;
    private final AndroidEnvironment android;
    private final Actions actions;
    private AetheriumView view;

    public AetheriumPages(final Aetherium.Subsystems sub, final Actions actions) {
        this.config = sub.config;
        this.iris = sub.iris;
        this.android = sub.android;
        this.actions = actions;
    }

    /** The view is created from {@link #build()}, so it is attached afterwards. */
    public void attach(final AetheriumView view) {
        this.view = view;
    }

    public List<Page> build() {
        final List<Page> pages = new ArrayList<Page>();
        pages.add(general());
        pages.add(quality());
        pages.add(performance());
        pages.add(backend());
        pages.add(effects());
        pages.add(shaders());
        pages.add(androidPage());
        return pages;
    }

    // ------------------------------------------------------------------ General

    private Page general() {
        final Page page = new Page("General", "General", "Master switch, presets and overlay", PixelArt.GEAR);
        page.add(bool("general.enabled", "Enable Aetherium", config.enabled));
        page.add(Setting.choice("general.preset", Setting.Kind.SEGMENTED, "Performance preset",
                        "Stages a full set of options. Editing any of them afterwards switches to Custom.", PRESETS,
                        () -> config.preset.get().ordinal() - 1,
                        v -> config.preset.set(AetheriumConfig.Preset.values()[Math.max(-1, v) + 1]))
                .withDefault(-1)
                .onPick(this::stagePreset));
        page.add(bool("general.hud", "Frame-time overlay", config.showFrameHud)
                .availableWhen(() -> Capabilities.HUD_OVERLAY, NOT_ON_THIS_VERSION));
        page.add(Setting.choice("general.hud_corner", Setting.Kind.DROPDOWN, "Overlay position",
                        "Screen corner the overlay is anchored to.", CORNERS,
                        () -> cornerIndex(config.hudCorner.get()),
                        v -> config.hudCorner.set(CORNER_KEYS[Math.max(0, Math.min(3, v))]))
                .withDefault(cornerIndex(config.hudCorner.getDefault()))
                .availableWhen(() -> Capabilities.HUD_OVERLAY, NOT_ON_THIS_VERSION));
        page.add(bool("general.notify", "Conflict notices", config.notifyConflicts));
        page.add(Setting.info("general.status", "Status", "What Aetherium is doing right now.", this::statusText));
        page.add(Setting.button("general.reset", "Reset Aetherium options",
                "Stages every Aetherium option back to its default. Vanilla options are left alone.", "Reset",
                actions::resetDefaults));
        page.add(Setting.button("general.vanilla", "Vanilla video settings",
                "Opens Minecraft's own video settings screen.", "Open", actions::openVanillaVideo));
        return page;
    }

    private void stagePreset(final int index) {
        if (this.view == null || index < 0 || index >= PRESET_VALUES.length) {
            return;
        }
        final int[] values = PRESET_VALUES[index];
        for (int i = 0; i < PRESET_ROWS.length; i++) {
            final Setting row = this.view.find(PRESET_ROWS[i]);
            if (row != null && row.isEditable()) {
                row.stage(values[i]);
            }
        }
    }

    private String statusText() {
        switch (Aetherium.getState()) {
            case ACTIVE:
                return "Active";
            case VANILLA:
                return "Paused (master switch off)";
            case INCOMPATIBLE:
                return "Standing down: " + Aetherium.conflicts().summarize();
            default:
                return "Not running";
        }
    }

    private static int cornerIndex(final String key) {
        for (int i = 0; i < CORNER_KEYS.length; i++) {
            if (CORNER_KEYS[i].equals(key)) {
                return i;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ Quality

    private Page quality() {
        final Page page = new Page("Quality", "Quality", "How the world looks", PixelArt.SPARKLES);
        page.add(Setting.choice("quality.graphics", Setting.Kind.SEGMENTED, "Graphics",
                "Fast skips transparent leaves and some effects. Fabulous uses extra passes for translucency.",
                GRAPHICS, VanillaOptions::getGraphics, VanillaOptions::setGraphics).presetMember());
        page.add(Setting.choice("quality.clouds", Setting.Kind.SEGMENTED, "Clouds", "Cloud rendering.",
                CLOUDS, VanillaOptions::getClouds, VanillaOptions::setClouds).presetMember());
        page.add(Setting.choice("quality.particles", Setting.Kind.SEGMENTED, "Particles",
                "Vanilla particle level. Combine with Particle density on the Performance tab.",
                PARTICLES, VanillaOptions::getParticles, VanillaOptions::setParticles).presetMember());
        page.add(flag("quality.smooth_lighting", "Smooth lighting", "Soft ambient-occlusion shading on blocks.",
                VanillaOptions::getSmoothLighting, VanillaOptions::setSmoothLighting).presetMember());
        page.add(Setting.slider("quality.biome_blend", "Biome blend",
                "Smooths grass, foliage and water colour between biomes. Higher costs chunk build time.",
                0, 7, 1, v -> v == 0 ? "Off" : (2 * v + 1) + "x" + (2 * v + 1),
                VanillaOptions::getBiomeBlend, VanillaOptions::setBiomeBlend).presetMember());
        page.add(flag("quality.shadows", "Entity shadows", "Round shadows under mobs and items.",
                VanillaOptions::getEntityShadows, VanillaOptions::setEntityShadows).presetMember());
        page.add(bool("quality.weather", "Weather", config.weather).presetMember());
        page.add(bool("quality.vignette", "Vignette", config.vignette).presetMember()
                .availableWhen(() -> Capabilities.VIGNETTE_TOGGLE, NOT_ON_THIS_VERSION));
        page.add(flag("quality.bob_view", "View bobbing", "Camera sway while walking.",
                VanillaOptions::getBobView, VanillaOptions::setBobView));
        page.add(Setting.slider("quality.brightness", "Brightness", "Vanilla brightness (gamma).",
                0, 100, 1, v -> v == 0 ? "Moody" : v == 100 ? "Bright" : v + "%",
                VanillaOptions::getBrightnessPercent, VanillaOptions::setBrightnessPercent));
        return page;
    }

    // -------------------------------------------------------------- Performance

    private Page performance() {
        final Page page = new Page("Performance", "Performance", "Frame rate and draw distance", PixelArt.BARS);
        page.add(Setting.slider("perf.render_distance", "Render distance",
                        "Chunks drawn around you. The single biggest FPS cost.", 2, 32, 1, v -> v + " chunks",
                        VanillaOptions::getRenderDistance,
                        v -> {
                            VanillaOptions.setRenderDistance(v);
                            ClientHooks.noteUserRenderDistance(v);
                        })
                .presetMember());
        page.add(Setting.slider("perf.simulation_distance", "Simulation distance",
                        "Chunks that tick (mobs, crops, redstone) in singleplayer.", 5, 32, 1, v -> v + " chunks",
                        VanillaOptions::getSimulationDistance, VanillaOptions::setSimulationDistance)
                .presetMember()
                .availableWhen(() -> Capabilities.SIMULATION_DISTANCE, "Added in Minecraft 1.18"));
        page.add(Setting.slider("perf.max_fps", "Max framerate", "Vanilla frame-rate limit.", 10,
                VanillaOptions.UNLIMITED_FPS, 10, v -> v >= VanillaOptions.UNLIMITED_FPS ? "Unlimited" : v + " FPS",
                VanillaOptions::getFramerateLimit, VanillaOptions::setFramerateLimit));
        page.add(flag("perf.vsync", "VSync", "Sync to the display refresh rate. Removes tearing, adds latency.",
                VanillaOptions::getVsync, VanillaOptions::setVsync));
        page.add(bool("perf.entity_culling", "Entity culling", config.entityCulling));
        page.add(intSlider("perf.cull_distance", "Entity cull distance", config.entityCullDistance, 8,
                v -> v + " blocks").presetMember());
        page.add(Setting.slider("perf.entity_distance", "Entity distance",
                "Vanilla entity render distance scaling.", 50, 500, 25, v -> v + "%",
                VanillaOptions::getEntityDistancePercent, VanillaOptions::setEntityDistancePercent).presetMember());
        page.add(intSlider("perf.particle_density", "Particle density", config.particleDensity, 5,
                v -> v + "%").presetMember());
        page.add(bool("perf.adaptive", "Adaptive render distance", config.adaptiveDistance).presetMember());
        page.add(intSlider("perf.adaptive_target", "Adaptive target", config.adaptiveTargetFps, 5, v -> v + " FPS"));
        return page;
    }

    // ------------------------------------------------------------------ Backend

    private Page backend() {
        final Page page = new Page("Backend", "Backend", "Renderer and runtime", PixelArt.CUBE);
        page.add(Setting.info("backend.api", "Rendering API",
                "Aetherium issues no GL calls of its own; it runs on any device that runs Minecraft.",
                () -> Aetherium.RENDERING_API + " (vanilla pipeline)"));
        page.add(Setting.info("backend.gpu", "GPU", "Reported by the driver.", GlInfo::renderer));
        page.add(Setting.info("backend.vendor", "Vendor", "Reported by the driver.", GlInfo::vendor));
        page.add(Setting.info("backend.driver", "Driver", "GL version string reported by the driver.", GlInfo::version));
        page.add(Setting.info("backend.java", "Java", "Runtime running the game.",
                () -> System.getProperty("java.version") + " (" + System.getProperty("java.vendor", "?") + ")"));
        page.add(Setting.info("backend.memory", "Memory", "Heap in use / maximum.", AetheriumPages::memoryText));
        page.add(Setting.info("backend.loader", "Loader", "Mod loader and game version.",
                () -> Aetherium.platform().platformName() + " - Minecraft " + Aetherium.platform().minecraftVersion()));
        page.add(Setting.info("backend.hooks", "Hooks", "Mixins applied on this version.",
                () -> AetheriumMixinPlugin.appliedCount() + " active"));
        page.add(Setting.info("backend.cap", "Frame cap", "Live cap from battery saver / thermal guard.",
                () -> {
                    final int cap = ClientHooks.currentCap();
                    if (cap <= 0) {
                        return "None";
                    }
                    return cap + " FPS" + (ClientHooks.isThermalCapped() ? " (thermal)" : "");
                }));
        page.add(bool("backend.debug", "Debug logging", config.debugLogging));
        page.add(bool("backend.delegate", "Hand off to conflicting mods", config.conflictAutoDelegate));
        return page;
    }

    private static String memoryText() {
        final Runtime rt = Runtime.getRuntime();
        final long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
        final long max = rt.maxMemory() >> 20;
        return used + " / " + max + " MB";
    }

    // ------------------------------------------------------------------ Effects

    private Page effects() {
        final Page page = new Page("Effects", "Effects", "Dynamic lights and brightness", PixelArt.SUN);
        page.add(Setting.choice("effects.dynamic_lights", Setting.Kind.SEGMENTED, "Dynamic lights",
                        config.dynamicLights.getComment(), LIGHTS,
                        () -> config.dynamicLights.get().ordinal(),
                        v -> config.dynamicLights.set(AetheriumConfig.LightMode.values()[clamp(v, 0, 2)]))
                .withDefault(config.dynamicLights.getDefault().ordinal())
                .presetMember()
                .availableWhen(() -> Capabilities.DYNAMIC_LIGHTS, NOT_ON_THIS_VERSION));
        page.add(bool("effects.held", "Held items glow", config.dynamicLightsHeld)
                .availableWhen(() -> Capabilities.DYNAMIC_LIGHTS, NOT_ON_THIS_VERSION));
        page.add(bool("effects.entities", "Glowing entities", config.dynamicLightsEntities)
                .availableWhen(() -> Capabilities.DYNAMIC_LIGHTS, NOT_ON_THIS_VERSION));
        page.add(Setting.info("effects.sources", "Active light sources", "Updated live.",
                () -> Integer.toString(ClientHooks.activeLightSources())));
        page.add(bool("effects.fullbright", "Fullbright", config.fullbright));
        page.add(intSlider("effects.fullbright_strength", "Fullbright strength", config.fullbrightStrength, 5,
                v -> v + "%"));
        return page;
    }

    // ------------------------------------------------------------------ Iris

    private Page shaders() {
        final Page page = new Page("Iris", "Iris Shaders", "Shader pack integration", PixelArt.IRIS);
        final BooleanSupplier present = iris::isPresent;
        page.add(Setting.info("iris.status", "Status", "Iris or Oculus, when installed.",
                () -> iris.isPresent() ? iris.getBrand() + " " + iris.getVersion() : "Not installed"));
        page.add(Setting.info("iris.pack", "Shader pack", "Whether a pack is rendering right now.",
                () -> iris.isShaderPackInUse() ? "In use" : "None"));
        page.add(flag("iris.enabled", "Shaders", "Turn the selected shader pack on or off.",
                iris::areShadersEnabled, on -> iris.setShadersEnabled(on, "Aetherium settings"))
                .availableWhen(present, "Iris is not installed"));
        page.add(bool("iris.integration", "Iris integration", config.irisIntegration));
        page.add(bool("iris.pause_lights", "Pause dynamic lights with shaders", config.pauseDynamicLights));
        page.add(Setting.button("iris.packs", "Shader packs", "Opens the Iris shader pack screen.", "Open",
                actions::openShaderPacks).availableWhen(present, "Iris is not installed"));
        return page;
    }

    // ------------------------------------------------------------------ Android

    private Page androidPage() {
        final Page page = new Page("Android", "Android", "Mobile launchers and power", PixelArt.ROBOT);
        page.add(Setting.info("android.device", "Device", "Detected launcher and renderer.",
                () -> android.isAndroid()
                        ? android.getLauncher().getDisplayName() + " - " + android.getRenderer().getDisplayName()
                        : "Desktop (not Android)"));
        page.add(Setting.info("android.cpu", "CPU", "Architecture and SIMD features.",
                () -> System.getProperty("os.arch", "?") + (android.hasNeon() ? " - NEON" : "")));
        page.add(bool("android.support", "Android support", config.androidSupport));
        final AetheriumConfig.AndroidRendererChoice[] choices = AetheriumConfig.AndroidRendererChoice.values();
        final String[] names = new String[choices.length];
        for (int i = 0; i < choices.length; i++) {
            names[i] = rendererName(choices[i]);
        }
        page.add(Setting.choice("android.renderer", Setting.Kind.DROPDOWN, "Renderer override",
                        config.androidForceRenderer.getComment(), names,
                        () -> config.androidForceRenderer.get().ordinal(),
                        v -> config.androidForceRenderer.set(choices[clamp(v, 0, choices.length - 1)]))
                .withDefault(config.androidForceRenderer.getDefault().ordinal()));
        page.add(bool("android.custom_env", "Read custom_env.txt", config.readCustomEnv));
        page.add(bool("android.battery", "Battery saver", config.batterySaver));
        page.add(intSlider("android.battery_cap", "Battery FPS cap", config.batteryFpsCap, 5, v -> v + " FPS"));
        page.add(bool("android.thermal", "Thermal guard", config.thermalGuard));
        page.add(intSlider("android.thermal_ceiling", "Thermal ceiling", config.thermalCeilingC, 1,
                v -> v + "\u00b0C"));
        page.add(Setting.info("android.temperature", "Temperature", "Last reading of the device sensor.",
                () -> {
                    if (!android.isThermalSensorUsable()) {
                        return "No sensor";
                    }
                    return (android.getLastThermalMilliCelsius() / 1000) + "\u00b0C";
                }));
        page.add(bool("android.touch", "Touch mode", config.touchMode));
        page.add(Setting.info("android.heap", "Heap usage", "Share of the maximum heap in use.",
                () -> Math.round(android.heapUsageFraction() * 100.0) + "%"));
        return page;
    }

    private static String rendererName(final AetheriumConfig.AndroidRendererChoice choice) {
        switch (choice) {
            case AUTO:
                return "Auto";
            case GL4ES:
                return "GL4ES";
            case ZINK:
                return "Zink";
            case LTW:
                return "LTW";
            case MOBILEGLUES:
                return "MobileGlues";
            case VIRGL:
                return "VirGL";
            case ANGLE:
                return "ANGLE";
            case NATIVE_VULKAN:
                return "Native Vulkan";
            default:
                return choice.name();
        }
    }

    // ------------------------------------------------------------------ builders

    private static Setting bool(final String id, final String label, final ConfigValue<Boolean> value) {
        return Setting.toggle(id, label, value.getComment(),
                        () -> value.get().booleanValue() ? 1 : 0,
                        v -> value.set(Boolean.valueOf(v != 0)))
                .withDefault(value.getDefault().booleanValue() ? 1 : 0);
    }

    private static Setting intSlider(final String id, final String label, final ConfigValue<Integer> value,
                                     final int step, final IntFunction<String> format) {
        final int min = ((Number) value.getMin()).intValue();
        final int max = ((Number) value.getMax()).intValue();
        return Setting.slider(id, label, value.getComment(), min, max, step, format,
                        () -> value.get().intValue(),
                        v -> value.set(Integer.valueOf(v)))
                .withDefault(value.getDefault().intValue());
    }

    /** A boolean row backed by vanilla getters/setters (no default: Reset leaves vanilla alone). */
    private static Setting flag(final String id, final String label, final String description,
                                final BoolGetter getter, final BoolSetter setter) {
        final IntSupplier reader = () -> getter.get() ? 1 : 0;
        final IntConsumer writer = v -> setter.set(v != 0);
        return Setting.toggle(id, label, description, reader, writer);
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    interface BoolGetter {
        boolean get();
    }

    interface BoolSetter {
        void set(boolean value);
    }
}
