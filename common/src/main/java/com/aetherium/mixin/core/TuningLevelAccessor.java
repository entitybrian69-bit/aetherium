package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import net.minecraft.client.renderer.ViewArea;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
// @era:qol16-end

/**
 * Far terrain (1.11.0, Minecraft 1.16.5): the view area, to find chunks inside the render distance
 * that are not built yet ({@code FarTerrain}).
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public interface TuningLevelAccessor {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Accessor("viewArea")
    //~ ViewArea aetherium$viewArea();
    // @era:qol16-end
}
