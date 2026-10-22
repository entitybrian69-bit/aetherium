package com.aetherium.mixin.core;

import com.aetherium.client.VanillaOptions;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity distance (Video settings): 1.8.9 has no entity distance slider, every entity is drawn up to
 * 64 blocks times its own size and weight. The weight read in the range check is scaled by the option.
 */
@Mixin(Entity.class)
public abstract class EntityDistanceMixin {

    @Redirect(method = "isInRangeToRenderDist", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/entity/Entity;renderDistanceWeight:D"))
    private double aetherium$scale(final Entity entity) {
        return entity.renderDistanceWeight * VanillaOptions.entityDistanceScale();
    }
}
