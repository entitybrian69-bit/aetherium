package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-group-begin g216
import com.aetherium.client.ClientHooks;
import com.aetherium.client.FarTerrain;
import com.aetherium.client.FarTerrainFrame;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-far-group-else none
// @era:t-far-group-end

/**
 * Far terrain, 1.21.6+: the opaque terrain group is drawn by {@code ChunkSectionsToRender.renderGroup}
 * (its signature changed twice; the group is read with {@code @Local}, so one hook fits all). Far
 * terrain goes right before it, with the matrices {@code TuningFarDrawMixin} recorded this frame.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkSectionsToRender")
public abstract class TuningFarGroupMixin {
    // @era:t-far-group-begin g216
    @Inject(method = "renderGroup", at = @At("HEAD"), require = 0)
    private void aetherium$farTerrain(final CallbackInfo ci,
                                      @Local(argsOnly = true) final ChunkSectionLayerGroup group) {
        if (group != ChunkSectionLayerGroup.OPAQUE || !FarTerrainFrame.ready()) {
            return;
        }
        final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        if (camera == null) {
            return;
        }
        final Vec3 pos = ClientHooks.cameraPosition(camera);
        FarTerrain.draw(FarTerrainFrame.MODEL_VIEW, FarTerrainFrame.PROJECTION, pos.x, pos.y, pos.z);
    }
    // @era:t-far-group-else none
    // @era:t-far-group-end
}
