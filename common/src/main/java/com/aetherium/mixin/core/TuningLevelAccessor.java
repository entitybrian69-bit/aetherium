package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-levelacc-begin none
// @era:t-levelacc-else v16
//~ import net.minecraft.client.renderer.ViewArea;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:t-levelacc-end

/**
 * Far terrain (1.11.0, Minecraft 1.16.5): the view area, to find chunks inside the render distance
 * that are not built yet ({@code FarTerrain}).
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public interface TuningLevelAccessor {
    // @era:t-levelacc-begin none
    // @era:t-levelacc-else v16
    //~ @Accessor("viewArea")
    //~ ViewArea aetherium$viewArea();

    //~ /** Directional culling (1.12.0): vanilla's neighbour lookup of the visibility search. */
    //~ @Invoker("getRelativeFrom")
    //~ ChunkRenderDispatcher.RenderChunk aetherium$relativeFrom(BlockPos camera, ChunkRenderDispatcher.RenderChunk from,
                                                             //~ Direction direction);
    // @era:t-levelacc-end
}
