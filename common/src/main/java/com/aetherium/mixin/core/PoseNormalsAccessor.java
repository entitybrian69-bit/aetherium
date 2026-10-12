package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:model-fast-begin v16|mojang|joml|none
// @era:model-fast-else trusted
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:model-fast-end

/**
 * Whether a pose's normal matrix is trusted (1.20.5..1.20.6): when it is not, vanilla normalises
 * every transformed model normal and the fast model path does the same. Applied only where
 * {@code Capabilities.FAST_MODELS_TRUSTED} is true.
 */
@Mixin(targets = "com.mojang.blaze3d.vertex.PoseStack$Pose")
public interface PoseNormalsAccessor {
    // @era:model-fast-begin v16|mojang|joml|none
    // @era:model-fast-else trusted
    //~ @Accessor("trustedNormals")
    //~ boolean aetherium$trustedNormals();
    // @era:model-fast-end
}
