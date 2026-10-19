package com.aetherium.mixin.core;

import com.aetherium.client.TuningBackport;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleExplosionHuge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Explosion clouds per tick: each huge explosion emitter spawns six big clouds a tick for eight
 * ticks; past the per-tick budget an emitter ends (TNT chains no longer bury the frame in clouds).
 * The small explosion puffs stay. Ending it early only removes particles.
 */
@Mixin(ParticleExplosionHuge.class)
public abstract class TuningExplosionMixin {
    @Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$budget(final CallbackInfo ci) {
        if (!TuningBackport.allowExplosion()) {
            ((Particle) (Object) this).setExpired();
            ci.cancel();
        }
    }
}
