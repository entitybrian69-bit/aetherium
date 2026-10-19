package com.aetherium.mixin.core;

// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.shader.ShaderRuntime;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:shader-end
import org.spongepowered.asm.mixin.Mixin;

/**
 * Shader pack engine (1.16.5): every world draw binds the pack program for what is being drawn,
 * binds of Minecraft's main framebuffer go to the pack's gbuffers during the world pass, and the
 * texture unit / program state vanilla sets is tracked (never queried from GL). One static call
 * each; a field read while no pack is loaded. Targeted by name like FogStateMixin.
 */
@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager")
public abstract class ShaderGlMixin {

    // @era:shader-begin none
    // @era:shader-else v16
    //~ @Inject(method = "_drawArrays", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$draw(final int mode, final int first, final int count, final CallbackInfo ci) {
        //~ if (ShaderRuntime.beforeDraw()) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = "_glUseProgram", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$useProgram(final int program, final CallbackInfo ci) {
        //~ ShaderRuntime.useProgram(program);
    //~ }

    //~ @ModifyVariable(method = "_glBindFramebuffer", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    //~ private static int aetherium$framebuffer(final int framebuffer) {
        //~ return ShaderRuntime.redirectFramebuffer(framebuffer);
    //~ }

    //~ @Inject(method = "_activeTexture", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$activeTexture(final int unit, final CallbackInfo ci) {
        //~ ShaderRuntime.activeTexture(unit);
    //~ }

    //~ @Inject(method = "_bindTexture", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$bindTexture(final int id, final CallbackInfo ci) {
        //~ ShaderRuntime.bindTexture(id);
    //~ }

    //~ @Inject(method = "_enableTexture", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$enableTexture(final CallbackInfo ci) {
        //~ ShaderRuntime.textureEnabled(true);
    //~ }

    //~ @Inject(method = "_disableTexture", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$disableTexture(final CallbackInfo ci) {
        //~ ShaderRuntime.textureEnabled(false);
    //~ }
    // @era:shader-end
}
