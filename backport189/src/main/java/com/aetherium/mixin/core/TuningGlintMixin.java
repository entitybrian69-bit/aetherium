package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enchantment glint off (items in hand, in the GUI and on the ground): skips the two extra glint passes. */
@Mixin(RenderItem.class)
public abstract class TuningGlintMixin {
    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$glint(final IBakedModel model, final CallbackInfo ci) {
        if (!Tuning.glint) {
            ci.cancel();
        }
    }
}
