package com.aetherium.mixin.core;

import com.aetherium.client.SceneryHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
// @era:fog-begin gl|shader17|shader19
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:fog-else params
//~ import com.llamalad7.mixinextras.injector.ModifyReturnValue;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.renderer.FogParameters;
//~ import net.minecraft.client.renderer.FogRenderer;
//~ import org.joml.Vector4f;
// @era:fog-else buffer|data
//~ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//~ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//~ import java.nio.ByteBuffer;
//~ import net.minecraft.client.renderer.fog.FogRenderer;
//~ import org.joml.Vector4f;
// @era:fog-end

/**
 * Fog settings (distance fog, fog start, water fog, clear lava). Each Minecraft generation hands
 * its terrain fog over differently, so each gets its own hook; the decisions are shared in
 * {@code SceneryHooks} / {@code Scenery}:
 * <ul>
 *   <li>up to 1.21.1: {@code FogRenderer.setupFog} TAIL rewrites the fog vanilla just set</li>
 *   <li>1.21.2 - 1.21.5: {@code setupFog} returns {@code FogParameters}; a new record replaces it</li>
 *   <li>1.21.6+: the four fog distances are wrapped on their way into the fog uniform buffer
 *       (written by {@code setupFog} up to 1.21.11 and by {@code updateBuffer(FogData)} on 26.x)</li>
 * </ul>
 * Applied only where {@code Capabilities.SCENERY_CONTROLS} is true.
 */
// @era:fog-begin gl|shader17|shader19|params
@Mixin(targets = "net.minecraft.client.renderer.FogRenderer")
// @era:fog-else buffer|data
//~ @Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
// @era:fog-end
public abstract class FogControlMixin {
    // @era:fog-begin shader19
    @Inject(method = "setupFog", at = @At("TAIL"), require = 0)
    private static void aetherium$fogControls(final Camera camera, final FogRenderer.FogMode mode, final float farPlane,
                                              final boolean thick, final float partialTick, final CallbackInfo ci) {
        if (mode == FogRenderer.FogMode.FOG_TERRAIN) {
            SceneryHooks.adjustFog(camera, farPlane, thick);
        }
    }
    // @era:fog-else gl|shader17
    //~ @Inject(method = "setupFog", at = @At("TAIL"), require = 0)
    //~ private static void aetherium$fogControls(final Camera camera, final FogRenderer.FogMode mode, final float farPlane,
                                              //~ final boolean thick, final CallbackInfo ci) {
        //~ if (mode == FogRenderer.FogMode.FOG_TERRAIN) {
            //~ SceneryHooks.adjustFog(camera, farPlane, thick);
        //~ }
    //~ }
    // @era:fog-else params
    //~ @ModifyReturnValue(method = "setupFog", at = @At("RETURN"), require = 0)
    //~ private static FogParameters aetherium$fogControls(final FogParameters original, final Camera camera,
                                                       //~ final FogRenderer.FogMode mode, final Vector4f color,
                                                       //~ final float renderDistance, final boolean thick,
                                                       //~ final float partialTick) {
        //~ return mode == FogRenderer.FogMode.FOG_TERRAIN
                //~ ? SceneryHooks.adjustFog(original, camera, renderDistance, thick) : original;
    //~ }
    // @era:fog-else buffer
    //~ @WrapOperation(method = "setupFog", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"),
            //~ require = 0)
    //~ private void aetherium$fogControls(final FogRenderer self, final ByteBuffer buffer, final int offset,
                                       //~ final Vector4f color, final float envStart, final float envEnd,
                                       //~ final float rdStart, final float rdEnd, final float skyEnd,
                                       //~ final float cloudEnd, final Operation<Void> original) {
        //~ final float[] fog = SceneryHooks.adjustFogBuffer(color.x(), color.y(), color.z(), envStart, envEnd,
                //~ rdStart, rdEnd);
        //~ if (fog == null) {
            //~ original.call(self, buffer, offset, color, envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
        //~ } else {
            //~ original.call(self, buffer, offset, color, fog[0], fog[1], fog[2], fog[3], skyEnd, cloudEnd);
        //~ }
    //~ }
    // @era:fog-else data
    //~ @WrapOperation(method = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"),
            //~ require = 0)
    //~ private void aetherium$fogControls(final FogRenderer self, final ByteBuffer buffer, final int offset,
                                       //~ final Vector4f color, final float envStart, final float envEnd,
                                       //~ final float rdStart, final float rdEnd, final float skyEnd,
                                       //~ final float cloudEnd, final Operation<Void> original) {
        //~ final float[] fog = SceneryHooks.adjustFogBuffer(color.x(), color.y(), color.z(), envStart, envEnd,
                //~ rdStart, rdEnd);
        //~ if (fog == null) {
            //~ original.call(self, buffer, offset, color, envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
        //~ } else {
            //~ original.call(self, buffer, offset, color, fog[0], fog[1], fog[2], fog[3], skyEnd, cloudEnd);
        //~ }
    //~ }
    // @era:fog-end
}
