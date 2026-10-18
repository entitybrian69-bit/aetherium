package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-map-begin none
// @era:t-map-else v16
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-map-end

/**
 * Map updates (1.10.0, Minecraft 1.16.5): a map texture is re-uploaded at most 4 times per second.
 * Vanilla rebuilds and uploads the whole 128x128 texture for every map data packet, which while
 * exploring with a map in hand (or with a wall of map art being updated) is every tick. An update
 * that arrives too early is applied when the map is next drawn, so the map never shows stale data
 * for more than 250 ms.
 */
@Mixin(targets = "net.minecraft.client.gui.MapRenderer$MapInstance")
public abstract class TuningMapMixin {
    // @era:t-map-begin none
    // @era:t-map-else v16
    //~ @Unique
    //~ private static final long AETHERIUM$SPACING_NANOS = 250_000_000L;

    //~ @Unique
    //~ private long aetherium$lastUpload;

    //~ @Unique
    //~ private boolean aetherium$pending;

    //~ @Unique
    //~ private boolean aetherium$forced;

    //~ @Shadow
    //~ private void updateTexture() {
        //~ throw new AssertionError();
    //~ }

    //~ @Inject(method = "updateTexture", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$throttle(final CallbackInfo ci) {
        //~ final long now = System.nanoTime();
        //~ if (!this.aetherium$forced && Tuning.mapThrottle && this.aetherium$lastUpload != 0L
                //~ && now - this.aetherium$lastUpload < AETHERIUM$SPACING_NANOS) {
            //~ this.aetherium$pending = true;
            //~ ci.cancel();
            //~ return;
        //~ }
        //~ this.aetherium$pending = false;
        //~ this.aetherium$lastUpload = now;
    //~ }

    //~ @Inject(method = "draw", at = @At("HEAD"), require = 0)
    //~ private void aetherium$catchUp(final CallbackInfo ci) {
        //~ if (this.aetherium$pending
                //~ && System.nanoTime() - this.aetherium$lastUpload >= AETHERIUM$SPACING_NANOS) {
            //~ this.aetherium$forced = true;
            //~ try {
                //~ this.updateTexture();
            //~ } finally {
                //~ this.aetherium$forced = false;
            //~ }
        //~ }
    //~ }
    // @era:t-map-end
}
