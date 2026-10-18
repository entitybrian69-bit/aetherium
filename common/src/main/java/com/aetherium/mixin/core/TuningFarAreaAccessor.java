package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-far-acc-begin none
// @era:t-far-acc-else v17
//~ import net.minecraft.client.renderer.ViewArea;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:t-far-acc-end

/**
 * Far terrain on 1.17 - 1.18.1 (no {@code isChunkCompiled} yet): the view area, to tell which chunks
 * inside the render distance vanilla has not built. Applied only there.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public interface TuningFarAreaAccessor {
    // @era:t-far-acc-begin none
    // @era:t-far-acc-else v17
    //~ @Accessor("viewArea")
    //~ ViewArea aetherium$farViewArea();
    // @era:t-far-acc-end
}
