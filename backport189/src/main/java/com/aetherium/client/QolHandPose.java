package com.aetherium.client;

import com.aetherium.qol.Qol;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;

/**
 * The first-person hand transforms of the Quality of life tab on 1.8.9 (QolHandMixin only hooks
 * them in): the arm sculpt (Punchy's arm editor), the lifted pose for "Show hand with items", the
 * lowered sword block, the hand editor, and the grip transform that puts vanilla's empty-hand arm
 * around a held item. 1.8.9 has one hand only (no off hand), so "Show both hands" and the mirror
 * have nothing to act on. The resting pivot (0.56, -0.52, -0.72) is the same as on later versions;
 * the arm and grip numbers below are worked out from 1.8.9's own transforms
 * (renderPlayerArm, transformFirstPersonItem, the first-person item display and preTransform).
 *
 * <p>Render thread only; no allocation.
 */
public final class QolHandPose {
    private static final float PIVOT_X = 0.56f;
    private static final float PIVOT_Y = -0.52f;
    private static final float PIVOT_Z = -0.72f;
    private static final float LIFT_X = -0.08f;
    private static final float LIFT_Y = 0.2f;
    private static final float LIFT_Z = 0.02f;
    /** Pixel (2.5, 2.5) of a flat held item (a sword's handle), in the space right before renderItem. */
    private static final float GRIP_X = 0.2f;
    private static final float GRIP_Y = -0.277f;
    private static final float GRIP_Z = 0.05f;
    /** transformFirstPersonItem scales by 0.4 and turns 45 degrees: undone for the arm. */
    private static final float ITEM_SCALE = 0.4f;
    private static final float ITEM_YAW = 45f;
    /** Where renderPlayerArm (no swing, no equip, item held) puts the palm, from its starting matrix. */
    private static final float PALM_X = 0.576f;
    private static final float PALM_Y = -0.36f;
    private static final float PALM_Z = -1.086f;
    private static final float ARM_SCALE = 0.8f;

    private QolHandPose() {
    }

    /** "Show hand with items": an item (not a map, which draws its own hands) in a visible player's hand. */
    public static boolean showArm(final AbstractClientPlayer player, final ItemStack stack) {
        return Qol.handVisible && stack != null && stack.getItem() != Items.filled_map && !player.isInvisible();
    }

    /** Right after vanilla opens the hand's matrix in renderItemInFirstPerson. */
    public static void apply(final AbstractClientPlayer player, final ItemStack stack) {
        final boolean editor = Qol.hand;
        final int shield = Qol.shieldOffset;
        final boolean showArm = showArm(player, stack);
        final boolean sculpt = Qol.arm;
        if (!editor && shield == 0 && !showArm && !sculpt) {
            return;
        }
        if (sculpt) {
            GlStateManager.translate(Qol.armX, Qol.armY, -Qol.armZ);
            final float scale = Qol.armScale;
            if (scale != 1f) {
                GlStateManager.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
                GlStateManager.scale(scale, scale, scale);
                GlStateManager.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
            }
        }
        if (showArm) {
            GlStateManager.translate(LIFT_X, LIFT_Y, LIFT_Z);
        }
        if (shield > 0 && stack != null && player.isUsingItem() && stack.getItemUseAction() == EnumAction.BLOCK) {
            final float k = shield / 100f;
            GlStateManager.translate(0.12f * k, -0.38f * k, 0f);
        }
        if (editor) {
            GlStateManager.translate(Qol.handX, Qol.handY, Qol.handZ);
            GlStateManager.translate(PIVOT_X, PIVOT_Y, PIVOT_Z);
            if (Qol.handYaw != 0f) {
                GlStateManager.rotate(Qol.handYaw, 0f, 1f, 0f);
            }
            if (Qol.handPitch != 0f) {
                GlStateManager.rotate(Qol.handPitch, 1f, 0f, 0f);
            }
            if (Qol.handRoll != 0f) {
                GlStateManager.rotate(Qol.handRoll, 0f, 0f, 1f);
            }
            final float scale = Qol.handScale;
            if (scale != 1f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.translate(-PIVOT_X, -PIVOT_Y, -PIVOT_Z);
        }
    }

    /**
     * In the held item's space (right before vanilla draws the item): moves vanilla's empty-hand
     * arm so its palm closes around the item's grip. The caller pushes the matrix before and pops
     * it after drawing the arm.
     */
    public static void toGrip() {
        GlStateManager.translate(GRIP_X, GRIP_Y, GRIP_Z);
        GlStateManager.rotate(-ITEM_YAW, 0f, 1f, 0f);
        final float s = ARM_SCALE / ITEM_SCALE;
        GlStateManager.scale(s, s, s);
        GlStateManager.translate(-PALM_X, -PALM_Y, -PALM_Z);
    }
}
