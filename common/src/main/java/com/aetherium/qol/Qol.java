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
    /** True while the settings screen shows staged (not yet applied) hand values live. */
    public static volatile boolean handPreview;
    /** The hand hook exists and Aetherium is active (last {@link #configure}). */
    private static volatile boolean handAllowed;

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

        handAllowed = active && QolSupport.HAND;
        handPreview = false;
        setHand(c.handEditor.get().booleanValue(), c.handMirror.get().booleanValue(), c.handX.get().intValue(),
                c.handY.get().intValue(), c.handZ.get().intValue(), c.handPitch.get().intValue(),
                c.handYaw.get().intValue(), c.handRoll.get().intValue(), c.handScale.get().intValue());
    }

    /**
     * Live preview from the settings screen: shows staged hand values (config units, see
     * {@link #HAND_PRESETS}) before Apply. {@link #endHandPreview} puts the applied values back.
     */
    public static void previewHand(final boolean enabled, final boolean mirror, final int x, final int y, final int z,
                                   final int pitch, final int yaw, final int roll, final int scale) {
        handPreview = handAllowed && enabled;
        setHand(enabled, mirror, x, y, z, pitch, yaw, roll, scale);
    }

    /** Ends {@link #previewHand}: back to the applied config. */
    public static void endHandPreview(final AetheriumConfig c) {
        handPreview = false;
        setHand(c.handEditor.get().booleanValue(), c.handMirror.get().booleanValue(), c.handX.get().intValue(),
                c.handY.get().intValue(), c.handZ.get().intValue(), c.handPitch.get().intValue(),
                c.handYaw.get().intValue(), c.handRoll.get().intValue(), c.handScale.get().intValue());
    }

    private static void setHand(final boolean enabled, final boolean mirror, final int x, final int y, final int z,
                                final int pitch, final int yaw, final int roll, final int scale) {
        handMirror = mirror;
        handX = clamp(x, -50, 50) / 100f;
        handY = clamp(y, -50, 50) / 100f;
        handZ = clamp(z, -50, 50) / 100f;
        handPitch = clamp(pitch, -90, 90);
        handYaw = clamp(yaw, -90, 90);
        handRoll = clamp(roll, -90, 90);
        handScale = clamp(scale, 25, 200) / 100f;
        hand = handAllowed && enabled;
    }

    // ------------------------------------------------------------------ hand presets

    /**
     * Values of each hand preset, in the order x, y, z, pitch, yaw, roll, scale (the config units:
     * hundredths of a block, degrees, percent). Index = {@code HandPreset.ordinal() - 1}.
     */
    public static final int[][] HAND_PRESETS = {
        {0, 0, 0, 0, 0, 0, 100},          // Default: vanilla
        {0, -22, 0, 0, 0, 0, 100},        // Lowered: well below the crosshair
        {-30, -4, -8, 0, 0, 0, 100},      // Centered: close to the middle of the screen
        {6, -6, 0, 0, 0, 0, 65},          // Small
        {-6, 4, 0, 0, 0, 0, 140},         // Big
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
