package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.GameRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Frame start: pacing for the battery/thermal caps plus frame-time stats.
 * {@code render} is the only method of that name on every version, and the
 * handler takes only the CallbackInfo, so one mixin fits all three signatures.
 */
@Mixin(GameRenderer.class)
public abstract class FrameMixin {

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void aetherium$frameStart(final CallbackInfo ci) {
        ClientHooks.onFrameStart();
    }
}
