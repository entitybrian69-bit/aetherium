package com.aetherium.mixin.core;

import com.aetherium.perf.FramePacer;
import com.aetherium.perf.RenderToggles;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Even frame pacing: the frame-rate limit waits against a fixed schedule instead of LWJGL's sleep. */
@Mixin(Minecraft.class)
public abstract class FramePacingMixin {

    @Redirect(method = "runGameLoop", require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;sync(I)V", remap = false))
    private void aetherium$pace(final int fps) {
        if (!RenderToggles.framePacing || !FramePacer.pace(fps)) {
            Display.sync(fps);
        }
    }
}
