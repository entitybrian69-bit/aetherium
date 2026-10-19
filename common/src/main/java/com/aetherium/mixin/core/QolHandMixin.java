package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hand-begin none
// @era:q-hand-else v16
//~ import com.aetherium.qol.Qol;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Vector3f;
//~ import net.minecraft.client.player.AbstractClientPlayer;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.world.InteractionHand;
//~ import net.minecraft.world.entity.HumanoidArm;
//~ import net.minecraft.world.item.ItemStack;
//~ import net.minecraft.world.item.Items;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hand-end

/**
 * Hand editor and shield offset (Quality of life, Minecraft 1.16.5). One extra transform right
 * after vanilla opens the hand's pose: everything drawn for that hand (arm, map, held item) moves
 * with it, and vanilla's own swing, equip and use animations still play on top. Rotation and
 * scale pivot around the hand's resting place, so turning the hand does not swing it around the
 * camera. Only the local view changes; nothing is sent to the server.
 *
 * <p>"Show hand with items" (Punchy-style visible hands): vanilla only draws the arm when the hand
 * is empty. With it on, the held item is lifted into view and vanilla's own arm is drawn in the
 * item's space right before the item, placed so the palm closes around the item's grip; that way
 * the arm follows every swing, eat, draw and block animation the item does.
 */
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
public abstract class QolHandMixin {
    // @era:q-hand-begin none
    // @era:q-hand-else v16
    //~ /** Where vanilla rests the right hand (applyItemArmTransform), the pivot for rotation and scale. */
    //~ private static final float PIVOT_X = 0.56f;
    //~ private static final float PIVOT_Y = -0.52f;
    //~ private static final float PIVOT_Z = -0.72f;
    //~ /** How far a held item is lifted into view when the arm is shown (right hand; x mirrors). */
    //~ private static final float LIFT_X = -0.08f;
    //~ private static final float LIFT_Y = 0.2f;
    //~ private static final float LIFT_Z = 0.02f;
    //~ /**
     //~ * Item grip relative to the item's resting place: pixel (2.5, 2.5) of a flat item (a sword's
     //~ * handle) under the first-person item transform. Worked out from vanilla's numbers offline.
     //~ */
    //~ private static final float GRIP_X = 0.071f;
    //~ private static final float GRIP_Y = -0.111f;
    //~ private static final float GRIP_Z = -0.042f;
    //~ /** Where renderPlayerArm (no swing, no equip) puts the palm, in camera space (right hand). */
    //~ private static final float PALM_X = 0.704f;
    //~ private static final float PALM_Y = -0.523f;
    //~ private static final float PALM_Z = -1.062f;
    //~ /** The arm is drawn a little smaller than vanilla's far-away empty arm, since it sits closer. */
    //~ private static final float ARM_SCALE = 0.8f;

    //~ @Shadow
    //~ private void renderPlayerArm(final PoseStack pose, final MultiBufferSource buffers, final int light,
                                 //~ final float equip, final float swing, final HumanoidArm arm) {
    //~ }

    //~ private static boolean aetherium$showArm(final AbstractClientPlayer player, final ItemStack stack) {
        //~ return Qol.handVisible && !stack.isEmpty() && stack.getItem() != Items.FILLED_MAP && !player.isInvisible();
    //~ }

    //~ private static HumanoidArm aetherium$arm(final AbstractClientPlayer player, final InteractionHand hand) {
        //~ return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    //~ }

    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemTransforms$TransformType;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            //~ require = 0)
    //~ private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
                                       //~ final InteractionHand hand, final float swing, final ItemStack stack,
                                       //~ final float equip, final PoseStack pose, final MultiBufferSource buffers,
                                       //~ final int light, final CallbackInfo ci) {
        //~ if (!aetherium$showArm(player, stack)) {
            //~ return;
        //~ }
        //~ final HumanoidArm arm = aetherium$arm(player, hand);
        //~ final float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        //~ pose.pushPose();
        //~ // From the item's resting place to the grip, then put renderPlayerArm's palm there.
        //~ pose.translate(side * GRIP_X, GRIP_Y, GRIP_Z);
        //~ pose.scale(ARM_SCALE, ARM_SCALE, ARM_SCALE);
        //~ pose.translate(-side * PALM_X, -PALM_Y, -PALM_Z);
        //~ this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
        //~ pose.popPose();
    //~ }

    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            //~ require = 0)
    //~ private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
                                         //~ final InteractionHand hand, final float swing, final ItemStack stack,
                                         //~ final float equip, final PoseStack pose, final MultiBufferSource buffers,
                                         //~ final int light, final CallbackInfo ci) {
        //~ final boolean editor = Qol.hand && (hand == InteractionHand.MAIN_HAND || Qol.handMirror);
        //~ final int shield = Qol.shieldOffset;
        //~ final boolean showArm = aetherium$showArm(player, stack);
        //~ if (!editor && shield == 0 && !showArm) {
            //~ return;
        //~ }
        //~ final HumanoidArm arm = aetherium$arm(player, hand);
        //~ final float side = arm == HumanoidArm.RIGHT ? 1f : -1f;
        //~ if (showArm) {
            //~ pose.translate(side * LIFT_X, LIFT_Y, LIFT_Z);
        //~ }
        //~ if (shield > 0 && player.isUsingItem() && player.getUsedItemHand() == hand && !stack.isEmpty()
                //~ && "BLOCK".equals(String.valueOf(stack.getUseAnimation()))) {
            //~ final float k = shield / 100f;
            //~ pose.translate(side * 0.12f * k, -0.38f * k, 0.0);
        //~ }
        //~ if (!editor) {
            //~ return;
        //~ }
        //~ pose.translate(side * Qol.handX, Qol.handY, Qol.handZ);
        //~ final float px = side * PIVOT_X;
        //~ pose.translate(px, PIVOT_Y, PIVOT_Z);
        //~ if (Qol.handYaw != 0f) {
            //~ pose.mulPose(Vector3f.YP.rotationDegrees(side * Qol.handYaw));
        //~ }
        //~ if (Qol.handPitch != 0f) {
            //~ pose.mulPose(Vector3f.XP.rotationDegrees(Qol.handPitch));
        //~ }
        //~ if (Qol.handRoll != 0f) {
            //~ pose.mulPose(Vector3f.ZP.rotationDegrees(side * Qol.handRoll));
        //~ }
        //~ final float scale = Qol.handScale;
        //~ if (scale != 1f) {
            //~ pose.scale(scale, scale, scale);
        //~ }
        //~ pose.translate(-px, -PIVOT_Y, -PIVOT_Z);
    //~ }
    // @era:q-hand-end
}
