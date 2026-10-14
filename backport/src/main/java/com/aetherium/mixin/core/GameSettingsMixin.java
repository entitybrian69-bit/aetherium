package com.aetherium.mixin.core;

import com.aetherium.client.VanillaOptions;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** options.txt never stores the fullbright gamma, only the player's own brightness. */
@Mixin(GameSettings.class)
public abstract class GameSettingsMixin {

    @Inject(method = "saveOptions", at = @At("HEAD"), require = 0)
    private void aetherium$beforeSave(final CallbackInfo ci) {
        VanillaOptions.onSave(true);
    }

    @Inject(method = "saveOptions", at = @At("RETURN"), require = 0)
    private void aetherium$afterSave(final CallbackInfo ci) {
        VanillaOptions.onSave(false);
    }
}
