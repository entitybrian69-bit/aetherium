package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-compile-begin rebuild19|rebuild202|compiler21
import com.aetherium.perf.SpriteActivity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:section-compile-else v16|param17
//~ import com.aetherium.perf.SectionSprites;
//~ import com.aetherium.perf.SpriteActivity;
//~ import java.util.Set;
//~ import net.minecraft.client.renderer.ChunkBufferBuilderPack;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:section-compile-else none
// @era:section-compile-end

/**
 * Visible-only animations: the animated sprites a section's mesh uses are collected while it is
 * built. Through 1.18.2 they go straight onto the CompiledChunk argument; from 1.19 the compiled
 * object is created right after the build on the same thread, which takes them
 * ({@link CompiledChunkSpritesMixin}).
 */
// @era:section-compile-begin compiler21
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionCompiler")
// @era:section-compile-else v16|param17
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
// @era:section-compile-else rebuild19
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
// @era:section-compile-else rebuild202
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
// @era:section-compile-else none
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
// @era:section-compile-end
public abstract class SectionSpritesMixin {
    // @era:section-compile-begin rebuild19|rebuild202|compiler21
    @Inject(method = "compile", at = @At("HEAD"), require = 0)
    private void aetherium$beginSprites(final CallbackInfoReturnable<Object> cir) {
        SpriteActivity.beginSection();
    }

    @Inject(method = "compile", at = @At("RETURN"), require = 0)
    private void aetherium$endSprites(final CallbackInfoReturnable<Object> cir) {
        SpriteActivity.endSectionPending();
    }
    // @era:section-compile-else v16|param17
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
    // @era:section-compile-else none
    // @era:section-compile-end
}
