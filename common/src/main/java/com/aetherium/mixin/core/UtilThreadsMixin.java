package com.aetherium.mixin.core;

import com.aetherium.perf.WorkerThreads;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Worker-pool size on 1.16.5-1.17.1, where vanilla hard-codes {@code clamp(cores - 1, 1, 7)} and
 * has no {@code max.bg.threads} property (1.18+ reads the property, set by the mixin plugin, and
 * this mixin is skipped). {@code @ModifyVariable} on the first int stored in {@code makeExecutor}
 * chains with other mods instead of conflicting the way a redirect would.
 */
@Mixin(targets = "net.minecraft.Util")
public abstract class UtilThreadsMixin {

    @ModifyVariable(method = "makeExecutor", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 0)
    private static int aetherium$poolSize(final int vanilla) {
        final int wanted = WorkerThreads.applied();
        return wanted > 0 && vanilla > 0 ? wanted : vanilla;
    }
}
