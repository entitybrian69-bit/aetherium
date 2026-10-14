package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Animated textures toggle: water, lava, fire, portals, ... stop animating (saves texture uploads). */
@Mixin(TextureMap.class)
public abstract class TextureAnimMixin {

    @Inject(method = "updateAnimations", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$freeze(final CallbackInfo ci) {
        if (ClientHooks.freezeTextureAnimations()) {
            ci.cancel();
        }
    }
}
