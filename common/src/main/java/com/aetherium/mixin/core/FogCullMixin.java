package com.aetherium.mixin.core;

import com.aetherium.client.FogCullAccess;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
// @era:fog-cull-begin section
import com.aetherium.client.FogCullHooks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:fog-cull-else info
//~ import com.aetherium.client.FogCullHooks;
//~ import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//~ import java.util.concurrent.atomic.AtomicBoolean;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:fog-cull-else bfs
//~ import com.aetherium.client.FogCullHooks;
//~ import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:fog-cull-else none
//~ // @era:fog-cull-x-begin none
//~ // @era:fog-cull-x-else extract
//~ //~ import net.minecraft.client.renderer.SectionOcclusionGraph;
//~ //~ import org.spongepowered.asm.mixin.Final;
//~ //~ import org.spongepowered.asm.mixin.Shadow;
//~ // @era:fog-cull-x-end
// @era:fog-cull-end

/**
 * Fog cull (1.17+, part of "Smarter chunk culling"; on 26.2+ the filter itself is in
 * FogCullExtractMixin, as the frustum pass moved to LevelExtractor): after vanilla filled its visible-section
 * list for the frustum, sections wholly inside solid fog are removed from it, so they are neither
 * drawn nor rebuilt while hidden (Sodium's fog occlusion does the same). The list is compacted in
 * place. When the fog limit moves, {@link #aetheriumRefreshFogCull} makes vanilla refill the list
 * (1.17: a new visibility search; later: a new frustum pass over the search result).
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class FogCullMixin implements FogCullAccess {

    // @era:fog-cull-begin section
    @Shadow
    @Final
    private ObjectArrayList<Object> visibleSections;

    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;

    @Inject(method = "applyFrustum", at = @At("TAIL"), require = 0)
    private void aetherium$fogCull(final CallbackInfo ci) {
        // ObjectArrayList implements List (get/set are array reads and writes).
        final List<Object> list = aetherium$list(this.visibleSections);
        if (!FogCullHooks.begin()) {
            return;
        }
        final int n = list.size();
        int kept = 0;
        for (int i = 0; i < n; i++) {
            final Object item = list.get(i);
            // @era:section-api-begin srd
            final BlockPos origin = ((SectionRenderDispatcher.RenderSection) item).getOrigin();
            // @era:section-api-else srd-ro|mesh
            //~ final BlockPos origin = ((SectionRenderDispatcher.RenderSection) item).getRenderOrigin();
            // @era:section-api-end
            if (!FogCullHooks.hidden(origin.getX(), origin.getY(), origin.getZ())) {
                list.set(kept++, item);
            }
        }
        if (kept != n) {
            list.subList(kept, n).clear();
        }
    }

    @Override
    public void aetheriumRefreshFogCull() {
        final Object graph = this.sectionOcclusionGraph;
        if (graph instanceof SectionOcclusionGraphAccessor) {
            ((SectionOcclusionGraphAccessor) graph).aetheriumNeedsFrustumUpdate().set(true);
        }
    }
    // @era:fog-cull-else info
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<Object> renderChunksInFrustum;

    //~ @Shadow
    //~ @Final
    //~ private AtomicBoolean needsFrustumUpdate;

    //~ @Inject(method = "applyFrustum", at = @At("TAIL"), require = 0)
    //~ private void aetherium$fogCull(final CallbackInfo ci) {
        //~ aetherium$filter(this.renderChunksInFrustum);
    //~ }

    //~ @Override
    //~ public void aetheriumRefreshFogCull() {
        //~ this.needsFrustumUpdate.set(true);
    //~ }
    // @era:fog-cull-else bfs
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<Object> renderChunks;

    //~ @Shadow
    //~ private boolean needsUpdate;

    //~ @Inject(method = "setupRender", at = @At("TAIL"), require = 0)
    //~ private void aetherium$fogCull(final CallbackInfo ci) {
        //~ aetherium$filter(this.renderChunks);
    //~ }

    //~ @Override
    //~ public void aetheriumRefreshFogCull() {
        //~ this.needsUpdate = true;
    //~ }
    // @era:fog-cull-else none
    //~ // @era:fog-cull-x-begin none
    //~ @Override
    //~ public void aetheriumRefreshFogCull() {
    //~ }
    //~ // @era:fog-cull-x-else extract
    //~ //~ @Shadow
    //~ //~ @Final
    //~ //~ private SectionOcclusionGraph sectionOcclusionGraph;

    //~ //~ /** 26.2+: the list is filtered by FogCullExtractMixin; the refresh still asks for a frustum pass. */
    //~ //~ @Override
    //~ //~ public void aetheriumRefreshFogCull() {
        //~ //~ final Object graph = this.sectionOcclusionGraph;
        //~ //~ if (graph instanceof SectionOcclusionGraphAccessor) {
            //~ //~ ((SectionOcclusionGraphAccessor) graph).aetheriumNeedsFrustumUpdate().set(true);
        //~ //~ }
    //~ //~ }
    //~ // @era:fog-cull-x-end
    // @era:fog-cull-end

    @SuppressWarnings("unchecked")
    private static List<Object> aetherium$list(final Object vanilla) {
        return (List<Object>) vanilla;
    }

    // @era:fog-cull-begin section|none
    // @era:fog-cull-else bfs|info
    //~ /** A list of LevelRenderer$RenderChunkInfo (package-private: reached through its accessor). */
    //~ private static void aetherium$filter(final ObjectArrayList<Object> vanilla) {
        //~ final List<Object> list = aetherium$list(vanilla);
        //~ if (!FogCullHooks.begin()) {
            //~ return;
        //~ }
        //~ final int n = list.size();
        //~ int kept = 0;
        //~ for (int i = 0; i < n; i++) {
            //~ final Object item = list.get(i);
            //~ if (!(item instanceof RenderChunkInfoAccessor)) {
                //~ return; // the accessor did not apply: leave vanilla's list alone
            //~ }
            //~ final ChunkRenderDispatcher.RenderChunk chunk = ((RenderChunkInfoAccessor) item).aetheriumChunk();
            //~ final BlockPos origin = chunk.getOrigin();
            //~ if (!FogCullHooks.hidden(origin.getX(), origin.getY(), origin.getZ())) {
                //~ list.set(kept++, item);
            //~ }
        //~ }
        //~ if (kept != n) {
            //~ list.subList(kept, n).clear();
        //~ }
    //~ }
    // @era:fog-cull-end
}
