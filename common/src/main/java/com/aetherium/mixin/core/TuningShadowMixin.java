package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.player.Player;
//~ import net.minecraft.world.level.LevelReader;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): no round shadow under mobs, items and other
 * entities beyond the distance (players keep theirs). The shadow samples up to 18 blocks below an
 * entity every frame, so far crowds save real work.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public abstract class TuningShadowMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Shadow
    //~ public abstract double distanceToSqr(Entity entity);

    //~ @Shadow
    //~ private static void renderShadow(final PoseStack poseStack, final MultiBufferSource buffers, final Entity entity,
                                     //~ final float strength, final float partialTicks, final LevelReader level,
                                     //~ final float radius) {
        //~ throw new AssertionError();
    //~ }

    //~ @Redirect(method = "render", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"),
            //~ require = 0)
    //~ private void aetherium$shadow(final PoseStack poseStack, final MultiBufferSource buffers, final Entity entity,
                                  //~ final float strength, final float partialTicks, final LevelReader level,
                                  //~ final float radius) {
        //~ if (!(entity instanceof Player) && Tuning.beyondDetail(this.distanceToSqr(entity))) {
            //~ return;
        //~ }
        //~ renderShadow(poseStack, buffers, entity, strength, partialTicks, level, radius);
    //~ }
    // @era:qol16-end
}
