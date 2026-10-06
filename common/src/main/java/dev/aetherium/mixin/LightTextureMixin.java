package dev.aetherium.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import dev.aetherium.gamma.GammaUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gamma utilities: rewrite the 16x16 lightmap right before it is uploaded (1.16.5 - 1.21.1 CPU lightmap path). */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
    @Shadow @Final private NativeImage lightPixels;
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "updateLightTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V"))
    private void aetherium$applyGamma(float partialTick, CallbackInfo ci) {
        GammaUtilities gamma = GammaUtilities.get();
        if (gamma.isIdentity() || minecraft.level == null) return;
        long dayTime = minecraft.level.getDayTime();
        int camSky = 15;
        if (minecraft.getCameraEntity() != null) {
            BlockPos p = BlockPos.containing(minecraft.getCameraEntity().getEyePosition());
            camSky = minecraft.level.getBrightness(LightLayer.SKY, p);
        }
        for (int sky = 0; sky < 16; sky++) {
            for (int block = 0; block < 16; block++) {
                int abgr = lightPixels.getPixelRGBA(block, sky);
                lightPixels.setPixelRGBA(block, sky, gamma.transformPacked(abgr, block, sky, dayTime, camSky, partialTick));
            }
        }
    }
}
