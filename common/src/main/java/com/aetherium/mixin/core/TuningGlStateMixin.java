package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-gl-state-begin g17
import com.aetherium.perf.RenderMods;
import com.aetherium.perf.Tuning;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-gl-state-else g212
//~ import com.aetherium.perf.RenderMods;
//~ import com.aetherium.perf.Tuning;
//~ import org.lwjgl.opengl.GL11;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-gl-state-else none
// @era:t-gl-state-else v16
//~ import com.aetherium.perf.RenderMods;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.platform.GlStateManager;
//~ import org.lwjgl.opengl.ARBFramebufferObject;
//~ import org.lwjgl.opengl.EXTFramebufferObject;
//~ import org.lwjgl.opengl.GL11;
//~ import org.lwjgl.opengl.GL30;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-gl-state-end

/**
 * GL call trimming in {@code GlStateManager} (1.12.0, Minecraft 1.16.5; render thread only).
 *
 * <p>Skip redundant GL calls: vanilla caches most capability switches, but not the framebuffer
 * binding, the viewport or the vertex-array client states. A render target binds itself and sets
 * the viewport every time it is used, and every chunk section re-enables the position, colour and
 * normal arrays. Those calls are skipped when they would not change anything. Texture-coordinate
 * arrays are never skipped (they depend on the client-active texture unit). Any read or draw
 * framebuffer binding, or a deleted framebuffer, forgets the cached binding.</p>
 *
 * <p>Cheaper damage tint: {@code setupOverlayColor} sets up texture unit 1 for the hurt/flash
 * overlay with 16 GL calls (texture matrix, 4 texture parameters, the combiner) each time an
 * entity batch is drawn. All of that is state of unit 1 and of the overlay texture, which nothing
 * else in the game changes, so after the first full setup for a texture only the texture switch
 * remains. {@code teardownOverlayColor} only disables unit 1's texture, which the short path
 * re-enables.</p>
 *
 * <p>Both are off next to OptiFine, Iris or Canvas ({@link RenderMods}), which change GL state
 * behind {@code GlStateManager}'s back.</p>
 */
@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager")
public abstract class TuningGlStateMixin {
    // @era:t-gl-state-begin g17
    @Unique
    private static boolean aetherium$viewportKnown;
    @Unique
    private static int aetherium$vx;
    @Unique
    private static int aetherium$vy;
    @Unique
    private static int aetherium$vw;
    @Unique
    private static int aetherium$vh;

    @Unique
    private static boolean aetherium$trim() {
        return Tuning.glStateCache && !RenderMods.PRESENT;
    }

