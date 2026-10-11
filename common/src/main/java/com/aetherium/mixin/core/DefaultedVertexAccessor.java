package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:qol16-end

/** Whether a fixed colour is set (vanilla then refuses the fast vertex path; so does Aetherium). 1.16.5, {@code STREAMING} only. */
@Mixin(targets = "com.mojang.blaze3d.vertex.DefaultedVertexConsumer")
public interface DefaultedVertexAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Accessor("defaultColorSet")
    //~ boolean aetherium$defaultColorSet();
    // @era:qol16-end
}
