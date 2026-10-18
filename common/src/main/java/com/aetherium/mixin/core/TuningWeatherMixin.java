package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-weather-begin none
// @era:t-weather-else w212
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.Constant;
//~ import org.spongepowered.asm.mixin.injection.ModifyConstant;
// @era:t-weather-else w219
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.Constant;
//~ import org.spongepowered.asm.mixin.injection.ModifyConstant;
// @era:t-weather-else w2111
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.OptionInstance;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-weather-end

/**
 * Lighter rain and snow, Minecraft 1.21.2+ (1.16.5 - 1.21.1 are in {@link TuningLevelMixin}): the
 * weather column radius around the camera shrinks from 10 to 6 blocks (5 to 3 on fast graphics),
 * or the vanilla weather radius option is capped at 6 where it exists (1.21.11+).
 */
@Mixin(targets = "net.minecraft.client.renderer.WeatherEffectRenderer")
public abstract class TuningWeatherMixin {
    // @era:t-weather-begin none
    // @era:t-weather-else w212
    //~ /** Lighter weather: the rain and snow radius 10 (fancy graphics) becomes 6. */
    //~ @ModifyConstant(method = "render", constant = @Constant(intValue = 10), require = 0)
    //~ private int aetherium$weatherRadiusFancy(final int radius) {
        //~ return Tuning.lightWeather ? 6 : radius;
    //~ }

    //~ /** Lighter weather: the rain and snow radius 5 (fast graphics) becomes 3. */
    //~ @ModifyConstant(method = "render", constant = @Constant(intValue = 5), require = 0)
    //~ private int aetherium$weatherRadiusFast(final int radius) {
        //~ return Tuning.lightWeather ? 3 : radius;
    //~ }
    // @era:t-weather-else w219
    //~ /** Lighter weather: the rain and snow radius 10 (fancy graphics) becomes 6. */
    //~ @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 10), require = 0)
    //~ private int aetherium$weatherRadiusFancy(final int radius) {
        //~ return Tuning.lightWeather ? 6 : radius;
    //~ }

    //~ /** Lighter weather: the rain and snow radius 5 (fast graphics) becomes 3. */
    //~ @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 5), require = 0)
    //~ private int aetherium$weatherRadiusFast(final int radius) {
        //~ return Tuning.lightWeather ? 3 : radius;
    //~ }
    // @era:t-weather-else w2111
    //~ /**
     //~ * Lighter weather: the vanilla weather radius option (3 - 10 blocks, default 10) is capped at 6
     //~ * while the option is on; lower settings are kept as they are.
     //~ */
    //~ @Redirect(method = "extractRenderState", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"), require = 0)
    //~ private Object aetherium$weatherRadius(final OptionInstance<?> option) {
        //~ final Object radius = option.get();
        //~ if (Tuning.lightWeather && radius instanceof Integer && ((Integer) radius).intValue() > 6) {
            //~ return Integer.valueOf(6);
        //~ }
        //~ return radius;
    //~ }
    // @era:t-weather-end
}
