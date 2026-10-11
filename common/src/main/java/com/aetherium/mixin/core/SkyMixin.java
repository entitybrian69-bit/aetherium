package com.aetherium.mixin.core;

import com.aetherium.qol.Scenery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:sky-begin mesh
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
// @era:sky-else v16
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.math.Matrix4f;
// @era:sky-else end17
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.renderer.ShaderInstance;
// @era:sky-else rb19
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.renderer.ShaderInstance;
// @era:sky-else rb193
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.VertexBuffer;
//~ import net.minecraft.client.renderer.ShaderInstance;
//~ import org.joml.Matrix4f;
// @era:sky-else renderer
// @era:sky-end

/**
 * Sky settings: the whole sky, the sun / moon / sunrise glow, and the stars.
 * <ul>
 *   <li>up to 1.21.1 ({@code LevelRenderer.renderSky}): HEAD cancel for the whole sky; the three
 *       immediate-mode draws (sunrise fan, sun, moon) are skipped with their vertex data released
 *       the way that version expects (popped buffer, released {@code RenderedBuffer}, closed
 *       {@code MeshData}); the stars are the second static-buffer draw.</li>
 *   <li>1.21.2+ ({@code SkyRenderer}): every part has its own method, cancelled at its head.</li>
 * </ul>
 * The cloud height is {@code CloudHeightMixin}. Applied only where
 * {@code Capabilities.SCENERY_CONTROLS} is true.
 */
// @era:sky-begin v16|end17|rb19|rb193|mesh
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
// @era:sky-else renderer
//~ @Mixin(targets = "net.minecraft.client.renderer.SkyRenderer")
// @era:sky-end
public abstract class SkyMixin {
    // @era:sky-begin v16|end17|rb19|rb193|mesh
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$sky(final CallbackInfo ci) {
        if (!Scenery.sky) {
            ci.cancel();
        }
    }
    // @era:sky-else renderer
    //~ @Inject(method = {"renderSkyDisc", "renderDarkDisc", "renderEndSky", "renderEndFlash", "renderSunMoonAndStars",
            //~ "renderSunriseAndSunset"}, at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$sky(final CallbackInfo ci) {
        //~ if (!Scenery.sky) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = {"renderSun", "renderMoon", "renderSunriseAndSunset"}, at = @At("HEAD"), cancellable = true,
            //~ require = 0)
    //~ private void aetherium$sunMoon(final CallbackInfo ci) {
        //~ if (!Scenery.sunMoon) {
            //~ ci.cancel();
        //~ }
    //~ }

    //~ @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$stars(final CallbackInfo ci) {
        //~ if (!Scenery.stars) {
            //~ ci.cancel();
        //~ }
    //~ }
    // @era:sky-end

    // @era:sky-begin mesh
    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/MeshData;)V"),
            require = 0)
    private void aetherium$sunMoon(final MeshData mesh, final Operation<Void> original) {
        if (Scenery.sunMoon) {
            original.call(mesh);
        } else {
            mesh.close();
        }
    }
    // @era:sky-else v16|end17
    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/BufferUploader;end(Lcom/mojang/blaze3d/vertex/BufferBuilder;)V"), require = 0)
    //~ private void aetherium$sunMoon(final BufferBuilder builder, final Operation<Void> original) {
        //~ if (Scenery.sunMoon) {
            //~ original.call(builder);
        //~ } else {
            //~ builder.popNextBuffer();
        //~ }
    //~ }
    // @era:sky-else rb19|rb193
    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/BufferUploader;drawWithShader(Lcom/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer;)V"),
            //~ require = 0)
    //~ private void aetherium$sunMoon(final BufferBuilder.RenderedBuffer rendered, final Operation<Void> original) {
        //~ if (Scenery.sunMoon) {
            //~ original.call(rendered);
        //~ } else {
            //~ rendered.release();
        //~ }
    //~ }
    // @era:sky-else renderer
    // @era:sky-end

    // @era:sky-begin rb193|mesh
    @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
            ordinal = 1), require = 0)
    private void aetherium$stars(final VertexBuffer buffer, final Matrix4f pose, final Matrix4f projection,
                                 final ShaderInstance shader, final Operation<Void> original) {
        if (Scenery.stars) {
            original.call(buffer, pose, projection, shader);
        }
    }
    // @era:sky-else v16
    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw(Lcom/mojang/math/Matrix4f;I)V", ordinal = 1), require = 0)
    //~ private void aetherium$stars(final VertexBuffer buffer, final Matrix4f matrix, final int mode, final Operation<Void> original) {
        //~ if (Scenery.stars) {
            //~ original.call(buffer, matrix, mode);
        //~ }
    //~ }
    // @era:sky-else end17|rb19
    //~ @WrapOperation(method = "renderSky", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V",
            //~ ordinal = 1), require = 0)
    //~ private void aetherium$stars(final VertexBuffer buffer, final Matrix4f pose, final Matrix4f projection,
                                 //~ final ShaderInstance shader, final Operation<Void> original) {
        //~ if (Scenery.stars) {
            //~ original.call(buffer, pose, projection, shader);
        //~ }
    //~ }
    // @era:sky-else renderer
    // @era:sky-end
}
