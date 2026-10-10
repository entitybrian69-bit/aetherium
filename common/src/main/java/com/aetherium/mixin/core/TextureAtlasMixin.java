package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.texture.TextureAtlas;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Animated textures" off: the atlas tick advances and re-uploads every animated sprite (water,
 * lava, fire, portals, sea lanterns, prismarine, command blocks...) twenty times a second with
 * glTexSubImage2D, which GL-over-GLES layers on phones handle badly. {@code tick()} is the single
 * entry point on every version (1.16.5 through 26.x); sprites simply keep their current frame.
 */
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$freezeAnimations(final CallbackInfo ci) {
        if (ClientHooks.freezeTextureAnimations()) {
            ci.cancel();
        }
    }
}
