package com.aetherium.client;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.zoom.ZoomController;

// @era:zoom-begin on
import com.aetherium.mixin.core.KeyMappingAccessor;

import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
// @era:zoom-else none
// @era:zoom-end

/**
 * The zoom keys and the bridge between Minecraft and {@link ZoomController}.
 *
 * <p>Three rebindable keys (Options &gt; Controls, category "Aetherium"): Zoom (Z), Zoom In (=)
 * and Zoom Out (-). They are added to {@code Options.keyMappings} right before options.txt is
 * read, which is how vanilla's own keys get their saved bindings, so no Fabric API is needed.
 * The mixins call the static hooks below; each is a field read and a branch while zoom is idle.</p>
 */
public final class ZoomKeys {
    public static final String CATEGORY = "key.categories.aetherium";
    private static final String FALLBACK_CATEGORY = "key.categories.misc";
    private static final int KEY_Z = 90;
    private static final int KEY_EQUAL = 61;
    private static final int KEY_MINUS = 45;

    private static final ZoomController CONTROLLER = new ZoomController();

    // @era:zoom-begin on
    private static KeyMapping zoom;
    private static KeyMapping zoomIn;
    private static KeyMapping zoomOut;
    // @era:zoom-else none
    // @era:zoom-end

    private ZoomKeys() {
    }

    static long nowMs() {
        return System.nanoTime() / 1_000_000L;
    }

    /** Copies the zoom options into the controller (Apply, startup). */
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

    // ------------------------------------------------------------------ hooks (mixins)

    public static double fov(final double original) {
        final double fov = CONTROLLER.fov(original, nowMs());
        lastWorldFov = fov;
        return fov;
    }

    /**
     * The world field of view of the last frame (vanilla's, with sprint, effects and zoom), for
     * the Quality of life overlays that project world points onto the HUD.
     */
    public static volatile double lastWorldFov = 70.0;

    public static double cameraDistance(final double original) {
        return CONTROLLER.cameraDistance(original, nowMs());
    }

    public static double sensitivity(final double original) {
        return CONTROLLER.sensitivity(original, nowMs());
    }

    public static boolean smoothCamera(final boolean original) {
        return CONTROLLER.smoothCamera(original);
    }

    /** True while the zoom key is engaged (cheap; lets 1.19+ skip boxing the sensitivity). */
    public static boolean isZooming() {
        return CONTROLLER.isZooming();
    }

    // @era:zoom-begin on
    /** Mouse wheel; true = consumed by zoom (the hotbar must not scroll). */
    public static boolean onScroll(final double delta) {
        final Minecraft mc = Minecraft.getInstance();
        return mc != null && ClientHooks.currentScreen(mc) == null && CONTROLLER.scroll(delta);
    }

