package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-loading-begin none
// @era:t-loading-else v16
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-loading-end

/**
 * Faster loading screens (1.12.0, Minecraft 1.16.5): while the built-in server prepares the spawn
 * area, {@code Minecraft.doLoadLevel} redraws the progress screen and sleeps 16 ms, in a loop. On
 * a phone each redraw competes with the server for the same few fast cores; sleeping 50 ms (20
 * redraws a second) leaves them to the server and the world opens sooner.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class TuningLoadingMixin {
    // @era:t-loading-begin none
    // @era:t-loading-else v16
    //~ @Redirect(method = "doLoadLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"), require = 0)
    //~ private void aetherium$progressSleep(final long millis) throws InterruptedException {
        //~ Thread.sleep(Tuning.fastLoading ? Math.max(millis, 50L) : millis);
    //~ }
    // @era:t-loading-end
}
