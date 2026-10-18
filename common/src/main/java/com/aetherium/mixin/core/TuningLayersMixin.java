package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.TuningClient;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
//~ import net.minecraft.client.renderer.entity.layers.ElytraLayer;
//~ import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
//~ import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
//~ import net.minecraft.client.renderer.entity.layers.RenderLayer;
//~ import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.player.Player;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): beyond the distance, mobs and armor stands are
 * drawn without their small add-on layers (armor, held items, elytra, heads worn on the head, stuck
 * arrows and bee stingers). Layers that change what the mob is (sheep wool, slime outer body, eyes,
 * creeper charge, saddles) always render; players keep everything.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.LivingEntityRenderer")
public abstract class TuningLayersMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @SuppressWarnings({"rawtypes", "unchecked"})
    //~ @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V"),
            //~ require = 0)
    //~ private void aetherium$layer(final RenderLayer layer, final PoseStack poseStack, final MultiBufferSource buffers,
                                 //~ final int light, final Entity entity, final float limbSwing, final float limbSwingAmount,
                                 //~ final float partialTicks, final float ageInTicks, final float netHeadYaw,
                                 //~ final float headPitch) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && !(entity instanceof Player)
                //~ && (layer instanceof HumanoidArmorLayer || layer instanceof ItemInHandLayer
                        //~ || layer instanceof ElytraLayer || layer instanceof CustomHeadLayer
                        //~ || layer instanceof StuckInBodyLayer)
                //~ && Tuning.beyondDetail(TuningClient.distanceToCamera(entity))) {
            //~ return;
        //~ }
        //~ layer.render(poseStack, buffers, light, entity, limbSwing, limbSwingAmount, partialTicks, ageInTicks,
                //~ netHeadYaw, headPitch);
    //~ }
    // @era:qol16-end
}
