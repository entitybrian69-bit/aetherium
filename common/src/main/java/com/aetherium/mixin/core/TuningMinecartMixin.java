package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.TuningClient;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.world.entity.vehicle.AbstractMinecart;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): the block shown inside a minecart (chest,
 * furnace, TNT, hopper, spawner) is not drawn beyond the distance; minecart rail systems full of
 * chest carts render a full block model per cart.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.MinecartRenderer")
public abstract class TuningMinecartMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Shadow
    //~ protected abstract void renderMinecartContents(AbstractMinecart cart, float partialTicks, BlockState state,
                                                   //~ PoseStack poseStack, MultiBufferSource buffers, int light);

    //~ @Redirect(method = "render(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/client/renderer/entity/MinecartRenderer;renderMinecartContents(Lnet/minecraft/world/entity/vehicle/AbstractMinecart;FLnet/minecraft/world/level/block/state/BlockState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            //~ require = 0)
    //~ private void aetherium$contents(final net.minecraft.client.renderer.entity.MinecartRenderer<?> self,
                                    //~ final AbstractMinecart cart, final float partialTicks, final BlockState state,
                                    //~ final PoseStack poseStack, final MultiBufferSource buffers, final int light) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && Tuning.beyondDetail(TuningClient.distanceToCamera(cart))) {
            //~ return;
        //~ }
        //~ this.renderMinecartContents(cart, partialTicks, state, poseStack, buffers, light);
    //~ }
    // @era:qol16-end
}
