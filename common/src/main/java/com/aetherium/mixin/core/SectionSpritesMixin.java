package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.SectionSprites;
//~ import com.aetherium.perf.SpriteActivity;
//~ import java.util.Set;
//~ import net.minecraft.client.renderer.ChunkBufferBuilderPack;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:qol16-end

/**
 * Collects the animated sprites a chunk section's mesh uses while it is built (worker threads,
 * or the render thread for vanilla's synchronous rebuilds) and keeps them with the compiled
 * section, so the visible sections can mark them every tick (1.16.5). Applied only where
 * {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class SectionSpritesMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "compile", at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginSprites(final float x, final float y, final float z,
                                        //~ final ChunkRenderDispatcher.CompiledChunk compiled,
                                        //~ final ChunkBufferBuilderPack buffers,
                                        //~ final CallbackInfoReturnable<Set<BlockEntity>> cir) {
        //~ SpriteActivity.beginSection();
    //~ }

    //~ @Inject(method = "compile", at = @At("RETURN"), require = 0)
    //~ private void aetherium$endSprites(final float x, final float y, final float z,
                                      //~ final ChunkRenderDispatcher.CompiledChunk compiled,
                                      //~ final ChunkBufferBuilderPack buffers,
                                      //~ final CallbackInfoReturnable<Set<BlockEntity>> cir) {
        //~ final Object[] sprites = SpriteActivity.endSection();
        //~ if ((Object) compiled instanceof SectionSprites) {
            //~ ((SectionSprites) (Object) compiled).aetheriumSetSprites(sprites);
        //~ }
    //~ }
    // @era:qol16-end
}
