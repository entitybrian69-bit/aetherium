package com.aetherium.mixin.core;

import com.aetherium.qol.ParticleFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The "block breaking" particle setting. Up to 1.21.8 the break burst and the hit crumbs come
 * from {@code ParticleEngine.destroy / crack}; from 1.21.9 {@code ClientLevel}
 * builds them itself in {@code addDestroyBlockEffect / addBreakingBlockEffect(s)} (the plural, with
 * an extra flag, is 26.3). Either way the
 * whole effect is skipped at its head (it only spawns particles). Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
// @era:block-particles-begin engine
@Mixin(targets = "net.minecraft.client.particle.ParticleEngine")
// @era:block-particles-else level
//~ @Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel")
// @era:block-particles-end
public abstract class BlockParticleMixin {
    // @era:block-particles-begin engine
    @Inject(method = {"destroy", "crack"}, at = @At("HEAD"), cancellable = true, require = 0)
    // @era:block-particles-else level
    //~ @Inject(method = {"addDestroyBlockEffect", "addBreakingBlockEffect", "addBreakingBlockEffects"}, at = @At("HEAD"),
            //~ cancellable = true, require = 0)
    // @era:block-particles-end
    private void aetherium$blockParticles(final CallbackInfo ci) {
        if (!ParticleFilter.blockBreaking()) {
            ci.cancel();
        }
    }
}
