package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-lightmap-begin v16
import com.aetherium.perf.Tuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-lightmap-else none
// @era:t-lightmap-end

/**
 * Calmer light table updates (1.12.0, Minecraft 1.16.5): {@code LightTexture.tick} asks for the
 * 16 x 16 light colour table to be recomputed (256 colours with gamma) and uploaded every tick,
 * mostly for the sky brightness at sunrise and sunset and the torch flicker. On odd ticks the
 * request is held over to the next tick, so the table follows at 10 updates a second. While a
 * lightning flash is on, every tick updates as in vanilla.
 */
@Mixin(targets = "net.minecraft.client.renderer.LightTexture")
public abstract class TuningLightmapMixin {
    // @era:t-lightmap-begin v16
    @Shadow
    private boolean updateLightTexture;

    @Unique
    private int aetherium$ticks;

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aetherium$tick(final CallbackInfo ci) {
        aetherium$ticks++;
    }

    @Inject(method = "updateLightTexture(F)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$rate(final float partialTicks, final CallbackInfo ci) {
        if (!updateLightTexture || !Tuning.lightmapRate || (aetherium$ticks & 1) == 0) {
            return;
        }
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null && level.getSkyFlashTime() > 0) {
            return;
        }
        ci.cancel(); // the request stays set and is served on the next (even) tick
    }
    // @era:t-lightmap-else none
    // @era:t-lightmap-end
}
