package com.aetherium.mixin.core;

import com.aetherium.client.VanillaOptions;

import net.minecraft.client.Options;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:zoom-begin on
import com.aetherium.client.ZoomKeys;

import net.minecraft.client.KeyMapping;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
// @era:zoom-else none
// @era:zoom-end

/**
 * Keeps the fullbright override out of options.txt (vanilla saves the user's real brightness),
 * and on versions with zoom adds the three zoom keys to the key list right before options.txt is
 * read (plus the Freelook key where freelook exists), so their saved bindings load like vanilla's own
 * and they appear in Options &gt; Controls.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    // @era:zoom-begin on
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load()V", at = @At("HEAD"), require = 0)
    private void aetherium$zoomKeys(final CallbackInfo ci) {
        this.keyMappings = ZoomKeys.append(this.keyMappings);
        this.keyMappings = com.aetherium.client.FreelookHooks.appendKeys(this.keyMappings);
        this.keyMappings = com.aetherium.client.QolClient.appendKeys(this.keyMappings);
    }
    // @era:zoom-else none
    // @era:zoom-end

    @Inject(method = "save", at = @At("HEAD"), require = 0)
    private void aetherium$saveBegin(final CallbackInfo ci) {
        VanillaOptions.onSave(true);
    }

    @Inject(method = "save", at = @At("RETURN"), require = 0)
    private void aetherium$saveEnd(final CallbackInfo ci) {
        VanillaOptions.onSave(false);
    }
}
