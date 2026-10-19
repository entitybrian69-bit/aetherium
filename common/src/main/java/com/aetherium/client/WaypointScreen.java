package com.aetherium.client;

// @era:q-world-begin v16
import com.aetherium.Aetherium;
import com.aetherium.gui.UiSound;
import com.aetherium.qol.WaypointView;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The waypoint list (Quality of life). A thin shell around the version-independent
 * {@link WaypointView}, like {@link AetheriumScreen} around the settings view.
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

    @Override
    public void render(final PoseStack pose, final int mouseX, final int mouseY, final float partialTick) {
        if (this.view != null) {
            this.view.render(McCanvas.INSTANCE.begin(pose), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        return this.view != null && this.view.mouseClicked(mouseX, mouseY, button)
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double amount) {
        return this.view != null && this.view.mouseScrolled(mouseX, mouseY, amount)
                || super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.view != null && this.view.keyPressed(key) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return this.view != null && this.view.charTyped(c) || super.charTyped(c, modifiers);
    }

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
// @era:q-world-else none
//~ /** The waypoint list exists on Minecraft 1.16.5 only for now (Quality of life test build). */
//~ public final class WaypointScreen {
    //~ private WaypointScreen() {
    //~ }
//~ }
// @era:q-world-end
