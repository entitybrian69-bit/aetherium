package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:qol16-end

/**
 * Far terrain (1.11.0, Minecraft 1.16.5): the chunk section at a position
 * ({@code ViewArea.getRenderChunkAt} is protected). Horizontal positions wrap around the view
 * area, so callers compare the section's origin; null only above or below the world.
 */
@Mixin(targets = "net.minecraft.client.renderer.ViewArea")
public interface TuningViewAreaAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Invoker("getRenderChunkAt")
    //~ ChunkRenderDispatcher.RenderChunk aetherium$renderChunkAt(BlockPos pos);
    // @era:qol16-end
}
