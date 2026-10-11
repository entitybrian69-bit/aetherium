package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.QolHooks;
//~ import com.aetherium.qol.ParticleFilter;
//~ import net.minecraft.client.particle.Particle;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.core.particles.ParticleOptions;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:qol16-end

/**
 * Per-type particle toggles: particles of a disabled category are never created; block
 * breaking and mining chips have their own switch. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
@Mixin(targets = "net.minecraft.client.particle.ParticleEngine")
public abstract class ParticleTypeMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$particleType(final ParticleOptions options, final double x, final double y, final double z,
                                        //~ final double dx, final double dy, final double dz,
                                        //~ final CallbackInfoReturnable<Particle> cir) {
        //~ if (QolHooks.dropParticle(options)) {
            //~ cir.setReturnValue(null);
        //~ }
    //~ }

    //~ @Inject(method = "destroy", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$blockBreak(final BlockPos pos, final BlockState state, final CallbackInfo ci) {
        //~ if (!ParticleFilter.blockBreaking()) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = "crack", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$blockCrack(final BlockPos pos, final Direction side, final CallbackInfo ci) {
        //~ if (!ParticleFilter.blockBreaking()) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:qol16-end
}
