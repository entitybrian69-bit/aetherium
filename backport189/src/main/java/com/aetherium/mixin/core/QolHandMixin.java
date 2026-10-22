package com.aetherium.mixin.core;

import com.aetherium.client.QolHandPose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Quality of life hands on 1.8.9: the transforms live in QolHandPose. */
@Mixin(ItemRenderer.class)
public abstract class QolHandMixin {

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private void renderPlayerArm(final AbstractClientPlayer player, final float equip, final float swing) {
    }

    @Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", ordinal = 0,
            shift = At.Shift.AFTER), require = 0)
    private void aetherium$handTransform(final float partialTicks, final CallbackInfo ci) {
        final AbstractClientPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            QolHandPose.apply(player, this.itemToRender);
        }
    }

    @Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"),
            require = 0)
    private void aetherium$armWithItem(final float partialTicks, final CallbackInfo ci) {
        final AbstractClientPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null && QolHandPose.showArm(player, this.itemToRender)) {
            GlStateManager.pushMatrix();
            QolHandPose.toGrip();
            this.renderPlayerArm(player, 0f, 0f);
            GlStateManager.popMatrix();
        }
    }
}
