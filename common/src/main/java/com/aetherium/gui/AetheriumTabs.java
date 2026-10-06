package com.aetherium.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigValue;
import com.aetherium.gui.widget.AetheriumWidgets;
import com.aetherium.render.gl.GlDevice;
import com.aetherium.render.mesh.ChunkMeshScheduler;
import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/**
 * The seven sidebar tabs, built from the live config object.
 *
 * <p>Every row is bound directly to a {@link ConfigValue}: the widget reads through
 * {@code get()} and writes through {@code set(...)}, and the owning screen calls
 * {@link #afterChange()} after each commit. That is the whole "live applied" design —
 * there is no staging copy, no "Apply all", and therefore no class of bug where the
 * GUI shows one thing and the engine does another.</p>
 *
 * <p>Adding an option to a version port is: declare the field in
 * {@code AetheriumConfig}, add one line here. The schema, the lang keys, and the
 * tab are then automatic, because the schema exporter walks the same reflection the
 * config uses.</p>
 */
public final class AetheriumTabs {
    /** Sidebar identity: label key, icon glyph, and the config group it maps to. */
    public record Tab(String id, String labelKey, String glyph, String group, boolean androidOnly) {
    }

    public static final Tab GENERAL = new Tab("general", "aetherium.tab.general", "\u2726", "general", false);
    public static final Tab PERFORMANCE = new Tab("performance", "aetherium.tab.performance", "\u26A1", "performance", false);
    public static final Tab QUALITY = new Tab("quality", "aetherium.tab.quality", "\u25C6", "quality", false);
    public static final Tab SHADERS = new Tab("shaders", "aetherium.tab.shaders", "\u2600", "shaders", false);
    public static final Tab UTILITIES = new Tab("utilities", "aetherium.tab.utilities", "\u269B", "utilities", false);
    public static final Tab ADVANCED = new Tab("advanced", "aetherium.tab.advanced", "\u2699", "advanced", false);
    public static final Tab ANDROID = new Tab("android", "aetherium.tab.android", "\u25B2", "android", true);

    public static final List<Tab> ALL = List.of(GENERAL, PERFORMANCE, QUALITY, SHADERS, UTILITIES, ADVANCED, ANDROID);

    private AetheriumTabs() {
    }

    /** Tabs visible on this platform; the Android row hides itself on desktop. */
    public static List<Tab> visibleTabs() {
        final boolean android = Aetherium.config().androidSupport.get() && isAndroidHost();
        final List<Tab> out = new ArrayList<>(ALL.size());
        for (final Tab tab : ALL) {
            if (!tab.androidOnly() || android) {
                out.add(tab);
            }
        }
        return out;
    }

