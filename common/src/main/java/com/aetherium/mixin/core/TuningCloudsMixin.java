package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-clouds-begin none
// @era:t-clouds-else k216
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.client.OptionInstance;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-clouds-else k261
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:t-clouds-else k263
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:t-clouds-end

/**
 * Cloud distance, Minecraft 1.21.6+ (1.16.5 - 1.21.1 are in {@link TuningSkyMixin}): the vanilla
 * cloud range is scaled by the option's percentage (Low-end mode caps it at 50%).
 */
@Mixin(targets = "net.minecraft.client.renderer.CloudRenderer")
public abstract class TuningCloudsMixin {
    // @era:t-clouds-begin none
    // @era:t-clouds-else k216
    //~ /** The vanilla cloud range (in chunks) scaled by the cloud distance option, at least 2 chunks. */
    //~ @Unique
    //~ private static int aetherium$scaled(final int range) {
        //~ final int percent = Tuning.cloudDistance;
        //~ return percent >= 100 ? range : Math.max(2, (range * percent + 50) / 100);
    //~ }

    //~ @Redirect(method = "render", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"), require = 0)
    //~ private Object aetherium$cloudRange(final OptionInstance<?> option) {
        //~ final Object range = option.get();
        //~ return range instanceof Integer ? Integer.valueOf(aetherium$scaled(((Integer) range).intValue())) : range;
    //~ }
    // @era:t-clouds-else k261
    //~ /** The vanilla cloud range (in chunks) scaled by the cloud distance option, at least 2 chunks. */
    //~ @Unique
    //~ private static int aetherium$scaled(final int range) {
        //~ final int percent = Tuning.cloudDistance;
        //~ return percent >= 100 ? range : Math.max(2, (range * percent + 50) / 100);
    //~ }

    //~ @ModifyVariable(method = "render(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V",
            //~ at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    //~ private int aetherium$cloudRange(final int range) {
        //~ return aetherium$scaled(range);
    //~ }
    // @era:t-clouds-else k263
    //~ /** The vanilla cloud range (in chunks) scaled by the cloud distance option, at least 2 chunks. */
    //~ @Unique
    //~ private static int aetherium$scaled(final int range) {
        //~ final int percent = Tuning.cloudDistance;
        //~ return percent >= 100 ? range : Math.max(2, (range * percent + 50) / 100);
    //~ }

    //~ @ModifyVariable(method = "prepare(ILnet/minecraft/client/CloudStatus;FILnet/minecraft/world/phys/Vec3;JF)V",
            //~ at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    //~ private int aetherium$cloudRange(final int range) {
        //~ return aetherium$scaled(range);
    //~ }
    // @era:t-clouds-end
}
