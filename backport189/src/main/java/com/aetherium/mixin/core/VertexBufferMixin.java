package com.aetherium.mixin.core;

import com.aetherium.client.TerrainRenderer;
import com.aetherium.render.CompactTerrain;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Terrain renderer: every vertex buffer remembers the face layout of the mesh it holds (or none). */
@Mixin(VertexBuffer.class)
public abstract class VertexBufferMixin implements TerrainRenderer.SortedBuffer {

    @Shadow
    private int count;

    @Unique
    private CompactTerrain.Layout aetherium$layout;

    @Inject(method = "bufferData", at = @At("HEAD"))
    private void aetherium$upload(final ByteBuffer data, final CallbackInfo ci) {
        this.aetherium$layout = TerrainRenderer.takeLayout();
    }

    @Override
    public CompactTerrain.Layout aetherium$layout() {
        return this.aetherium$layout;
    }

    @Override
    public int aetherium$count() {
        return this.count;
    }
}
