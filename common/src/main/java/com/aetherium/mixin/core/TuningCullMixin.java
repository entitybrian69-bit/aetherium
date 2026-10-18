package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-cull-begin c202
import com.aetherium.client.OceanFloor;
import com.aetherium.perf.Tuning;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
// @era:t-cull-else c17
//~ import com.aetherium.client.OceanFloor;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
// @era:t-cull-else c215
//~ import com.aetherium.client.OceanFloor;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//~ import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
//~ import net.minecraft.core.SectionPos;
// @era:t-cull-else none
// @era:t-cull-else v16
//~ import com.aetherium.client.OceanFloor;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-cull-end

/**
 * The visibility search of {@code LevelRenderer.setupRender} (1.12.0, Minecraft 1.16.5).
 *
 * <p>Directional culling: vanilla's breadth-first search walks from the camera section to its
 * neighbours through faces that can see each other, but it may step back towards the camera along
 * an axis, which lets it wind through caves into sections no straight line from the eye reaches.
 * Any straight line from the camera only ever moves away from it on each axis, so a step that
 * moves back towards the camera on its axis is refused here. Sections that are really visible are
 * still reached along the line of sight; far fewer hidden caves and rooms are drawn and built.</p>
 *
 * <p>Ocean floor culling ({@code OceanFloor}, off by default) refuses steps into sections deep
 * below an ocean floor while the camera is above the water.</p>
 *
 * <p>A redirect of the single call site (offset 1001) instead of a cancellable inject, so the
 * search allocates nothing per step.</p>
 */
