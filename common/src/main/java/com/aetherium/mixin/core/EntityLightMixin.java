package com.aetherium.mixin.core;

import com.aetherium.lighting.LightField;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Dynamic lights for entities: mobs and items near a light source are lit too.
 * A return-value modifier, so the per-entity call allocates nothing.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityLightMixin {

    @ModifyReturnValue(method = "getBlockLightLevel", at = @At("RETURN"), require = 0)
    private int aetherium$entityLight(final int original, final Entity entity, final BlockPos pos) {
        if (LightField.isEmpty()) {
            return original;
        }
        return LightField.adjustLevel(original, pos.getX(), pos.getY(), pos.getZ());
    }
}
