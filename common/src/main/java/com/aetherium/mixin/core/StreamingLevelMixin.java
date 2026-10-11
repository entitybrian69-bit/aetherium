package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import java.util.concurrent.Executor;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.client.renderer.culling.Frustum;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyArg;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Terrain streaming in {@code LevelRenderer} (1.16.5):
 * <ul>
 *   <li>the chunk mesh dispatcher gets Aetherium's own mesh threads instead of the pool it shares
 *       with world generation;</li>
 *   <li>sections dirtied repeatedly while chunks arrive are rebuilt once
 *       ({@code RebuildCoalescer}), by answering "not dirty yet" at the one {@code isDirty()} call
 *       of {@code setupRender} while a section is still settling;</li>
 *   <li>the visible-section search that runs after every frame with chunk uploads runs at most
 *       20 times a second while uploads stream in (camera moves still update it at once).</li>
 * </ul>
 * Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class StreamingLevelMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @ModifyArg(method = "allChanged", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/LevelRenderer;Ljava/util/concurrent/Executor;ZLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)V"),
            //~ index = 2, require = 0)
    //~ private Executor aetherium$meshExecutor(final Executor vanilla) {
        //~ return QolHooks.meshExecutor(vanilla);
    //~ }

    //~ @Inject(method = "setupRender", at = @At("HEAD"), require = 0)
    //~ private void aetherium$camera(final Camera camera, final Frustum frustum, final boolean hasCapturedFrustum,
                                  //~ final int frame, final boolean spectator, final CallbackInfo ci) {
        //~ QolHooks.setRebuildCamera(camera);
    //~ }

    //~ @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final ChunkRenderDispatcher.RenderChunk chunk, final Operation<Boolean> original) {
        //~ // Direct call instead of original.call: no argument array per section per frame.
        //~ return chunk.isDirty() && !QolHooks.deferRebuild(chunk);
    //~ }

    //~ @ModifyExpressionValue(method = "compileChunksUntil", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;uploadAllPendingUploads()Z"), require = 0)
    //~ private boolean aetherium$throttleVisibility(final boolean uploaded) {
        //~ return QolHooks.visibilityUpdateAfterUploads(uploaded);
    //~ }
    // @era:qol16-end
}
