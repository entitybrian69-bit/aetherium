package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enchantment glint off on worn armour too. */
@Mixin(LayerArmorBase.class)
public abstract class TuningArmorGlintMixin {
    @Inject(method = "renderEnchantedGlint", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aetherium$glint(final RenderLivingBase<?> renderer, final EntityLivingBase entity,
                                        final ModelBase model, final float limbSwing, final float limbSwingAmount,
                                        final float partialTicks, final float ageInTicks, final float netHeadYaw,
                                        final float headPitch, final float scale, final CallbackInfo ci) {
        if (!Tuning.glint) {
            ci.cancel();
        }
    }
}
