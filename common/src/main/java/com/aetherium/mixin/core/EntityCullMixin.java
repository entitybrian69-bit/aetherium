package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Distance culling: entities past the cull distance are not rendered at all. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityCullMixin {

    // @era:entity-cull-begin five
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$cull(final Entity entity, final Frustum frustum, final double camX, final double camY,
                                final double camZ, final CallbackInfoReturnable<Boolean> cir) {
        if (ClientHooks.shouldCullEntity(entity, camX, camY, camZ)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
    // @era:entity-cull-else six
    //~ @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$cull(final Entity entity, final Frustum frustum, final double camX, final double camY,
    //~                             final double camZ, final float partialTick, final CallbackInfoReturnable<Boolean> cir) {
    //~     if (ClientHooks.shouldCullEntity(entity, camX, camY, camZ)) {
    //~         cir.setReturnValue(Boolean.FALSE);
    //~     }
    //~ }
    // @era:entity-cull-end
}
