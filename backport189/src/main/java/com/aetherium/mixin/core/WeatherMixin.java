package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Weather toggle (rain and snow) and the rain splash particles. */
@Mixin(EntityRenderer.class)
public abstract class WeatherMixin {

    @Inject(method = "renderRainSnow", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$weather(final float partialTicks, final CallbackInfo ci) {
        if (ClientHooks.hideWeather()) {
            ci.cancel();
        }
    }

    @Inject(method = "addRainParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$splashes(final CallbackInfo ci) {
        if (ClientHooks.hideRainParticles()) {
            ci.cancel();
        }
    }
}
