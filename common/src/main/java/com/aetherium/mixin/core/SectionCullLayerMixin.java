package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-cull-begin pop17|rendered19|mesh21
import com.aetherium.client.SectionCull;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-cull-else v16|none
// @era:section-cull-end
// @era:layer-sig-begin nopose
import org.joml.Matrix4f;
// @era:layer-sig-else mojang
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
// @era:layer-sig-else joml
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import org.joml.Matrix4f;
// @era:layer-sig-else v16|none
// @era:layer-sig-end

/**
 * The Aetherium renderer on 1.17..1.21.4: which layer vanilla is drawing and from where (the
 * camera position vanilla subtracts from every section origin), and the per-frame switch.
 * Applied only where {@code Capabilities.SECTION_CULL} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class SectionCullLayerMixin {
    // @era:section-cull-begin pop17|rendered19|mesh21
    @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    private void aetherium$cullFrame(final CallbackInfo ci) {
        if (SectionCull.frame()) {
            // Just turned on: rebuild so the meshes already on the GPU are sorted too.
            ((LevelRenderer) (Object) this).allChanged();
        }
    }

    @Unique
    private static boolean aetherium$terrain(final RenderType type) {
        return type == RenderType.solid() || type == RenderType.cutoutMipped() || type == RenderType.cutout();
    }
    // @era:section-cull-else v16|none
    // @era:section-cull-end

    // @era:layer-sig-begin nopose
    @Inject(method = "renderSectionLayer", at = @At("HEAD"), require = 0)
    private void aetherium$beginLayer(final RenderType type, final double camX, final double camY, final double camZ,
                                      final Matrix4f modelView, final Matrix4f projection, final CallbackInfo ci) {
        SectionCull.beginLayer(aetherium$terrain(type), camX, camY, camZ);
    }

    @Inject(method = "renderSectionLayer", at = @At("RETURN"), require = 0)
    private void aetherium$endLayer(final RenderType type, final double camX, final double camY, final double camZ,
                                    final Matrix4f modelView, final Matrix4f projection, final CallbackInfo ci) {
        SectionCull.endLayer();
    }
    // @era:layer-sig-else mojang|joml
    //~ @Inject(method = {"renderChunkLayer", "renderSectionLayer"}, at = @At("HEAD"), require = 0)
    //~ private void aetherium$beginLayer(final RenderType type, final PoseStack poseStack, final double camX,
                                      //~ final double camY, final double camZ, final Matrix4f projection,
                                      //~ final CallbackInfo ci) {
        //~ SectionCull.beginLayer(aetherium$terrain(type), camX, camY, camZ);
    //~ }

    //~ @Inject(method = {"renderChunkLayer", "renderSectionLayer"}, at = @At("RETURN"), require = 0)
    //~ private void aetherium$endLayer(final RenderType type, final PoseStack poseStack, final double camX,
                                    //~ final double camY, final double camZ, final Matrix4f projection,
                                    //~ final CallbackInfo ci) {
        //~ SectionCull.endLayer();
    //~ }
    // @era:layer-sig-else v16|none
    // @era:layer-sig-end
}
