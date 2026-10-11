package com.aetherium.mixin.core;

import com.aetherium.client.QolHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Gives the client's queued packet tasks a time budget per frame instead of running them all
 * in one frame ({@code Minecraft.runTick} calls {@code runAllTasks()} once per frame on all 33
 * versions, probe dumps). Blocking waits ({@code managedBlock}) are untouched. Applied where
 * {@code Capabilities.PACKET_BUDGET} is true.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class PacketBudgetMixin {
    @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"),
            require = 0)
    private void aetherium$budgetTasks(final net.minecraft.client.Minecraft self, final Operation<Void> original) {
        if (!QolHooks.drainTasks(self)) {
            original.call(self);
        }
    }
}
