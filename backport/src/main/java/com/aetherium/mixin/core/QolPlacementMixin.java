package com.aetherium.mixin.core;

import com.aetherium.client.QolClient;
import com.aetherium.qol.AccuratePlacement;
import com.aetherium.qol.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Accurate block placement (Quality of life): while use is held with a block, the next block is
 * placed as soon as the crosshair reaches the next free spot instead of on vanilla's fixed
 * 4-tick repeat, so lines of blocks come out without gaps or doubles. Only the client's own
 * repeat timer changes, so it works on any server.
 */
@Mixin(Minecraft.class)
public abstract class QolPlacementMixin {
    @Shadow
    private int rightClickDelayTimer;

    @Shadow
    public RayTraceResult objectMouseOver;

    @Shadow
    public EntityPlayerSP player;

    @Shadow
    public WorldClient world;

    @Shadow
    public GameSettings gameSettings;

    @Inject(method = "processKeyBinds", at = @At("HEAD"), require = 0)
    private void aetherium$accuratePlacement(final CallbackInfo ci) {
        if (!Qol.accuratePlacement) {
            return;
        }
        if (this.player == null || this.world == null || !this.gameSettings.keyBindUseItem.isKeyDown()
                || this.player.isHandActive() || !aetherium$holdsBlock()) {
            QolClient.PLACEMENT.reset();
            return;
        }
        if (this.rightClickDelayTimer <= 0) {
            return;
        }
        final long clicked = aetherium$clicked();
        if (clicked != AccuratePlacement.NONE && QolClient.PLACEMENT.placeEarly(clicked, aetherium$target())) {
            this.rightClickDelayTimer = 0;
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), require = 0)
    private void aetherium$placementAttempt(final CallbackInfo ci) {
        if (Qol.accuratePlacement && this.player != null && this.world != null) {
            QolClient.PLACEMENT.placed(aetherium$target());
        }
    }

    private boolean aetherium$holdsBlock() {
        return this.player.getHeldItemMainhand().getItem() instanceof ItemBlock
                || this.player.getHeldItemOffhand().getItem() instanceof ItemBlock;
    }

    private long aetherium$clicked() {
        final RayTraceResult hit = this.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return AccuratePlacement.NONE;
        }
        return hit.getBlockPos().toLong();
    }

    private long aetherium$target() {
        final RayTraceResult hit = this.objectMouseOver;
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || hit.getBlockPos() == null) {
            return AccuratePlacement.NONE;
        }
        final BlockPos pos = hit.getBlockPos();
        if (this.world.getBlockState(pos).getBlock().isReplaceable(this.world, pos)) {
            return pos.toLong();
        }
        return pos.offset(hit.sideHit).toLong();
    }
}
