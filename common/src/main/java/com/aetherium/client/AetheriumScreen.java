package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.gui.AetheriumView;
import com.aetherium.gui.ScreenHost;
import com.aetherium.gui.Setting;
import com.aetherium.util.AetheriumLog;

import java.util.List;

// @era:gui-begin graphics
import net.minecraft.client.gui.GuiGraphics;
// @era:gui-else stack
//~ import com.mojang.blaze3d.vertex.PoseStack;
// @era:gui-else extractor
//~ import net.minecraft.client.gui.GuiGraphicsExtractor;
// @era:gui-end
import net.minecraft.client.gui.screens.Screen;
// @era:input-begin doubles
// @era:input-else events
//~ import net.minecraft.client.input.KeyEvent;
//~ import net.minecraft.client.input.MouseButtonEvent;
// @era:input-end
import net.minecraft.network.chat.Component;

/**
 * Replaces vanilla's Video Settings screen. A thin, version-specific shell:
 * all layout, animation and input logic lives in the version-independent
 * {@link AetheriumView}; this class only adapts Minecraft's render and input
 * callbacks to it.
 */
public final class AetheriumScreen extends Screen implements ScreenHost, AetheriumPages.Actions {

    private static final AetheriumLog LOGGER = AetheriumLog.of(AetheriumScreen.class);

    /** Remembered across openings so the screen reopens on the last tab. */
    private static int lastPage;

    private final Screen parent;
    private AetheriumView view;
    /** A child screen (vanilla video settings, Iris) may change live values; re-read them on return. */
    private boolean childOpened;

    public AetheriumScreen(final Screen parent) {
        super(Component.nullToEmpty("Aetherium Video Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        GlInfo.ensureLoaded();
        if (this.view == null) {
            final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
            if (sub == null) {
                ClientHooks.setScreen(this.minecraft, this.parent);
                return;
            }
            final AetheriumPages pages = new AetheriumPages(sub, this);
            this.view = new AetheriumView(pages.build(), this, lastPage);
            pages.attach(this.view);
        } else if (this.childOpened) {
            this.view.reload();
        }
        this.childOpened = false;
        this.view.resize(this.width, this.height);
    }

    // ------------------------------------------------------------------ rendering

    // @era:gui-begin graphics
    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        if (this.view != null) {
            this.view.render(McCanvas.INSTANCE.begin(graphics), mouseX, mouseY);
        }
    }
    // @era:gui-else stack
    //~ @Override
    //~ public void render(final PoseStack pose, final int mouseX, final int mouseY, final float partialTick) {
        //~ if (this.view != null) {
            //~ this.view.render(McCanvas.INSTANCE.begin(pose), mouseX, mouseY);
        //~ }
    //~ }
    // @era:gui-else extractor
    //~ @Override
    //~ public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                                   //~ final float partialTick) {
        //~ if (this.view != null) {
            //~ this.view.render(McCanvas.INSTANCE.begin(graphics), mouseX, mouseY);
        //~ }
    //~ }
    // @era:gui-end

    // The view paints an opaque background, so vanilla's (a blur pass on 1.21+) is skipped.
    // @era:background-begin render
    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
    }
    // @era:background-else none
    // @era:background-else extract
    //~ @Override
    //~ public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY,
                                  //~ final float partialTick) {
    //~ }
    // @era:background-end

    // ------------------------------------------------------------------ input

    // @era:input-begin doubles
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.view != null && this.view.mouseClicked(mouseX, mouseY, button)
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return this.view != null && this.view.mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        return this.view != null && this.view.mouseDragged(mouseX, mouseY, button)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.view != null && this.view.keyPressed(key) || super.keyPressed(key, scanCode, modifiers);
    }
    // @era:input-else events
    //~ @Override
    //~ public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        //~ return this.view != null && this.view.mouseClicked(event.x(), event.y(), event.button())
                //~ || super.mouseClicked(event, doubleClick);
    //~ }

    //~ @Override
    //~ public boolean mouseReleased(final MouseButtonEvent event) {
        //~ return this.view != null && this.view.mouseReleased(event.x(), event.y(), event.button())
                //~ || super.mouseReleased(event);
    //~ }

    //~ @Override
    //~ public boolean mouseDragged(final MouseButtonEvent event, final double dragX, final double dragY) {
        //~ return this.view != null && this.view.mouseDragged(event.x(), event.y(), event.button())
                //~ || super.mouseDragged(event, dragX, dragY);
    //~ }

    //~ @Override
    //~ public boolean keyPressed(final KeyEvent event) {
        //~ return this.view != null && this.view.keyPressed(event.key()) || super.keyPressed(event);
    //~ }
    // @era:input-end

    // @era:scroll-begin four
    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        return this.view != null && this.view.mouseScrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    // @era:scroll-else three
    //~ @Override
    //~ public boolean mouseScrolled(final double mouseX, final double mouseY, final double amount) {
        //~ return this.view != null && this.view.mouseScrolled(mouseX, mouseY, amount)
                //~ || super.mouseScrolled(mouseX, mouseY, amount);
    //~ }
    // @era:scroll-end

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void onClose() {
        ClientHooks.setScreen(this.minecraft, this.parent);
    }

    /** Runs whenever this screen is replaced (closed or a child opened): staged edits are applied, never lost. */
    @Override
    public void removed() {
        if (this.view != null) {
            this.view.onClose();
            lastPage = this.view.selectedPage();
        }
    }

    // ------------------------------------------------------------------ ScreenHost

    @Override
    public void applyChanges(final List<Setting> changed) {
        boolean reload = false;
        // Graphics first: on 1.21.11+ the graphics preset rewrites several other options,
        // so anything else the user staged must land after it.
        for (final Setting setting : changed) {
            if ("quality.graphics".equals(setting.id)) {
                commit(setting);
            }
        }
        for (final Setting setting : changed) {
            commit(setting);
            reload |= setting.needsChunkReload();
        }
        VanillaOptions.save();
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null) {
            sub.store.requestSave();
        }
        ClientHooks.onSettingsApplied();
        if (reload) {
            VanillaOptions.reloadChunks();
        }
    }

    private static void commit(final Setting setting) {
        try {
            setting.commit();
        } catch (final RuntimeException | LinkageError error) {
            LOGGER.warn("Could not apply {}: {}", setting.id, error.toString());
        }
    }

    @Override
    public void requestClose() {
        onClose();
    }

    @Override
    public int currentFps() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub == null ? 0 : (int) Math.round(sub.frameStats.getFps());
    }

    @Override
    public String footerLeft() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        final String mc = sub == null ? "?" : sub.platform.minecraftVersion();
        return "Aetherium Mod v" + Aetherium.VERSION + " \u00b7 Minecraft " + mc;
    }

    @Override
    public String footerRight() {
        return "Rendering API: " + Aetherium.RENDERING_API + " \u00b7 " + GlInfo.renderer();
    }

    @Override
    public boolean touchMode() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub != null && sub.config.touchMode.get().booleanValue();
    }

    // ------------------------------------------------------------------ Actions

    @Override
    public void openShaderPacks() {
        this.childOpened = true;
        if (!ClientHooks.openShaderPacks(this.minecraft, this)) {
            this.childOpened = false;
        }
    }

    @Override
    public void resetDefaults() {
        if (this.view != null) {
            this.view.resetDefaults();
        }
    }

    @Override
    public void openVanillaVideo() {
        this.childOpened = true;
        ClientHooks.openVanillaVideoSettings(this.minecraft, this);
    }
}
