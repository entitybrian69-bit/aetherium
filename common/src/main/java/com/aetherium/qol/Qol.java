package com.aetherium.qol;

import com.aetherium.config.AetheriumConfig;

/**
 * Live values of the Quality of life options, read by the hooks every tick or frame.
 *
 * <p>Like {@code perf.Tuning}: {@link #configure} copies the config into plain static fields at
 * startup and after Apply, so a hook costs one field read while its feature is off. A feature
 * whose hook does not exist on this Minecraft version ({@link QolSupport}) stays off whatever the
 * config says.</p>
 */
public final class Qol {
    public static volatile boolean accuratePlacement;
    public static volatile boolean scrollTransfer;
    public static volatile boolean shiftDrag;
    public static volatile boolean sortButton;
    public static volatile boolean foodTooltip;
    public static volatile boolean foodHud;
    public static volatile boolean waypoints;
    public static volatile boolean waypointBeams;
    public static volatile boolean crossDimension;
    public static volatile boolean deathPoint;
    public static volatile boolean dimensionCoords;
    public static volatile String dimensionCoordsCorner = "top-left";
    /** Fire overlay height, 0..100 (100 = vanilla). */
    public static volatile int fireOverlay = 100;
    /** Shield lowering, 0..100 (0 = vanilla). */
    public static volatile int shieldOffset;
    public static volatile boolean damageNumbers;
    public static volatile boolean damageHeals;

    /** True when a custom crosshair replaces vanilla's. */
    public static volatile boolean crosshair;
    public static volatile int[] crosshairRects = new int[0];
    public static volatile int[] crosshairOutlineRects = new int[0];
    public static volatile int crosshairArgb = 0xFFFFFFFF;
    public static volatile int crosshairTargetArgb = 0xFFFF3030;
    public static volatile boolean crosshairTarget;

    /** True when the hand editor changes the first-person hand. */
    public static volatile boolean hand;
    public static volatile boolean handMirror = true;
    /** Translation in blocks, rotation in degrees, scale as a factor. */
    public static volatile float handX;
    public static volatile float handY;
    public static volatile float handZ;
    public static volatile float handPitch;
    public static volatile float handYaw;
    public static volatile float handRoll;
    public static volatile float handScale = 1f;

    private Qol() {
    }

    /** Startup and after Apply. {@code active} is false while Aetherium is paused or standing down. */
    public static void configure(final AetheriumConfig c, final boolean active) {
        accuratePlacement = active && QolSupport.PLACEMENT && c.accuratePlacement.get().booleanValue();
        scrollTransfer = active && QolSupport.INVENTORY && c.scrollTransfer.get().booleanValue();
        shiftDrag = active && QolSupport.INVENTORY && c.shiftDrag.get().booleanValue();
        sortButton = active && QolSupport.INVENTORY && c.sortButton.get().booleanValue();
        foodTooltip = active && QolSupport.FOOD && c.foodTooltip.get().booleanValue();
        foodHud = active && QolSupport.HUD && c.foodHud.get().booleanValue();
        waypoints = active && QolSupport.WORLD && c.waypoints.get().booleanValue();
        waypointBeams = c.waypointBeams.get().booleanValue();
        crossDimension = c.waypointsCrossDimension.get().booleanValue();
        deathPoint = active && QolSupport.WORLD && c.deathPoint.get().booleanValue();
        dimensionCoords = active && QolSupport.HUD && c.dimensionCoords.get().booleanValue();
        dimensionCoordsCorner = c.dimensionCoordsCorner.get();
        fireOverlay = active && QolSupport.FIRE ? clamp(c.fireOverlay.get().intValue(), 0, 100) : 100;
        shieldOffset = active && QolSupport.HAND ? clamp(c.shieldOffset.get().intValue(), 0, 100) : 0;
        damageNumbers = active && QolSupport.WORLD && c.damageNumbers.get().booleanValue();
        damageHeals = c.damageNumbersHeals.get().booleanValue();

        final AetheriumConfig.CrosshairStyle style = c.crosshairStyle.get();
        crosshair = active && QolSupport.HUD && style != AetheriumConfig.CrosshairStyle.VANILLA;
        final int[] rects = Crosshair.shape(Crosshair.Style.values()[style.ordinal()], c.crosshairSize.get().intValue(),
                c.crosshairThickness.get().intValue(), c.crosshairGap.get().intValue());
        crosshairRects = rects;
        crosshairOutlineRects = c.crosshairOutline.get().booleanValue() ? Crosshair.outline(rects) : new int[0];
        final int opacity = clamp(c.crosshairOpacity.get().intValue(), 10, 100);
        crosshairArgb = Crosshair.argb(Crosshair.rgb(Crosshair.Color.values()[c.crosshairColor.get().ordinal()]), opacity);
        crosshairTargetArgb = Crosshair.argb(Crosshair.TARGET_RGB, opacity);
        crosshairTarget = c.crosshairTargetColor.get().booleanValue();

        hand = active && QolSupport.HAND && c.handEditor.get().booleanValue();
        handMirror = c.handMirror.get().booleanValue();
        handX = c.handX.get().intValue() / 100f;
        handY = c.handY.get().intValue() / 100f;
        handZ = c.handZ.get().intValue() / 100f;
        handPitch = c.handPitch.get().intValue();
        handYaw = c.handYaw.get().intValue();
        handRoll = c.handRoll.get().intValue();
        handScale = clamp(c.handScale.get().intValue(), 25, 200) / 100f;
    }

    // ------------------------------------------------------------------ hand presets

    /**
     * Values of each hand preset, in the order x, y, z, pitch, yaw, roll, scale (the config units:
     * hundredths of a block, degrees, percent). Index = {@code HandPreset.ordinal() - 1}.
     */
    public static final int[][] HAND_PRESETS = {
        {0, 0, 0, 0, 0, 0, 100},          // Default: vanilla
        {0, -12, 0, 0, 0, 0, 100},        // Lowered: out of the way of the crosshair
        {-14, -4, -6, 0, 6, 0, 100},      // Centered: closer to the middle of the screen
        {4, -4, 0, 0, 0, 0, 70},          // Small
        {-2, 4, 6, 0, 0, 0, 130},         // Big
        {-8, -10, 8, -6, 10, -4, 115},    // Punchy: larger, lower and turned in, like an arm reaching forward
    };

    // ------------------------------------------------------------------ food

    /** Saturation a food restores: nutrition x saturation modifier x 2 (vanilla's formula). */
    public static float saturation(final int nutrition, final float modifier) {
        return nutrition * modifier * 2f;
    }

    /** "6" or "7.2", at most one decimal. */
    public static String oneDecimal(final float value) {
        final float rounded = Math.round(value * 10f) / 10f;
        return rounded == (int) rounded ? Integer.toString((int) rounded) : Float.toString(rounded);
    }

    private static int clamp(final int v, final int lo, final int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
