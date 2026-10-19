package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import com.aetherium.client.QolClient;
import com.aetherium.qol.AccuratePlacement;
import com.aetherium.qol.Qol;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Accurate block placement (Quality of life, every version). While Use is held with a block
 * in hand, vanilla repeats the placement every 4 ticks ({@code rightClickDelay}). Here the timer is
 * cut short as soon as the crosshair points at a new block space, following the rules of
 * {@link com.aetherium.qol.AccuratePlacement}: never two attempts at the same space in a row,
 * vanilla's rhythm when building onto the block just placed, at most one placement every 2 ticks.
 * The placement itself is vanilla's {@code startUseItem}, so servers see a normal right click.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class QolPlacementMixin {
        @Shadow
    private int rightClickDelay;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public ClientLevel level;

    @Shadow
    @Final
    public Options options;

    @Inject(method = "handleKeybinds", at = @At("HEAD"), require = 0)
    private void aetherium$accuratePlacement(final CallbackInfo ci) {
        if (!Qol.accuratePlacement) {
            return;
        }
        if (this.player == null || this.level == null || !this.options.keyUse.isDown() || this.player.isUsingItem()
                || !aetherium$holdsBlock()) {
            QolClient.PLACEMENT.reset();
            return;
        }
        if (this.rightClickDelay <= 0) {
            return;
        }
        final long clicked = aetherium$clicked();
        if (clicked != AccuratePlacement.NONE && QolClient.PLACEMENT.placeEarly(clicked, aetherium$target())) {
            this.rightClickDelay = 0;
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), require = 0)
    private void aetherium$placementAttempt(final CallbackInfo ci) {
        if (Qol.accuratePlacement && this.player != null && this.level != null) {
            QolClient.PLACEMENT.placed(aetherium$target());
        }
    }

    private boolean aetherium$holdsBlock() {
        return this.player.getMainHandItem().getItem() instanceof BlockItem
                || this.player.getOffhandItem().getItem() instanceof BlockItem;
    }

    /** The block the crosshair is on, packed, or NONE. */
    private long aetherium$clicked() {
        final HitResult hit = this.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return AccuratePlacement.NONE;
        }
        return ((BlockHitResult) hit).getBlockPos().asLong();
    }

    /** The space a block would go to: the clicked block when it is replaceable, else its neighbour on the clicked face. */
    private long aetherium$target() {
        final HitResult hit = this.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return AccuratePlacement.NONE;
        }
        final BlockHitResult block = (BlockHitResult) hit;
        final BlockPos pos = block.getBlockPos();
        // @era:q-place-begin replace
        final boolean replaceable = this.level.getBlockState(pos).canBeReplaced();
        // @era:q-place-else material
        //~ final boolean replaceable = this.level.getBlockState(pos).getMaterial().isReplaceable();
        // @era:q-place-end
        if (replaceable) {
            return pos.asLong();
        }
        return pos.relative(block.getDirection()).asLong();
    }
        }
