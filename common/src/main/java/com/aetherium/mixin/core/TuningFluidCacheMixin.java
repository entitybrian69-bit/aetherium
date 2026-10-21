package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.world.level.block.Block;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.material.FluidState;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Cached block fluids (1.17.2, Minecraft 1.16.5): {@code BlockState.getFluidState()} asks the
 * block every time (a virtual call; for water, waterlogged and other water-filled blocks also a
 * property lookup and a second state lookup), and physics, mob path finding, lighting and chunk
 * meshing call it millions of times. Vanilla's own blocks answer purely from the state, so the
 * answer is worked out once when the block caches are built at start-up and read from a field
 * afterwards. Blocks of other mods (any class outside Minecraft's own) are always asked.
 */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase")
public abstract class TuningFluidCacheMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Unique
    //~ private FluidState aetherium$fluid;

    //~ @Shadow
    //~ public abstract Block getBlock();

    //~ @Shadow
    //~ protected abstract BlockState asState();

    //~ @Inject(method = "initCache", at = @At("TAIL"), require = 0)
    //~ private void aetherium$cacheFluid(final CallbackInfo ci) {
        //~ final Block block = this.getBlock();
        //~ if (block != null && block.getClass().getName().startsWith("net.minecraft.")) {
            //~ this.aetherium$fluid = block.getFluidState(this.asState());
        //~ }
    //~ }

    //~ @Redirect(method = "getFluidState", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/block/Block;getFluidState(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/material/FluidState;"),
            //~ require = 0)
    //~ private FluidState aetherium$cachedFluid(final Block block, final BlockState state) {
        //~ final FluidState cached = this.aetherium$fluid;
        //~ if (cached != null && Tuning.fastBlockData) {
            //~ return cached;
        //~ }
        //~ return block.getFluidState(state);
    //~ }
    // @era:boost16-end
}
