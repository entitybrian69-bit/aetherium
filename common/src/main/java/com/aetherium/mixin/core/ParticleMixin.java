package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.particle.ParticleEngine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Particle density: deterministically drops a share of particles before they are ever ticked or drawn. */
@Mixin(ParticleEngine.class)
public abstract class ParticleMixin {

    @Inject(method = "add", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$density(final CallbackInfo ci) {
        if (ClientHooks.shouldDropParticle()) {
            ci.cancel();
        }
    }
}
