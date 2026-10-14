package com.aetherium.mixin.core;

import com.aetherium.client.FreelookHooks;
import com.aetherium.client.ZoomKeys;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Mouse look: freelook takes the movement, zoom lowers the sensitivity and smooths it. */
@Mixin(EntityRenderer.class)
public abstract class CameraMixin {

    @Redirect(method = "updateCameraAndRender", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/entity/EntityPlayerSP;turn(FF)V"))
    private void aetherium$turn(final EntityPlayerSP player, final float yaw, final float pitch) {
        if (FreelookHooks.turn(yaw, pitch)) {
            return;
        }
        if (ZoomKeys.isZooming()) {
            final float scale = (float) ZoomKeys.sensitivity(1.0);
            player.turn(yaw * scale, pitch * scale);
            return;
        }
        player.turn(yaw, pitch);
    }

    @Redirect(method = "updateCameraAndRender", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/settings/GameSettings;smoothCamera:Z"))
    private boolean aetherium$smooth(final GameSettings settings) {
        return ZoomKeys.smoothCamera(settings.smoothCamera);
    }
}
