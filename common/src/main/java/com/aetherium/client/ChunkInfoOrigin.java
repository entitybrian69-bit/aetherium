package com.aetherium.client;

import net.minecraft.core.BlockPos;

/**
 * Added to {@code LevelRenderer$RenderChunkInfo} (1.16.5-1.20.1) by {@code RenderChunkInfoMixin}:
 * the block origin of the section the entry stands for. The class is not public on those versions,
 * so the visible-list walker reaches it through this interface.
 */
public interface ChunkInfoOrigin {

    BlockPos aetheriumOrigin();

    /** The {@code ChunkRenderDispatcher$RenderChunk} itself (typed Object: the class is version-specific). */
    Object aetheriumChunk();
}
