package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.aetherium.perf.PreparedUploads;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import java.util.concurrent.CompletableFuture;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:qol16-end

/**
 * Converts finished chunk meshes for the Aetherium pipeline on the worker thread that built
 * them, right before vanilla queues their upload for the render thread
 * ({@code PreparedUploads}). Applied only where {@code Capabilities.STREAMING} and
 * {@code Capabilities.EXPERIMENTAL_CHUNK_RENDERER} are true; does nothing while the pipeline is off.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher")
public abstract class ChunkPrepareMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "uploadChunkLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$prepare(final BufferBuilder builder, final VertexBuffer buffer,
                                   //~ final CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        //~ if (QolHooks.prepareUploads() && (Object) buffer instanceof VertexBufferAccess) {
            //~ ((VertexBufferAccess) (Object) buffer).aetheriumPrepare(builder);
        //~ }
    //~ }

    //~ @Inject(method = "dispose", at = @At("HEAD"), require = 0)
    //~ private void aetherium$dispose(final CallbackInfo ci) {
        //~ PreparedUploads.clear();
    //~ }
    // @era:qol16-end
}
