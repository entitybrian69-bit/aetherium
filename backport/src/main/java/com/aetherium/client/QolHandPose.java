package com.aetherium.client;

import com.aetherium.qol.Qol;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;

/**
 * The first-person hand transforms of the Quality of life tab on 1.12.2 (QolHandMixin only hooks
 * them in): the arms sculpt (both hands, Punchy's arm editor), the lifted pose for "Show hand
 * with items", the lowered shield, the hand editor, and the grip transform that puts vanilla's
 * empty-hand arm around a held item. Same numbers as the main mod: 1.12.2's first-person hand
 * pivot and empty-arm transform are the ones later versions kept. Applied to the GL matrix stack.
 *
 * <p>Render thread only; no allocation.
 */
public final class QolHandPose {
    /** Where vanilla rests the right hand (transformSideFirstPerson), the pivot for rotation and scale. */
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
    /** Where renderArmFirstPerson (no swing, no equip) puts the palm, in camera space (right hand). */
    private static final float PALM_X = 0.704f;
    private static final float PALM_Y = -0.523f;
    private static final float PALM_Z = -1.062f;
    /** The arm is drawn a little smaller than vanilla's far-away empty arm, since it sits closer. */
    private static final float ARM_SCALE = 0.8f;

    private QolHandPose() {
    }

    /** "Show hand with items": an item (not a map, which draws its own hands) in a visible player's hand. */
    public static boolean showArm(final AbstractClientPlayer player, final ItemStack stack) {
        return Qol.handVisible && !stack.isEmpty() && stack.getItem() != Items.FILLED_MAP && !player.isInvisible();
    }

    public static EnumHandSide arm(final AbstractClientPlayer player, final EnumHand hand) {
        return hand == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
    }

    /**
     * Right after vanilla opens the hand's matrix: everything drawn for that hand (arm, map, item)
     * moves with it, and vanilla's swing, equip and use animations still play on top.
     *
     * @return true when this is an empty off hand that "Show both hands" wants drawn: the caller
     *         draws vanilla's empty arm then (vanilla skips an empty off hand entirely)
     */
    public static boolean apply(final AbstractClientPlayer player, final EnumHand hand, final ItemStack stack) {
        final boolean editor = Qol.hand && (hand == EnumHand.MAIN_HAND || Qol.handMirror);
        final int shield = Qol.shieldOffset;
        final boolean showArm = showArm(player, stack);
        final boolean sculpt = Qol.arm;
        final boolean emptyOffHand = Qol.armBoth && hand == EnumHand.OFF_HAND && stack.isEmpty()
                && !player.isInvisible();
        if (!editor && shield == 0 && !showArm && !sculpt && !emptyOffHand) {
            return false;
        }
        final float side = arm(player, hand) == EnumHandSide.RIGHT ? 1f : -1f;
        final float px = side * PIVOT_X;
        if (sculpt) {
            // Punchy's arm sculpt: both arms and what they hold, with or without an item.
            GlStateManager.translate(side * Qol.armX, Qol.armY, -Qol.armZ);
            final float scale = Qol.armScale;
            if (scale != 1f) {
                GlStateManager.translate(px, PIVOT_Y, PIVOT_Z);
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(-px, -PIVOT_Y, -PIVOT_Z);
            }
        }
        if (showArm) {
            GlStateManager.translate(side * LIFT_X, LIFT_Y, LIFT_Z);
        }
        if (shield > 0 && player.isHandActive() && player.getActiveHand() == hand && !stack.isEmpty()
                && stack.getItemUseAction() == EnumAction.BLOCK) {
            final float k = shield / 100f;
            GlStateManager.translate(side * 0.12f * k, -0.38f * k, 0f);
        }
        if (editor) {
            GlStateManager.translate(side * Qol.handX, Qol.handY, Qol.handZ);
            GlStateManager.translate(px, PIVOT_Y, PIVOT_Z);
            if (Qol.handYaw != 0f) {
                rotateY(side * Qol.handYaw);
            }
            if (Qol.handPitch != 0f) {
                rotateX(Qol.handPitch);
            }
            if (Qol.handRoll != 0f) {
                rotateZ(side * Qol.handRoll);
            }
            final float scale = Qol.handScale;
            if (scale != 1f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.translate(-px, -PIVOT_Y, -PIVOT_Z);
        }
        return emptyOffHand;
    }

    /**
     * In the held item's space (right before vanilla draws the item): moves vanilla's empty-hand
     * arm (renderArmFirstPerson without swing or equip) so its palm closes around the item's grip.
     * The caller pushes the matrix before and pops it after drawing the arm.
     */
    public static void toGrip(final EnumHandSide arm) {
        final float side = arm == EnumHandSide.RIGHT ? 1f : -1f;
        GlStateManager.translate(side * GRIP_X, GRIP_Y, GRIP_Z);
        GlStateManager.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        GlStateManager.translate(-side * PALM_X, -PALM_Y, -PALM_Z);
    }

    private static void rotateX(final float degrees) {
        GlStateManager.rotate(degrees, 1f, 0f, 0f);
    }

    private static void rotateY(final float degrees) {
        GlStateManager.rotate(degrees, 0f, 1f, 0f);
    }

    private static void rotateZ(final float degrees) {
        GlStateManager.rotate(degrees, 0f, 0f, 1f);
    }
}
