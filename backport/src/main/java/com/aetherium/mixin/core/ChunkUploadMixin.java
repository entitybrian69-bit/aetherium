package com.aetherium.mixin.core;

import com.aetherium.client.TerrainRenderer;
import com.aetherium.perf.UploadBudget;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Terrain renderer, upload side: each solid / cutout mesh is sorted by face direction right before
 * vanilla uploads it. Smooth chunk loading: the per-frame upload time is capped.
 */
@Mixin(ChunkRenderDispatcher.class)
public abstract class ChunkUploadMixin {

    @Shadow
    private void uploadVertexBuffer(final BufferBuilder buffer, final VertexBuffer vertexBuffer) {
        throw new AssertionError();
    }

    @Redirect(method = "uploadChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadVertexBuffer(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/client/renderer/vertex/VertexBuffer;)V"))
    private void aetherium$sortedUpload(final ChunkRenderDispatcher self, final BufferBuilder buffer,
                                        final VertexBuffer vertexBuffer, final BlockRenderLayer layer,
                                        final BufferBuilder sameBuffer, final RenderChunk chunk,
                                        final CompiledChunk compiled, final double distanceSq) {
        TerrainRenderer.beforeUpload(buffer, layer);
        uploadVertexBuffer(buffer, vertexBuffer);
    }

    @ModifyVariable(method = "runChunkUploads", at = @At("HEAD"), argsOnly = true, require = 0)
    private long aetherium$budget(final long finishTimeNano) {
        if (finishTimeNano == 0L || !UploadBudget.enabled()) {
            return finishTimeNano;
        }
        return Math.min(finishTimeNano, System.nanoTime() + UploadBudget.currentBudgetNanos());
    }
}
