package com.aetherium.mixin.core;

import com.aetherium.client.ChestSections;
import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.client.ChestBaking;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.level.block.ChestBlock;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import net.minecraft.world.level.block.entity.ChestBlockEntity;
//~ import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.block.state.properties.ChestType;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): rebuilds a chest's section when a player starts or
 * stops breaking it (a chest being broken is drawn the normal way so the cracks show on it), and
 * offers the section rebuild used when a chest opens or closes.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class TuningDestroyMixin implements ChestSections {
// @era:boost16-begin none
    @Override
    public void aetherium$chestChanged(final int x, final int y, final int z) {
    }
// @era:boost16-else v16
    //~ @Shadow
    //~ private ClientLevel level;

    //~ @Shadow
    //~ protected abstract void setSectionDirty(int x, int y, int z, boolean playerChanged);

    //~ @Override
    //~ public void aetherium$chestChanged(final int x, final int y, final int z) {
        //~ this.setSectionDirty(x >> 4, y >> 4, z >> 4, false);
        //~ final ClientLevel world = this.level;
        //~ if (world == null) {
            //~ return;
        //~ }
        //~ final BlockPos pos = new BlockPos(x, y, z);
        //~ final BlockState state = world.getBlockState(pos);
        //~ if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE
                //~ && state.hasProperty(ChestBlock.FACING)) {
            //~ final BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            //~ if (other.getX() >> 4 != x >> 4 || other.getZ() >> 4 != z >> 4) {
                //~ this.setSectionDirty(other.getX() >> 4, other.getY() >> 4, other.getZ() >> 4, false);
            //~ }
        //~ }
    //~ }

    //~ @Inject(method = "destroyBlockProgress", at = @At("HEAD"), require = 0)
    //~ private void aetherium$breaking(final int breaker, final BlockPos pos, final int progress, final CallbackInfo ci) {
        //~ final long at = pos.asLong();
        //~ final boolean wasBreaking = ChestBaking.destroying(at);
        //~ final long released = ChestBaking.destroyProgress(breaker, at, progress);
        //~ if (!Tuning.batchedChests || !ChestBaking.enabled()) {
            //~ return;
        //~ }
        //~ if (released != Long.MIN_VALUE) {
            //~ this.aetherium$chestAt(BlockPos.of(released));
        //~ }
        //~ if (!wasBreaking && progress >= 0 && progress < 10) {
            //~ this.aetherium$chestAt(pos);
        //~ }
    //~ }

    //~ @Unique
    //~ private void aetherium$chestAt(final BlockPos pos) {
        //~ final ClientLevel world = this.level;
        //~ if (world == null) {
            //~ return;
        //~ }
        //~ final BlockEntity entity = world.getBlockEntity(pos);
        //~ if (entity instanceof ChestBlockEntity || entity instanceof EnderChestBlockEntity) {
            //~ this.aetherium$chestChanged(pos.getX(), pos.getY(), pos.getZ());
        //~ }
    //~ }

    //~ @Inject(method = "setLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$newWorld(final ClientLevel world, final CallbackInfo ci) {
        //~ ChestBaking.clearDestroying();
    //~ }
// @era:boost16-end
}