    private static boolean isAndroidHost() {
        final String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("android") || System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT).contains("android");
    }

    /**
     * Builds the widgets for one tab.
     *
     * @param parent  the screen, used for "open shader screen" style actions
     * @param x       content left edge
     * @param y       content top edge
     * @param width   content width
     * @param rowY    out-param: the caller receives the next free row through this
     *                single-element array (a second one would mean a second object)
     */
    public static List<AbstractWidget> build(final AetheriumVideoOptionsScreen parent, final String tabId,
                                             final int x, final int y, final int width, final int[] rowY) {
        Objects.requireNonNull(parent, "parent");
        final AetheriumConfig config = Aetherium.config();
        final AetheriumTheme theme = parent.theme();
        final List<AbstractWidget> out = new ArrayList<>(24);
        switch (tabId) {
            case "general":
                buildGeneral(parent, theme, config, x, y, width, rowY, out);
                break;
            case "performance":
                buildPerformance(parent, theme, config, x, y, width, rowY, out);
                break;
            case "quality":
                buildQuality(parent, theme, config, x, y, width, rowY, out);
                break;
            case "shaders":
                buildShaders(parent, theme, config, x, y, width, rowY, out);
                break;
            case "utilities":
                buildUtilities(parent, theme, config, x, y, width, rowY, out);
                break;
            case "android":
                buildAndroid(parent, theme, config, x, y, width, rowY, out);
                break;
            case "advanced":
            default:
                buildAdvanced(parent, theme, config, x, y, width, rowY, out);
                break;
        }
        return out;
    }

    // ------------------------------------------------------------------ sections

    private static void buildGeneral(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                     final AetheriumConfig config, final int x, final int y, final int width,
                                     final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.renderer_mode");
        out.add(toggle(theme, config.enabled, x, nextRow(rowY, width),
                () -> {
                    // The single most important interaction in the mod: Aetherium <->
                    // Compatibility, live. Nothing else may depend on a restart.
                    parent.markNeedsHotSwap();
                    Aetherium.store().requestSave();
                }));
        addNote(theme, x, rowY, out, config.enabled.get()
                ? "Aetherium renderer active: " + backendSummary()
                : "Compatibility mode: vanilla renders the world; utilities (gamma, lights, GUI) stay on.");

        addHeader(theme, x, rowY, out, "aetherium.section.overlay");
        out.add(toggle(theme, config.showFrameHud, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.showFrameGraph, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.showBackendTag, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(cycle(theme, "aetherium.option.general.hud.corner", new String[]{"top-left", "top-right", "bottom-left", "bottom-right"},
                () -> indexOf(config.hudCorner.get()), index -> config.hudCorner.set(new String[]{"top-left", "top-right", "bottom-left", "bottom-right"}[index]),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(toggle(theme, config.notifyConflicts, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));

        addHeader(theme, x, rowY, out, "aetherium.section.conflicts");
        addNote(theme, x, rowY, out, Aetherium.conflicts().hasAnything()
                ? "Detected: " + Aetherium.conflicts().summarize()
                : "No conflicting renderer mods detected.");
        out.add(action(theme, "aetherium.button.write_conflict_report", x, nextRow(rowY, width) - 2, 150,
                parent::writeConflictReport, AetheriumTheme.TEXT));
    }

    private static void buildPerformance(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                         final AetheriumConfig config, final int x, final int y, final int width,
                                         final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.backend");
        out.add(cycle(theme, "aetherium.option.performance.backend",
                new String[]{"auto", "gl46_dsa", "vulkan_13", "gl_core", "gl_legacy", "compatibility"},
                () -> config.backend.get().ordinal(),
                index -> {
                    final AetheriumConfig.BackendChoice[] choices = AetheriumConfig.BackendChoice.values();
                    config.backend.set(choices[MathUtil.clamp(index, 0, choices.length - 1)]);
                },
                () -> {
                    // Backend changes are the one case that needs a safe point; the
                    // screen shows an "Apply now" affordance until it lands.
                    parent.markNeedsHotSwap();
                    Aetherium.store().requestSave();
                }, x, nextRow(rowY, width)));
        addNote(theme, x, rowY, out, "Resolved now: " + backendSummary());

        addHeader(theme, x, rowY, out, "aetherium.section.meshing");
        out.add(toggle(theme, config.asyncMeshing, x, nextRow(rowY, width), () -> {
            final ChunkMeshScheduler scheduler = ClientHooks.scheduler();
            if (scheduler != null) {
                scheduler.onConfigurationChanged();
            }
            Aetherium.store().requestSave();
        }));
        out.add(toggle(theme, config.persistentBuffers, x, nextRow(rowY, width), () -> {
            parent.markNeedsHotSwap();
            Aetherium.store().requestSave();
        }));
        out.add(toggle(theme, config.indirectDraw, x, nextRow(rowY, width), () -> {
            parent.markNeedsHotSwap();
            Aetherium.store().requestSave();
        }));
        out.add(toggle(theme, config.hzb, x, nextRow(rowY, width), () -> {
            parent.markNeedsHotSwap();
            Aetherium.store().requestSave();
        }));
        out.add(slider(theme, "aetherium.option.performance.hzb_levels", 1, 12, "%d",
                () -> config.hzbDepth.get(), value -> config.hzbDepth.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.performance.mesh_workers", 0, 16, "%d workers (0 = auto)",
                () -> config.meshWorkers.get(), value -> config.meshWorkers.set(value.intValue()),
                () -> {
                    final ChunkMeshScheduler scheduler = ClientHooks.scheduler();
                    if (scheduler != null) {
                        scheduler.onConfigurationChanged();
                    }
                    Aetherium.store().requestSave();
                }, x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.performance.upload_budget", 1024, 131072, "%d KB/frame",
                () -> config.uploadBudgetKb.get(), value -> config.uploadBudgetKb.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.performance.target_fps", 0, 480, "%s",
                () -> config.targetFps.get(), value -> config.targetFps.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));

        addHeader(theme, x, rowY, out, "aetherium.section.shaders_compile");
        out.add(toggle(theme, config.asyncShaderCompile, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.programBinaryCache, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(action(theme, "aetherium.button.clear_program_cache", x, nextRow(rowY, width) - 2, 150, () -> {
            final GlDevice device = ClientHooks.device();
            final int cleared = device == null || device.getProgramCache() == null
                    ? -1 : device.getProgramCache().clearAll();
            parent.setStatus(cleared < 0 ? "No device yet: nothing to clear" : "Cleared " + cleared + " cached programs");
        }, AetheriumTheme.WARNING));
    }

    private static void buildQuality(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                     final AetheriumConfig config, final int x, final int y, final int width,
                                     final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.vanilla_equivalents");
        out.add(cycle(theme, "aetherium.option.quality.fog", new String[]{"off", "fast", "fancy"},
                () -> config.fogQuality.get().ordinal(),
                index -> config.fogQuality.set(AetheriumConfig.Ternary.values()[MathUtil.clamp(index, 0, 2)]),
                parent::markNeedsHotSwap, x, nextRow(rowY, width)));
        out.add(cycle(theme, "aetherium.option.quality.clouds", new String[]{"off", "fast", "fancy"},
                () -> config.cloudQuality.get().ordinal(),
                index -> config.cloudQuality.set(AetheriumConfig.Ternary.values()[MathUtil.clamp(index, 0, 2)]),
                parent::markNeedsHotSwap, x, nextRow(rowY, width)));
        out.add(cycle(theme, "aetherium.option.quality.weather", new String[]{"off", "fast", "fancy"},
                () -> config.weatherQuality.get().ordinal(),
                index -> config.weatherQuality.set(AetheriumConfig.Ternary.values()[MathUtil.clamp(index, 0, 2)]),
                parent::markNeedsHotSwap, x, nextRow(rowY, width)));
        out.add(toggle(theme, config.smoothLighting, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.biomeBlend, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.entityCulling, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.quality.max_direct_light", 0, 15, "%d",
                () -> config.maxDirectLight.get(), value -> config.maxDirectLight.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        addNote(theme, x, rowY, out, "Render distance, FOV, mipmap levels and particles stay in vanilla's own "
                + "Video Settings: Aetherium does not duplicate them, so there is only ever one place to change them.");
    }

    private static void buildShaders(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                     final AetheriumConfig config, final int x, final int y, final int width,
                                     final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.iris");
        addNote(theme, x, rowY, out, Aetherium.iris().describe());
        out.add(toggle(theme, config.irisIntegration, x, nextRow(rowY, width), () -> {
            Aetherium.iris().bind();
            Aetherium.store().requestSave();
        }));
        out.add(toggle(theme, config.pauseDynamicLights, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.reloadShadersOnWorldChange, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        final boolean shaderScreen = Aetherium.iris().isPresent();
        out.add(action(theme, "aetherium.button.open_shader_screen", x, nextRow(rowY, width) - 2, 170, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (!Aetherium.iris().openShaderScreen(parent)) {
                parent.setStatus("No Iris/Oculus shader screen is available (is a shader mod installed?)");
                return;
            }
            if (minecraft != null) {
                parent.setStatus("Opened the shader screen");
            }
        }, shaderScreen ? AetheriumTheme.ACCENT : AetheriumTheme.TEXT_DISABLED));
        out.add(action(theme, "aetherium.button.toggle_shaders", x, nextRow(rowY, width) + 18, 150, () -> {
            final boolean target = !Aetherium.iris().areShadersEnabled();
            final boolean ok = Aetherium.iris().setShadersEnabled(target, "GUI toggle");
            parent.setStatus(ok ? "Shaders " + (target ? "enabled" : "disabled") + " via the Iris API"
                    : "Could not toggle shaders: no Iris/Oculus config API");
        }, AetheriumTheme.TEXT));
        addNote(theme, x, rowY, out, "Sun path rotation read from Iris: "
                + String.format(Locale.ROOT, "%.1f\u00B0", Aetherium.iris().getSunPathRotation()));
    }

    private static void buildUtilities(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                       final AetheriumConfig config, final int x, final int y, final int width,
                                       final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.gamma");
        out.add(toggle(theme, config.gammaEnabled, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.utilities.gamma.amount", 0.0, 20.0, "%.2fx",
                () -> config.gammaAmount.get(), value -> config.gammaAmount.set(value),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(toggle(theme, config.caveVision, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.utilities.gamma.cave_vision_floor", 0, 15, "%d",
                () -> config.caveVisionFloor.get(), value -> config.caveVisionFloor.set(value),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(toggle(theme, config.nightVisionBoost, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.utilities.gamma.night_vision_level", 0, 15, "%d",
                () -> config.nightVisionLevel.get(), value -> config.nightVisionLevel.set(value),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(toggle(theme, config.timeBasedGamma, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.utilities.gamma.time_based_night", 0.5, 20.0, "%.2fx",
                () -> config.gammaNightTarget.get(), value -> config.gammaNightTarget.set(value),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));

        addHeader(theme, x, rowY, out, "aetherium.section.gamma_curve");
        // The curve editor is a text-bound strip plus a live preview of the response:
        // a graphical drag editor needs a per-version render path, and a curve you
        // cannot read is worse than a number you can.
        out.add(slider(theme, "aetherium.option.gamma_curve_point1", 0.0, 1.0, "%.3f",
                () -> curvePoint(1), value -> setCurvePoint(1, value, config),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.gamma_curve_point2", 0.0, 1.0, "%.3f",
                () -> curvePoint(2), value -> setCurvePoint(2, value, config),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.gamma_curve_point3", 0.0, 1.0, "%.3f",
                () -> curvePoint(3), value -> setCurvePoint(3, value, config),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        addNote(theme, x, rowY, out, "Curve: " + Aetherium.gamma().describeCurve()
                + (Aetherium.gamma().getParseError() == null ? "" : "  (error: " + Aetherium.gamma().getParseError() + ')'));
        addNote(theme, x, rowY, out, "Lightmap access: " + com.aetherium.gamma.LightmapWriter.describeShape());

        addHeader(theme, x, rowY, out, "aetherium.section.dynamic_lights");
        out.add(toggle(theme, config.dynamicLights, x, nextRow(rowY, width), () -> {
            if (!config.dynamicLights.get()) {
                Aetherium.lights().clear();
            }
            Aetherium.store().requestSave();
        }));
        out.add(slider(theme, "aetherium.option.utilities.dynamic_lights.quality", 0, 3, "%d",
                () -> config.dynamicLightsQuality.get(), value -> config.dynamicLightsQuality.set(value.intValue()),
                () -> {
                    Aetherium.lights().invalidateCache();
                    Aetherium.store().requestSave();
                }, x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.utilities.dynamic_lights.range", 4, 15, "%d blocks",
                () -> config.dynamicLightsRange.get(), value -> config.dynamicLightsRange.set(value.intValue()),
                () -> {
                    Aetherium.lights().invalidateCache();
                    Aetherium.store().requestSave();
                }, x, nextRow(rowY, width)));
        out.add(toggle(theme, config.dynamicLightsColored, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.dynamicLightsEntities, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.utilities.dynamic_lights.intensity", 0.1, 2.0, "%.2fx",
                () -> config.dynamicLightsIntensity.get(), value -> config.dynamicLightsIntensity.set(value),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        addNote(theme, x, rowY, out, Aetherium.lights().describe());
    }

    private static void buildAndroid(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                     final AetheriumConfig config, final int x, final int y, final int width,
                                     final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.android_detection");
        addNote(theme, x, rowY, out, "Launcher and renderer: " + com.aetherium.android.AndroidEnvironment
                .probe(config).describe());
        out.add(toggle(theme, config.androidSupport, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.readCustomEnv, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(cycle(theme, "aetherium.option.android.force_renderer",
                new String[]{"auto", "gl4es", "zink", "ltw", "mobileglues", "virgl", "angle", "native_vulkan"},
                () -> config.androidForceRenderer.get().ordinal(),
                index -> config.androidForceRenderer.set(AetheriumConfig.AndroidRendererChoice.values()[MathUtil.clamp(index, 0, 7)]),
                () -> {
                    parent.markNeedsHotSwap();
                    Aetherium.store().requestSave();
                }, x, nextRow(rowY, width)));

        addHeader(theme, x, rowY, out, "aetherium.section.android_power");
        out.add(toggle(theme, config.mobileMemoryMode, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.batterySaver, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.thermalThrottle, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(slider(theme, "aetherium.option.android.memory_budget_mb", 256, 8192, "%d MB",
                () -> config.memoryBudgetMb.get(), value -> config.memoryBudgetMb.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(slider(theme, "aetherium.option.android.thermal_ceiling_c", 40, 95, "%d \u00B0C",
                () -> config.thermalCeilingC.get(), value -> config.thermalCeilingC.set(value.intValue()),
                () -> Aetherium.store().requestSave(), x, nextRow(rowY, width)));
        out.add(toggle(theme, config.touchMode, x, nextRow(rowY, width), () -> {
            // Rebuilding the screen is the only way to re-derive hit targets.
            parent.rebuildForLayout();
            Aetherium.store().requestSave();
        }));
        final var governor = ClientHooks.governor();
        addNote(theme, x, rowY, out, governor == null ? "Power governor unavailable" : governor.describe());
        addNote(theme, x, rowY, out, "ABI: arm64-v8a only. armeabi-v7a is not built - a 32-bit process cannot "
                + "address the arenas the persistent-mapping path needs (docs/ANDROID.md).");
    }

    private static void buildAdvanced(final AetheriumVideoOptionsScreen parent, final AetheriumTheme theme,
                                      final AetheriumConfig config, final int x, final int y, final int width,
                                      final int[] rowY, final List<AbstractWidget> out) {
        addHeader(theme, x, rowY, out, "aetherium.section.diagnostics");
        out.add(toggle(theme, config.debugLogging, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.glErrors, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.strictMixins, x, nextRow(rowY, width), () -> {
            parent.setStatus("strict_mixins takes effect on the next launch (mixin config is read at transform time)");
            Aetherium.store().requestSave();
        }));
        out.add(toggle(theme, config.failFastUnsupportedGpu, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));
        out.add(toggle(theme, config.conflictAutoDelegate, x, nextRow(rowY, width), () -> Aetherium.store().requestSave()));

        addHeader(theme, x, rowY, out, "aetherium.section.experimental");
        // Stated before the widget, not only in its tooltip: an option that does nothing
        // must not look like an option that works. Aetherium.initialize refuses this switch
        // at runtime and downgrades to shadow mode; the row says so on screen.
        addNote(theme, x, rowY, out, "NOT SHIPPED - refused at startup, falls back to shadow (Compatibility) mode.");
        out.add(toggle(theme, config.experimentalFullRenderer, x, nextRow(rowY, width), () -> {
            parent.setStatus("Full-replacement mode is refused at runtime; see README 'Honest status'. "
                    + "Running in shadow mode.");
            Aetherium.store().requestSave();
        }));
        addNote(theme, x, rowY, out, "Shadow mode means Aetherium builds and culls its own command stream and measures it, "
                + "while vanilla still draws the world. That is what makes the numbers comparable today.");
        addNote(theme, x, rowY, out, com.aetherium.mixin.core.OptionsScreenMixin.describeHijack());
        final GlDevice device = ClientHooks.device();
        if (device != null) {
            addNote(theme, x, rowY, out, device.describe() + " | " + device.getCapabilities().describe());
            addNote(theme, x, rowY, out, String.format(Locale.ROOT,
                    "frames=%d, gl_errors=%d, %s", device.getFramesRecorded(), device.getGlErrorsObserved(),
                    device.getProgramCache() == null ? "no program cache" : device.getProgramCache().describe()));
        }
        final ChunkMeshScheduler scheduler = ClientHooks.scheduler();
        if (scheduler != null) {
            addNote(theme, x, rowY, out, scheduler.describe());
            addNote(theme, x, rowY, out, String.format(Locale.ROOT, "avg build %.2f ms, %d budget-exhausted frames",
                    scheduler.getAverageBuildMs(), scheduler.getUploadBudgetExhaustedFrames()));
        }
    }

    // ------------------------------------------------------------------- helpers

    /** @return the y for the next row, advancing the caller's cursor */
    private static int nextRow(final int[] rowY, final int width) {
        final int y = rowY[0];
        rowY[0] = y + width * 0 + 24;
        return y;
    }

    private static void addHeader(final AetheriumTheme theme, final int x, final int[] rowY,
                                  final List<AbstractWidget> out, final String key) {
        rowY[0] += 6;
        out.add(new HeaderWidget(theme, x, rowY[0], key));
        rowY[0] += 14;
    }

    private static void addNote(final AetheriumTheme theme, final int x, final int[] rowY,
                                final List<AbstractWidget> out, final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        out.add(new NoteWidget(theme, x, rowY[0], text));
        rowY[0] += 11 * (1 + text.length() / 90);
    }

    private static AbstractWidget toggle(final AetheriumTheme theme, final ConfigValue<Boolean> option, final int x,
                                          final int y, final Runnable afterChange) {
        return new AetheriumWidgets.ToggleWidget(theme, x, y, 300, theme.rowHeight(), option, null, afterChange);
    }

    private static AbstractWidget slider(final AetheriumTheme theme, final String labelKey, final double min, final double max,
                                          final String format, final java.util.function.DoubleSupplier getter,
                                          final java.util.function.DoubleConsumer setter, final Runnable afterChange,
                                          final int x, final int y) {
        return new AetheriumWidgets.SliderWidget(theme, x, y, 300, theme.rowHeight(), Component.translatable(labelKey).getString(),
                min, max, getter, value -> {
                    setter.accept(value);
                    afterChange.run();
                }, format, null);
    }

    private static AbstractWidget cycle(final AetheriumTheme theme, final String labelKey, final String[] values,
                                         final java.util.function.Supplier<Integer> getter,
                                         final AetheriumWidgets.CycleWidget.IntConsumer setter, final Runnable afterChange,
                                         final int x, final int y) {
        return new AetheriumWidgets.CycleWidget(theme, x, y, 300, theme.rowHeight(),
                Component.translatable(labelKey).getString(), values, () -> {
                    final int raw = getter.get();
                    return raw < 0 ? 0 : raw;
                }, value -> {
                    setter.accept(value);
                    afterChange.run();
                }, null);
    }

    private static AbstractWidget action(final AetheriumTheme theme, final String labelKey, final int x, final int y,
                                          final int width, final Runnable action, final int accent) {
        return new AetheriumWidgets.ActionWidget(theme, x, y, width, theme.rowHeight() - 2,
                Component.translatable(labelKey), action, () -> true, accent);
    }

    private static String backendSummary() {
        final GlDevice device = ClientHooks.device();
        if (device != null) {
            return device.getBackend().getId() + " (" + device.getCapabilities().getGpu().describe() + ')';
        }
        return Aetherium.config().backend.get().name().toLowerCase(Locale.ROOT) + " (no live device yet)";
    }

    private static int indexOf(final String value) {
        final String[] options = {"top-left", "top-right", "bottom-left", "bottom-right"};
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private static double curvePoint(final int index) {
        final String spec = Aetherium.config().gammaCurve.get();
        final String[] points = spec.split(",");
        if (index >= points.length) {
            return 0.0;
        }
        final String[] pair = points[index].split(":");
        try {
            return Double.parseDouble(pair[1].trim());
        } catch (final IndexOutOfBoundsException | NumberFormatException error) {
            return 0.0;
        }
    }

    private static void setCurvePoint(final int index, final double value, final AetheriumConfig config) {
        final String[] points = config.gammaCurve.get().split(",");
        if (index >= points.length) {
            return;
        }
        final String[] pair = points[index].split(":");
        if (pair.length != 2) {
            return;
        }
        points[index] = String.format(Locale.ROOT, "%s:%.3f", pair[0].trim(), MathUtil.clamp(value, 0.0, 1.0));
        config.gammaCurve.set(String.join(",", points));
    }

    /** Section header row: not a control, but it must scroll with the list. */
    static final class HeaderWidget extends AbstractWidget {
        private final AetheriumTheme theme;
        private final String key;

        HeaderWidget(final AetheriumTheme theme, final int x, final int y, final String key) {
            super(x, y, 300, 12, Component.translatable(key));
            this.theme = theme;
            this.key = key;
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.font != null) {
                guiGraphics.drawString(minecraft.font, Component.translatable(this.key)
                        .withStyle(style -> style.withColor(AetheriumTheme.ACCENT & 0xFFFFFF)), this.getX(), this.getY(),
                        AetheriumTheme.ACCENT, false);
            }
            guiGraphics.fill(this.getX(), this.getY() + 11, this.getX() + this.getWidth(), this.getY() + 12, AetheriumTheme.BORDER);
        }

        @Override
        protected void updateNarration(final net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    /** Wrapping note text; {@code text} is measured, never truncated mid-word. */
    static final class NoteWidget extends AbstractWidget {
        private final AetheriumTheme theme;
        private final String text;

        NoteWidget(final AetheriumTheme theme, final int x, final int y, final String text) {
            super(x, y, 300, 10, Component.literal(text));
            this.theme = theme;
            this.text = text;
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            this.theme.drawLabel(guiGraphics, this.text, this.getX(), this.getY(), AetheriumTheme.TEXT_DIM);
        }

        @Override
        protected void updateNarration(final net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
