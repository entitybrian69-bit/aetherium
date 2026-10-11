package com.aetherium.mixin.core;

import com.aetherium.client.ChunkInfoOrigin;

import net.minecraft.core.BlockPos;
// @era:section-vis-begin sections|render-origin
// @era:section-vis-else list16|list17|list18
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
// @era:section-vis-end

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Section origin of a visible-list entry on 1.16.5-1.20.1, where the list holds the non-public
 * {@code LevelRenderer$RenderChunkInfo} instead of the sections themselves. Applied only where
 * {@code Capabilities.CHUNK_INFO_LIST} is true; on later versions the target class does not exist
 * and the body is empty.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer$RenderChunkInfo")
public abstract class RenderChunkInfoMixin implements ChunkInfoOrigin {

    // @era:section-vis-begin sections|render-origin
    @Override
    public BlockPos aetheriumOrigin() {
        return BlockPos.ZERO;
    }

    @Override
    public Object aetheriumChunk() {
        return null;
    }
    // @era:section-vis-else list16|list17|list18
    //~ @Shadow
    //~ @Final
    //~ private ChunkRenderDispatcher.RenderChunk chunk;

    //~ @Override
    //~ public BlockPos aetheriumOrigin() {
        //~ return this.chunk.getOrigin();
    //~ }

    //~ @Override
    //~ public Object aetheriumChunk() {
        //~ return this.chunk;
    //~ }
    // @era:section-vis-end
}
