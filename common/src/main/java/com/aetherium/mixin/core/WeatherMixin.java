package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Weather off: skips rain/snow geometry and splash particles. Up to 1.21.1 that
 * lives in LevelRenderer; from 1.21.2 in WeatherEffectRenderer, whose method set
 * changes almost every version, so every candidate name is listed with
 * require = 0 and a CallbackInfo-only handler (valid for any descriptor).
 */
// @era:weather-begin effect
@Mixin(net.minecraft.client.renderer.WeatherEffectRenderer.class)
// @era:weather-else level
//~ @Mixin(net.minecraft.client.renderer.LevelRenderer.class)
// @era:weather-end
public abstract class WeatherMixin {

    // @era:weather-begin effect
    @Inject(method = {"render", "extractRenderState", "prepare", "renderOit", "tickRainParticles"},
            at = @At("HEAD"), cancellable = true, require = 0)
    // @era:weather-else level
    //~ @Inject(method = {"renderSnowAndRain", "tickRain"}, at = @At("HEAD"), cancellable = true, require = 0)
    // @era:weather-end
    private void aetherium$hideWeather(final CallbackInfo ci) {
        if (ClientHooks.hideWeather()) {
            ci.cancel();
        }
    }
}
