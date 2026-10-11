package com.aetherium.mixin.core;

import com.aetherium.lighting.LightField;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dynamic lights for blocks: raises the packed block light that chunk meshing
 * reads. Hooks the most specific overload (the short one delegates to it), with
 * the full descriptor because the name is overloaded.
 *
 * <p>Runs on mesh worker threads, millions of times while chunks are built, so it
 * is a MixinExtras return-value modifier: no callback object is allocated and
 * nothing is boxed. With no light source the cost is one volatile read and a
 * length check, which is why the hook is always installed and dynamic lights can
 * be switched on and off live, without a restart.</p>
 */
@Mixin(LevelRenderer.class)
public abstract class LightLevelMixin {

    // @era:light-hook-begin color
    @ModifyReturnValue(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), require = 0)
    private static int aetherium$light(final int original, final net.minecraft.world.level.BlockAndTintGetter level,
                                       final BlockState state, final BlockPos pos) {
        return adjust(original, pos);
    }
    // @era:light-hook-else brightness
    //~ @ModifyReturnValue(method = "getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            //~ at = @At("RETURN"), require = 0)
    //~ private static int aetherium$light(final int original, final LevelRenderer.BrightnessGetter getter,
                                       //~ final net.minecraft.world.level.BlockAndTintGetter level, final BlockState state,
                                       //~ final BlockPos pos) {
        //~ return adjust(original, pos);
    //~ }
    // @era:light-hook-else coords
    //~ @ModifyReturnValue(method = "getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            //~ at = @At("RETURN"), require = 0)
    //~ private static int aetherium$light(final int original, final LevelRenderer.BrightnessGetter getter,
                                       //~ final net.minecraft.world.level.BlockAndLightGetter level, final BlockState state,
                                       //~ final BlockPos pos) {
        //~ return adjust(original, pos);
    //~ }
    // @era:light-hook-else none
    // @era:light-hook-end

    private static int adjust(final int original, final BlockPos pos) {
        if (LightField.isEmpty()) {
            return original;
        }
        return LightField.adjustPacked(original, pos.getX(), pos.getY(), pos.getZ());
    }
}
