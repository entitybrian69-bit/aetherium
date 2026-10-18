package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-glint-begin none
// @era:t-glint-else v16
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
// @era:t-glint-end

/**
 * Enchantment glint toggle (1.10.0, Minecraft 1.16.5): the item, block-item and armor foil buffers
 * get {@code foil = false} when the glint is off, so the extra glint pass is never drawn.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.ItemRenderer")
public abstract class TuningGlintMixin {
    // @era:t-glint-begin none
    // @era:t-glint-else v16
    //~ @ModifyVariable(method = {"getFoilBuffer", "getFoilBufferDirect", "getArmorFoilBuffer"}, at = @At("HEAD"),
            //~ argsOnly = true, ordinal = 1, require = 0)
    //~ private static boolean aetherium$glint(final boolean foil) {
        //~ return foil && Tuning.glint;
    //~ }
    // @era:t-glint-end
}
