package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-compile-queue-begin none
// @era:t-compile-queue-else q212
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Position;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-compile-queue-else q2111
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Position;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-compile-queue-end

/**
 * Build chunks in view first, Minecraft 1.21.2 - 26.1 (the 1.16.5 - 1.21.1 versions are in
 * {@link TuningBuildOrderMixin}): the compile queue hands out the task with the smallest squared
 * distance to the camera. That distance is weighted by where the section lies relative to the view
 * direction: straight ahead counts as is, behind as up to 2.5 times. Sections within 24 blocks are
 * never reordered, and rebuilds keep vanilla's own quota logic.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.CompileTaskDynamicQueue")
public abstract class TuningCompileQueueMixin {
    // @era:t-compile-queue-begin none
    // @era:t-compile-queue-else q212
    //~ @Redirect(method = "poll", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"),
            //~ require = 0)
    //~ private double aetherium$priority(final BlockPos origin, final Position cameraPos) {
        //~ final double distSq = origin.distToCenterSqr(cameraPos);
        //~ if (!Tuning.viewFirstBuilding || distSq <= 24.0 * 24.0 || !(cameraPos instanceof Vec3)) {
            //~ return distSq;
        //~ }
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ return distSq;
        //~ }
        //~ final Vec3 eye = (Vec3) cameraPos;
        //~ final var look = camera.getLookVector();
        //~ final double cos = ((origin.getX() + 0.5 - eye.x) * look.x() + (origin.getY() + 0.5 - eye.y) * look.y()
                //~ + (origin.getZ() + 0.5 - eye.z) * look.z()) / Math.sqrt(distSq);
        //~ return distSq * (1.0 + 0.75 * (1.0 - cos));
    //~ }
    // @era:t-compile-queue-else q2111
    //~ @Redirect(method = "poll", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(Lnet/minecraft/core/Position;)D"),
            //~ require = 0)
    //~ private double aetherium$priority(final BlockPos origin, final Position cameraPos) {
        //~ final double distSq = origin.distToCenterSqr(cameraPos);
        //~ if (!Tuning.viewFirstBuilding || distSq <= 24.0 * 24.0 || !(cameraPos instanceof Vec3)) {
            //~ return distSq;
        //~ }
        //~ final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        //~ if (camera == null) {
            //~ return distSq;
        //~ }
        //~ final Vec3 eye = (Vec3) cameraPos;
        //~ final var look = camera.forwardVector();
        //~ final double cos = ((origin.getX() + 0.5 - eye.x) * look.x() + (origin.getY() + 0.5 - eye.y) * look.y()
                //~ + (origin.getZ() + 0.5 - eye.z) * look.z()) / Math.sqrt(distSq);
        //~ return distSq * (1.0 + 0.75 * (1.0 - cos));
    //~ }
    // @era:t-compile-queue-end
}
