package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-server-idle-begin none
// @era:t-server-idle-else v16
//~ import com.aetherium.perf.Tuning;
//~ import java.util.concurrent.locks.LockSupport;
//~ import net.minecraft.server.MinecraftServer;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-server-idle-end

/**
 * Calmer built-in server (1.12.0, Minecraft 1.16.5): between ticks the server thread waits in
 * {@code managedBlock(() -> !haveTime())}, parking for 0.1 ms per round, which wakes it about
 * 10,000 times a second only to find nothing to do. For the server itself (not the client, not the
 * chunk-cache executors, whose waits depend on futures completing elsewhere) the park becomes 1 ms.
 * Every new task still wakes the thread at once ({@code tell} unparks it), so the only change is
 * that a tick can start up to 1 ms later, against a 50 ms tick.
 */
@Mixin(targets = "net.minecraft.util.thread.BlockableEventLoop")
public abstract class TuningServerIdleMixin {
    // @era:t-server-idle-begin none
    // @era:t-server-idle-else v16
    //~ @Redirect(method = "waitForTasks", at = @At(value = "INVOKE",
            //~ target = "Ljava/util/concurrent/locks/LockSupport;parkNanos(Ljava/lang/Object;J)V"), require = 0)
    //~ private void aetherium$park(final Object blocker, final long nanos) {
        //~ if (Tuning.serverIdle && (Object) this instanceof MinecraftServer) {
            //~ LockSupport.parkNanos(blocker, Math.max(nanos, 1_000_000L));
        //~ } else {
            //~ LockSupport.parkNanos(blocker, nanos);
        //~ }
    //~ }
    // @era:t-server-idle-end
}
