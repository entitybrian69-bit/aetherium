package com.aetherium.mixin.core;

import com.aetherium.client.RenderChunkDirtyAccess;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-api-begin v16|crd|srd|srd-ro|mesh
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-api-else none
// @era:section-api-end

/**
 * Records when a chunk section became dirty, so repeated dirty marks while terrain streams in
 * can be merged into one rebuild ({@code RebuildCoalescer}). Two field stores per
 * {@code setDirty}. The section class is {@code ChunkRenderDispatcher.RenderChunk} up to 1.20.1 and
 * {@code SectionRenderDispatcher.RenderSection} from 1.20.2; 26.2 dropped the dirty flag. Applied only
 * where {@code Capabilities.COALESCE} is true.
 */
// @era:section-api-begin srd|srd-ro|mesh|none
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
// @era:section-api-else v16|crd
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
// @era:section-api-end
public abstract class RenderChunkDirtyMixin implements RenderChunkDirtyAccess {
    // @era:section-api-begin v16|crd|srd|srd-ro|mesh
    @Shadow
    private boolean dirty;

    @Unique
    private long aetherium$firstDirty;

    @Unique
    private long aetherium$lastDirty;

    @Inject(method = "setDirty", at = @At("HEAD"), require = 0)
    private void aetherium$stampDirty(final boolean playerChanged, final CallbackInfo ci) {
        final long now = System.nanoTime();
        if (!this.dirty || this.aetherium$firstDirty == 0L) {
            this.aetherium$firstDirty = now;
        }
        this.aetherium$lastDirty = now;
    }

    @Inject(method = "setNotDirty", at = @At("HEAD"), require = 0)
    private void aetherium$clearDirty(final CallbackInfo ci) {
        this.aetherium$firstDirty = 0L;
    }

    @Override
    public long aetheriumFirstDirty() {
        return this.aetherium$firstDirty;
    }

    @Override
    public long aetheriumLastDirty() {
        return this.aetherium$lastDirty;
    }
    // @era:section-api-else none
    //~ @Override
    //~ public long aetheriumFirstDirty() {
        //~ return 0L;
    //~ }

    //~ @Override
    //~ public long aetheriumLastDirty() {
        //~ return 0L;
    //~ }
    // @era:section-api-end
}
