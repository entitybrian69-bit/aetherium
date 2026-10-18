package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-name-tag-begin none
// @era:t-name-tag-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//~ import net.minecraft.network.chat.Component;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.player.Player;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-name-tag-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): name tags of named mobs and armor stands are
 * not drawn beyond the distance (each one is a text draw with its own background). Player name
 * tags always render.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderer")
public abstract class TuningNameTagMixin {
    // @era:t-name-tag-begin none
    // @era:t-name-tag-else v16
    //~ @Shadow
    //~ @Final
    //~ protected EntityRenderDispatcher entityRenderDispatcher;

    //~ @Shadow
    //~ protected abstract void renderNameTag(Entity entity, Component name, PoseStack poseStack,
                                          //~ MultiBufferSource buffers, int light);

    //~ @Redirect(method = "render", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V"),
            //~ require = 0)
    //~ private void aetherium$nameTag(final net.minecraft.client.renderer.entity.EntityRenderer<?> self,
                                   //~ final Entity entity, final Component name, final PoseStack poseStack,
                                   //~ final MultiBufferSource buffers, final int light) {
        //~ if (!(entity instanceof Player) && Tuning.beyondDetail(this.entityRenderDispatcher.distanceToSqr(entity))) {
            //~ return;
        //~ }
        //~ this.renderNameTag(entity, name, poseStack, buffers, light);
    //~ }
    // @era:t-name-tag-end
}
