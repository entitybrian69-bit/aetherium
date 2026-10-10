package com.aetherium.mixin.core;

import com.aetherium.lighting.LightField;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dynamic lights for entities: mobs and items near a light source are lit too. */
@Mixin(EntityRenderer.class)
public abstract class EntityLightMixin {

    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$entityLight(final Entity entity, final BlockPos pos, final CallbackInfoReturnable<Integer> cir) {
        if (LightField.isEmpty()) {
            return;
        }
        final int base = cir.getReturnValueI();
        final int adjusted = LightField.adjustLevel(base, pos.getX(), pos.getY(), pos.getZ());
        if (adjusted != base) {
            cir.setReturnValue(Integer.valueOf(adjusted));
        }
    }
}
