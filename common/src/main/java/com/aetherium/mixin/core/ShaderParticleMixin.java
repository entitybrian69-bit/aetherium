package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import net.minecraft.client.particle.ParticleEngine;

import org.spongepowered.asm.mixin.Mixin;

/** Shader pack engine (1.16.5): particles draw with gbuffers_textured_lit. Once per frame. */
@Mixin(ParticleEngine.class)
public abstract class ShaderParticleMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Inject(method = "render", at = @At("HEAD"), require = 0)
    //~ private void aetherium$particlesStart(final PoseStack poseStack, final MultiBufferSource.BufferSource buffers,
                                          //~ final LightTexture lightTexture, final Camera camera, final float partialTick,
                                          //~ final CallbackInfo ci) {
        //~ ShaderRuntime.setPhase(4);
    //~ }

    //~ @Inject(method = "render", at = @At("RETURN"), require = 0)
    //~ private void aetherium$particlesEnd(final PoseStack poseStack, final MultiBufferSource.BufferSource buffers,
                                        //~ final LightTexture lightTexture, final Camera camera, final float partialTick,
                                        //~ final CallbackInfo ci) {
        //~ ShaderRuntime.endPhase(4);
    //~ }
    // @era:shader-end
}
