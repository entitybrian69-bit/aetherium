package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block entity distance: chests, signs, banners, ... beyond the chosen distance are not drawn. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class BlockEntityCullMixin {

    @Inject(method = "renderTileEntity(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true,
            require = 0)
    private void aetherium$cull(final TileEntity blockEntity, final float partialTicks, final int destroyStage,
                                final CallbackInfo ci) {
        if (ClientHooks.shouldCullBlockEntity(blockEntity)) {
            ci.cancel();
        }
    }
}
