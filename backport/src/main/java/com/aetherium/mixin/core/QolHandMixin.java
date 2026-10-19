package com.aetherium.mixin.core;

import com.aetherium.client.QolHandPose;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hand editor, shield offset, "Show hand with items", "Show both hands" and the arms sculpt
 * (Quality of life) on 1.12.2. Two hooks in vanilla's renderItemInFirstPerson: one extra transform
 * right after it opens the hand's matrix (everything drawn for that hand moves with it, and
 * vanilla's swing, equip and use animations still play on top), and vanilla's empty-hand arm
 * drawn in the item's space right before the item, so the arm follows every animation the item
 * does. The transforms are in {@link QolHandPose}. Only the local view changes.
 */
@Mixin(ItemRenderer.class)
public abstract class QolHandMixin {
    @Shadow
    private void renderArmFirstPerson(final float equip, final float swing, final EnumHandSide side) {
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V",
                    ordinal = 0, shift = At.Shift.AFTER), require = 0)
    private void aetherium$handTransform(final AbstractClientPlayer player, final float partialTicks, final float pitch,
                                         final EnumHand hand, final float swing, final ItemStack stack,
                                         final float equip, final CallbackInfo ci) {
        if (QolHandPose.apply(player, hand, stack)) {
            // Vanilla skips an empty off hand entirely (it only draws an empty main hand).
            this.renderArmFirstPerson(equip, swing, QolHandPose.arm(player, hand));
        }
    }

    @Inject(method = "renderItemInFirstPerson(Lnet/minecraft/client/entity/AbstractClientPlayer;FFLnet/minecraft/util/EnumHand;FLnet/minecraft/item/ItemStack;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItemSide(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;Z)V"),
            require = 0)
    private void aetherium$armWithItem(final AbstractClientPlayer player, final float partialTicks, final float pitch,
                                       final EnumHand hand, final float swing, final ItemStack stack,
                                       final float equip, final CallbackInfo ci) {
        if (QolHandPose.showArm(player, stack)) {
            final EnumHandSide arm = QolHandPose.arm(player, hand);
            GlStateManager.pushMatrix();
            QolHandPose.toGrip(arm);
            this.renderArmFirstPerson(0f, 0f, arm);
            GlStateManager.popMatrix();
        }
    }
}
