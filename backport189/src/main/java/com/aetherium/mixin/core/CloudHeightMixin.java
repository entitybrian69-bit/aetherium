package com.aetherium.mixin.core;

import com.aetherium.qol.Scenery;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cloud height (vanilla clouds and Forge's cloud renderer both ask the dimension). */
@Mixin(WorldProvider.class)
public abstract class CloudHeightMixin {

    @Shadow
    protected World worldObj;

    @Inject(method = "getCloudHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$height(final CallbackInfoReturnable<Float> cir) {
        if (this.worldObj != null && this.worldObj.isRemote && Scenery.cloudHeight > 0) {
            cir.setReturnValue(Float.valueOf(Scenery.cloudHeight(cir.getReturnValue().floatValue())));
        }
    }
}
