package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.gui.UiSound;
import com.aetherium.qol.WaypointView;

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
//~ import net.minecraft.client.input.CharacterEvent;
//~ import net.minecraft.client.input.KeyEvent;
//~ import net.minecraft.client.input.MouseButtonEvent;
// @era:input-end
import net.minecraft.network.chat.Component;

/**
 * The waypoint list (Quality of life, every version). A thin shell around the version-independent
 * {@link WaypointView}, like {@link AetheriumScreen} around the settings view: only the render and
 * input signatures of each Minecraft version are adapted here.
 */
public final class WaypointScreen extends Screen implements WaypointView.Host {
    private final Screen parent;
    private WaypointView view;

    public WaypointScreen(final Screen parent) {
        super(Component.nullToEmpty("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.view == null) {
            this.view = new WaypointView(QolClient.waypoints(), this);
        }
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
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.view != null && this.view.keyPressed(key) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return this.view != null && this.view.charTyped(c) || super.charTyped(c, modifiers);
    }
    // @era:input-else events
    //~ @Override
    //~ public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        //~ return this.view != null && this.view.mouseClicked(event.x(), event.y(), event.button())
                //~ || super.mouseClicked(event, doubleClick);
    //~ }

    //~ @Override
    //~ public boolean keyPressed(final KeyEvent event) {
        //~ return this.view != null && this.view.keyPressed(event.key()) || super.keyPressed(event);
    //~ }

    //~ @Override
    //~ public boolean charTyped(final CharacterEvent event) {
        //~ final int codepoint = event.codepoint();
        //~ return this.view != null && Character.isBmpCodePoint(codepoint) && this.view.charTyped((char) codepoint)
                //~ || super.charTyped(event);
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientHooks.setScreen(this.minecraft, this.parent);
    }

    @Override
    public void removed() {
        if (this.view != null) {
            this.view.onClose();
        }
    }

    // ------------------------------------------------------------------ WaypointView.Host

    @Override
    public double playerX() {
        return this.minecraft.player == null ? 0.0 : this.minecraft.player.getX();
    }

    @Override
    public double playerY() {
        return this.minecraft.player == null ? 0.0 : this.minecraft.player.getY();
    }

    @Override
    public double playerZ() {
        return this.minecraft.player == null ? 0.0 : this.minecraft.player.getZ();
    }

    @Override
    public String dimension() {
        return QolClient.dimension(this.minecraft);
    }

    @Override
    public void close() {
        onClose();
    }

    @Override
    public void playSound(final UiSound sound) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null && sub.config.uiSounds.get().booleanValue()) {
            UiSounds.play(this.minecraft, sound);
        }
    }

    @Override
    public boolean darkMode() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub != null && sub.config.darkMode.get().booleanValue();
    }
}
