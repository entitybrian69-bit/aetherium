package com.aetherium.mixin.core;

import com.aetherium.client.VanillaOptions;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Entity distance: scales vanilla's entity render distance weight (1.12.2 has no slider for it). */
@Mixin(RenderGlobal.class)
public abstract class EntityDistanceMixin {

    @Redirect(method = "setupTerrain", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;setRenderDistanceWeight(D)V"))
    private void aetherium$scale(final double weight) {
        Entity.setRenderDistanceWeight(weight * VanillaOptions.entityDistanceScale());
    }
}
