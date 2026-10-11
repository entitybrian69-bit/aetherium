package com.aetherium.mixin.core;

import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Mixin;
// @era:near-build-begin none
// @era:near-build-else v16
//~ import com.aetherium.perf.NearBuild;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.world.level.lighting.LevelLightEngine;

//~ import org.spongepowered.asm.mixin.injection.At;
// @era:near-build-end

/**
 * Removes two render-thread stalls of 1.16.5 (see {@link com.aetherium.perf.NearBuild}): dirty
 * sections near the camera that the player did not edit are rebuilt on the worker threads instead
 * of synchronously in {@code setupRender}, and client light updates in {@code renderLevel} get a
 * per-frame time budget. Wrap-operations, so other mods hooking the same calls still chain.
 */
@Mixin(LevelRenderer.class)
public abstract class NearBuildMixin {
    // @era:near-build-begin none
    // @era:near-build-else v16
    //~ @WrapOperation(method = "setupRender", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;rebuildChunkSync(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;)V"),
            //~ require = 0)
    //~ private void aetherium$threadedNearBuild(final ChunkRenderDispatcher dispatcher, final ChunkRenderDispatcher.RenderChunk chunk,
                                             //~ final Operation<Void> original) {
        //~ if (NearBuild.threaded() && !chunk.isDirtyFromPlayer()) {
            //~ // Exactly what compileChunksUntil does for far sections; vanilla marks it not dirty next.
            //~ chunk.rebuildChunkAsync(dispatcher);
            //~ return;
        //~ }
        //~ original.call(dispatcher, chunk);
    //~ }

    //~ @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runUpdates(IZZ)I"), require = 0)
    //~ private int aetherium$lightBudget(final LevelLightEngine engine, final int maxUpdates, final boolean skyLight,
                                      //~ final boolean blockLight, final Operation<Integer> original) {
        //~ if (!NearBuild.lightBudget()) {
            //~ return original.call(engine, maxUpdates, skyLight, blockLight).intValue();
        //~ }
        //~ return NearBuild.runLight(
                //~ slice -> original.call(engine, slice, skyLight, blockLight).intValue(),
                //~ engine::hasLightWork);
    //~ }
    // @era:near-build-end
}
