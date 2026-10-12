package com.aetherium.mixin.core;

import com.aetherium.perf.SectionSprites;

import org.spongepowered.asm.mixin.Mixin;
// @era:anim-begin on
import com.aetherium.perf.SpriteActivity;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:anim-else v16
//~ import org.spongepowered.asm.mixin.Unique;
// @era:anim-else none
// @era:anim-end

/** The animated sprites a compiled section's mesh uses (CompiledChunk to 1.20.1, CompiledSection 1.20.2..1.21.4). */
// @era:section-api-begin srd
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$CompiledSection")
// @era:section-api-else v16|crd
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$CompiledChunk")
// @era:section-api-else srd-ro|mesh|none
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$CompiledChunk")
// @era:section-api-end
public abstract class CompiledChunkSpritesMixin implements SectionSprites {
    // @era:anim-begin v16|on
    @Unique
    private Object[] aetherium$sprites;

    @Override
    public Object[] aetheriumSprites() {
        return this.aetherium$sprites;
    }

    @Override
    public void aetheriumSetSprites(final Object[] sprites) {
        this.aetherium$sprites = sprites;
    }
    // @era:anim-else none
    //~ @Override
    //~ public Object[] aetheriumSprites() {
        //~ return null;
    //~ }

    //~ @Override
    //~ public void aetheriumSetSprites(final Object[] sprites) {
    //~ }
    // @era:anim-end

    // @era:section-compile-begin rebuild19|rebuild202|compiler21
    /** 1.19+: doTask creates this right after the build on the same thread; it takes the build's sprites. */
    @Inject(method = "<init>()V", at = @At("RETURN"), require = 0)
    private void aetherium$takeSprites(final CallbackInfo ci) {
        final Object[] sprites = SpriteActivity.takePending();
        if (sprites != null) {
            this.aetherium$sprites = sprites;
        }
    }
    // @era:section-compile-else v16|param17|none
    // @era:section-compile-end
}
