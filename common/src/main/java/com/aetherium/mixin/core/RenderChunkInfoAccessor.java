package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:fog-cull-begin section|none
// @era:fog-cull-else bfs|info
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:fog-cull-end

/** The section of a visible-list entry (1.17..1.20.1; FogCullMixin). */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer$RenderChunkInfo")
public interface RenderChunkInfoAccessor {

    // @era:fog-cull-begin section|none
    // @era:fog-cull-else bfs|info
    //~ @Accessor("chunk")
    //~ ChunkRenderDispatcher.RenderChunk aetheriumChunk();
    // @era:fog-cull-end
}
