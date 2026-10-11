package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import org.spongepowered.asm.mixin.injection.At;
// @era:qol16-end

/**
 * Gives the client's queued packet tasks a time budget per frame instead of running them all
 * in one frame ({@code Minecraft.runTick} calls {@code runAllTasks()} once per frame). Blocking
 * waits ({@code managedBlock}) are untouched. Applied only where {@code Capabilities.STREAMING}
 * is true.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class PacketBudgetMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"),
            //~ require = 0)
    //~ private void aetherium$budgetTasks(final net.minecraft.client.Minecraft self, final Operation<Void> original) {
        //~ if (!QolHooks.drainTasks(self)) {
            //~ original.call(self);
        //~ }
    //~ }
    // @era:qol16-end
}
