package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-shadow-begin s17
import com.aetherium.client.TuningClient;
import com.aetherium.perf.Tuning;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-shadow-else none
// @era:t-shadow-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.player.Player;
//~ import net.minecraft.world.level.LevelReader;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-shadow-else s212
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//~ import net.minecraft.client.renderer.entity.state.PlayerRenderState;
//~ import net.minecraft.world.level.LevelReader;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-shadow-else s219
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.entity.state.EntityRenderState;
//~ import net.minecraft.world.entity.EntityType;
//~ import net.minecraft.world.level.Level;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-shadow-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): no round shadow under mobs, items and other
 * entities beyond the distance (players keep theirs). The shadow samples up to 18 blocks below an
 * entity every frame, so far crowds save real work.
 */
// @era:t-shadow-begin none|v16|s17|s212
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
// @era:t-shadow-else s219
//~ @Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderer")
// @era:t-shadow-end
public abstract class TuningShadowMixin {
    // @era:t-shadow-begin s17
    // 1.17-1.21.1: the static shadow pass itself, wherever vanilla calls it from.
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$shadow(final PoseStack poseStack, final MultiBufferSource buffers, final Entity entity,
                                         final float strength, final float partialTicks, final LevelReader level,
                                         final float radius, final CallbackInfo ci) {
        if (!(entity instanceof Player) && Tuning.beyondDetail(TuningClient.distanceToCamera(entity))) {
            ci.cancel();
        }
    }
    // @era:t-shadow-else none
    // @era:t-shadow-else v16
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
    // @era:t-shadow-else s212
    //~ // 1.21.2-1.21.8: the shadow pass reads a render state that already carries the camera distance.
    //~ @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$shadow(final PoseStack poseStack, final MultiBufferSource buffers,
                                         //~ final EntityRenderState state, final float strength, final float partialTicks,
                                         //~ final LevelReader level, final float radius, final CallbackInfo ci) {
        //~ if (!(state instanceof PlayerRenderState) && Tuning.beyondDetail(state.distanceToCameraSq)) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:t-shadow-else s219
    //~ // 1.21.9+: shadows are worked out while the render state is extracted; a far entity gets none.
    //~ @Inject(method = "extractShadow", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$shadow(final EntityRenderState state, final Minecraft mc, final Level level,
                                  //~ final CallbackInfo ci) {
        //~ if (!com.aetherium.client.PlayerType.is(state.entityType) && Tuning.beyondDetail(state.distanceToCameraSq)) {
            //~ state.shadowPieces.clear();
            //~ state.shadowRadius = 0.0F;
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:t-shadow-end
}
