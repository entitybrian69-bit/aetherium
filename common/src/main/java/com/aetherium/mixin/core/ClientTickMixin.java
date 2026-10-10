package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 20 Hz driver for dynamic lights, adaptive distance and the power guards. */
@Mixin(Minecraft.class)
public abstract class ClientTickMixin {

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aetherium$tick(final CallbackInfo ci) {
        ClientHooks.onClientTick((Minecraft) (Object) this);
    }
}
