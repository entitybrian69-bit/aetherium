package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.aetherium.client.VertexBufferAccess;
//~ import com.aetherium.perf.PreparedUploads;
//~ import com.aetherium.perf.UploadBudget;
//~ import com.mojang.blaze3d.systems.RenderSystem;
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
 * ({@code PreparedUploads}); does nothing there while the pipeline is off.
 *
 * <p>Meshes built on the render thread (block edits next to the player) are never touched: vanilla
 * builds them in the vertex builders it shares with the entity and held-item renderers, and each
 * mesh must leave those builders through its own upload, in order. Their uploads are instead run
 * in full in the same frame ({@code UploadBudget.requestDrainAll}), so the shared builders are empty
 * again before anything else draws with them.</p>
 *
 * <p>Applied only where {@code Capabilities.STREAMING} and
 * {@code Capabilities.AETHERIUM_RENDERER} are true.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher")
public abstract class ChunkPrepareMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "uploadChunkLayer", at = @At("HEAD"), require = 0)
    //~ private void aetherium$prepare(final BufferBuilder builder, final VertexBuffer buffer,
                                   //~ final CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        //~ if (RenderSystem.isOnRenderThread()) {
            //~ // A block edit rebuilt on the render thread, in the builders vanilla shares with the
            //~ // entity and held-item renderers: leave it to vanilla's in-order upload, and run that
            //~ // upload this frame, before those renderers use the builders again.
            //~ UploadBudget.requestDrainAll();
            //~ return;
        //~ }
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
