package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-leaf-cull-begin v16|f17
import com.aetherium.client.TuningClient;
import com.aetherium.perf.Tuning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-leaf-cull-else none
// @era:t-leaf-cull-else f212
//~ import com.aetherium.client.TuningClient;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import com.llamalad7.mixinextras.sugar.Local;
//~ import net.minecraft.world.level.BlockAndTintGetter;
//~ import net.minecraft.world.level.block.Block;
//~ import net.minecraft.world.level.block.Blocks;
//~ import net.minecraft.world.level.block.LeavesBlock;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-leaf-cull-else f215
//~ import com.aetherium.client.TuningClient;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.world.level.BlockAndTintGetter;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import net.minecraft.world.level.block.Block;
//~ import net.minecraft.world.level.block.Blocks;
//~ import net.minecraft.world.level.block.LeavesBlock;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-leaf-cull-end

/**
 * Leaf culling (1.10.0, Minecraft 1.16.5): leaf faces buried inside a tree are not meshed.
 * Vanilla draws every face between two leaf blocks (leaves never occlude), which is most of a
 * forest's geometry. Runs on the chunk build threads; other blocks pay one instanceof and one
 * compare.
 *
 * <p>Chorus culling (1.12.0, part of End optimizations): the end caps of two joined chorus plant
 * arms face each other inside the joint; vanilla draws both because chorus plants never occlude.
 * Only faces on the block boundary reach this check, so nothing else of the stem changes.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public abstract class TuningLeafCullMixin {
    // @era:t-leaf-cull-begin f17
    @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"),
            require = 0)
    private boolean aetherium$leafFace(final BlockState state, final BlockGetter level, final BlockPos pos,
                                       final Direction side, final BlockPos neighbour) {
        if (!Block.shouldRenderFace(state, level, pos, side, neighbour)) {
            return false;
        }
        final Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            return !(Tuning.leafCulling && TuningClient.leafNeighbourHidden(level, neighbour, side));
        }
        return !(block == Blocks.CHORUS_PLANT && Tuning.endOptimizations
                && TuningClient.chorusNeighbourHidden(state, level, neighbour, side));
    }
    // @era:t-leaf-cull-else none
    // @era:t-leaf-cull-else v16
    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"),
            //~ require = 0)
    //~ private boolean aetherium$leafFace(final BlockState state, final BlockGetter level, final BlockPos pos,
                                       //~ final Direction side) {
        //~ if (!Block.shouldRenderFace(state, level, pos, side)) {
            //~ return false;
        //~ }
        //~ final Block block = state.getBlock();
        //~ if (block instanceof LeavesBlock) {
            //~ return !(Tuning.leafCulling && TuningClient.leafFaceHidden(level, pos, side));
        //~ }
        //~ return !(block == Blocks.CHORUS_PLANT && Tuning.endOptimizations
                //~ && TuningClient.chorusFaceHidden(state, level, pos, side));
    //~ }
    // @era:t-leaf-cull-else f212
    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"),
            //~ require = 0)
    //~ private boolean aetherium$leafFace(final BlockState state, final BlockState neighbour, final Direction side,
                                       //~ @Local(argsOnly = true) final BlockAndTintGetter level,
                                       //~ @Local(argsOnly = true) final BlockPos pos) {
        //~ // 1.21.2-1.21.4: vanilla passes the neighbour state only; the level and the position are
        //~ // the tesselate method's own arguments (MixinExtras @Local).
        //~ if (!Block.shouldRenderFace(state, neighbour, side)) {
            //~ return false;
        //~ }
        //~ final Block block = state.getBlock();
        //~ if (block instanceof LeavesBlock) {
            //~ return !(Tuning.leafCulling && TuningClient.leafFaceHidden(level, pos, side));
        //~ }
        //~ return !(block == Blocks.CHORUS_PLANT && Tuning.endOptimizations
                //~ && TuningClient.chorusFaceHidden(state, level, pos, side));
    //~ }
    // @era:t-leaf-cull-else f215
    //~ @Shadow
    //~ private static boolean shouldRenderFace(final BlockAndTintGetter level, final BlockState state, final boolean cull,
                                            //~ final Direction side, final BlockPos neighbour) {
        //~ throw new AssertionError();
    //~ }

    //~ // 1.21.5+: the face test moved into ModelBlockRenderer itself and receives the neighbour position.
    //~ @Redirect(method = {"tesselateWithAO", "tesselateWithoutAO"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;shouldRenderFace(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z"),
            //~ require = 0)
    //~ private boolean aetherium$leafFace(final BlockAndTintGetter level, final BlockState state, final boolean cull,
                                       //~ final Direction side, final BlockPos neighbour) {
        //~ if (!shouldRenderFace(level, state, cull, side, neighbour)) {
            //~ return false;
        //~ }
        //~ if (!cull) {
            //~ return true;
        //~ }
        //~ final Block block = state.getBlock();
        //~ if (block instanceof LeavesBlock) {
            //~ return !(Tuning.leafCulling && TuningClient.leafNeighbourHidden(level, neighbour, side));
        //~ }
        //~ return !(block == Blocks.CHORUS_PLANT && Tuning.endOptimizations
                //~ && TuningClient.chorusNeighbourHidden(state, level, neighbour, side));
    //~ }
    // @era:t-leaf-cull-end
}
