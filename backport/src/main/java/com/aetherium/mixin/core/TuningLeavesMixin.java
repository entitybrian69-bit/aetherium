package com.aetherium.mixin.core;

import com.aetherium.client.TuningBackport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Leaves option: after vanilla sets the leaves' look from the graphics option, Fast or Fancy overrides it. */
@Mixin(RenderGlobal.class)
public abstract class TuningLeavesMixin {
    @Inject(method = "loadRenderers", at = @At("RETURN"), require = 0)
    private void aetherium$leaves(final CallbackInfo ci) {
        TuningBackport.applyLeaves(Minecraft.getMinecraft());
    }
}
