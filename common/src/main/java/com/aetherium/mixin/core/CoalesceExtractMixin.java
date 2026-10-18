package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:level-extract-begin none
// @era:level-extract-else x262
//~ import com.aetherium.client.CoalesceHooks;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.llamalad7.mixinextras.sugar.Local;
//~ import net.minecraft.client.SectionUpdateTracker;
//~ import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:level-extract-end

/**
 * Rebuild coalescing on 26.2+: {@code LevelExtractor.extract} asks each visible section's dirty
 * state {@code isDirty()} once per frame and queues the dirty ones for a rebuild (clearing the
 * flag). A section still being dirtied by arriving chunk data answers "not yet" until it settles,
 * so it stays dirty and is rebuilt once instead of once per packet; player edits are never held.
 * The same decision as {@code CoalesceMixin} on 1.17 - 26.1. Applied only where
 * {@code Capabilities.COALESCE_EXTRACT} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.extract.LevelExtractor")
public abstract class CoalesceExtractMixin {
    // @era:level-extract-begin none
    // @era:level-extract-else x262
    //~ @WrapOperation(method = "extract", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/SectionUpdateTracker$SectionDirtyState;isDirty()Z"), require = 0)
    //~ private boolean aetherium$coalesce(final SectionUpdateTracker.SectionDirtyState state,
                                       //~ final Operation<Boolean> original,
                                       //~ @Local final SectionRenderDispatcher.RenderSection section) {
        //~ // Direct calls instead of original.call: no argument array per section per frame.
        //~ return state.isDirty() && !CoalesceHooks.defer(state, section.getRenderOrigin(), state.isDirtyFromPlayer(),
                //~ section.sectionMesh.get() != CompiledSectionMesh.UNCOMPILED);
    //~ }
    // @era:level-extract-end
}
