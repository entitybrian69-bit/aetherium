package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hand-begin v16
import com.aetherium.qol.Qol;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hand-else none
// @era:q-hand-end

/**
 * Hand editor and shield offset (Quality of life, Minecraft 1.16.5). One extra transform right
 * after vanilla opens the hand's pose: everything drawn for that hand (arm, map, held item) moves
 * with it, and vanilla's own swing, equip and use animations still play on top. Rotation and
 * scale pivot around the hand's resting place, so turning the hand does not swing it around the
 * camera. Only the local view changes; nothing is sent to the server.
 */
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
public abstract class QolHandMixin {
    // @era:q-hand-begin v16
    /** Where vanilla rests the right hand (applyItemArmTransform), the pivot for rotation and scale. */
    private static final float PIVOT_X = 0.56f;
    private static final float PIVOT_Y = -0.52f;
    private static final float PIVOT_Z = -0.72f;

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
                                         final InteractionHand hand, final float swing, final ItemStack stack,
                                         final float equip, final PoseStack pose, final MultiBufferSource buffers,
                                         final int light, final CallbackInfo ci) {
        final boolean editor = Qol.hand && (hand == InteractionHand.MAIN_HAND || Qol.handMirror);
        final int shield = Qol.shieldOffset;
        if (!editor && shield == 0) {
            return;
        }
        final HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        final float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        if (shield > 0 && player.isUsingItem() && player.getUsedItemHand() == hand && !stack.isEmpty()
                && stack.getUseAnimation() == UseAnim.BLOCK) {
            final float k = shield / 100f;
            pose.translate(side * 0.12f * k, -0.38f * k, 0.0);
        }
        if (!editor) {
            return;
        }
        pose.translate(side * Qol.handX, Qol.handY, Qol.handZ);
        final float px = side * PIVOT_X;
        pose.translate(px, PIVOT_Y, PIVOT_Z);
        if (Qol.handYaw != 0f) {
            pose.mulPose(Vector3f.YP.rotationDegrees(side * Qol.handYaw));
        }
        if (Qol.handPitch != 0f) {
            pose.mulPose(Vector3f.XP.rotationDegrees(Qol.handPitch));
        }
        if (Qol.handRoll != 0f) {
            pose.mulPose(Vector3f.ZP.rotationDegrees(side * Qol.handRoll));
        }
        final float scale = Qol.handScale;
        if (scale != 1f) {
            pose.scale(scale, scale, scale);
        }
        pose.translate(-px, -PIVOT_Y, -PIVOT_Z);
    }
    // @era:q-hand-else none
    // @era:q-hand-end
}
