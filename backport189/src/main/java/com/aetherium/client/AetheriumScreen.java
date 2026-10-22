package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.backport.AetheriumBackport;
import com.aetherium.gui.AetheriumView;
import com.aetherium.gui.ScreenHost;
import com.aetherium.gui.Setting;
import com.aetherium.gui.UiSound;
import com.aetherium.util.AetheriumLog;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Replaces 1.12.2's Video Settings screen. All layout, animation and input logic lives in the
 * shared {@link AetheriumView}; this class adapts {@link GuiScreen}'s callbacks to it.
 */
public final class AetheriumScreen extends GuiScreen implements ScreenHost, AetheriumPages.Actions {

    private static final AetheriumLog LOGGER = AetheriumLog.of(AetheriumScreen.class);

    // GLFW key codes the view understands.
    private static final int GLFW_ESCAPE = 256;
    private static final int GLFW_ENTER = 257;
    private static final int GLFW_DOWN = 264;
    private static final int GLFW_UP = 265;
    private static final int GLFW_PAGE_UP = 266;
    private static final int GLFW_PAGE_DOWN = 267;

    private static int lastPage;

    private final GuiScreen parent;
    private AetheriumView view;
    private AetheriumPages pages;
    private boolean childOpened;
    private int pressedButton = -1;

    public AetheriumScreen(final GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        GlInfo.ensureLoaded();
        Keyboard.enableRepeatEvents(true);
        if (this.view == null) {
            final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
            if (sub == null) {
                this.mc.displayGuiScreen(this.parent);
                return;
            }
            final AetheriumPages pages = new AetheriumPages(sub, this);
            this.view = new AetheriumView(pages.build(), this, lastPage);
            pages.attach(this.view);
            this.pages = pages;
        } else if (this.childOpened) {
            this.view.reload();
        }
        this.childOpened = false;
        this.view.resize(this.width, this.height);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        if (this.view != null) {
            final int scale = new ScaledResolution(this.mc).getScaleFactor();
            this.view.render(McCanvas.INSTANCE.begin(scale), mouseX, mouseY);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    // ------------------------------------------------------------------ input

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        this.pressedButton = button;
        if (this.view == null || !this.view.mouseClicked(mouseX, mouseY, button)) {
            super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
        final int button = this.pressedButton >= 0 ? this.pressedButton : Math.max(0, state);
        this.pressedButton = -1;
        if (this.view == null || !this.view.mouseReleased(mouseX, mouseY, button)) {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    protected void mouseClickMove(final int mouseX, final int mouseY, final int button, final long held) {
        if (this.view != null) {
            this.view.mouseDragged(mouseX, mouseY, button);
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
        final int key = glfwKey(keyCode);
        if (this.view != null && key >= 0 && this.view.keyPressed(key)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            requestClose();
        }
    }

    private static int glfwKey(final int lwjgl2) {
        switch (lwjgl2) {
            case Keyboard.KEY_ESCAPE:
                return GLFW_ESCAPE;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                return GLFW_ENTER;
            case Keyboard.KEY_DOWN:
                return GLFW_DOWN;
            case Keyboard.KEY_UP:
                return GLFW_UP;
            case Keyboard.KEY_PRIOR:
                return GLFW_PAGE_UP;
            case Keyboard.KEY_NEXT:
                return GLFW_PAGE_DOWN;
            default:
                return -1;
        }
    }

    // ------------------------------------------------------------------ lifecycle

    /** Runs whenever this screen is replaced (closed or a child opened): staged edits are applied. */
    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (this.view != null) {
            this.view.onClose();
            lastPage = this.view.selectedPage();
        }
        if (this.pages != null) {
            this.pages.endPreview();
        }
    }

    @Override
    public void beforeFrame() {
        if (this.pages != null) {
            this.pages.previewFrame();
        }
    }

    /** In a world the level is drawn behind this screen, so the hand editor can preview live. */
    @Override
    public boolean livePreviewAvailable() {
        return this.mc != null && this.mc.theWorld != null;
    }

    // ------------------------------------------------------------------ ScreenHost

    @Override
    public void applyChanges(final List<Setting> changed) {
        boolean reload = false;
        for (final Setting setting : changed) {
            try {
                setting.commit();
            } catch (final RuntimeException | LinkageError error) {
                LOGGER.warn("Could not apply {}: {}", setting.id, error.toString());
            }
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

    @Override
    public void requestClose() {
        this.mc.displayGuiScreen(this.parent);
    }

    @Override
    public int currentFps() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub == null ? 0 : (int) Math.round(sub.frameStats.getFps());
    }

    @Override
    public String footerLeft() {
        return "Aetherium Backport v" + AetheriumBackport.VERSION + " \u00b7 Minecraft 1.12.2";
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

    @Override
    public boolean darkMode() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        return sub != null && sub.config.darkMode.get().booleanValue();
    }

    @Override
    public void setDarkMode(final boolean dark) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null) {
            sub.config.darkMode.set(Boolean.valueOf(dark));
            sub.store.requestSave();
        }
    }

    @Override
    public void playSound(final UiSound sound) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null && sub.config.uiSounds.get().booleanValue()) {
            UiSounds.play(this.mc, sound);
        }
    }

    /** CI smoke test only. */
    public void selectTabForTest(final int index) {
        if (this.view != null) {
            this.view.selectTab(index);
        }
    }

    // ------------------------------------------------------------------ Actions

    @Override
    public void resetDefaults() {
        if (this.view != null) {
            this.view.resetDefaults();
        }
    }

    @Override
    public void openWaypoints() {
        QolClient.openWaypoints(this.mc, this);
    }

    @Override
    public void openVanillaVideo() {
        this.childOpened = true;
        ClientHooks.openVanillaVideoSettings(this.mc, this);
    }
}
