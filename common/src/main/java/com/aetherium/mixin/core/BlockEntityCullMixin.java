package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
// @era:be-render-begin render
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:be-render-else extract|extract-flag
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:be-render-end

/**
 * Block entity distance: skips chests, signs, banners, heads... past the configured distance
 * before any model work happens. Up to 1.21.8 the dispatcher renders directly; from 1.21.9 it
 * extracts a render state, and returning null is vanilla's own "not visible" answer. Beacons and
 * end gateways are exempt (their beams are meant to be seen from far away).
 */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityCullMixin {

    // @era:be-render-begin render
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$cull(final BlockEntity blockEntity, final float partialTick, @Coerce final Object poseStack,
                                @Coerce final Object buffers, final CallbackInfo ci) {
        if (ClientHooks.shouldCullBlockEntity(blockEntity)) {
            ci.cancel();
        }
    }
    // @era:be-render-else extract
    //~ @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$cull(final BlockEntity blockEntity, final float partialTick, @Coerce final Object crumbling,
                                //~ final CallbackInfoReturnable<Object> cir) {
        //~ if (ClientHooks.shouldCullBlockEntity(blockEntity)) {
            //~ cir.setReturnValue(null);
        //~ }
    //~ }
    // @era:be-render-else extract-flag
    //~ @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$cull(final BlockEntity blockEntity, final float partialTick, @Coerce final Object crumbling,
                                //~ final boolean flag, final CallbackInfoReturnable<Object> cir) {
        //~ if (ClientHooks.shouldCullBlockEntity(blockEntity)) {
            //~ cir.setReturnValue(null);
        //~ }
    //~ }
    // @era:be-render-end
}
