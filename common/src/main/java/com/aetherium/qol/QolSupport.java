package com.aetherium.qol;

/**
 * Which Quality of life hooks exist on this build. Each flag follows the era of the file that
 * implements it (first written for Minecraft 1.16.5; the other versions report the feature as
 * missing until it is ported): its mixin is not applied ({@link #mixin}) and its settings rows say
 * "not on this version". The Quality of life tab itself is only shown when {@link #ANY} is true.
 */
public final class QolSupport {
    private QolSupport() {
    }

    // @era:q-place-begin none
    public static final boolean PLACEMENT = false;
    // @era:q-place-else v16
    //~ public static final boolean PLACEMENT = true;
    // @era:q-place-end

    // @era:q-inv-begin none
    public static final boolean INVENTORY = false;
    // @era:q-inv-else v16
    //~ public static final boolean INVENTORY = true;
    // @era:q-inv-end

    // @era:q-food-begin none
    public static final boolean FOOD = false;
    // @era:q-food-else v16
    //~ public static final boolean FOOD = true;
    // @era:q-food-end

    // @era:q-hud-begin none
    public static final boolean HUD = false;
    // @era:q-hud-else v16
    //~ public static final boolean HUD = true;
    // @era:q-hud-end

    // @era:q-world-begin none
    public static final boolean WORLD = false;
    // @era:q-world-else v16
    //~ public static final boolean WORLD = true;
    // @era:q-world-end

    // @era:q-hand-begin none
    public static final boolean HAND = false;
    // @era:q-hand-else v16
    //~ public static final boolean HAND = true;
    // @era:q-hand-end

    // @era:q-fire-begin none
    public static final boolean FIRE = false;
    // @era:q-fire-else v16
    //~ public static final boolean FIRE = true;
    // @era:q-fire-end

    /** True when at least one Quality of life feature exists, so the tab is worth showing. */
    public static final boolean ANY = PLACEMENT || INVENTORY || FOOD || HUD || WORLD || HAND || FIRE;

    /** Whether a {@code Qol*Mixin} should be applied (called by the mixin plugin). */
    public static boolean mixin(final String simpleName) {
        switch (simpleName) {
            case "QolPlacementMixin":
                return PLACEMENT;
            case "QolContainerScreenMixin":
            case "QolScrollMixin":
            case "QolSlotMixin":
                return INVENTORY;
            case "QolTooltipMixin":
                return FOOD;
            case "QolGuiMixin":
                return HUD;
            case "QolLevelRenderMixin":
            case "QolDeathScreenMixin":
                return WORLD;
            case "QolHandMixin":
                return HAND;
            case "QolFireMixin":
                return FIRE;
            default:
                return false;
        }
    }
}
