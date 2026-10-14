package com.aetherium.client;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.zoom.ZoomController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Zume-style zoom on 1.12.2: the keys (Zoom, Zoom In, Zoom Out in the "Aetherium" controls
 * category) and the bridge from the hooks to the shared {@link ZoomController}.
 */
public final class ZoomKeys {

    public static final String CATEGORY = "key.categories.aetherium";

    private static final ZoomController CONTROLLER = new ZoomController();
    private static KeyBinding zoom;
    private static KeyBinding zoomIn;
    private static KeyBinding zoomOut;

    private ZoomKeys() {
    }

    static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }

    /** Registers the three keys (mod init, client only). */
    public static void register() {
        if (zoom != null) {
            return;
        }
        zoom = newKey("key.aetherium.zoom", Keyboard.KEY_Z);
        zoomIn = newKey("key.aetherium.zoom_in", Keyboard.KEY_EQUALS);
        zoomOut = newKey("key.aetherium.zoom_out", Keyboard.KEY_MINUS);
    }

    static KeyBinding newKey(final String name, final int code) {
        final KeyBinding key = new KeyBinding(name, KeyConflictContext.IN_GAME, code, CATEGORY);
        ClientRegistry.registerKeyBinding(key);
        return key;
    }

    public static void configure(final AetheriumConfig config) {
        final ZoomController.Settings s = new ZoomController.Settings();
        s.defaultZoom = config.zoomDefault.get().intValue() / 100.0;
        s.toggle = config.zoomToggle.get().booleanValue();
        s.cinematic = config.zoomCinematic.get().booleanValue();
        s.scrolling = config.zoomScrolling.get().booleanValue();
        s.speed = config.zoomSpeed.get().intValue();
        s.smoothnessMs = config.zoomSmoothnessMs.get().intValue();
        s.sensitivityFloor = config.zoomSensitivityFloor.get().intValue() / 100.0;
        s.minFov = config.zoomMinFov.get().intValue();
        s.maxThirdPersonDistance = config.zoomThirdPersonDistance.get().intValue();
        s.thirdPersonToggle = config.zoomThirdPersonToggle.get().booleanValue();
        CONTROLLER.configure(s);
    }

    public static void reset() {
        CONTROLLER.reset();
    }

    // ------------------------------------------------------------------ hooks

    public static double fov(final double original) {
        return CONTROLLER.fov(original, nowMs());
    }

    public static double cameraDistance(final double original) {
        return CONTROLLER.cameraDistance(original, nowMs());
    }

    public static double sensitivity(final double original) {
        return CONTROLLER.sensitivity(original, nowMs());
    }

    public static boolean smoothCamera(final boolean original) {
        return CONTROLLER.smoothCamera(original);
    }

    public static boolean isZooming() {
        return CONTROLLER.isZooming();
    }

    /** A mouse wheel notch while no screen is open; true when zoom used it. */
    public static boolean onScroll(final double delta) {
        final Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.currentScreen == null && CONTROLLER.scroll(delta);
    }

    public static void frame(final Minecraft mc) {
        if (zoom == null || mc == null) {
            return;
        }
        if (mc.world == null || mc.player == null) {
            if (CONTROLLER.isZooming()) {
                CONTROLLER.reset();
            }
            return;
        }
        final boolean noScreen = mc.currentScreen == null;
        final boolean held = noScreen && isDown(zoom);
        final ZoomController.View view;
        switch (mc.gameSettings.thirdPersonView) {
            case 0:
                view = ZoomController.View.FIRST_PERSON;
                break;
            case 2:
                view = ZoomController.View.THIRD_PERSON_FRONT;
                break;
            default:
                view = ZoomController.View.THIRD_PERSON_BACK;
                break;
        }
        CONTROLLER.frame(nowMs(), held, noScreen && isDown(zoomIn), noScreen && isDown(zoomOut), view);
    }

    /** Live key state (also for mouse buttons, which 1.12.2 encodes as negative codes). */
    static boolean isDown(final KeyBinding key) {
        final int code = key.getKeyCode();
        if (code == 0) {
            return false;
        }
        try {
            return code < 0 ? org.lwjgl.input.Mouse.isButtonDown(code + 100) : Keyboard.isKeyDown(code);
        } catch (final RuntimeException error) {
            return key.isKeyDown();
        }
    }

    public static String describe() {
        if (zoom == null) {
            return "Not registered";
        }
        return zoom.getDisplayName() + " / " + zoomIn.getDisplayName() + " / " + zoomOut.getDisplayName();
    }
}
