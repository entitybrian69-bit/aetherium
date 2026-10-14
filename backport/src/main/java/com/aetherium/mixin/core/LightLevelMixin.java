package com.aetherium.mixin.core;

import com.aetherium.lighting.LightField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Dynamic lights on blocks: the light the chunk mesher reads (worker threads; allocation-free). */
@Mixin(ChunkCache.class)
public abstract class LightLevelMixin {

    @Inject(method = "getCombinedLight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$light(final BlockPos pos, final int lightValue, final CallbackInfoReturnable<Integer> cir) {
        if (!LightField.isEmpty()) {
            final int packed = cir.getReturnValue().intValue();
            final int adjusted = LightField.adjustPacked(packed, pos.getX(), pos.getY(), pos.getZ());
            if (adjusted != packed) {
                cir.setReturnValue(Integer.valueOf(adjusted));
            }
        }
    }
}
