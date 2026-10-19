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
    /** Draw the arm with held items (Punchy-style visible hands). */
    public static volatile boolean handVisible;
    /** Translation in blocks, rotation in degrees, scale as a factor. */
    public static volatile float handX;
    public static volatile float handY;
    public static volatile float handZ;
    public static volatile float handPitch;
    public static volatile float handYaw;
    public static volatile float handRoll;
    public static volatile float handScale = 1f;
    /** Punchy-style "visible hands at all times": the empty off hand is drawn too. */
    public static volatile boolean armBoth;
    /** True when the arm values below differ from vanilla (both hands, with or without an item). */
    public static volatile boolean arm;
    /** Both arms, in blocks: sideways (outwards positive), height, distance (farther positive). */
    public static volatile float armX;
    public static volatile float armY;
    public static volatile float armZ;
    public static volatile float armScale = 1f;
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
        setHand(c);
    }

    /**
     * Live preview from the settings screen: shows staged hand values (config units, see
     * {@link #HAND_PRESETS}) before Apply. {@link #endHandPreview} puts the applied values back.
     */
    public static void previewHand(final int[] values) {
        handPreview = handAllowed;
        setHand(values);
    }

    /** The config keys behind {@link #previewHand}, in the order of its values (switches are 0/1). */
    public static final String[] HAND_KEYS = {
        "qol.hand.enabled", "qol.hand.visible", "qol.hand.mirror",
        "qol.hand.x", "qol.hand.y", "qol.hand.z", "qol.hand.pitch", "qol.hand.yaw", "qol.hand.roll", "qol.hand.scale",
        "qol.arm.both", "qol.arm.x", "qol.arm.y", "qol.arm.z", "qol.arm.scale",
    };

    /** The applied config as {@link #previewHand} values. */
    public static int[] handValues(final AetheriumConfig c) {
        return new int[]{
            bit(c.handEditor.get()), bit(c.handVisible.get()), bit(c.handMirror.get()),
            c.handX.get().intValue(), c.handY.get().intValue(), c.handZ.get().intValue(),
            c.handPitch.get().intValue(), c.handYaw.get().intValue(), c.handRoll.get().intValue(),
            c.handScale.get().intValue(),
            bit(c.armBoth.get()), c.armX.get().intValue(), c.armY.get().intValue(), c.armZ.get().intValue(),
            c.armScale.get().intValue(),
        };
    }

    private static int bit(final Boolean value) {
        return value.booleanValue() ? 1 : 0;
    }

    /** Ends {@link #previewHand}: back to the applied config. */
    public static void endHandPreview(final AetheriumConfig c) {
        handPreview = false;
        setHand(c);
    }

    private static void setHand(final AetheriumConfig c) {
        setHand(handValues(c));
    }

    private static void setHand(final int[] v) {
        handMirror = v[2] != 0;
        handX = clamp(v[3], -200, 200) / 100f;
        handY = clamp(v[4], -200, 200) / 100f;
        handZ = clamp(v[5], -200, 200) / 100f;
        handPitch = clamp(v[6], -180, 180);
        handYaw = clamp(v[7], -180, 180);
        handRoll = clamp(v[8], -180, 180);
        handScale = clamp(v[9], 10, 300) / 100f;
        armX = clamp(v[11], -200, 200) / 100f;
        armY = clamp(v[12], -200, 200) / 100f;
        armZ = clamp(v[13], -200, 200) / 100f;
        armScale = clamp(v[14], 10, 300) / 100f;
        hand = handAllowed && v[0] != 0;
        handVisible = handAllowed && v[1] != 0;
        armBoth = handAllowed && v[10] != 0;
        arm = handAllowed && (armX != 0f || armY != 0f || armZ != 0f || armScale != 1f);
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
