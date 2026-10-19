package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import net.minecraft.client.renderer.RenderType;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//~ import net.minecraft.client.renderer.RenderStateShard;
// @era:shader-end
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): which RenderType is set up decides the gbuffers program of the
 * draws that follow (terrain, water, entities, glint, ...). One call per render-type batch.
 */
// @era:shader-begin none
@Mixin(Minecraft.class)
// @era:shader-else v16
//~ @Mixin(RenderStateShard.class)
// @era:shader-end
public abstract class ShaderStateShardMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Shadow
    //~ @Final
    //~ protected String name;

    //~ @Inject(method = "setupRenderState", at = @At("HEAD"), require = 0)
    //~ private void aetherium$typeSetup(final CallbackInfo ci) {
        //~ if ((Object) this instanceof RenderType) {
            //~ ShaderRuntime.typeSetup(this.name);
        //~ }
    //~ }

    //~ @Inject(method = "clearRenderState", at = @At("HEAD"), require = 0)
    //~ private void aetherium$typeClear(final CallbackInfo ci) {
        //~ if ((Object) this instanceof RenderType) {
            //~ ShaderRuntime.typeClear();
        //~ }
    //~ }
    // @era:shader-end
}
