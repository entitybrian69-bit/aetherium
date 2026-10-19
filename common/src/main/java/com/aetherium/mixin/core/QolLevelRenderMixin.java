package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-world-begin hud
// @era:q-world-else v16
//~ import com.aetherium.client.QolWorldRenderer;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.GameRenderer;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-world-end

/**
 * Waypoints and damage numbers in the world (Quality of life, Minecraft 1.16.5), drawn after the
 * level so they sit on top of terrain, entities and weather.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class QolLevelRenderMixin {
    // @era:q-world-begin hud
    // @era:q-world-else v16
    //~ @Inject(method = "renderLevel", at = @At("TAIL"), require = 0)
    //~ private void aetherium$qolWorld(final PoseStack pose, final float partialTick, final long finishNanos,
                                    //~ final boolean outline, final Camera camera, final GameRenderer gameRenderer,
                                    //~ final LightTexture light, final Matrix4f projection, final CallbackInfo ci) {
        //~ QolWorldRenderer.render(pose, camera);
    //~ }
    // @era:q-world-end
}
