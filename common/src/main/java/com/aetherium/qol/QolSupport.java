package com.aetherium.qol;

/**
 * Which Quality of life hooks exist on this build. Each flag follows the era of the file that
 * implements it (a version without a hook for a feature reports it as missing): its mixin is not applied ({@link #mixin}) and its settings rows say
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

    public static final boolean FOOD = true;

    public static final boolean HUD = true;

    /** Minecraft 26.2+: the in-game HUD is net.minecraft.client.gui.Hud instead of Gui (QolHudMixin). */
    // @era:q-hud-begin v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261
    public static final boolean HUD_CLASS = false;
    // @era:q-hud-else x262|x263
    //~ public static final boolean HUD_CLASS = true;
    // @era:q-hud-end

    // @era:q-world-begin none
    public static final boolean WORLD = false;
    // @era:q-world-else v16
    //~ public static final boolean WORLD = true;
    // @era:q-world-end

    /**
     * Waypoints and damage numbers are drawn inside the world renderer (1.16.5); elsewhere they
     * are projected onto the HUD ({@code QolOverlay}).
     */
    // @era:q-world-begin none
    public static final boolean WORLD_IN_LEVEL = false;
    // @era:q-world-else v16
    //~ public static final boolean WORLD_IN_LEVEL = true;
    // @era:q-world-end

    // @era:q-hand-begin v16|t17|d194|d215|s219|s262
    public static final boolean HAND = true;
    // @era:q-hand-else none
    //~ public static final boolean HAND = false;
    // @era:q-hand-end

    // @era:q-fire-begin v16|f17|f214|f219|f262
    public static final boolean FIRE = true;
    // @era:q-fire-else none
    //~ public static final boolean FIRE = false;
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
                return HUD && !HUD_CLASS;
            case "QolHudMixin":
                return HUD_CLASS;
            case "QolLevelRenderMixin":
                return WORLD_IN_LEVEL;
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
