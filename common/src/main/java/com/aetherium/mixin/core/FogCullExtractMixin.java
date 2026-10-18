package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:fog-cull-x-begin none
// @era:fog-cull-x-else extract
//~ import com.aetherium.client.FogCullHooks;
//~ import java.util.List;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:fog-cull-x-end

/**
 * Fog cull on 26.2+ (part of "Smarter chunk culling"): the frustum pass that fills the
 * renderer's visible-section list moved from LevelRenderer to {@code LevelExtractor.applyFrustum}.
 * Right after it, sections wholly inside solid fog are removed from the list (compacted in place),
 * so they are neither drawn nor queued for rebuilds this frame - exactly what FogCullMixin does on
 * 1.20.2 - 26.1. Applied only where {@code Capabilities.FOG_CULL_EXTRACT} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.extract.LevelExtractor")
public abstract class FogCullExtractMixin {
    // @era:fog-cull-x-begin none
    // @era:fog-cull-x-else extract
    //~ @Shadow
    //~ @Final
    //~ private LevelRenderer levelRenderer;

    //~ @Inject(method = "applyFrustum", at = @At("TAIL"), require = 0)
    //~ private void aetherium$fogCull(final CallbackInfo ci) {
        //~ // ObjectArrayList implements List (get/set are array reads and writes).
        //~ final List<SectionRenderDispatcher.RenderSection> list = aetherium$list(this.levelRenderer.visibleSections());
        //~ if (!FogCullHooks.begin()) {
            //~ return;
        //~ }
        //~ final int n = list.size();
        //~ int kept = 0;
        //~ for (int i = 0; i < n; i++) {
            //~ final SectionRenderDispatcher.RenderSection section = list.get(i);
            //~ final BlockPos origin = section.getRenderOrigin();
            //~ if (!FogCullHooks.hidden(origin.getX(), origin.getY(), origin.getZ())) {
                //~ list.set(kept++, section);
            //~ }
        //~ }
        //~ if (kept != n) {
            //~ list.subList(kept, n).clear();
        //~ }
    //~ }

    //~ @SuppressWarnings("unchecked")
    //~ private static List<SectionRenderDispatcher.RenderSection> aetherium$list(final Object vanilla) {
        //~ return (List<SectionRenderDispatcher.RenderSection>) vanilla;
    //~ }
    // @era:fog-cull-x-end
}
