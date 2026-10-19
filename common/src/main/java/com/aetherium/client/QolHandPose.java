package com.aetherium.client;

import com.aetherium.qol.Qol;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The first-person hand transforms of the Quality of life tab, shared by every Minecraft version
 * (QolHandMixin only adapts the hook signatures): the arms sculpt (both hands, Punchy's arm
 * editor), the lifted pose for "Show hand with items", the lowered shield, the hand editor, and
 * the grip transform that puts vanilla's empty-hand arm around a held item.
 *
 * <p>Render thread only; no allocation (one reused quaternion where the math library needs it).
 */
public final class QolHandPose {
    /** Where vanilla rests the right hand (applyItemArmTransform), the pivot for rotation and scale. */
    private static final float PIVOT_X = 0.56f;
    private static final float PIVOT_Y = -0.52f;
    private static final float PIVOT_Z = -0.72f;
    /** How far a held item is lifted into view when the arm is shown (right hand; x mirrors). */
    private static final float LIFT_X = -0.08f;
    private static final float LIFT_Y = 0.2f;
    private static final float LIFT_Z = 0.02f;
    /**
     * Item grip relative to the item's resting place: pixel (2.5, 2.5) of a flat item (a sword's
     * handle) under the first-person item transform, worked out from vanilla's numbers.
     */
    private static final float GRIP_X = 0.071f;
    private static final float GRIP_Y = -0.111f;
    private static final float GRIP_Z = -0.042f;
    /** Where renderPlayerArm (no swing, no equip) puts the palm, in camera space (right hand). */
    private static final float PALM_X = 0.704f;
    private static final float PALM_Y = -0.523f;
    private static final float PALM_Z = -1.062f;
    /** The arm is drawn a little smaller than vanilla's far-away empty arm, since it sits closer. */
    private static final float ARM_SCALE = 0.8f;

    // @era:math-begin joml
    private static final org.joml.Quaternionf SCRATCH = new org.joml.Quaternionf();
    // @era:math-else mojang
    // @era:math-end

    private QolHandPose() {
    }

    /** "Show hand with items": an item (not a map, which draws its own hands) in a visible player's hand. */
    public static boolean showArm(final AbstractClientPlayer player, final ItemStack stack) {
        return Qol.handVisible && !stack.isEmpty() && stack.getItem() != Items.FILLED_MAP && !player.isInvisible();
    }

    public static HumanoidArm arm(final AbstractClientPlayer player, final InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    /**
     * Right after vanilla opens the hand's pose: everything drawn for that hand (arm, map, item)
     * moves with it, and vanilla's swing, equip and use animations still play on top.
     *
     * @return true when this is an empty off hand that "Show both hands" wants drawn: the caller
     *         draws vanilla's empty arm then (vanilla skips an empty off hand entirely)
     */
    public static boolean apply(final AbstractClientPlayer player, final InteractionHand hand, final ItemStack stack,
                                final PoseStack pose) {
        final boolean editor = Qol.hand && (hand == InteractionHand.MAIN_HAND || Qol.handMirror);
        final int shield = Qol.shieldOffset;
        final boolean showArm = showArm(player, stack);
        final boolean sculpt = Qol.arm;
        final boolean emptyOffHand = Qol.armBoth && hand == InteractionHand.OFF_HAND && stack.isEmpty()
                && !player.isInvisible();
        if (!editor && shield == 0 && !showArm && !sculpt && !emptyOffHand) {
            return false;
        }
        final float side = arm(player, hand) == HumanoidArm.RIGHT ? 1f : -1f;
        final float px = side * PIVOT_X;
        if (sculpt) {
            // Punchy's arm sculpt: both arms and what they hold, with or without an item.
            pose.translate(side * Qol.armX, Qol.armY, -Qol.armZ);
            final float scale = Qol.armScale;
            if (scale != 1f) {
                pose.translate(px, PIVOT_Y, PIVOT_Z);
                pose.scale(scale, scale, scale);
                pose.translate(-px, -PIVOT_Y, -PIVOT_Z);
            }
        }
        if (showArm) {
            pose.translate(side * LIFT_X, LIFT_Y, LIFT_Z);
        }
        if (shield > 0 && player.isUsingItem() && player.getUsedItemHand() == hand && !stack.isEmpty()
                && "BLOCK".equals(String.valueOf(stack.getUseAnimation()))) {
            final float k = shield / 100f;
            pose.translate(side * 0.12f * k, -0.38f * k, 0f);
        }
        if (editor) {
            pose.translate(side * Qol.handX, Qol.handY, Qol.handZ);
            pose.translate(px, PIVOT_Y, PIVOT_Z);
            if (Qol.handYaw != 0f) {
                rotateY(pose, side * Qol.handYaw);
            }
            if (Qol.handPitch != 0f) {
                rotateX(pose, Qol.handPitch);
            }
            if (Qol.handRoll != 0f) {
                rotateZ(pose, side * Qol.handRoll);
            }
            final float scale = Qol.handScale;
            if (scale != 1f) {
                pose.scale(scale, scale, scale);
            }
            pose.translate(-px, -PIVOT_Y, -PIVOT_Z);
        }
        return emptyOffHand;
    }

    /**
     * In the held item's space (right before vanilla draws the item): moves vanilla's empty-hand
     * arm (renderPlayerArm without swing or equip) so its palm closes around the item's grip.
     * The caller pushes the pose before and pops it after drawing the arm.
     */
    public static void toGrip(final PoseStack pose, final HumanoidArm arm) {
        final float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        pose.translate(side * GRIP_X, GRIP_Y, GRIP_Z);
        pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        pose.translate(-side * PALM_X, -PALM_Y, -PALM_Z);
    }

    // @era:math-begin joml
    private static void rotate(final PoseStack pose, final org.joml.Quaternionf rotation) {
        // @era:pose-rot-begin mul
        pose.mulPose(rotation);
        // @era:pose-rot-else rotate
        //~ pose.rotate(rotation);
        // @era:pose-rot-end
    }
    // @era:math-else mojang
    // @era:math-end

    private static void rotateX(final PoseStack pose, final float degrees) {
        // @era:math-begin joml
        rotate(pose, SCRATCH.rotationX((float) Math.toRadians(degrees)));
        // @era:math-else mojang
        //~ pose.mulPose(com.mojang.math.Vector3f.XP.rotationDegrees(degrees));
        // @era:math-end
    }

    private static void rotateY(final PoseStack pose, final float degrees) {
        // @era:math-begin joml
        rotate(pose, SCRATCH.rotationY((float) Math.toRadians(degrees)));
        // @era:math-else mojang
        //~ pose.mulPose(com.mojang.math.Vector3f.YP.rotationDegrees(degrees));
        // @era:math-end
    }

    private static void rotateZ(final PoseStack pose, final float degrees) {
        // @era:math-begin joml
        rotate(pose, SCRATCH.rotationZ((float) Math.toRadians(degrees)));
        // @era:math-else mojang
        //~ pose.mulPose(com.mojang.math.Vector3f.ZP.rotationDegrees(degrees));
        // @era:math-end
    }
}
