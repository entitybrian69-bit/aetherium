package com.aetherium.mixin.core;

import com.aetherium.client.TerrainRenderer;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Terrain renderer, draw side: one ranged draw (or none) per chunk instead of the whole mesh. */
@Mixin(VboRenderList.class)
public abstract class ChunkDrawMixin {

    @Unique
    private BlockPos aetherium$origin;

    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void aetherium$begin(final EnumWorldBlockLayer layer, final CallbackInfo ci) {
        TerrainRenderer.beginLayer(layer);
    }

    @Inject(method = "renderChunkLayer", at = @At("RETURN"))
    private void aetherium$end(final EnumWorldBlockLayer layer, final CallbackInfo ci) {
        TerrainRenderer.endLayer();
        this.aetherium$origin = null;
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getVertexBufferByLayer(I)Lnet/minecraft/client/renderer/vertex/VertexBuffer;"))
    private VertexBuffer aetherium$chunk(final RenderChunk chunk, final int layer) {
        this.aetherium$origin = chunk.getPosition();
        return chunk.getVertexBufferByLayer(layer);
    }

    @Redirect(method = "renderChunkLayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/vertex/VertexBuffer;drawArrays(I)V"))
    private void aetherium$draw(final VertexBuffer buffer, final int mode) {
        TerrainRenderer.draw(buffer, mode, this.aetherium$origin);
    }
}
