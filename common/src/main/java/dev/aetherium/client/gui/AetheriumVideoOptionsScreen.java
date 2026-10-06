package dev.aetherium.client.gui;

import dev.aetherium.android.AndroidLauncherCompat;
import dev.aetherium.chunk.AetheriumChunkBuilder;
import dev.aetherium.client.gui.widget.GammaCurveEditor;
import dev.aetherium.client.gui.widget.PurpleButton;
import dev.aetherium.client.gui.widget.PurpleCycle;
import dev.aetherium.client.gui.widget.PurpleInfo;
import dev.aetherium.client.gui.widget.PurpleSlider;
import dev.aetherium.client.gui.widget.PurpleToggle;
import dev.aetherium.client.gui.widget.PurpleWidget;
import dev.aetherium.client.render.EntityCuller;
import dev.aetherium.client.render.LiveSettingsApplier;
import dev.aetherium.compat.ConflictDetector;
import dev.aetherium.compat.IrisCompat;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.AetheriumRenderEngine;
import dev.aetherium.frame.FrameTimeTracker;
import dev.aetherium.gui.OptionCategory;
import dev.aetherium.gui.anim.Animator;
import dev.aetherium.gui.anim.ParticleField;
import dev.aetherium.gui.theme.PurpleTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Replacement for VideoSettingsScreen. Sidebar of categories on the left, scrollable settings panel on the right,
 * live FPS/frame-time preview, purple morph transition on open and on tab change. Every control writes straight to
 * {@link AetheriumConfig} and calls {@link #apply()} so changes take effect immediately.
 */
public class AetheriumVideoOptionsScreen extends Screen {
    private static final int SIDEBAR_W = 118;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 4;
    private static final int PAD = 10;

    private final Screen parent;
    private final AetheriumConfig cfg = AetheriumConfig.get();
    private OptionCategory category = OptionCategory.GENERAL;

    private final Animator openAnim = new Animator(0f).easing(Animator.Easing.OUT_QUINT);
    private final Animator panelAnim = new Animator(1f).easing(Animator.Easing.OUT_CUBIC);
    private final Animator scrollAnim = new Animator(0f).easing(Animator.Easing.OUT_CUBIC);
    private final ParticleField particles = new ParticleField();
    private final List<PurpleWidget> panelWidgets = new ArrayList<>();
    private final List<PurpleButton> sidebarButtons = new ArrayList<>();
    private int contentHeight;
    private boolean touch;
    private double lastDragY;

    public AetheriumVideoOptionsScreen(Screen parent) {
        super(Component.literal("Aetherium Video Settings"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ layout

    private int panelX() { return SIDEBAR_W + PAD; }
    private int panelY() { return 28; }
    private int panelW() { return width - panelX() - PAD; }
    private int panelH() { return height - panelY() - 34; }
    private int rowH() { return touch ? Math.max(ROW_H, AndroidLauncherCompat.touchMinHitPx(minecraft.options.guiScale().get())) : ROW_H; }

    @Override
    protected void init() {
        touch = AndroidLauncherCompat.isAndroid() && cfg.android.touchOptimization;
        openAnim.snap(0f);
        openAnim.animateTo(1f, 420);
        particles.burst(width / 2f, height / 2f, width * 0.8f, height * 0.6f, 60);
        buildSidebar();
        rebuildPanel(false);
    }

    private void buildSidebar() {
        sidebarButtons.clear();
        int y = panelY();
        int h = rowH() + 2;
        for (OptionCategory c : OptionCategory.values()) {
            if (c == OptionCategory.ANDROID && !AndroidLauncherCompat.isAndroid()) continue;
            final OptionCategory cat = c;
            PurpleButton b = new PurpleButton(PAD, y, SIDEBAR_W - PAD * 2, h, Component.translatableWithFallback(c.translationKey, c.fallback), () -> switchCategory(cat));
            sidebarButtons.add(b);
            addRenderableWidget(b);
            y += h + 3;
        }
        int by = height - 28;
        addRenderableWidget(new PurpleButton(PAD, by, SIDEBAR_W - PAD * 2, rowH(), Component.translatable("gui.done"), this::onClose));
        addRenderableWidget(new PurpleButton(panelX(), by, 120, rowH(),
                Component.literal(cfg.advanced.engineMode == AetheriumConfig.EngineMode.AETHERIUM ? "Engine: Aetherium" : "Engine: Compatibility"), () -> {
                    cfg.advanced.engineMode = cfg.advanced.engineMode == AetheriumConfig.EngineMode.AETHERIUM
                            ? AetheriumConfig.EngineMode.COMPATIBILITY : AetheriumConfig.EngineMode.AETHERIUM;
                    apply();
                    rebuildAll();
                }));
    }

    private void switchCategory(OptionCategory c) {
        if (c == category) return;
        category = c;
        panelAnim.snap(0f);
        panelAnim.animateTo(1f, 260);
        particles.burst(panelX() + panelW() / 2f, panelY() + panelH() / 2f, panelW() * 0.9f, panelH() * 0.6f, 36);
        scrollAnim.snap(0f);
        rebuildPanel(true);
    }

    private void rebuildAll() {
        clearWidgets();
        buildSidebar();
        rebuildPanel(false);
    }

    private void rebuildPanel(boolean removeOld) {
        if (removeOld) for (PurpleWidget w : panelWidgets) removeWidget(w);
        panelWidgets.clear();
        List<PurpleWidget> rows = new ArrayList<>();
        int w = panelW() - PAD * 2 - 6;
        switch (category) {
            case GENERAL -> buildGeneral(rows, w);
            case PERFORMANCE -> buildPerformance(rows, w);
            case QUALITY -> buildQuality(rows, w);
            case SHADERS -> buildShaders(rows, w);
            case UTILITIES -> buildUtilities(rows, w);
            case ADVANCED -> buildAdvanced(rows, w);
            case ANDROID -> buildAndroid(rows, w);
        }
        int y = 0;
        for (PurpleWidget r : rows) {
            r.setX(panelX() + PAD);
            r.setY(panelY() + PAD + y);
            y += r.getHeight() + ROW_GAP;
            panelWidgets.add(r);
            addRenderableWidget(r);
        }
        contentHeight = y;
    }

    // ------------------------------------------------------------------ categories

    private void buildGeneral(List<PurpleWidget> rows, int w) {
        rows.add(slider(w, "Render Distance", 2, 32, 1, () -> cfg.general.renderDistance, v -> cfg.general.renderDistance = (int) v, v -> (int) v + " chunks"));
        rows.add(slider(w, "Simulation Distance", 5, 32, 1, () -> cfg.general.simulationDistance, v -> cfg.general.simulationDistance = (int) v, v -> (int) v + " chunks"));
        rows.add(slider(w, "GUI Scale", 0, 6, 1, () -> cfg.general.guiScale, v -> cfg.general.guiScale = (int) v, v -> v == 0 ? "Auto" : String.valueOf((int) v)));
        rows.add(toggle(w, "Fullscreen", () -> cfg.general.fullscreen, v -> cfg.general.fullscreen = v));
        rows.add(toggle(w, "VSync", () -> cfg.general.vsync, v -> cfg.general.vsync = v));
    }

    private void buildPerformance(List<PurpleWidget> rows, int w) {
        rows.add(slider(w, "Chunk Builder Threads", 0, 16, 1, () -> cfg.performance.chunkBuilderThreads, v -> cfg.performance.chunkBuilderThreads = (int) v,
                v -> v == 0 ? "Auto (" + AetheriumChunkBuilder.autoThreads() + ")" : String.valueOf((int) v)));
        rows.add(cycle(w, "Entity Culling", AetheriumConfig.EntityCullingMode.values(), () -> cfg.performance.entityCulling, v -> cfg.performance.entityCulling = v));
        rows.add(toggle(w, "Fog Occlusion", () -> cfg.performance.fogOcclusion, v -> cfg.performance.fogOcclusion = v));
        rows.add(toggle(w, "Fast Math", () -> cfg.performance.fastMath, v -> cfg.performance.fastMath = v));
        rows.add(toggle(w, "Dynamic Entity Batching", () -> cfg.performance.dynamicEntityBatching, v -> cfg.performance.dynamicEntityBatching = v));
        rows.add(toggle(w, "Async Chunk Meshing", () -> cfg.performance.asyncChunkMeshing, v -> cfg.performance.asyncChunkMeshing = v));
        rows.add(toggle(w, "HZB Occlusion Culling", () -> cfg.performance.hzbOcclusionCulling, v -> cfg.performance.hzbOcclusionCulling = v));
        rows.add(toggle(w, "GPU-Driven Rendering", () -> cfg.performance.gpuDrivenRendering, v -> cfg.performance.gpuDrivenRendering = v));
        rows.add(toggle(w, "Persistent Mapped Buffers", () -> cfg.performance.persistentMappedBuffers, v -> cfg.performance.persistentMappedBuffers = v));
        rows.add(toggle(w, "Multi-Draw Indirect", () -> cfg.performance.multiDrawIndirect, v -> cfg.performance.multiDrawIndirect = v));
    }

    private void buildQuality(List<PurpleWidget> rows, int w) {
        rows.add(slider(w, "Mipmap Levels", 0, 4, 1, () -> cfg.quality.mipmapLevels, v -> cfg.quality.mipmapLevels = (int) v, v -> String.valueOf((int) v)));
        rows.add(cycle(w, "Anisotropic Filtering", new Integer[]{1, 2, 4, 8, 16}, () -> cfg.quality.anisotropicFiltering, v -> cfg.quality.anisotropicFiltering = v, v -> v == 1 ? "Off" : v + "x"));
        rows.add(cycle(w, "Texture Filtering", AetheriumConfig.TextureFiltering.values(), () -> cfg.quality.textureFiltering, v -> cfg.quality.textureFiltering = v));
        rows.add(toggle(w, "Smooth Lighting", () -> cfg.quality.smoothLighting, v -> cfg.quality.smoothLighting = v));
        rows.add(slider(w, "Biome Blend", 0, 7, 1, () -> cfg.quality.biomeBlend, v -> cfg.quality.biomeBlend = (int) v, v -> v == 0 ? "Off" : (int) (v * 2 + 1) + "x" + (int) (v * 2 + 1)));
        rows.add(cycle(w, "Cloud Quality", AetheriumConfig.CloudQuality.values(), () -> cfg.quality.cloudQuality, v -> cfg.quality.cloudQuality = v));
        rows.add(cycle(w, "Particle Density", AetheriumConfig.ParticleDensity.values(), () -> cfg.quality.particleDensity, v -> cfg.quality.particleDensity = v));
    }

    private void buildShaders(List<PurpleWidget> rows, int w) {
        IrisCompat iris = IrisCompat.get();
        rows.add(info(w, "Shader Provider", () -> iris.isPresent() ? iris.flavor() : "Not installed"));
        PurpleToggle compat = toggle(w, "Iris Compatibility", () -> cfg.shaders.irisCompatibility, v -> cfg.shaders.irisCompatibility = v);
        compat.active = iris.isPresent();
        rows.add(compat);
        PurpleToggle enabled = toggle(w, "Shaders Enabled", iris::areShadersEnabled, iris::setShadersEnabled);
        enabled.active = iris.isPresent() && cfg.shaders.irisCompatibility;
        rows.add(enabled);
        PurpleButton pack = new PurpleButton(0, 0, w, rowH(), Component.literal("Shader Pack Selector…"), () ->
                iris.createShaderPackScreen(this).ifPresent(s -> minecraft.setScreen((Screen) s)));
        pack.active = iris.isPresent();
        pack.setTooltip(Tooltip.create(Component.literal(iris.isPresent() ? "Opens the " + iris.flavor() + " pack browser" : "Install Iris (Fabric/NeoForge) or Oculus (Forge)")));
        rows.add(pack);
        PurpleToggle dyn = toggle(w, "Dynamic Lights in Shaders", () -> cfg.shaders.shaderDynamicLights, v -> cfg.shaders.shaderDynamicLights = v);
        dyn.active = iris.isPresent();
        rows.add(dyn);
    }

    private void buildUtilities(List<PurpleWidget> rows, int w) {
        rows.add(toggle(w, "Dynamic Lights", () -> cfg.utilities.dynamicLights, v -> cfg.utilities.dynamicLights = v));
        rows.add(cycle(w, "Dynamic Light Quality", AetheriumConfig.DynamicLightQuality.values(), () -> cfg.utilities.dynamicLightQuality, v -> cfg.utilities.dynamicLightQuality = v));
        rows.add(toggle(w, "Colored Dynamic Lights", () -> cfg.utilities.coloredDynamicLights, v -> cfg.utilities.coloredDynamicLights = v));
        rows.add(toggle(w, "Light: Entities", () -> cfg.utilities.dynamicLightsEntities, v -> cfg.utilities.dynamicLightsEntities = v));
        rows.add(toggle(w, "Light: Dropped Items", () -> cfg.utilities.dynamicLightsItems, v -> cfg.utilities.dynamicLightsItems = v));
        rows.add(toggle(w, "Light: Self", () -> cfg.utilities.dynamicLightsSelf, v -> cfg.utilities.dynamicLightsSelf = v));
        rows.add(toggle(w, "Gamma Utilities", () -> cfg.utilities.gammaUtilities, v -> cfg.utilities.gammaUtilities = v));
        rows.add(slider(w, "Brightness", 0, 10, 0.1, () -> cfg.utilities.brightness, v -> cfg.utilities.brightness = v, v -> String.format("%.0f%%", v * 100)));
        rows.add(toggle(w, "Night Vision", () -> cfg.utilities.nightVision, v -> cfg.utilities.nightVision = v));
        rows.add(toggle(w, "Cave Vision", () -> cfg.utilities.caveVision, v -> cfg.utilities.caveVision = v));
        rows.add(toggle(w, "Time-based Gamma", () -> cfg.utilities.timeBasedGamma, v -> cfg.utilities.timeBasedGamma = v));
        rows.add(toggle(w, "Custom Gamma Curve", () -> cfg.utilities.customGammaCurve, v -> cfg.utilities.customGammaCurve = v));
        GammaCurveEditor editor = new GammaCurveEditor(0, 0, w, 80, Component.literal("Gamma Curve Editor"), () -> cfg.utilities.gammaCurve, pts -> { cfg.utilities.gammaCurve = pts; apply(); });
        rows.add(editor);
    }

    private void buildAdvanced(List<PurpleWidget> rows, int w) {
        AetheriumRenderEngine engine = AetheriumRenderEngine.get();
        rows.add(info(w, "Active Backend", engine::statusLine));
        rows.add(info(w, "GPU", () -> engine.capabilities() == null ? "?" : engine.capabilities().renderer()));
        rows.add(cycle(w, "Graphics API", AetheriumConfig.GraphicsApi.values(), () -> cfg.advanced.graphicsApi, v -> cfg.advanced.graphicsApi = v));
        rows.add(cycle(w, "Engine Mode", AetheriumConfig.EngineMode.values(), () -> cfg.advanced.engineMode, v -> cfg.advanced.engineMode = v));
        rows.add(toggle(w, "Debug Overlay", () -> cfg.advanced.debugOverlay, v -> cfg.advanced.debugOverlay = v));
        rows.add(toggle(w, "Frame Time Graph", () -> cfg.advanced.frameTimeGraph, v -> cfg.advanced.frameTimeGraph = v));
        rows.add(slider(w, "Vertex Arena (MB)", 0, 2048, 64, () -> cfg.advanced.vertexArenaMb, v -> cfg.advanced.vertexArenaMb = (int) v, v -> v == 0 ? "Auto" : (int) v + " MB"));
        rows.add(toggle(w, "Shader Binary Cache", () -> cfg.advanced.shaderBinaryCache, v -> cfg.advanced.shaderBinaryCache = v));
        rows.add(toggle(w, "Auto-disable Conflicting Mods", () -> cfg.advanced.disableConflictingMods, v -> cfg.advanced.disableConflictingMods = v));
        if (!ConflictDetector.detected().isEmpty()) rows.add(info(w, "Detected Renderer Mods", () -> String.join(", ", ConflictDetector.detected())));
    }

    private void buildAndroid(List<PurpleWidget> rows, int w) {
        rows.add(info(w, "Detected Launcher", AndroidLauncherCompat::launcherDisplayName));
        rows.add(info(w, "Detected Renderer", () -> AndroidLauncherCompat.renderer() + (AndroidLauncherCompat.rendererRaw().isEmpty() ? "" : " (" + AndroidLauncherCompat.rendererRaw() + ")")));
        rows.add(info(w, "JVM Heap", () -> AndroidLauncherCompat.heapMiB() + " MiB" + (AndroidLauncherCompat.isArm64() ? " · arm64" : " · arm32")));
        rows.add(info(w, "Battery / Temperature", () -> {
            int b = AndroidLauncherCompat.batteryPercent(), t = AndroidLauncherCompat.deviceTemperatureC();
            return (b < 0 ? "?" : b + "%") + (AndroidLauncherCompat.isCharging() ? " ⚡" : "") + " / " + (t < 0 ? "?" : t + "°C");
        }));
        rows.add(cycle(w, "Aetherium Backend", AetheriumConfig.AndroidBackend.values(), () -> cfg.android.backend, v -> cfg.android.backend = v));
        rows.add(toggle(w, "Mobile Memory Mode", () -> cfg.android.mobileMemoryMode, v -> cfg.android.mobileMemoryMode = v));
        rows.add(toggle(w, "Touch Optimization", () -> cfg.android.touchOptimization, v -> { cfg.android.touchOptimization = v; apply(); rebuildAll(); }));
        rows.add(toggle(w, "Battery Saver Mode", () -> cfg.android.batterySaver, v -> cfg.android.batterySaver = v));
        rows.add(slider(w, "Battery Saver Min. Render Distance", 2, 16, 1, () -> cfg.android.batterySaverMinRenderDistance, v -> cfg.android.batterySaverMinRenderDistance = (int) v, v -> (int) v + " chunks"));
        rows.add(toggle(w, "Thermal Throttle Protection", () -> cfg.android.thermalThrottleProtection, v -> cfg.android.thermalThrottleProtection = v));
        rows.add(slider(w, "Thermal Limit", 35, 60, 1, () -> cfg.android.thermalLimitCelsius, v -> cfg.android.thermalLimitCelsius = (int) v, v -> (int) v + "°C"));
    }

    // ------------------------------------------------------------------ widget factories (all apply live)

    private PurpleToggle toggle(int w, String label, java.util.function.BooleanSupplier g, Consumer<Boolean> s) {
        return new PurpleToggle(0, 0, w, rowH(), Component.literal(label), g, v -> { s.accept(v); apply(); });
    }

    private PurpleSlider slider(int w, String label, double min, double max, double step, java.util.function.DoubleSupplier g,
                                java.util.function.DoubleConsumer s, java.util.function.DoubleFunction<String> fmt) {
        return new PurpleSlider(0, 0, w, rowH(), Component.literal(label), min, max, step, g, v -> { s.accept(v); apply(); }, fmt);
    }

    private <E extends Enum<E>> PurpleCycle<E> cycle(int w, String label, E[] values, java.util.function.Supplier<E> g, Consumer<E> s) {
        return new PurpleCycle<>(0, 0, w, rowH(), Component.literal(label), values, g, v -> { s.accept(v); apply(); }, PurpleCycle::prettyEnum);
    }

    private <T> PurpleCycle<T> cycle(int w, String label, T[] values, java.util.function.Supplier<T> g, Consumer<T> s, java.util.function.Function<T, String> namer) {
        return new PurpleCycle<>(0, 0, w, rowH(), Component.literal(label), values, g, v -> { s.accept(v); apply(); }, namer);
    }

    private PurpleInfo info(int w, String label, java.util.function.Supplier<String> v) {
        return new PurpleInfo(0, 0, w, rowH(), Component.literal(label), v);
    }

    private void apply() {
        cfg.fireChanged();                 // persists + notifies engine, lights, gamma
        LiveSettingsApplier.applyAll(cfg); // vanilla Options + GL state
        EntityCuller.applyConfig(cfg);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float open = openAnim.value();
        long now = System.currentTimeMillis();
        float breathe = PurpleTheme.breathe(now, 3600f);

        g.fill(0, 0, width, height, PurpleTheme.scaleAlpha(PurpleTheme.BG_DEEP, open));
        int glowTop = PurpleTheme.withAlpha(PurpleTheme.ACCENT, 0.10f + 0.06f * breathe);
        g.fillGradient(0, 0, width, height / 2, PurpleTheme.scaleAlpha(glowTop, open), 0x00000000);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        float open = openAnim.value();
        float panel = panelAnim.value();
        long now = System.currentTimeMillis();
        float breathe = PurpleTheme.breathe(now, 3600f);
        particles.ambient(width, height);
        particles.update();

        renderBackground(g, mouseX, mouseY, partialTick);

        // ---- morph: slide from below + fade ----
        int slide = Math.round((1f - open) * 24f);
        g.pose().pushPose();
        g.pose().translate(0, slide, 0);

        // sidebar
        g.fill(0, 0, SIDEBAR_W, height, PurpleTheme.scaleAlpha(PurpleTheme.SIDEBAR, open));
        g.fill(SIDEBAR_W - 1, 0, SIDEBAR_W, height, PurpleTheme.scaleAlpha(PurpleTheme.lerp(PurpleTheme.ACCENT_DIM, PurpleTheme.ACCENT, breathe), open));
        g.drawString(font, Component.literal("AETHERIUM"), PAD, 9, PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_BRIGHT, open), false);
        g.drawString(font, Component.literal("video settings"), PAD, 18, PurpleTheme.scaleAlpha(PurpleTheme.TEXT_MUTED, open), false);

        // panel (breathing border)
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        int panelBg = PurpleTheme.lerp(PurpleTheme.PANEL, PurpleTheme.PANEL_LIGHT, breathe * 0.35f);
        g.fill(px, py, px + pw, py + ph, PurpleTheme.scaleAlpha(panelBg, open));
        g.renderOutline(px, py, pw, ph, PurpleTheme.scaleAlpha(PurpleTheme.lerp(PurpleTheme.OUTLINE, PurpleTheme.ACCENT, breathe * 0.5f), open));

        // category title + live preview
        Component title = Component.translatableWithFallback(category.translationKey, category.fallback);
        g.drawString(font, title, px + PAD, py - 12, PurpleTheme.scaleAlpha(PurpleTheme.ACCENT_BRIGHT, open * panel), false);
        FrameTimeTracker ft = FrameTimeTracker.get();
        String preview = String.format("%.0f FPS · %.2f ms · p99 %.2f ms", ft.fps(), ft.average(), ft.percentile(0.99));
        g.drawString(font, preview, px + pw - font.width(preview) - PAD, py - 12, PurpleTheme.scaleAlpha(PurpleTheme.TEXT_MUTED, open), false);

        // highlight active category
        for (PurpleButton b : sidebarButtons) {
            boolean active = b.getMessage().getString().equals(title.getString());
            if (active) g.fill(b.getX() - 4, b.getY(), b.getX() - 2, b.getY() + b.getHeight(), PurpleTheme.scaleAlpha(PurpleTheme.ACCENT, open));
        }

        // widgets with scissor + scroll
        int scroll = Math.round(scrollAnim.value());
        float widgetAlpha = open * (0.35f + 0.65f * panel);
        int panelSlide = Math.round((1f - panel) * 10f);
        g.enableScissor(px, py, px + pw, py + ph);
        g.pose().pushPose();
        g.pose().translate(0, -scroll + panelSlide, 0);
        for (PurpleWidget w : panelWidgets) {
            w.setPanelAlpha(widgetAlpha);
            w.render(g, mouseX, mouseY + scroll - panelSlide - slide, partialTick);
        }
        g.pose().popPose();
        g.disableScissor();

        // scrollbar
        if (contentHeight > ph - PAD) {
            int trackH = ph - 8;
            int thumbH = Math.max(12, (int) (trackH * (ph / (float) contentHeight)));
            int thumbY = py + 4 + (int) ((trackH - thumbH) * (scroll / (float) Math.max(1, maxScroll())));
            g.fill(px + pw - 5, py + 4, px + pw - 3, py + 4 + trackH, PurpleTheme.scaleAlpha(PurpleTheme.OUTLINE, open));
            g.fill(px + pw - 5, thumbY, px + pw - 3, thumbY + thumbH, PurpleTheme.scaleAlpha(PurpleTheme.ACCENT, open));
        }

        // non-panel widgets (sidebar, done, engine switch)
        for (var r : renderables) {
            if (r instanceof PurpleWidget pwid && !panelWidgets.contains(pwid)) {
                pwid.setPanelAlpha(open);
                pwid.render(g, mouseX, mouseY - slide, partialTick);
            }
        }

        // particles
        for (int i = 0; i < particles.count(); i++) {
            float a = particles.alpha(i) * open;
            int sx = Math.round(particles.x[i]), sy = Math.round(particles.y[i]);
            int s = Math.max(1, Math.round(particles.size[i]));
            g.fill(sx - s, sy - s, sx + s, sy + s, PurpleTheme.withAlpha(PurpleTheme.ACCENT_BRIGHT, a * 0.55f));
            g.fill(sx - s * 2, sy - s * 2, sx + s * 2, sy + s * 2, PurpleTheme.withAlpha(PurpleTheme.ACCENT, a * 0.12f));
        }
        g.pose().popPose();
        // Tooltips: AbstractWidget#render defers them to Screen#renderWithTooltip, no manual pass needed.
    }

    // ------------------------------------------------------------------ input (mouse, touch swipe, scroll)

    private int maxScroll() { return Math.max(0, contentHeight - (panelH() - PAD)); }

    private boolean inPanel(double mx, double my) {
        return mx >= panelX() && mx < panelX() + panelW() && my >= panelY() && my < panelY() + panelH();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inPanel(mouseX, mouseY)) {
            float target = Math.max(0, Math.min(maxScroll(), scrollAnim.target() - (float) scrollY * (rowH() + ROW_GAP) * 2));
            scrollAnim.animateTo(target, 160);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastDragY = mouseY;
        if (inPanel(mouseX, mouseY)) {
            double adj = mouseY + scrollAnim.value();
            for (PurpleWidget w : panelWidgets) {
                if (w.mouseClicked(mouseX, adj, button)) { setFocused(w); if (button == 0) setDragging(true); return true; }
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (getFocused() instanceof PurpleWidget w && panelWidgets.contains(w) && isDragging()) {
            boolean handled = w.mouseDragged(mouseX, mouseY + scrollAnim.value(), button, dragX, dragY);
            if (handled || !touch) return handled;
        }
        if (touch && inPanel(mouseX, mouseY) && !(getFocused() instanceof PurpleSlider)) {
            scrollAnim.snap((float) Math.max(0, Math.min(maxScroll(), scrollAnim.value() - (mouseY - lastDragY))));
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (getFocused() instanceof PurpleWidget w && panelWidgets.contains(w)) {
            setDragging(false);
            return w.mouseReleased(mouseX, mouseY + scrollAnim.value(), button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        cfg.fireChanged();
        minecraft.setScreen(parent);
    }
}