// @era:t-cull-begin c202|c215
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
// @era:t-cull-else v16|c17|none
//~ @Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
// @era:t-cull-end
public abstract class TuningCullMixin {
    // @era:t-cull-begin c202
    /**
     * Directional culling: refuse a search step that moves back towards the camera on its axis.
     * The only caller is the visibility search, so returning "no neighbour" here is the same as
     * the 1.16.5 redirect of its call site.
     */
    @Inject(method = "getRelativeFrom", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$neighbour(final BlockPos camera, final SectionRenderDispatcher.RenderSection from, final Direction direction,
                                     final CallbackInfoReturnable<?> cir) {
        if (aetherium$refuse(camera.getX() >> 4, camera.getY() >> 4, camera.getZ() >> 4, from.getOrigin().getX() >> 4, from.getOrigin().getY() >> 4, from.getOrigin().getZ() >> 4, direction)) {
            cir.setReturnValue(null);
        }
    }

    /**
     * Directional culling, and ocean floor culling ({@code OceanFloor}, off by default): no step into
     * a section deep below an ocean floor while the camera is above the water.
     */
    private static boolean aetherium$refuse(final int cx, final int cy, final int cz, final int fx, final int fy,
                                            final int fz, final Direction direction) {
        if (Tuning.directionalCulling && aetherium$towardsCamera(cx, cy, cz, fx, fy, fz, direction)) {
            return true;
        }
        return OceanFloor.belowSection(fx + direction.getStepX(), fy + direction.getStepY(), fz + direction.getStepZ());
    }

    /** True when stepping from section ({@code fx},{@code fy},{@code fz}) in {@code direction} moves back towards the camera's section. */
    private static boolean aetherium$towardsCamera(final int cx, final int cy, final int cz, final int fx, final int fy,
                                                   final int fz, final Direction direction) {
        final int sx = direction.getStepX();
        if (sx != 0) {
            return sx > 0 ? fx < cx : fx > cx;
        }
        final int sy = direction.getStepY();
        if (sy != 0) {
            return sy > 0 ? fy < cy : fy > cy;
        }
        final int sz = direction.getStepZ();
        return sz > 0 ? fz < cz : fz > cz;
    }
    // @era:t-cull-else c17
    //~ /**
     //~ * Directional culling: refuse a search step that moves back towards the camera on its axis.
     //~ * The only caller is the visibility search, so returning "no neighbour" here is the same as
     //~ * the 1.16.5 redirect of its call site.
     //~ */
    //~ @Inject(method = "getRelativeFrom", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$neighbour(final BlockPos camera, final ChunkRenderDispatcher.RenderChunk from, final Direction direction,
                                     //~ final CallbackInfoReturnable<?> cir) {
        //~ if (aetherium$refuse(camera.getX() >> 4, camera.getY() >> 4, camera.getZ() >> 4, from.getOrigin().getX() >> 4, from.getOrigin().getY() >> 4, from.getOrigin().getZ() >> 4, direction)) {
            //~ cir.setReturnValue(null);
        //~ }
    //~ }

    //~ /**
     //~ * Directional culling, and ocean floor culling ({@code OceanFloor}, off by default): no step into
     //~ * a section deep below an ocean floor while the camera is above the water.
     //~ */
    //~ private static boolean aetherium$refuse(final int cx, final int cy, final int cz, final int fx, final int fy,
                                            //~ final int fz, final Direction direction) {
        //~ if (Tuning.directionalCulling && aetherium$towardsCamera(cx, cy, cz, fx, fy, fz, direction)) {
            //~ return true;
        //~ }
        //~ return OceanFloor.belowSection(fx + direction.getStepX(), fy + direction.getStepY(), fz + direction.getStepZ());
    //~ }

    //~ /** True when stepping from section ({@code fx},{@code fy},{@code fz}) in {@code direction} moves back towards the camera's section. */
    //~ private static boolean aetherium$towardsCamera(final int cx, final int cy, final int cz, final int fx, final int fy,
                                                   //~ final int fz, final Direction direction) {
        //~ final int sx = direction.getStepX();
        //~ if (sx != 0) {
            //~ return sx > 0 ? fx < cx : fx > cx;
        //~ }
        //~ final int sy = direction.getStepY();
        //~ if (sy != 0) {
            //~ return sy > 0 ? fy < cy : fy > cy;
        //~ }
        //~ final int sz = direction.getStepZ();
        //~ return sz > 0 ? fz < cz : fz > cz;
    //~ }
    // @era:t-cull-else c215
    //~ /**
     //~ * Directional culling: refuse a search step that moves back towards the camera on its axis.
     //~ * The only caller is the visibility search, so returning "no neighbour" here is the same as
     //~ * the 1.16.5 redirect of its call site.
     //~ */
    //~ @Inject(method = "getRelativeFrom", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$neighbour(final long camera, final SectionRenderDispatcher.RenderSection from, final Direction direction,
                                     //~ final CallbackInfoReturnable<?> cir) {
        //~ if (aetherium$refuse(SectionPos.x(camera), SectionPos.y(camera), SectionPos.z(camera), SectionPos.x(from.getSectionNode()), SectionPos.y(from.getSectionNode()), SectionPos.z(from.getSectionNode()), direction)) {
            //~ cir.setReturnValue(null);
        //~ }
    //~ }

    //~ /**
     //~ * Directional culling, and ocean floor culling ({@code OceanFloor}, off by default): no step into
     //~ * a section deep below an ocean floor while the camera is above the water.
     //~ */
    //~ private static boolean aetherium$refuse(final int cx, final int cy, final int cz, final int fx, final int fy,
                                            //~ final int fz, final Direction direction) {
        //~ if (Tuning.directionalCulling && aetherium$towardsCamera(cx, cy, cz, fx, fy, fz, direction)) {
            //~ return true;
        //~ }
        //~ return OceanFloor.belowSection(fx + direction.getStepX(), fy + direction.getStepY(), fz + direction.getStepZ());
    //~ }

    //~ /** True when stepping from section ({@code fx},{@code fy},{@code fz}) in {@code direction} moves back towards the camera's section. */
    //~ private static boolean aetherium$towardsCamera(final int cx, final int cy, final int cz, final int fx, final int fy,
                                                   //~ final int fz, final Direction direction) {
        //~ final int sx = direction.getStepX();
        //~ if (sx != 0) {
            //~ return sx > 0 ? fx < cx : fx > cx;
        //~ }
        //~ final int sy = direction.getStepY();
        //~ if (sy != 0) {
            //~ return sy > 0 ? fy < cy : fy > cy;
        //~ }
        //~ final int sz = direction.getStepZ();
        //~ return sz > 0 ? fz < cz : fz > cz;
    //~ }
    // @era:t-cull-else none
    // @era:t-cull-else v16
    //~ @Redirect(method = "setupRender", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/LevelRenderer;getRelativeFrom(Lnet/minecraft/core/BlockPos;Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;Lnet/minecraft/core/Direction;)Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk;"),
            //~ require = 0)
    //~ private ChunkRenderDispatcher.RenderChunk aetherium$neighbour(final LevelRenderer self, final BlockPos camera,
                                                                  //~ final ChunkRenderDispatcher.RenderChunk from,
                                                                  //~ final Direction direction) {
        //~ if (Tuning.directionalCulling && towardsCamera(camera, from.getOrigin(), direction)) {
            //~ return null;
        //~ }
        //~ final ChunkRenderDispatcher.RenderChunk next =
                //~ ((TuningLevelAccessor) (Object) self).aetherium$relativeFrom(camera, from, direction);
        //~ if (next != null && OceanFloor.below(next.getOrigin())) {
            //~ return null;
        //~ }
        //~ return next;
    //~ }

    //~ /** True when stepping from the section at {@code origin} in {@code direction} moves back towards the camera's section. */
    //~ private static boolean towardsCamera(final BlockPos camera, final BlockPos origin, final Direction direction) {
        //~ final int sx = direction.getStepX();
        //~ if (sx != 0) {
            //~ final int from = origin.getX() >> 4;
            //~ final int cam = camera.getX() >> 4;
            //~ return sx > 0 ? from < cam : from > cam;
        //~ }
        //~ final int sy = direction.getStepY();
        //~ if (sy != 0) {
            //~ final int from = origin.getY() >> 4;
            //~ final int cam = camera.getY() >> 4;
            //~ return sy > 0 ? from < cam : from > cam;
        //~ }
        //~ final int sz = direction.getStepZ();
        //~ final int from = origin.getZ() >> 4;
        //~ final int cam = camera.getZ() >> 4;
        //~ return sz > 0 ? from < cam : from > cam;
    //~ }
    // @era:t-cull-end
}
