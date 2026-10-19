package com.aetherium.qol;

/**
 * Which Quality of life groups exist on 1.12.2 (Aetherium Backport). Replaces the main mod's
 * era-switched class of the same name; the shared {@link Qol} reads these flags.
 */
public final class QolSupport {
    private QolSupport() {
    }

    public static final boolean PLACEMENT = true;
    public static final boolean INVENTORY = true;
    public static final boolean FOOD = true;
    public static final boolean HUD = true;
    public static final boolean HUD_CLASS = false;
    public static final boolean WORLD = true;
    /** 1.12.2 draws waypoints and damage numbers in the world (RenderWorldLastEvent). */
    public static final boolean WORLD_IN_LEVEL = true;
    public static final boolean HAND = true;
    public static final boolean FIRE = true;

    public static final boolean ANY = PLACEMENT || INVENTORY || FOOD || HUD || WORLD || HAND || FIRE;

    /** Whether a {@code Qol*Mixin} should be applied (called by the mixin plugin). */
    public static boolean mixin(final String simpleName) {
        switch (simpleName) {
            case "QolPlacementMixin":
                return PLACEMENT;
            case "QolHandMixin":
                return HAND;
            case "QolFireMixin":
                return FIRE;
            default:
                return false;
        }
    }
}
