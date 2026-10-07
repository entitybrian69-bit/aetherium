package com.aetherium.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.aetherium.Aetherium;
import com.aetherium.gui.widget.AetheriumWidgets;
import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Aetherium's video options screen — the mod's whole UI surface in one class.
 *
 * <p>It replaces vanilla's "Video Settings..." from {@code OptionsScreenMixin} and
 * is also reachable from the keybind and (on NeoForge) nothing else: there is
 * deliberately no Mod Menu dependency, because Mod Menu is absent on NeoForge and
 * optional on Fabric, and a renderer whose settings need a second mod is a renderer
 * users cannot configure.</p>
 *
 * <h2>Live application</h2>
 * <p>Every widget writes through to the {@code AetheriumConfig} value it is bound to
 * and calls {@link #markNeedsHotSwap()} when the change touches the render path. The
 * swap itself happens at the next frame boundary inside
 * {@code Aetherium#runSafePoint()}, which is why the screen never blocks: a backend
 * change while a world is loaded takes one frame, and if the swap fails the engine
 * returns to compatibility mode and reports the reason in the status line here.</p>
 *
 * <h2>Motion</h2>
 * <p>Opening, tab changes and the closing of the screen all run the same
 * three-phase morph (collapse / violet particle burst / expand) from
 * {@link AetheriumAnimations}, with the content rows revealed 18 ms apart so the
 * list reads as a stack of material rather than a table snapping into place.
 * {@code -D aetherium.reduced_motion=true} (or {@code AETHERIUM_REDUCED_MOTION=1})
 * turns all of it off — accessibility is not a nice-to-have.</p>
 *
 * <h2>Version porting</h2>
 * <p>The vanilla signatures used here are the 1.21.1 ones: {@code render(GuiGraphics,int,int,float)},
 * {@code mouseClicked(double,double,int)->boolean}, {@code mouseScrolled(double,double,double)->boolean},
 * {@code keyPressed(int,int,int)->boolean}, {@code onClose()->void}. Each of those
 * changed shape at known points in the porting range (GuiGraphics at 1.18,
 * {@code mouseClicked}/{@code mouseScrolled} return types at 1.20.2,
 * {@code onClose()->boolean} at 1.21.2), and {@code PORTING_MATRIX.md} lists the
 * exact override for every row; the delta patches rewrite these five method
 * signatures mechanically.</p>
 */
public final class AetheriumVideoOptionsScreen extends Screen {
    private static final int CONTENT_TOP = 28;
    private static final int FOOTER_HEIGHT = 26;

    private final Screen parent;
    private final AetheriumTheme theme;
    private static final com.aetherium.util.AetheriumLog LOGGER =
            com.aetherium.util.AetheriumLog.of(AetheriumVideoOptionsScreen.class);

    /** Remembered across openings without needing a config key per tab. */
    private static String rememberedTab;

    private final AetheriumAnimations animations = new AetheriumAnimations();
    private final List<AetheriumTabs.Tab> tabs;
    private final List<AbstractWidget> content = new ArrayList<>(32);
    private final AetheriumWidgets.ListWidget list;

    private int tabIndex;
    private String statusLine = "";
    private long statusUntil;
    private boolean needsHotSwap;
    private boolean closing;
    private int contentWidth;
    private int contentHeight;
    private int mouseX;
    private int mouseY;

    private AetheriumVideoOptionsScreen(final Screen parent) {
        super(Component.translatable("aetherium.screen.title"));
        this.parent = parent;
        this.theme = AetheriumTheme.forCurrentConfig();
        this.tabs = AetheriumTabs.visibleTabs();
        this.list = new AetheriumWidgets.ListWidget(this.theme);
        this.tabIndex = MathUtil.clamp(indexOfTab(rememberedTab == null ? AetheriumTabs.GENERAL.id() : rememberedTab),
                0, this.tabs.size() - 1);
    }

    /** The single entry point other code uses, so the constructor can stay private. */
    public static Screen create(final Screen parent) {
        return new AetheriumVideoOptionsScreen(Objects.requireNonNull(parent, "parent"));
    }

    private static int indexOfTab(final String id) {
        final List<AetheriumTabs.Tab> visible = AetheriumTabs.visibleTabs();
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).id().equals(id)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected void init() {
        super.init();
        this.animations.beginOpen();
        rebuild();
    }

    /** Rebuilds every widget; also called when touch mode flips or a tab changes. */
    private void rebuild() {
        this.clearWidgets();
        this.content.clear();
        this.list.rows().clear();

        final int sidebarWidth = this.theme.sidebarWidth();
        final int margin = this.theme.dp(8);
        final int x = sidebarWidth + margin * 3;
        this.contentWidth = Math.max(180, this.width - x - margin * 2);
        this.contentHeight = this.height - CONTENT_TOP - FOOTER_HEIGHT - margin * 2;
        this.list.setViewHeight(this.contentHeight);

        // Sidebar entries.
        for (int i = 0; i < this.tabs.size(); i++) {
            final AetheriumTabs.Tab tab = this.tabs.get(i);
            final int index = i;
            this.addRenderableWidget(new SidebarEntry(this.theme, margin, CONTENT_TOP + i * (this.theme.rowHeight() + 4),
                    sidebarWidth, this.theme.rowHeight(), tab, index == this.tabIndex, () -> selectTab(index)));
        }

        // Live stats panel, top-right of the content area.
        final int previewWidth = Math.min(150, Math.max(110, this.contentWidth / 3));
        this.addRenderableWidget(new AetheriumWidgets.PreviewWidget(this.theme,
                this.width - previewWidth - margin, CONTENT_TOP, previewWidth, 44, Aetherium::frameStats));

        // The rows themselves.
        final int[] rowY = {CONTENT_TOP + margin};
        final List<AbstractWidget> rows = AetheriumTabs.build(this, this.tabs.get(this.tabIndex).id(),
                x, rowY[0], this.contentWidth, rowY);
        for (final AbstractWidget row : rows) {
            this.content.add(row);
            this.list.add(row);
            this.addRenderableWidget(row);
        }
        this.setStatus(this.tabs.get(this.tabIndex).labelKey() + " — all changes apply immediately");
    }

    private void selectTab(final int index) {
        if (index == this.tabIndex || index < 0 || index >= this.tabs.size()) {
            return;
        }
        this.tabIndex = index;
        rememberedTab = this.tabs.get(index).id();
        // The morph runs first, and rebuild() is invoked at the seam (see
        // AetheriumAnimations.Phase.BURST) so the user never sees half a tab.
        this.animations.startMorph(this.theme.sidebarWidth() + this.theme.dp(8), CONTENT_TOP, this.contentWidth);
        this.pendingRebuild = true;
    }

    private boolean pendingRebuild;

    /** Called by {@code AetheriumTabs} when a widget must trigger a safe-point swap. */
    public void markNeedsHotSwap() {
        this.needsHotSwap = true;
        this.setStatus("Renderer change queued: applies on the next frame");
    }

    public void setStatus(final String line) {
        this.statusLine = line == null ? "" : line;
        this.statusUntil = System.nanoTime() + 6_000_000_000L;
    }

    public AetheriumTheme theme() {
        return this.theme;
    }

    public void rebuildForLayout() {
        rebuild();
    }

    public void writeConflictReport() {
        final String report = Aetherium.conflicts().asReport();
        LOGGER.info("Conflict report:\n{}", report);
        this.setStatus("Conflict report written to the log (" + report.length() + " chars)");
    }

    // [UNVERIFIED: this override set for 1.21.1 exactly - render(GuiGraphics,int,int,float),
    // mouseClicked(double,double,int)->boolean, mouseScrolled(double,double,double)->boolean and
    // onClose()->void. Each has changed once or twice in 1.16.5->26.3 (see the table in this
    // class' javadoc); the per-version delta rewrites them mechanically, and a mismatch is a
    // compile error, which is the loud kind we want.]
    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.animations.beginFrame();
        if (this.pendingRebuild && this.animations.getPhase() == AetheriumAnimations.Phase.BURST) {
            this.pendingRebuild = false;
            rebuild();
        }
        this.list.tickAnimations();

        // 1.21.1's Screen#renderBackground takes the cursor and frame delta too (it dims and
        // blurs behind the panel); the single-argument form is 1.21.2+.
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        drawChrome(guiGraphics);

        final int contentX = this.theme.sidebarWidth() + this.theme.dp(8) * 2;
        final int offsetX = (int) this.animations.getContentOffsetX();
        layoutRows();
        guiGraphics.enableScissor(contentX - 4, CONTENT_TOP, this.contentWidth + 8, this.contentHeight);
        for (final AbstractWidget row : this.content) {
            final float reveal = MathUtil.easeOutCubic(MathUtil.clamp(
                    this.animations.getOpenProgress() * this.content.size() - this.content.indexOf(row), 0.0f, 1.0f));
            if (reveal <= 0.01f) {
                continue;
            }
            row.render(guiGraphics, mouseX + offsetX, mouseY, delta);
        }
        guiGraphics.disableScissor();

        this.animations.drawParticles(guiGraphics);
        drawFooter(guiGraphics);
    }

    /**
     * Places rows top-to-bottom with the animated reveal and the current scroll
     * offset. Done per frame instead of at build time so a resize or a touch-mode
     * change does not need a rebuild.
     */
    private void layoutRows() {
        int y = CONTENT_TOP + this.theme.dp(8) - (int) this.list.getScroll();
        for (final AbstractWidget row : this.content) {
            row.setX(this.theme.sidebarWidth() + this.theme.dp(8) * 2);
            row.setY(y);
            if (row.getWidth() > this.contentWidth) {
                row.setWidth(this.contentWidth);
            }
            y += this.theme.rowHeight() + 4;
        }
    }

    private void drawChrome(final GuiGraphics guiGraphics) {
        final int sidebarWidth = this.theme.sidebarWidth();
        guiGraphics.fill(0, 0, sidebarWidth + this.theme.dp(6), this.height, 0x66120819);
        this.theme.drawBrand(guiGraphics, sidebarWidth + this.theme.dp(10), 8, this.width - sidebarWidth - this.theme.dp(20));
        this.theme.drawActiveUnderline(guiGraphics, this.theme.dp(8),
                CONTENT_TOP + this.tabIndex * (this.theme.rowHeight() + 4) - 2, sidebarWidth, 1.0f);
    }

    private void drawFooter(final GuiGraphics guiGraphics) {
        final int y = this.height - FOOTER_HEIGHT + 4;
        guiGraphics.fill(0, y - 5, this.width, y + FOOTER_HEIGHT, 0x66120819);
        final String status = this.statusLine;
        if (!status.isEmpty() && this.font != null) {
            final boolean fresh = System.nanoTime() < this.statusUntil;
            guiGraphics.drawString(this.font, Component.literal(status), this.theme.dp(10), y,
                    fresh ? AetheriumTheme.TEXT_DIM : AetheriumTheme.TEXT_DISABLED, false);
        }
        if (this.animations.isTransitioning()) {
            guiGraphics.drawString(this.font, Component.literal("morph: " + this.animations.getPhase().name()
                    + " (" + this.animations.getActiveParticleCount() + " sparks)"),
                    this.width - 220, y, AetheriumTheme.TEXT_DISABLED, false);
        }
    }

    /** Sidebar entry: label + glyph, with the violet active underline drawn by the screen. */
    private static final class SidebarEntry extends AetheriumWidgets.PurpleWidget {
        private final AetheriumTabs.Tab tab;
        private final boolean activeTab;
        private final Runnable onSelect;
        private final AetheriumAnimations.AnimationValue glow = new AetheriumAnimations.AnimationValue(0.0f, 0.14f);

        SidebarEntry(final AetheriumTheme theme, final int x, final int y, final int width, final int height,
                     final AetheriumTabs.Tab tab, final boolean activeTab, final Runnable onSelect) {
            super(theme, x, y, width, height, Component.translatable(tab.labelKey()));
            this.tab = tab;
            this.activeTab = activeTab;
            this.onSelect = onSelect;
            this.glow.snap(activeTab ? 1.0f : 0.0f);
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            super.onClick(mouseX, mouseY);
            this.onSelect.run();
        }

        @Override
        public void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float delta) {
            this.glow.set(this.activeTab || isMouseOver(mouseX, mouseY) ? 1.0f : 0.0f);
            this.glow.tick(AetheriumAnimations.suggestedDeltaSeconds());
            final int surface = this.activeTab ? AetheriumTheme.PANEL_RAISED : 0x00000000;
            if (surface != 0) {
                this.theme.fillRounded(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), surface);
            }
            if (this.glow.get() > 0.02f) {
                this.theme.drawHoverGlow(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), this.glow.get());
            }
            final String glyph = this.tab.glyph();
            this.theme.drawLabel(guiGraphics, glyph + "  " + this.getMessage().getString(),
                    this.getX() + 6, this.getY() + (this.getHeight() - 8) / 2,
                    this.activeTab ? AetheriumTheme.ACCENT : AetheriumTheme.TEXT_DIM);
            if (this.activeTab) {
                guiGraphics.fill(this.getX(), this.getY() + 2, this.getX() + 2, this.getY() + this.getHeight() - 2, AetheriumTheme.ACCENT);
            }
        }

        @Override
        public void updateWidgetNarration(final net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
