package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Entity culling: entities beyond the cull distance are not drawn. */
@Mixin(Render.class)
public abstract class EntityCullMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$cull(final Entity entity, final ICamera camera, final double camX, final double camY,
                                final double camZ, final CallbackInfoReturnable<Boolean> cir) {
        if (ClientHooks.shouldCullEntity(entity, camX, camY, camZ)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
}
