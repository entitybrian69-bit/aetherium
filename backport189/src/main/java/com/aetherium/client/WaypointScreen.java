package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.gui.UiSound;
import com.aetherium.qol.WaypointView;
import java.io.IOException;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * The waypoint list (Quality of life) on 1.12.2: a thin {@link GuiScreen} shell around the shared
 * {@link WaypointView}, like {@link AetheriumScreen} around the settings view.
 */
public final class WaypointScreen extends GuiScreen implements WaypointView.Host {
    private static final int GLFW_ESCAPE = 256;
    private static final int GLFW_ENTER = 257;
    private static final int GLFW_BACKSPACE = 259;

    private final GuiScreen parent;
    private WaypointView view;

    public WaypointScreen(final GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        if (this.view == null) {
            this.view = new WaypointView(QolClient.waypoints(), this);
        }
        this.view.resize(this.width, this.height);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (this.view != null) {
            final int scale = new ScaledResolution(this.mc).getScaleFactor();
            this.view.render(McCanvas.INSTANCE.begin(scale), mouseX, mouseY);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        if (this.view == null || !this.view.mouseClicked(mouseX, mouseY, button)) {
            super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        final int wheel = Mouse.getEventDWheel();
        if (wheel != 0 && this.view != null) {
            final int x = Mouse.getEventX() * this.width / this.mc.displayWidth;
            final int y = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.view.mouseScrolled(x, y, wheel > 0 ? 1.0 : -1.0);
        }
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) throws IOException {
        if (this.view != null) {
            final int key = keyCode == Keyboard.KEY_ESCAPE ? GLFW_ESCAPE
                    : keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER ? GLFW_ENTER
                    : keyCode == Keyboard.KEY_BACK ? GLFW_BACKSPACE : -1;
            // LWJGL 2 reports the character together with the key: special keys go to keyPressed,
            // everything printable to charTyped (both are no-ops unless a rename is in progress).
            if (key >= 0 ? this.view.keyPressed(key) : this.view.charTyped(typedChar)) {
                return;
            }
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            close();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (this.view != null) {
            this.view.onClose();
        }
    }

    // ------------------------------------------------------------------ WaypointView.Host

    @Override
    public double playerX() {
        return this.mc.thePlayer == null ? 0.0 : this.mc.thePlayer.posX;
    }

    @Override
    public double playerY() {
        return this.mc.thePlayer == null ? 0.0 : this.mc.thePlayer.posY;
    }

    @Override
    public double playerZ() {
        return this.mc.thePlayer == null ? 0.0 : this.mc.thePlayer.posZ;
    }

    @Override
    public String dimension() {
        return QolClient.dimension(this.mc);
    }

    @Override
    public void close() {
        this.mc.displayGuiScreen(this.parent);
    }

    @Override
    public void playSound(final UiSound sound) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null && sub.config.uiSounds.get().booleanValue()) {
            UiSounds.play(this.mc, sound);
        }
    }

    @Override
    public boolean darkMode() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub != null && sub.config.darkMode.get().booleanValue();
    }
}
