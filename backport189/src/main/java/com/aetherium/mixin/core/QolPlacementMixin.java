package com.aetherium.mixin.core;

import com.aetherium.client.QolClient;
import com.aetherium.qol.AccuratePlacement;
import com.aetherium.qol.Qol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
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
 *
 * <p>1.8.9 handles the keys inside runTick: the hook runs right before vanilla's "use held and
 * the repeat timer is 0" check (the third read of rightClickDelayTimer in runTick), so clearing
 * the timer there places the block in the same tick.
 */
@Mixin(Minecraft.class)
public abstract class QolPlacementMixin {

    @Shadow
    private int rightClickDelayTimer;

    @Shadow
    public MovingObjectPosition objectMouseOver;

    @Shadow
    public EntityPlayerSP thePlayer;

    @Shadow
    public WorldClient theWorld;

    @Shadow
    public GameSettings gameSettings;

    @Inject(method = "runTick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", opcode = 180, ordinal = 2),
            require = 0)
    private void aetherium$accuratePlacement(final CallbackInfo ci) {
        if (!Qol.accuratePlacement) {
            return;
        }
        if (this.thePlayer == null || this.theWorld == null || !this.gameSettings.keyBindUseItem.isKeyDown()
                || this.thePlayer.isUsingItem() || !aetherium$holdsBlock()) {
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
        if (Qol.accuratePlacement && this.thePlayer != null && this.theWorld != null) {
            QolClient.PLACEMENT.placed(aetherium$target());
        }
    }

    private boolean aetherium$holdsBlock() {
        final ItemStack held = this.thePlayer.getHeldItem();
        return held != null && held.getItem() instanceof ItemBlock;
    }

    private long aetherium$clicked() {
        final MovingObjectPosition hit = this.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null) {
            return AccuratePlacement.NONE;
        }
        return hit.getBlockPos().toLong();
    }

    private long aetherium$target() {
        final MovingObjectPosition hit = this.objectMouseOver;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || hit.getBlockPos() == null) {
            return AccuratePlacement.NONE;
        }
        final BlockPos pos = hit.getBlockPos();
        if (this.theWorld.getBlockState(pos).getBlock().isReplaceable(this.theWorld, pos)) {
            return pos.toLong();
        }
        return pos.offset(hit.sideHit).toLong();
    }
}