    /** Vanilla still records the viewport in GlStateManager.Viewport; only the repeated GL call is skipped. */
    @Redirect(method = "_viewport", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V"), require = 0)
    private static void aetherium$viewport(final int x, final int y, final int w, final int h) {
        if (aetherium$trim() && aetherium$viewportKnown && x == aetherium$vx && y == aetherium$vy
                && w == aetherium$vw && h == aetherium$vh) {
            return;
        }
        aetherium$viewportKnown = true;
        aetherium$vx = x;
        aetherium$vy = y;
        aetherium$vw = w;
        aetherium$vh = h;
        GL11.glViewport(x, y, w, h);
    }

    private static final int GL_FRAMEBUFFER = 36160;

    @Unique
    private static int aetherium$fbo = -1;

    /** 1.17 - 1.21.1 bind every framebuffer through GL30 without a cache (1.21.2 added its own). */
    @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V"), require = 0)
    private static void aetherium$bindCore(final int target, final int id) {
        if (target != GL_FRAMEBUFFER) {
            aetherium$fbo = -1; // a read or draw binding: the combined one is no longer known
        } else if (aetherium$trim() && id == aetherium$fbo) {
            return;
        } else {
            aetherium$fbo = id;
        }
        GL30.glBindFramebuffer(target, id);
    }

    @Inject(method = "_glDeleteFramebuffers", at = @At("HEAD"), require = 0)
    private static void aetherium$deleteFbo(final int id, final CallbackInfo ci) {
        aetherium$fbo = -1;
    }
    // @era:t-gl-state-else g212
    //~ @Unique
    //~ private static boolean aetherium$viewportKnown;
    //~ @Unique
    //~ private static int aetherium$vx;
    //~ @Unique
    //~ private static int aetherium$vy;
    //~ @Unique
    //~ private static int aetherium$vw;
    //~ @Unique
    //~ private static int aetherium$vh;

    //~ @Unique
    //~ private static boolean aetherium$trim() {
        //~ return Tuning.glStateCache && !RenderMods.PRESENT;
    //~ }

    //~ /** Vanilla still records the viewport in GlStateManager.Viewport; only the repeated GL call is skipped. */
    //~ @Redirect(method = "_viewport", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V"), require = 0)
    //~ private static void aetherium$viewport(final int x, final int y, final int w, final int h) {
        //~ if (aetherium$trim() && aetherium$viewportKnown && x == aetherium$vx && y == aetherium$vy
                //~ && w == aetherium$vw && h == aetherium$vh) {
            //~ return;
        //~ }
        //~ aetherium$viewportKnown = true;
        //~ aetherium$vx = x;
        //~ aetherium$vy = y;
        //~ aetherium$vw = w;
        //~ aetherium$vh = h;
        //~ GL11.glViewport(x, y, w, h);
    //~ }
    // @era:t-gl-state-else none
    // @era:t-gl-state-else v16
    //~ private static final int GL_FRAMEBUFFER = 36160;
    //~ private static final int GL_VERTEX_ARRAY = 32884;
    //~ private static final int GL_NORMAL_ARRAY = 32885;
    //~ private static final int GL_COLOR_ARRAY = 32886;

    //~ /** Bound GL_FRAMEBUFFER, or -1 when unknown. */
    //~ @Unique
    //~ private static int aetherium$fbo = -1;
    //~ @Unique
    //~ private static boolean aetherium$viewportKnown;
    //~ @Unique
    //~ private static int aetherium$vx;
    //~ @Unique
    //~ private static int aetherium$vy;
    //~ @Unique
    //~ private static int aetherium$vw;
    //~ @Unique
    //~ private static int aetherium$vh;
    //~ /** Bit 0..2: vertex, normal, colour array known enabled. */
    //~ @Unique
    //~ private static int aetherium$arrays;
    //~ /** Overlay texture whose unit-1 setup is complete, or -1. */
    //~ @Unique
    //~ private static int aetherium$overlayTexture = -1;
    //~ @Unique
    //~ private static int aetherium$overlaySize;

    //~ @Unique
    //~ private static boolean aetherium$trim() {
        //~ return Tuning.glStateCache && !RenderMods.PRESENT;
    //~ }

    //~ @Unique
    //~ private static boolean aetherium$bind(final int target, final int id) {
        //~ if (target != GL_FRAMEBUFFER) {
            //~ aetherium$fbo = -1; // a read or draw binding: the combined one is no longer known
            //~ return true;
        //~ }
        //~ if (aetherium$trim() && id == aetherium$fbo) {
            //~ return false;
        //~ }
        //~ aetherium$fbo = id;
        //~ return true;
    //~ }

    //~ @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V"), require = 0)
    //~ private static void aetherium$bindCore(final int target, final int id) {
        //~ if (aetherium$bind(target, id)) {
            //~ GL30.glBindFramebuffer(target, id);
        //~ }
    //~ }

    //~ @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBFramebufferObject;glBindFramebuffer(II)V"), require = 0)
    //~ private static void aetherium$bindArb(final int target, final int id) {
        //~ if (aetherium$bind(target, id)) {
            //~ ARBFramebufferObject.glBindFramebuffer(target, id);
        //~ }
    //~ }

    //~ @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/EXTFramebufferObject;glBindFramebufferEXT(II)V"), require = 0)
    //~ private static void aetherium$bindExt(final int target, final int id) {
        //~ if (aetherium$bind(target, id)) {
            //~ EXTFramebufferObject.glBindFramebufferEXT(target, id);
        //~ }
    //~ }

    //~ @Inject(method = "_glDeleteFramebuffers", at = @At("HEAD"), require = 0)
    //~ private static void aetherium$deleteFbo(final int id, final CallbackInfo ci) {
        //~ aetherium$fbo = -1;
    //~ }

    //~ @Redirect(method = "_viewport", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glViewport(IIII)V"), require = 0)
    //~ private static void aetherium$viewport(final int x, final int y, final int w, final int h) {
        //~ if (aetherium$trim() && aetherium$viewportKnown && x == aetherium$vx && y == aetherium$vy
                //~ && w == aetherium$vw && h == aetherium$vh) {
            //~ return;
        //~ }
        //~ aetherium$viewportKnown = true;
        //~ aetherium$vx = x;
        //~ aetherium$vy = y;
        //~ aetherium$vw = w;
        //~ aetherium$vh = h;
        //~ GL11.glViewport(x, y, w, h);
    //~ }

    //~ @Unique
    //~ private static int aetherium$arrayBit(final int cap) {
        //~ return cap == GL_VERTEX_ARRAY ? 1 : cap == GL_NORMAL_ARRAY ? 2 : cap == GL_COLOR_ARRAY ? 4 : 0;
    //~ }

    //~ @Redirect(method = "_enableClientState", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnableClientState(I)V"), require = 0)
    //~ private static void aetherium$enableArray(final int cap) {
        //~ final int bit = aetherium$arrayBit(cap);
        //~ if (bit != 0 && aetherium$trim() && (aetherium$arrays & bit) != 0) {
            //~ return;
        //~ }
        //~ GL11.glEnableClientState(cap);
        //~ aetherium$arrays |= bit;
    //~ }

    //~ @Redirect(method = "_disableClientState", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisableClientState(I)V"), require = 0)
    //~ private static void aetherium$disableArray(final int cap) {
        //~ GL11.glDisableClientState(cap);
        //~ aetherium$arrays &= ~aetherium$arrayBit(cap);
    //~ }

    //~ @Inject(method = "setupOverlayColor(II)V", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private static void aetherium$overlayFast(final int texture, final int size, final CallbackInfo ci) {
        //~ if (Tuning.cheapDamageTint && !RenderMods.PRESENT && texture == aetherium$overlayTexture
                //~ && size == aetherium$overlaySize) {
            //~ GlStateManager._activeTexture(33985);
            //~ GlStateManager._enableTexture();
            //~ GlStateManager._bindTexture(texture);
            //~ GlStateManager._activeTexture(33984);
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = "setupOverlayColor(II)V", at = @At("RETURN"), require = 0)
    //~ private static void aetherium$overlayFull(final int texture, final int size, final CallbackInfo ci) {
        //~ aetherium$overlayTexture = texture;
        //~ aetherium$overlaySize = size;
    //~ }
    // @era:t-gl-state-end
}
