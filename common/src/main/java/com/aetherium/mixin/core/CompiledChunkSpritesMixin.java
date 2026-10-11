package com.aetherium.mixin.core;

import com.aetherium.perf.SectionSprites;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import org.spongepowered.asm.mixin.Unique;
// @era:qol16-end

/**
 * The animated sprites a compiled chunk section uses (1.16.5); written once by the thread that
 * built it, before the section is handed to the render thread. Applied only where
 * {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$CompiledChunk")
public abstract class CompiledChunkSpritesMixin implements SectionSprites {
    // @era:qol16-begin none
    @Override
    public Object[] aetheriumSprites() {
        return null;
    }

    @Override
    public void aetheriumSetSprites(final Object[] sprites) {
    }
    // @era:qol16-else v16
    //~ @Unique
    //~ private Object[] aetherium$sprites;

    //~ @Override
    //~ public Object[] aetheriumSprites() {
        //~ return this.aetherium$sprites;
    //~ }

    //~ @Override
    //~ public void aetheriumSetSprites(final Object[] sprites) {
        //~ this.aetherium$sprites = sprites;
    //~ }
    // @era:qol16-end
}
