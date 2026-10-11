package com.aetherium.mixin.core;

import com.aetherium.client.VisibleSectionSource;
import com.aetherium.perf.SectionVisibility;

// @era:section-vis-begin sections
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
// @era:section-vis-else render-origin
//~ import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
// @era:section-vis-else list16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import net.minecraft.core.BlockPos;
// @era:section-vis-else list17|list18
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import it.unimi.dsi.fastutil.objects.ObjectArrayList;
//~ import net.minecraft.core.BlockPos;
// @era:section-vis-end
import net.minecraft.client.renderer.LevelRenderer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes vanilla's per-frame visible-section list (occlusion graph + frustum, the list terrain is
 * drawn from) to the entity occlusion test. Read-only: nothing here changes what vanilla draws.
 * Indexed loops, no iterator or lambda allocation; runs once per frame on the first entity test.
 */
@Mixin(LevelRenderer.class)
public abstract class VisibleSectionsMixin implements VisibleSectionSource {

    // @era:section-vis-begin sections
    @Shadow
    @Final
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Override
    public void aetheriumCollectVisible(final SectionVisibility out) {
        final ObjectArrayList<SectionRenderDispatcher.RenderSection> list = this.visibleSections;
        for (int i = 0, n = list.size(); i < n; i++) {
            final BlockPos origin = list.get(i).getOrigin();
            out.mark(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        }
    }
    // @era:section-vis-else render-origin
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    //~ @Override
    //~ public void aetheriumCollectVisible(final SectionVisibility out) {
        //~ final ObjectArrayList<SectionRenderDispatcher.RenderSection> list = this.visibleSections;
        //~ for (int i = 0, n = list.size(); i < n; i++) {
            //~ final BlockPos origin = list.get(i).getRenderOrigin();
            //~ out.mark(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        //~ }
    //~ }
    // @era:section-vis-else list16
    //~ @Shadow
    //~ @Final
    //~ private ObjectList<?> renderChunks;

    //~ @Override
    //~ public void aetheriumCollectVisible(final SectionVisibility out) {
        //~ final ObjectList<?> list = this.renderChunks;
        //~ for (int i = 0, n = list.size(); i < n; i++) {
            //~ final BlockPos origin = ((ChunkInfoOrigin) list.get(i)).aetheriumOrigin();
            //~ out.mark(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        //~ }
    //~ }
    // @era:section-vis-else list17
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<?> renderChunks;

    //~ @Override
    //~ public void aetheriumCollectVisible(final SectionVisibility out) {
        //~ final ObjectArrayList<?> list = this.renderChunks;
        //~ for (int i = 0, n = list.size(); i < n; i++) {
            //~ final BlockPos origin = ((ChunkInfoOrigin) list.get(i)).aetheriumOrigin();
            //~ out.mark(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        //~ }
    //~ }
    // @era:section-vis-else list18
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<?> renderChunksInFrustum;

    //~ @Override
    //~ public void aetheriumCollectVisible(final SectionVisibility out) {
        //~ final ObjectArrayList<?> list = this.renderChunksInFrustum;
        //~ for (int i = 0, n = list.size(); i < n; i++) {
            //~ final BlockPos origin = ((ChunkInfoOrigin) list.get(i)).aetheriumOrigin();
            //~ out.mark(origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        //~ }
    //~ }
    // @era:section-vis-end
}
