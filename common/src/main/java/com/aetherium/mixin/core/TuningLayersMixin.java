package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-layers-begin v16
import com.aetherium.client.TuningClient;
import com.aetherium.perf.ModelLod;
import com.aetherium.perf.Tuning;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-layers-else none
// @era:t-layers-else l212
//~ import com.aetherium.perf.ModelLod;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
//~ import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
//~ import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
//~ import net.minecraft.client.renderer.entity.layers.RenderLayer;
//~ import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
//~ import net.minecraft.client.renderer.entity.layers.WingsLayer;
//~ import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
//~ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//~ import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//~ import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-layers-else l219
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.SubmitNodeCollector;
//~ import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
//~ import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
//~ import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
//~ import net.minecraft.client.renderer.entity.layers.RenderLayer;
//~ import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
//~ import net.minecraft.client.renderer.entity.layers.WingsLayer;
//~ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//~ import net.minecraft.world.entity.EntityType;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-layers-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): beyond the distance, mobs and armor stands are
 * drawn without their small add-on layers (armor, held items, elytra, heads worn on the head, stuck
 * arrows and bee stingers). Layers that change what the mob is (sheep wool, slime outer body, eyes,
 * creeper charge, saddles) always render; players keep everything.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.LivingEntityRenderer")
public abstract class TuningLayersMixin {
    // @era:t-layers-begin v16
    /** Mob model detail levels (1.11.0): the level for this mob's model parts. */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), require = 0)
    private void aetherium$lodBegin(final LivingEntity entity, final float yaw, final float partialTicks,
                                    final PoseStack poseStack, final MultiBufferSource buffers, final int light,
                                    final CallbackInfo ci) {
        if (!Tuning.modelLod || Tuning.detailDistanceSq == Double.MAX_VALUE) {
            ModelLod.end();
            return;
        }
        ModelLod.begin(TuningClient.distanceToCamera(entity), entity instanceof Player || entity instanceof ArmorStand);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), require = 0)
    private void aetherium$lodEnd(final CallbackInfo ci) {
        ModelLod.end();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"),
            require = 0)
    private void aetherium$layer(final RenderLayer layer, final PoseStack poseStack, final MultiBufferSource buffers,
                                 final int light, final Entity entity, final float limbSwing, final float limbSwingAmount,
                                 final float partialTicks, final float ageInTicks, final float netHeadYaw,
                                 final float headPitch) {
        if (Tuning.detailDistanceSq != Double.MAX_VALUE && !(entity instanceof Player)
                && (layer instanceof HumanoidArmorLayer || layer instanceof ItemInHandLayer
                        || layer instanceof ElytraLayer || layer instanceof CustomHeadLayer
                        || layer instanceof StuckInBodyLayer)
                && Tuning.beyondDetail(TuningClient.distanceToCamera(entity))) {
            return;
        }
        layer.render(poseStack, buffers, light, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                netHeadYaw, headPitch);
    }
    // @era:t-layers-else none
    // @era:t-layers-else l212
    //~ // 1.21.2-1.21.8: rendering works from an extracted state that already holds the camera distance.
    //~ @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At("HEAD"), require = 0)
    //~ private void aetherium$lodBegin(final LivingEntityRenderState state, final PoseStack poseStack,
                                    //~ final MultiBufferSource buffers, final int light, final CallbackInfo ci) {
        //~ if (!Tuning.modelLod || Tuning.detailDistanceSq == Double.MAX_VALUE) {
            //~ ModelLod.end();
            //~ return;
        //~ }
        //~ ModelLod.begin(state.distanceToCameraSq,
                //~ state instanceof PlayerRenderState || state instanceof ArmorStandRenderState);
    //~ }

    //~ @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At("RETURN"), require = 0)
    //~ private void aetherium$lodEnd(final CallbackInfo ci) {
        //~ ModelLod.end();
    //~ }

    //~ @SuppressWarnings({"rawtypes", "unchecked"})
    //~ @Redirect(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"),
            //~ require = 0)
    //~ private void aetherium$layer(final RenderLayer layer, final PoseStack poseStack, final MultiBufferSource buffers,
                                 //~ final int light, final EntityRenderState state, final float yRot, final float xRot) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && !(state instanceof PlayerRenderState)
                //~ && (layer instanceof HumanoidArmorLayer || layer instanceof ItemInHandLayer
                        //~ || layer instanceof WingsLayer || layer instanceof CustomHeadLayer
                        //~ || layer instanceof StuckInBodyLayer)
                //~ && Tuning.beyondDetail(state.distanceToCameraSq)) {
            //~ return;
        //~ }
        //~ layer.render(poseStack, buffers, light, state, yRot, xRot);
    //~ }
    // @era:t-layers-else l219
    //~ /*
     //~ * 1.21.9+: layers are submitted for a later batched draw, so the model detail level (which works
     //~ * while a model is drawn) no longer applies here; far mobs still skip armour, held items, wings,
     //~ * heads and stuck arrows.
     //~ */
    //~ @SuppressWarnings({"rawtypes", "unchecked"})
    //~ @Redirect(method = "submit", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"),
            //~ require = 0)
    //~ private void aetherium$layer(final RenderLayer layer, final PoseStack poseStack, final SubmitNodeCollector nodes,
                                 //~ final int light, final EntityRenderState state, final float yRot, final float xRot) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && !com.aetherium.client.PlayerType.is(state.entityType)
                //~ && (layer instanceof HumanoidArmorLayer || layer instanceof ItemInHandLayer
                        //~ || layer instanceof WingsLayer || layer instanceof CustomHeadLayer
                        //~ || layer instanceof StuckInBodyLayer)
                //~ && Tuning.beyondDetail(state.distanceToCameraSq)) {
            //~ return;
        //~ }
        //~ layer.submit(poseStack, nodes, light, state, yRot, xRot);
    //~ }
    // @era:t-layers-end
}
