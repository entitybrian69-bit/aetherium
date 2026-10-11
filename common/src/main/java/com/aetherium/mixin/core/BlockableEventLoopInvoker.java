package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Runs one queued main-thread task, for the per-frame packet budget. {@code pollTask()Z} exists on
 * all 33 versions (protected or public, probe dumps). Applied where
 * {@code Capabilities.PACKET_BUDGET} is true.
 */
@Mixin(targets = "net.minecraft.util.thread.BlockableEventLoop")
public interface BlockableEventLoopInvoker {
    @Invoker("pollTask")
    boolean aetherium$pollTask();
}
