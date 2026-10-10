package com.aetherium.mixin.core;

import com.aetherium.lighting.LightField;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic lights for blocks: raises the packed block light that chunk meshing
 * reads. Hooks the most specific overload (the short one delegates to it), with
 * the full descriptor because the name is overloaded. Runs on mesh worker
 * threads; the empty check keeps it free when no light source exists, and the
 * return value is only replaced (boxed) when it actually changes.
 */
@Mixin(LevelRenderer.class)
public abstract class LightLevelMixin {

    // @era:light-hook-begin color
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true, require = 0)
    private static void aetherium$light(final net.minecraft.world.level.BlockAndTintGetter level, final BlockState state,
                                        final BlockPos pos, final CallbackInfoReturnable<Integer> cir) {
        adjust(pos, cir);
    }
    // @era:light-hook-else brightness
    //~ @Inject(method = "getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
    //~         at = @At("RETURN"), cancellable = true, require = 0)
    //~ private static void aetherium$light(final LevelRenderer.BrightnessGetter getter,
    //~                                     final net.minecraft.world.level.BlockAndTintGetter level, final BlockState state,
    //~                                     final BlockPos pos, final CallbackInfoReturnable<Integer> cir) {
    //~     adjust(pos, cir);
    //~ }
    // @era:light-hook-else coords
    //~ @Inject(method = "getLightCoords(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
    //~         at = @At("RETURN"), cancellable = true, require = 0)
    //~ private static void aetherium$light(final LevelRenderer.BrightnessGetter getter,
    //~                                     final net.minecraft.world.level.BlockAndLightGetter level, final BlockState state,
    //~                                     final BlockPos pos, final CallbackInfoReturnable<Integer> cir) {
    //~     adjust(pos, cir);
    //~ }
    // @era:light-hook-else none
    // @era:light-hook-end

    private static void adjust(final BlockPos pos, final CallbackInfoReturnable<Integer> cir) {
        if (LightField.isEmpty()) {
            return;
        }
        final int base = cir.getReturnValueI();
        final int adjusted = LightField.adjustPacked(base, pos.getX(), pos.getY(), pos.getZ());
        if (adjusted != base) {
            cir.setReturnValue(Integer.valueOf(adjusted));
        }
    }
}
