package com.aetherium.mixin.core;

import com.aetherium.client.TerrainRenderer;
import net.minecraft.client.renderer.ChunkRenderContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Terrain renderer: the position every chunk offset of this layer is relative to. */
@Mixin(ChunkRenderContainer.class)
public abstract class ChunkContainerMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void aetherium$camera(final double x, final double y, final double z, final CallbackInfo ci) {
        TerrainRenderer.setCamera(x, y, z);
    }
}
