package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:model-fast-begin none
// @era:model-fast-else v16|mojang|joml|trusted
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:model-fast-end

/** Whether a fixed colour is set (vanilla then refuses the fast vertex path; so does Aetherium). 1.16.5..1.20.6, {@code FAST_MODELS} only. */
@Mixin(targets = "com.mojang.blaze3d.vertex.DefaultedVertexConsumer")
public interface DefaultedVertexAccessor {
    // @era:model-fast-begin none
    // @era:model-fast-else v16|mojang|joml|trusted
    //~ @Accessor("defaultColorSet")
    //~ boolean aetherium$defaultColorSet();
    // @era:model-fast-end
}
