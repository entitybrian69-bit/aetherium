package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import java.util.Set;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): the layer bookkeeping of a section being meshed, so
 * a baked chest opens the cutout layer exactly as a block would.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$CompiledChunk")
public interface TuningCompiledAccessor {
// @era:boost16-begin none
// @era:boost16-else v16
    //~ @Accessor("hasLayer")
    //~ Set<RenderType> aetherium$hasLayer();

    //~ @Accessor("hasBlocks")
    //~ Set<RenderType> aetherium$hasBlocks();

    //~ @Accessor("isCompletelyEmpty")
    //~ void aetherium$setCompletelyEmpty(boolean empty);
// @era:boost16-end
}
