package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-api-begin srd|srd-ro
import com.aetherium.client.CoalesceHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.injection.At;
// @era:section-api-else v16|none
// @era:section-api-else crd
//~ import com.aetherium.client.CoalesceHooks;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:section-api-else mesh
//~ import com.aetherium.client.CoalesceHooks;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:section-api-end

/**
 * Rebuild coalescing, 1.17 - 26.1: the section compile loop asks each visible section
 * {@code isDirty()} once per frame ({@code setupRender} on 1.17, {@code compileChunks} on
 * 1.18 - 1.20.1, {@code compileSections} from 1.20.2); a section that is still being dirtied by
 * arriving chunk data answers "not yet" until it settles, so it is rebuilt once instead of once
 * per packet. Player edits and never-built sections are never held. 1.16.5 does the same in
 * {@code StreamingLevelMixin}; 26.2 dropped the per-section dirty flag, so the body is empty
 * there. Applied only where {@code Capabilities.COALESCE} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class CoalesceMixin {
    // @era:section-api-begin srd
    @WrapOperation(method = "compileSections", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;isDirty()Z"), require = 0)
    private boolean aetherium$coalesce(final SectionRenderDispatcher.RenderSection section, final Operation<Boolean> original) {
        return section.isDirty() && !CoalesceHooks.defer(section, section.getOrigin(), section.isDirtyFromPlayer(),
                section.getCompiled() != SectionRenderDispatcher.CompiledSection.UNCOMPILED);
    }
    // @era:section-api-else v16|none
    // @era:section-api-else crd
    //~ @WrapOperation(method = {"setupRender", "compileChunks"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final ChunkRenderDispatcher.RenderChunk section, final Operation<Boolean> original) {
        //~ // Direct call instead of original.call: no argument array per section per frame.
        //~ return section.isDirty() && !CoalesceHooks.defer(section, section.getOrigin(), section.isDirtyFromPlayer(),
                //~ section.getCompiledChunk() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED);
    //~ }
    // @era:section-api-else srd-ro
    //~ @WrapOperation(method = "compileSections", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final SectionRenderDispatcher.RenderSection section, final Operation<Boolean> original) {
        //~ return section.isDirty() && !CoalesceHooks.defer(section, section.getRenderOrigin(), section.isDirtyFromPlayer(),
                //~ section.getCompiled() != SectionRenderDispatcher.CompiledSection.UNCOMPILED);
    //~ }
    // @era:section-api-else mesh
    //~ @WrapOperation(method = "compileSections", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final SectionRenderDispatcher.RenderSection section, final Operation<Boolean> original) {
        //~ return section.isDirty() && !CoalesceHooks.defer(section, section.getRenderOrigin(), section.isDirtyFromPlayer(),
                //~ section.getSectionMesh() != CompiledSectionMesh.UNCOMPILED);
    //~ }
    // @era:section-api-end
}
