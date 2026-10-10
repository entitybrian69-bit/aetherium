package com.aetherium.mixin.core;

import com.aetherium.client.VanillaOptions;

import net.minecraft.client.Options;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the fullbright override out of options.txt: vanilla saves the user's real brightness. */
@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(method = "save", at = @At("HEAD"), require = 0)
    private void aetherium$saveBegin(final CallbackInfo ci) {
        VanillaOptions.onSave(true);
    }

    @Inject(method = "save", at = @At("RETURN"), require = 0)
    private void aetherium$saveEnd(final CallbackInfo ci) {
        VanillaOptions.onSave(false);
    }
}
