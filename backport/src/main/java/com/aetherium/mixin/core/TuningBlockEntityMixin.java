package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Block entities per frame: past the limit, the rest of this frame's chests, signs, banners... are skipped. */
@Mixin(TileEntityRendererDispatcher.class)
public abstract class TuningBlockEntityMixin {
    @Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;FI)V", at = @At("HEAD"), cancellable = true,
            require = 0)
    private void aetherium$limit(final TileEntity blockEntity, final float partialTicks, final int destroyStage,
                                 final CallbackInfo ci) {
        if (Tuning.blockEntityLimit != Integer.MAX_VALUE && !Tuning.allowBlockEntity()) {
            ci.cancel();
        }
    }
}