    /** Once per frame, before the camera and the FOV are computed. */
    public static void frame(final Minecraft mc) {
        if (zoom == null || mc == null) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            if (CONTROLLER.isZooming()) {
                CONTROLLER.reset();
            }
            return;
        }
        final boolean held = ClientHooks.currentScreen(mc) == null && zoom.isDown();
        final ZoomController.View view;
        if (mc.options.getCameraType().isFirstPerson()) {
            view = ZoomController.View.FIRST_PERSON;
        } else if (mc.options.getCameraType().isMirrored()) {
            view = ZoomController.View.THIRD_PERSON_FRONT;
        } else {
            view = ZoomController.View.THIRD_PERSON_BACK;
        }
        CONTROLLER.frame(nowMs(), held, zoomIn.isDown(), zoomOut.isDown(), view);
    }

    /** Options.load HEAD: returns the key list with the three zoom keys appended (once). */
    public static KeyMapping[] append(final KeyMapping[] existing) {
        if (zoom == null) {
            zoom = newKey("key.aetherium.zoom", KEY_Z);
            zoomIn = newKey("key.aetherium.zoom_in", KEY_EQUAL);
            zoomOut = newKey("key.aetherium.zoom_out", KEY_MINUS);
        }
        if (existing == null) {
            return new KeyMapping[] {zoom, zoomIn, zoomOut};
        }
        for (final KeyMapping key : existing) {
            if (key == zoom) {
                return existing;
            }
        }
        final KeyMapping[] out = java.util.Arrays.copyOf(existing, existing.length + 3);
        out[existing.length] = zoom;
        out[existing.length + 1] = zoomIn;
        out[existing.length + 2] = zoomOut;
        return out;
    }

    // @era:keys-begin string
    /** A key in the Aetherium category (registered on first use); shared with the Freelook key. */
    static KeyMapping newKey(final String name, final int code) {
        return new KeyMapping(name, code, registerCategory());
    }

    /**
     * The Controls screen sorts categories through a fixed table and throws on unknown names,
     * so the Aetherium category is added to it; if that is impossible the keys go under Misc.
     */
    private static String registerCategory() {
        try {
            final Map<String, Integer> order = KeyMappingAccessor.aetherium$categorySortOrder();
            if (!order.containsKey(CATEGORY)) {
                int last = 0;
                for (final Integer value : order.values()) {
                    last = Math.max(last, value.intValue());
                }
                order.put(CATEGORY, Integer.valueOf(last + 1));
            }
            return CATEGORY;
        } catch (final Throwable error) {
            return FALLBACK_CATEGORY;
        }
    }
    // @era:keys-else category-rl
    //~ private static KeyMapping.Category keyCategory;

    //~ /** A key in the Aetherium category (registered on first use); shared with the Freelook key. */
    //~ static KeyMapping newKey(final String name, final int code) {
        //~ return new KeyMapping(name, code, keyCategory());
    //~ }

    //~ /**
     //~ * The "Aetherium" category, named by the lang key {@code key.category.aetherium.main}.
     //~ * KeyMapping.Category.register throws for an id registered twice, so it runs once; if it
     //~ * fails anyway the keys go under Misc.
     //~ */
    //~ private static KeyMapping.Category keyCategory() {
        //~ if (keyCategory == null) {
            //~ try {
                //~ keyCategory = KeyMapping.Category.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("aetherium", "main"));
            //~ } catch (final Throwable error) {
                //~ keyCategory = KeyMapping.Category.MISC;
            //~ }
        //~ }
        //~ return keyCategory;
    //~ }
    // @era:keys-else category-id
    //~ private static KeyMapping.Category keyCategory;

    //~ /** A key in the Aetherium category (registered on first use); shared with the Freelook key. */
    //~ static KeyMapping newKey(final String name, final int code) {
        //~ return new KeyMapping(name, code, keyCategory());
    //~ }

    //~ /**
     //~ * The "Aetherium" category, named by the lang key {@code key.category.aetherium.main}.
     //~ * KeyMapping.Category.register throws for an id registered twice, so it runs once; if it
     //~ * fails anyway the keys go under Misc.
     //~ */
    //~ private static KeyMapping.Category keyCategory() {
        //~ if (keyCategory == null) {
            //~ try {
                //~ keyCategory = KeyMapping.Category.register(net.minecraft.resources.Identifier.fromNamespaceAndPath("aetherium", "main"));
            //~ } catch (final Throwable error) {
                //~ keyCategory = KeyMapping.Category.MISC;
            //~ }
        //~ }
        //~ return keyCategory;
    //~ }
    // @era:keys-end

    /** "Z / = / -" for the settings screen. */
    public static String describe() {
        if (zoom == null) {
            return "Not registered";
        }
        return zoom.getTranslatedKeyMessage().getString() + " / " + zoomIn.getTranslatedKeyMessage().getString()
                + " / " + zoomOut.getTranslatedKeyMessage().getString();
    }
    // @era:zoom-else none
    //~ public static boolean onScroll(final double delta) {
        //~ return false;
    //~ }

    //~ public static void frame(final net.minecraft.client.Minecraft mc) {
    //~ }

    //~ public static String describe() {
        //~ return "Not available on this version";
    //~ }
    // @era:zoom-end
}
