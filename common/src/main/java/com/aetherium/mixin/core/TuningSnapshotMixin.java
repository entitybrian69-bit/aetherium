package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.level.Level;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.chunk.LevelChunk;
//~ import net.minecraft.world.level.material.FluidState;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:boost16-end

/**
 * Faster chunk snapshots (1.17.2, Minecraft 1.16.5): before a chunk section is meshed, the main
 * thread copies the blocks and fluids around it (18 x 18 x 18) into a {@code RenderChunkRegion},
 * looking each position up in the chunk twice: once for the block, once more for the fluid. The
 * fluid of a position is the fluid of its block, so the second lookup reads it from the block
 * that was just copied. Same result for every normal chunk; other chunk types and the debug world
 * (whose blocks are not stored in its chunks) keep vanilla's lookup.
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.RenderChunkRegion")
public abstract class TuningSnapshotMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Shadow
    //~ @Final
    //~ protected BlockState[] blockStates;

    //~ @Shadow
    //~ @Final
    //~ protected Level level;

    //~ @Shadow
    //~ protected abstract int index(BlockPos pos);

    //~ @Redirect(method = "<init>", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/chunk/LevelChunk;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"),
            //~ require = 0)
    //~ private FluidState aetherium$fluidFromBlock(final LevelChunk chunk, final BlockPos pos) {
        //~ if (Tuning.fastSectionSnapshots && chunk != null && chunk.getClass() == LevelChunk.class && !this.level.isDebug()) {
            //~ final BlockState state = this.blockStates[this.index(pos)];
            //~ if (state != null) {
                //~ return state.getFluidState();
            //~ }
        //~ }
        //~ return chunk.getFluidState(pos);
    //~ }
    // @era:boost16-end
}
