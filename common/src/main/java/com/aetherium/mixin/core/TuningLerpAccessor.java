package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:boost16-end

/**
 * Skip unseen entity updates (1.17.2, Minecraft 1.16.5): the movement smoothing state of a living
 * entity, so a skipped update can jump straight to the position the server last sent.
 */
@Mixin(targets = "net.minecraft.world.entity.LivingEntity")
public interface TuningLerpAccessor {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Accessor("lerpSteps")
    //~ int aetherium$lerpSteps();

    //~ @Accessor("lerpSteps")
    //~ void aetherium$setLerpSteps(int steps);

    //~ @Accessor("lerpX")
    //~ double aetherium$lerpX();

    //~ @Accessor("lerpY")
    //~ double aetherium$lerpY();

    //~ @Accessor("lerpZ")
    //~ double aetherium$lerpZ();

    //~ @Accessor("lerpYRot")
    //~ double aetherium$lerpYRot();

    //~ @Accessor("lerpXRot")
    //~ double aetherium$lerpXRot();

    //~ @Accessor("lyHeadRot")
    //~ double aetherium$lerpHeadRot();

    //~ @Accessor("lerpHeadSteps")
    //~ int aetherium$lerpHeadSteps();

    //~ @Accessor("lerpHeadSteps")
    //~ void aetherium$setLerpHeadSteps(int steps);
    // @era:boost16-end
}
