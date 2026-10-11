package com.aetherium.mixin.core;

import com.aetherium.client.FreelookHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Freelook: while active, mouse movement turns the free camera instead of the player.
 * MouseHandler.turnPlayer calls LocalPlayer.turn(DD)V on all 33 versions (probe dumps).
 */
@Mixin(targets = "net.minecraft.client.MouseHandler")
public abstract class FreelookMouseMixin {
    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 0)
    private void aetherium$freelookTurn(final LocalPlayer player, final double dx, final double dy,
                                        final Operation<Void> original) {
        if (!FreelookHooks.turn(dx, dy)) {
            original.call(player, dx, dy);
        }
    }
}
