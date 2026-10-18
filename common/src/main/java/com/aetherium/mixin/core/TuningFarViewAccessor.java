package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-acc-begin none
// @era:t-far-acc-else v17
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:t-far-acc-end

/** Far terrain on 1.17 - 1.18.1: the section at a block position ({@link TuningFarAreaAccessor}). */
@Mixin(targets = "net.minecraft.client.renderer.ViewArea")
public interface TuningFarViewAccessor {
    // @era:t-far-acc-begin none
    // @era:t-far-acc-else v17
    //~ @Invoker("getRenderChunkAt")
    //~ ChunkRenderDispatcher.RenderChunk aetherium$farRenderChunkAt(BlockPos pos);
    // @era:t-far-acc-end
}
