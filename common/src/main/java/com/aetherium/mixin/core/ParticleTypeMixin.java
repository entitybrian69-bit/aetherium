package com.aetherium.mixin.core;

import com.aetherium.client.SceneryHooks;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Per-type particle settings: {@code ParticleEngine.createParticle} (same signature on every
 * version) returns no particle for a disabled category. Block break / hit particles do not go
 * through it and are handled by {@code BlockParticleMixin}. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
@Mixin(targets = "net.minecraft.client.particle.ParticleEngine")
public abstract class ParticleTypeMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$particleType(final ParticleOptions options, final double x, final double y, final double z,
                                        final double dx, final double dy, final double dz,
                                        final CallbackInfoReturnable<Particle> cir) {
        if (SceneryHooks.dropParticle(options)) {
            cir.setReturnValue(null);
        }
    }
}
