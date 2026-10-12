package com.aetherium.mixin.core;

import com.aetherium.client.SectionCullLayersAccess;
import com.aetherium.render.CompactTerrain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
// @era:cull-new-begin old|srd5
// @era:cull-new-else csm6|uber26|uber262
//~ import com.aetherium.client.SectionCullLayoutAccess;
//~ import com.mojang.blaze3d.vertex.MeshData;
//~ import java.util.Map;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.SectionCompiler;
//~ import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:cull-new-else pearl
//~ import com.aetherium.client.SectionCullLayoutAccess;
//~ import com.mojang.blaze3d.vertex.MeshData;
//~ import java.util.Map;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
//~ import net.minecraft.client.renderer.chunk.SectionCompiler;
//~ import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:cull-new-end

/**
 * The Aetherium renderer on 1.21.6+: a compiled section mesh is built once per section rebuild from
 * the builder's results, and only ever drawn with the vertices of those results. It keeps the face
 * groups of each sorted layer. Applied only where {@code Capabilities.SECTION_CULL_MESH} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.CompiledSectionMesh")
public abstract class SectionCullCompiledMixin implements SectionCullLayersAccess {
    @Unique
    private CompactTerrain.Layout[] aetherium$layouts;

    @Override
    public CompactTerrain.Layout aetheriumCullLayout(final int layerOrdinal) {
        final CompactTerrain.Layout[] layouts = this.aetherium$layouts;
        return layouts != null && layerOrdinal >= 0 && layerOrdinal < layouts.length ? layouts[layerOrdinal] : null;
    }

    // @era:cull-new-begin old|srd5
    // @era:cull-new-else csm6|uber26|uber262
    //~ @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    //~ private void aetherium$keepLayouts(final TranslucencyPointOfView pointOfView, final SectionCompiler.Results results,
                                       //~ final CallbackInfo ci) {
        //~ CompactTerrain.Layout[] layouts = null;
        //~ for (final Map.Entry<ChunkSectionLayer, MeshData> entry : results.renderedLayers.entrySet()) {
            //~ final Object mesh = entry.getValue();
            //~ if (!(mesh instanceof SectionCullLayoutAccess)) {
                //~ continue;
            //~ }
            //~ final CompactTerrain.Layout layout = ((SectionCullLayoutAccess) mesh).aetheriumCullLayout();
            //~ if (layout != null) {
                //~ if (layouts == null) {
                    //~ layouts = new CompactTerrain.Layout[ChunkSectionLayer.values().length];
                //~ }
                //~ layouts[entry.getKey().ordinal()] = layout;
            //~ }
        //~ }
        //~ this.aetherium$layouts = layouts;
    //~ }
    // @era:cull-new-else pearl
    //~ @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    //~ private void aetherium$keepLayouts(final TranslucencyPointOfView pointOfView, final SectionCompiler.Results results,
                                       //~ final long buildTime, final CallbackInfo ci) {
        //~ this.aetherium$layouts = aetherium$collect(results.renderedLayers);
    //~ }

    //~ @Unique
    //~ private static CompactTerrain.Layout[] aetherium$collect(final Map<ChunkSectionLayer, MeshData> layers) {
        //~ CompactTerrain.Layout[] layouts = null;
        //~ for (final Map.Entry<ChunkSectionLayer, MeshData> entry : layers.entrySet()) {
            //~ final Object mesh = entry.getValue();
            //~ if (!(mesh instanceof SectionCullLayoutAccess)) {
                //~ continue;
            //~ }
            //~ final CompactTerrain.Layout layout = ((SectionCullLayoutAccess) mesh).aetheriumCullLayout();
            //~ if (layout != null) {
                //~ if (layouts == null) {
                    //~ layouts = new CompactTerrain.Layout[ChunkSectionLayer.values().length];
                //~ }
                //~ layouts[entry.getKey().ordinal()] = layout;
            //~ }
        //~ }
        //~ return layouts;
    //~ }
    // @era:cull-new-end
}
