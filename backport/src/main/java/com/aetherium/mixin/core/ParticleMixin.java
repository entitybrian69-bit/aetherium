package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Particle density and the per-type particle toggles. */
@Mixin(ParticleManager.class)
public abstract class ParticleMixin {

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$density(final Particle particle, final CallbackInfo ci) {
        if (ClientHooks.shouldDropParticle()) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnEffectParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$type(final int particleId, final double x, final double y, final double z,
                                final double dx, final double dy, final double dz, final int[] parameters,
                                final CallbackInfoReturnable<Particle> cir) {
        if (ClientHooks.particleTypeHidden(particleId)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = {"addBlockDestroyEffects", "addBlockHitEffects"}, at = @At("HEAD"), cancellable = true,
            require = 0)
    private void aetherium$blockBreak(final CallbackInfo ci) {
        if (ClientHooks.hideBlockBreakParticles()) {
            ci.cancel();
        }
    }
}
