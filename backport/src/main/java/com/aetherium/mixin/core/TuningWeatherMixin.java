package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Lighter rain and snow: the weather column radius around the camera shrinks from 10 to 6 blocks
 * (5 to 3 on fast graphics), like the main mod. Both constants occur once in renderRainSnow.
 */
@Mixin(EntityRenderer.class)
public abstract class TuningWeatherMixin {
    @ModifyConstant(method = "renderRainSnow", constant = @Constant(intValue = 10), require = 0)
    private int aetherium$fancyRadius(final int radius) {
        return Tuning.lightWeather ? 6 : radius;
    }

    @ModifyConstant(method = "renderRainSnow", constant = @Constant(intValue = 5), require = 0)
    private int aetherium$fastRadius(final int radius) {
        return Tuning.lightWeather ? 3 : radius;
    }
}
