package dev.aetherium.mixin;

import dev.aetherium.Aetherium;
import dev.aetherium.engine.AetheriumRenderEngine;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Frame lifecycle: engine init on first frame, begin/end frame around the whole vanilla render. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void aetherium$beginFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        AetheriumRenderEngine engine = AetheriumRenderEngine.get();
        if (!engine.isInitialized()) Aetherium.initRenderThread();
        engine.beginFrame(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("RETURN"))
    private void aetherium$endFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        AetheriumRenderEngine.get().endFrame();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void aetherium$shutdown(CallbackInfo ci) {
        AetheriumRenderEngine.get().shutdown();
    }
}
