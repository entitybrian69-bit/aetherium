package com.aetherium.mixin.core;

import com.aetherium.client.RenderChunkDirtyAccess;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Records when a chunk section became dirty, so repeated dirty marks while terrain streams in
 * can be merged into one rebuild ({@code RebuildCoalescer}). Two field stores per
 * {@code setDirty}. Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
public abstract class RenderChunkDirtyMixin implements RenderChunkDirtyAccess {
    // @era:qol16-begin none
    @Override
    public long aetheriumFirstDirty() {
        return 0L;
    }

    @Override
    public long aetheriumLastDirty() {
        return 0L;
    }
    // @era:qol16-else v16
    //~ @Shadow
    //~ private boolean dirty;

    //~ @Unique
    //~ private long aetherium$firstDirty;

    //~ @Unique
    //~ private long aetherium$lastDirty;

    //~ @Inject(method = "setDirty", at = @At("HEAD"), require = 0)
    //~ private void aetherium$stampDirty(final boolean playerChanged, final CallbackInfo ci) {
        //~ final long now = System.nanoTime();
        //~ if (!this.dirty || this.aetherium$firstDirty == 0L) {
            //~ this.aetherium$firstDirty = now;
        //~ }
        //~ this.aetherium$lastDirty = now;
    //~ }

    //~ @Inject(method = "setNotDirty", at = @At("HEAD"), require = 0)
    //~ private void aetherium$clearDirty(final CallbackInfo ci) {
        //~ this.aetherium$firstDirty = 0L;
    //~ }

    //~ @Override
    //~ public long aetheriumFirstDirty() {
        //~ return this.aetherium$firstDirty;
    //~ }

    //~ @Override
    //~ public long aetheriumLastDirty() {
        //~ return this.aetherium$lastDirty;
    //~ }
    // @era:qol16-end
}
