package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-sort-begin none
// @era:t-sort-else s212
//~ import com.aetherium.perf.Tuning;
//~ import org.spongepowered.asm.mixin.injection.Constant;
//~ import org.spongepowered.asm.mixin.injection.ModifyConstant;
// @era:t-sort-end

/**
 * Calmer translucent sorting, Minecraft 1.21.2+ (1.16.5 - 1.21.1 are in {@link TuningLevelMixin}).
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class TuningSortMixin {
    // @era:t-sort-begin none
    // @era:t-sort-else s212
    //~ /**
     //~ * Calmer translucent sorting: when the camera enters a new block, vanilla re-sorts at least 15
     //~ * nearby translucent sections in that frame (or an eighth of them); with the option on, at
     //~ * least 8. The remaining sections are still re-sorted on later moves.
     //~ */
    //~ @ModifyConstant(method = "scheduleTranslucentSectionResort", constant = @Constant(intValue = 15), require = 0)
    //~ private int aetherium$sortCount(final int original) {
        //~ return Tuning.calmSorting ? 8 : original;
    //~ }
    // @era:t-sort-end
}
