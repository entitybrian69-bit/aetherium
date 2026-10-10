package com.aetherium.mixin.core;

// @era:options-begin instances
import com.aetherium.client.VanillaOptions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: the gamma option reports the override while it is active.
 * Overriding the read (not writing the value) bypasses vanilla's 0..1
 * validation and never reaches options.txt. One identity compare per read.
 */
@Mixin(net.minecraft.client.OptionInstance.class)
public abstract class OptionInstanceMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$gamma(final CallbackInfoReturnable<Object> cir) {
        if (VanillaOptions.overridesGamma(this)) {
            cir.setReturnValue(VanillaOptions.gammaOverrideBoxed());
        }
    }
}
// @era:options-else fields
//~ import org.spongepowered.asm.mixin.Mixin;

//~ /** Before 1.19 gamma is a plain field written directly by VanillaOptions; the plugin skips this mixin. */
//~ @Mixin(net.minecraft.client.Options.class)
//~ public abstract class OptionInstanceMixin {
//~ }
// @era:options-end
