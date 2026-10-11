package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:qol16-end

/**
 * Runs one queued main-thread task, for the per-frame packet budget. Applied only where
 * {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.util.thread.BlockableEventLoop")
public interface BlockableEventLoopInvoker {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Invoker("pollTask")
    //~ boolean aetherium$pollTask();
    // @era:qol16-end
}
