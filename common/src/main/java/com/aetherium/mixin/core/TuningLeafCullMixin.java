package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-leaf-cull-begin none
// @era:t-leaf-cull-else v16
//~ import com.aetherium.client.TuningClient;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.world.level.BlockGetter;
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
    // @era:t-leaf-cull-begin none
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
    // @era:t-leaf-cull-end
}
