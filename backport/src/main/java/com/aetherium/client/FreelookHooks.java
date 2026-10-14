package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.qol.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * Freelook on 1.12.2: hold (or toggle) the key to turn the camera without turning the player.
 * Mouse movement is diverted in {@code EntityRenderer.updateCameraAndRender}; the camera angles are
 * replaced in Forge's {@code CameraSetup} event, which also places the third-person camera.
 */
public final class FreelookHooks {

    private static final Freelook FREELOOK = new Freelook();
    private static boolean toggle;
    private static boolean thirdPerson = true;
    private static KeyBinding key;
    private static int restoreView = -1;
    private static int forcedView = -1;

    private FreelookHooks() {
    }

    public static void register() {
        if (key == null) {
            key = ZoomKeys.newKey("key.aetherium.freelook", Keyboard.KEY_LMENU);
        }
    }

    public static void configure(final AetheriumConfig config) {
        toggle = config.freelookToggle.get().booleanValue();
        thirdPerson = config.freelookThirdPerson.get().booleanValue();
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        if (!active && FREELOOK.active()) {
            stop(Minecraft.getMinecraft());
        }
    }

    public static void onLevelChange() {
        if (FREELOOK.active()) {
            stop(Minecraft.getMinecraft());
        }
    }

    public static void frame(final Minecraft mc) {
        if (key == null || mc == null) {
            return;
        }
        if (mc.world == null || mc.player == null) {
            if (FREELOOK.active()) {
                stop(mc);
            }
            return;
        }
        final boolean down = mc.currentScreen == null && ZoomKeys.isDown(key) && Aetherium.isActive();
        final int change = FREELOOK.update(down, toggle);
        if (change == Freelook.STARTED) {
            FREELOOK.begin(mc.player.rotationYaw, mc.player.rotationPitch);
            restoreView = -1;
            forcedView = -1;
            if (thirdPerson && mc.gameSettings.thirdPersonView == 0) {
                restoreView = 0;
                forcedView = 1;
                setView(mc, 1);
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
        if (mc != null && restoreView >= 0 && mc.gameSettings.thirdPersonView == forcedView) {
            setView(mc, restoreView);
        }
        restoreView = -1;
        forcedView = -1;
    }

    private static void setView(final Minecraft mc, final int view) {
        final boolean wasFirst = mc.gameSettings.thirdPersonView == 0;
        mc.gameSettings.thirdPersonView = view;
        if (wasFirst != (view == 0) && mc.entityRenderer != null) {
            mc.entityRenderer.loadEntityShader(view == 0 ? mc.getRenderViewEntity() : null);
        }
    }

    // ------------------------------------------------------------------ hooks

    /** Mouse movement this frame; true when freelook consumed it. */
    public static boolean turn(final double dx, final double dy) {
        if (!FREELOOK.active()) {
            return false;
        }
        FREELOOK.turn(dx, dy);
        return true;
    }

    public static boolean active() {
        return FREELOOK.active();
    }

    public static float yaw() {
        return FREELOOK.yaw();
    }

    public static float pitch() {
        return FREELOOK.pitch();
    }

    public static String describeKey() {
        return key == null ? "Not registered" : key.getDisplayName();
    }
}
