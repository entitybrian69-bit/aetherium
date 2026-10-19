package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hand-begin v16|t17|d194|d215|s219|s262
import com.aetherium.client.QolHandPose;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hand-else none
// @era:q-hand-end

/**
 * Hand editor, shield offset, "Show hand with items" and the arms sculpt (Quality of life, every
 * version). Two hooks in vanilla's renderArmWithItem: one extra transform right after it opens the
 * hand's pose (everything drawn for that hand moves with it, and vanilla's swing, equip and use
 * animations still play on top), and vanilla's empty-hand arm drawn in the item's space right
 * before the item, so the arm follows every animation the item does. The transforms themselves
 * are in {@link com.aetherium.client.QolHandPose}; this class only adapts the hook signatures:
 * the item transform enum and its "left hand" flag change in 1.19.4 and 1.21.5, the buffers became
 * a submit collector in 1.21.9 and the method was renamed in 26.2. Only the local view changes.
 */
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
public abstract class QolHandMixin {
    // @era:q-hand-begin v16|t17|d194|d215
    @Shadow
    private void renderPlayerArm(final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers, final int light,
                                 final float equip, final float swing, final HumanoidArm arm) {
    }
    // @era:q-hand-else s219|s262
    //~ @Shadow
    //~ private void renderPlayerArm(final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector buffers, final int light,
    //~                              final float equip, final float swing, final HumanoidArm arm) {
    //~ }
    // @era:q-hand-else none
    // @era:q-hand-end

    // @era:q-hand-begin d194
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
            require = 0)
    private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
                                         final InteractionHand hand, final float swing, final ItemStack stack,
                                         final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
                                         final int light, final CallbackInfo ci) {
        if (QolHandPose.apply(player, hand, stack, pose)) {
            // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
            this.renderPlayerArm(pose, buffers, light, equip, swing, QolHandPose.arm(player, hand));
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            require = 0)
    private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
                                       final InteractionHand hand, final float swing, final ItemStack stack,
                                       final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
                                       final int light, final CallbackInfo ci) {
        if (QolHandPose.showArm(player, stack)) {
            final HumanoidArm arm = QolHandPose.arm(player, hand);
            pose.pushPose();
            QolHandPose.toGrip(pose, arm);
            this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
            pose.popPose();
        }
    }
    // @era:q-hand-else v16|t17
    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
    //~         require = 0)
    //~ private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                      final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                      final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
    //~                                      final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.apply(player, hand, stack, pose)) {
    //~         // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
    //~         this.renderPlayerArm(pose, buffers, light, equip, swing, QolHandPose.arm(player, hand));
    //~     }
    //~ }

    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemTransforms$TransformType;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
    //~         require = 0)
    //~ private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                    final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                    final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
    //~                                    final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.showArm(player, stack)) {
    //~         final HumanoidArm arm = QolHandPose.arm(player, hand);
    //~         pose.pushPose();
    //~         QolHandPose.toGrip(pose, arm);
    //~         this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
    //~         pose.popPose();
    //~     }
    //~ }
    // @era:q-hand-else d215
    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
    //~         require = 0)
    //~ private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                      final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                      final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
    //~                                      final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.apply(player, hand, stack, pose)) {
    //~         // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
    //~         this.renderPlayerArm(pose, buffers, light, equip, swing, QolHandPose.arm(player, hand));
    //~     }
    //~ }

    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
    //~         require = 0)
    //~ private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                    final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                    final float equip, final PoseStack pose, final net.minecraft.client.renderer.MultiBufferSource buffers,
    //~                                    final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.showArm(player, stack)) {
    //~         final HumanoidArm arm = QolHandPose.arm(player, hand);
    //~         pose.pushPose();
    //~         QolHandPose.toGrip(pose, arm);
    //~         this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
    //~         pose.popPose();
    //~     }
    //~ }
    // @era:q-hand-else s219
    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
    //~         require = 0)
    //~ private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                      final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                      final float equip, final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector buffers,
    //~                                      final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.apply(player, hand, stack, pose)) {
    //~         // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
    //~         this.renderPlayerArm(pose, buffers, light, equip, swing, QolHandPose.arm(player, hand));
    //~     }
    //~ }

    //~ @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"),
    //~         require = 0)
    //~ private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                    final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                    final float equip, final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector buffers,
    //~                                    final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.showArm(player, stack)) {
    //~         final HumanoidArm arm = QolHandPose.arm(player, hand);
    //~         pose.pushPose();
    //~         QolHandPose.toGrip(pose, arm);
    //~         this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
    //~         pose.popPose();
    //~     }
    //~ }
    // @era:q-hand-else s262
    //~ @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER),
    //~         require = 0)
    //~ private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                      final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                      final float equip, final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector buffers,
    //~                                      final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.apply(player, hand, stack, pose)) {
    //~         // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
    //~         this.renderPlayerArm(pose, buffers, light, equip, swing, QolHandPose.arm(player, hand));
    //~     }
    //~ }

    //~ @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
    //~         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"),
    //~         require = 0)
    //~ private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTick, final float pitch,
    //~                                    final InteractionHand hand, final float swing, final ItemStack stack,
    //~                                    final float equip, final PoseStack pose, final net.minecraft.client.renderer.SubmitNodeCollector buffers,
    //~                                    final int light, final CallbackInfo ci) {
    //~     if (QolHandPose.showArm(player, stack)) {
    //~         final HumanoidArm arm = QolHandPose.arm(player, hand);
    //~         pose.pushPose();
    //~         QolHandPose.toGrip(pose, arm);
    //~         this.renderPlayerArm(pose, buffers, light, 0f, 0f, arm);
    //~         pose.popPose();
    //~     }
    //~ }
    // @era:q-hand-else none
    // @era:q-hand-end
}
