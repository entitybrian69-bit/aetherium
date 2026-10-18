package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.math.Vector3f;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.world.phys.AABB;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Build chunks in view first (1.12.0, Minecraft 1.16.5): the chunk build queue is a priority
 * queue ordered by the squared distance stored when a rebuild task is created. That distance is
 * weighted by where the section lies relative to the view direction: straight ahead counts as is,
 * to the side as 1.75 times, behind as 2.5 times (about 1.6 times farther). Sections within 24
 * blocks are never reordered, so the ground under you still comes first.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
public abstract class TuningBuildOrderMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Redirect(method = "createCompileTask", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;getDistToPlayerSqr()D"),
            //~ require = 0)
    //~ private double aetherium$priority(final ChunkRenderDispatcher.RenderChunk self) {
        //~ final Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        //~ final Vec3 eye = camera.getPosition();
        //~ final AABB bb = self.bb;
        //~ final double dx = bb.minX + 8.0 - eye.x;
        //~ final double dy = bb.minY + 8.0 - eye.y;
        //~ final double dz = bb.minZ + 8.0 - eye.z;
        //~ final double distSq = dx * dx + dy * dy + dz * dz;
        //~ if (!Tuning.viewFirstBuilding || distSq <= 24.0 * 24.0) {
            //~ return distSq;
        //~ }
        //~ final Vector3f look = camera.getLookVector();
        //~ final double cos = (dx * look.x() + dy * look.y() + dz * look.z()) / Math.sqrt(distSq);
        //~ return distSq * (1.0 + 0.75 * (1.0 - cos));
    //~ }
    // @era:qol16-end
}
