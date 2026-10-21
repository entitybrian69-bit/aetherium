package com.aetherium.mixin.core;

import com.aetherium.client.ChestLid;
import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.client.ChestBaking;
//~ import com.aetherium.client.ChestSections;
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.world.level.Level;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): exposes the lid state of a EnderChestBlockEntity to the mesh
 * workers and rebuilds the chest's section when it starts to open (so it is drawn the normal,
 * animated way) and when it has fully closed again (so it goes back into the section mesh).
 * Client world only.
 */
@Mixin(targets = "net.minecraft.world.level.block.entity.EnderChestBlockEntity")
public abstract class TuningEnderLidMixin implements ChestLid {
// @era:boost16-begin none
    @Override
    public float aetherium$openness() {
        return 0f;
    }

    @Override
    public float aetherium$previousOpenness() {
        return 0f;
    }

    @Override
    public int aetherium$openCount() {
        return 0;
    }
// @era:boost16-else v16
    //~ @Shadow
    //~ public float openness;

    //~ @Shadow
    //~ public float oOpenness;

    //~ @Shadow
    //~ public int openCount;

    //~ @Unique
    //~ private boolean aetherium$moving;

    //~ @Override
    //~ public float aetherium$openness() {
        //~ return this.openness;
    //~ }

    //~ @Override
    //~ public float aetherium$previousOpenness() {
        //~ return this.oOpenness;
    //~ }

    //~ @Override
    //~ public int aetherium$openCount() {
        //~ return this.openCount;
    //~ }

    //~ @Inject(method = "triggerEvent", at = @At("HEAD"), require = 0)
    //~ private void aetherium$opening(final int id, final int type, final CallbackInfoReturnable<Boolean> cir) {
        //~ if (id == 1 && type > 0 && this.openCount <= 0 && this.openness <= 0f) {
            //~ this.aetherium$changed();
        //~ }
    //~ }

    //~ @Inject(method = "tick", at = @At("HEAD"), require = 0)
    //~ private void aetherium$tickStart(final CallbackInfo ci) {
        //~ this.aetherium$moving = this.openness > 0f || this.oOpenness > 0f;
    //~ }

    //~ @Inject(method = "tick", at = @At("TAIL"), require = 0)
    //~ private void aetherium$tickEnd(final CallbackInfo ci) {
        //~ if (this.aetherium$moving && ChestBaking.closed(this.openness, this.oOpenness, this.openCount)) {
            //~ this.aetherium$changed();
        //~ }
    //~ }

    //~ @Unique
    //~ private void aetherium$changed() {
        //~ if (!Tuning.batchedChests || !ChestBaking.enabled()) {
            //~ return;
        //~ }
        //~ final Level level = ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getLevel();
        //~ if (level == null || !level.isClientSide) {
            //~ return;
        //~ }
        //~ final Object renderer = Minecraft.getInstance().levelRenderer;
        //~ if (renderer instanceof ChestSections) {
            //~ final BlockPos pos = ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getBlockPos();
            //~ ((ChestSections) renderer).aetherium$chestChanged(pos.getX(), pos.getY(), pos.getZ());
        //~ }
    //~ }
// @era:boost16-end
}
