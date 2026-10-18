package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-group-begin none
// @era:t-far-group-else g216
//~ import com.aetherium.client.FarTerrain;
//~ import com.aetherium.client.FarTerrainFrame;
//~ import com.llamalad7.mixinextras.sugar.Local;
//~ import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-group-end

/**
 * Far terrain, 1.21.6+: the opaque terrain group is drawn by {@code ChunkSectionsToRender.renderGroup}
 * (its signature changed twice; the group is read with {@code @Local}, so one hook fits all). Far
 * terrain goes right before it, with the matrices and camera position recorded this frame
 * ({@code TuningFarDrawMixin}, plus {@code TuningFarProjMixin} on 26.x).
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkSectionsToRender")
public abstract class TuningFarGroupMixin {
    // @era:t-far-group-begin none
    // @era:t-far-group-else g216
    //~ @Inject(method = "renderGroup", at = @At("HEAD"), require = 0)
    //~ private void aetherium$farTerrain(final CallbackInfo ci,
                                      //~ @Local(argsOnly = true) final ChunkSectionLayerGroup group) {
        //~ if (group != ChunkSectionLayerGroup.OPAQUE || !FarTerrainFrame.ready()) {
            //~ return;
        //~ }
        //~ FarTerrain.draw(FarTerrainFrame.MODEL_VIEW, FarTerrainFrame.PROJECTION, FarTerrainFrame.camX(),
                //~ FarTerrainFrame.camY(), FarTerrainFrame.camZ());
    //~ }
    // @era:t-far-group-end
}
