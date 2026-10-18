package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-block-entity-begin none
// @era:t-block-entity-else v16
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-block-entity-end

/**
 * Block entity limit (1.11.0, Minecraft 1.16.5): at most {@code Tuning.blockEntityLimit} block
 * entities are drawn per frame. {@code BlockEntityRenderDispatcher.render} has already checked the
 * renderer's view distance when it calls {@code tryRender}, and the level renderer walks the visible
 * chunks nearest first, so the ones left out are the farthest.
 */
@Mixin(targets = "net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher")
public abstract class TuningBlockEntityMixin {
    // @era:t-block-entity-begin none
    // @era:t-block-entity-else v16
    //~ @Shadow
    //~ private static void tryRender(final BlockEntity blockEntity, final Runnable action) {
        //~ throw new AssertionError();
    //~ }

    //~ @Redirect(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;tryRender(Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Runnable;)V"),
            //~ require = 0)
    //~ private void aetherium$limit(final BlockEntity blockEntity, final Runnable action) {
        //~ if (!Tuning.allowBlockEntity()) {
            //~ return;
        //~ }
        //~ tryRender(blockEntity, action);
    //~ }
    // @era:t-block-entity-end
}
