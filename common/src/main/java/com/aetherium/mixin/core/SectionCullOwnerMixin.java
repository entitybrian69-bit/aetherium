package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:section-cull-begin pop17|rendered19|mesh21
import com.aetherium.client.SectionCullAccess;
import com.mojang.blaze3d.vertex.VertexBuffer;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-cull-else v16|none
// @era:section-cull-end

/**
 * Tells a section's solid, cutout-mipped and cutout vertex buffers which section they belong to
 * (the Aetherium renderer on 1.17..1.21.4; translucent and tripwire stay vanilla). The buffers are
 * created once with the section and reused for every rebuild. Applied only where
 * {@code Capabilities.SECTION_CULL} is true.
 */
// @era:section-api-begin srd
@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
// @era:section-api-else v16|crd
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk")
// @era:section-api-else srd-ro|mesh|none
//~ @Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection")
// @era:section-api-end
public abstract class SectionCullOwnerMixin {
    // @era:section-cull-begin pop17|rendered19|mesh21
    @Shadow
    @Final
    private Map<RenderType, VertexBuffer> buffers;

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void aetherium$tagBuffers(final CallbackInfo ci) {
        aetherium$tag(this.buffers.get(RenderType.solid()), this);
        aetherium$tag(this.buffers.get(RenderType.cutoutMipped()), this);
        aetherium$tag(this.buffers.get(RenderType.cutout()), this);
    }

    @Unique
    private static void aetherium$tag(final VertexBuffer buffer, final Object section) {
        final Object raw = buffer;
        if (raw instanceof SectionCullAccess) {
            ((SectionCullAccess) raw).aetheriumSetCullSection(section);
        }
    }
    // @era:section-cull-else v16|none
    // @era:section-cull-end
}
