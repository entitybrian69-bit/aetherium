package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin v16
import com.aetherium.perf.ExplosionBudget;
import com.aetherium.perf.Tuning;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-else none
// @era:qol16-end

/**
 * Explosion particles (1.10.0, Minecraft 1.16.5): beyond the per-tick budget an explosion shows
 * the small puff instead of the big cloud ({@link ExplosionBudget}). Client side only; the server's
 * addParticle does nothing anyway.
 */
@Mixin(targets = "net.minecraft.world.level.Explosion")
public abstract class TuningExplosionMixin {
    // @era:qol16-begin v16
    @Redirect(method = "finalizeExplosion", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
            require = 0)
    private void aetherium$cloud(final Level level, final ParticleOptions particle, final double x, final double y,
                                 final double z, final double dx, final double dy, final double dz) {
        ParticleOptions shown = particle;
        if (particle == (Object) ParticleTypes.EXPLOSION_EMITTER && level.isClientSide()
                && !ExplosionBudget.allowEmitter(level.getGameTime(), Tuning.explosionEmitters)) {
            shown = (ParticleOptions) (Object) ParticleTypes.EXPLOSION;
        }
        level.addParticle(shown, x, y, z, dx, dy, dz);
    }
    // @era:qol16-else none
    // @era:qol16-end
}
