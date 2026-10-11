package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.qol.Freelook;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * Minecraft side of freelook (every version): the rebindable Freelook key (Left Alt), the free
 * camera angles and the optional switch to third person. The mixins only call the static hooks
 * below; each is one field read while freelook is idle. Render thread only.
 */
public final class FreelookHooks {
    private static final int KEY_LEFT_ALT = 342;
    private static final Freelook FREELOOK = new Freelook();

    private static boolean toggle;
    private static boolean thirdPerson = true;
    private static KeyMapping key;
    /** Camera type to restore when freelook ends, or null when freelook did not change it. */
    private static CameraType restoreCamera;
    private static CameraType forcedCamera;

    private FreelookHooks() {
    }

    /** After Apply and at startup (from QolHooks.configure). */
    public static void configure(final AetheriumConfig config) {
        toggle = config.freelookToggle.get().booleanValue();
        thirdPerson = config.freelookThirdPerson.get().booleanValue();
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        if (!active && FREELOOK.active()) {
            stop(Minecraft.getInstance());
        }
    }

    /** World change / disconnect. */
    public static void onLevelChange() {
        if (FREELOOK.active()) {
            stop(Minecraft.getInstance());
        }
    }

    // ------------------------------------------------------------------ per frame

    /** GameRenderer.render HEAD (via ClientHooks), before the camera is set up. */
    public static void frame(final Minecraft mc) {
        if (key == null || mc == null) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            if (FREELOOK.active()) {
                stop(mc);
            }
            return;
        }
        final boolean down = ClientHooks.currentScreen(mc) == null && key.isDown() && Aetherium.isActive();
        final int change = FREELOOK.update(down, toggle);
        if (change == Freelook.STARTED) {
            // @era:rot-begin getters
            FREELOOK.begin(mc.player.getYRot(), mc.player.getXRot());
            // @era:rot-else fields
            //~ FREELOOK.begin(mc.player.yRot, mc.player.xRot);
            // @era:rot-end
            restoreCamera = null;
            forcedCamera = null;
            if (thirdPerson && mc.options.getCameraType().isFirstPerson()) {
                restoreCamera = mc.options.getCameraType();
                forcedCamera = CameraType.THIRD_PERSON_BACK;
                setCamera(mc, forcedCamera);
            }
        } else if (change == Freelook.STOPPED) {
            restore(mc);
        }
    }

    private static void stop(final Minecraft mc) {
        FREELOOK.stop();
        restore(mc);
    }

    private static void restore(final Minecraft mc) {
        // Only undo our own switch: if the player pressed F5 meanwhile, their choice stays.
        if (mc != null && restoreCamera != null && mc.options.getCameraType() == forcedCamera) {
            setCamera(mc, restoreCamera);
        }
        restoreCamera = null;
        forcedCamera = null;
    }

    private static void setCamera(final Minecraft mc, final CameraType type) {
        final boolean wasFirst = mc.options.getCameraType().isFirstPerson();
        mc.options.setCameraType(type);
        if (wasFirst != type.isFirstPerson()) {
            mc.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? mc.getCameraEntity() : null);
        }
    }

    // ------------------------------------------------------------------ hooks (mixins)

    /** MouseHandler.turnPlayer: true when the mouse turned the free camera instead of the player. */
    public static boolean turn(final double dx, final double dy) {
        if (!FREELOOK.active()) {
            return false;
        }
        FREELOOK.turn(dx, dy);
        return true;
    }

    public static float cameraYaw(final float original) {
        return FREELOOK.active() ? FREELOOK.yaw() : original;
    }

    public static float cameraPitch(final float original) {
        return FREELOOK.active() ? FREELOOK.pitch() : original;
    }

    public static boolean active() {
        return FREELOOK.active();
    }

    /** Options.load HEAD: the key list with the Freelook key appended (once). */
    public static KeyMapping[] appendKeys(final KeyMapping[] existing) {
        if (key == null) {
            key = ZoomKeys.newKey("key.aetherium.freelook", KEY_LEFT_ALT);
        }
        if (existing == null) {
            return new KeyMapping[] {key};
        }
        for (final KeyMapping k : existing) {
            if (k == key) {
                return existing;
            }
        }
        final KeyMapping[] out = java.util.Arrays.copyOf(existing, existing.length + 1);
        out[existing.length] = key;
        return out;
    }

    /** "Left Alt" for the settings screen. */
    public static String describeKey() {
        return key == null ? "Not registered" : key.getTranslatedKeyMessage().getString();
    }
}
