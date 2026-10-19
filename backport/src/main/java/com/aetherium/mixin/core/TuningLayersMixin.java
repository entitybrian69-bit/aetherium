package com.aetherium.mixin.core;

import com.aetherium.perf.Tuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Entity detail distance: past it, armour, held items, capes and stuck arrows are skipped. */
@Mixin(RenderLivingBase.class)
public abstract class TuningLayersMixin {
    @Inject(method = "renderLayers", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$layers(final EntityLivingBase entity, final float limbSwing, final float limbSwingAmount,
                                  final float partialTicks, final float ageInTicks, final float netHeadYaw,
                                  final float headPitch, final float scale, final CallbackInfo ci) {
        if (Tuning.detailDistanceSq != Double.MAX_VALUE
                && Tuning.beyondDetail(Minecraft.getMinecraft().getRenderManager()
                        .getDistanceToCamera(entity.posX, entity.posY, entity.posZ))) {
            ci.cancel();
        }
    }
}
