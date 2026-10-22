package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttle map updates: 1.12.2 re-colours and re-uploads a map's 128 x 128 texture every frame it
 * is drawn (in hand or in an item frame). With the option on, each map uploads at most four times
 * a second; a map wall no longer costs a texture upload per map per frame.
 */
@Mixin(targets = "net.minecraft.client.gui.MapItemRenderer$Instance")
public abstract class TuningMapMixin {
    @Unique
    private static final long AETHERIUM$SPACING_NANOS = 250_000_000L;

    @Unique
    private long aetherium$lastUpload;

    @Inject(method = "updateMapTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$throttle(final CallbackInfo ci) {
        if (!Tuning.mapThrottle) {
            return;
        }
        final long now = System.nanoTime();
        if (this.aetherium$lastUpload != 0L && now - this.aetherium$lastUpload < AETHERIUM$SPACING_NANOS) {
            ci.cancel();
            return;
        }
        this.aetherium$lastUpload = now;
    }
}
