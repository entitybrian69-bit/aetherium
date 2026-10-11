package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:mesh-alloc-begin v16
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:mesh-alloc-else none
// @era:mesh-alloc-end

/**
 * Less garbage from smooth lighting while meshing (1.16.5 - 1.21.4): the ambient-occlusion calculation
 * allocates a {@code BlockPos.MutableBlockPos} for every quad of every block. Each thread reuses
 * one; {@code calculate} sets it before every read and does not call itself.
 *
 * <p>Applied only where {@code Capabilities.MESH_ALLOC} is true. Targeted by name: the class is
 * package-private.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer$AmbientOcclusionFace")
public abstract class AoFaceAllocMixin {
    // @era:mesh-alloc-begin v16
    @Unique
    private static final ThreadLocal<BlockPos.MutableBlockPos> AETHERIUM$POS = new ThreadLocal<BlockPos.MutableBlockPos>();

    @Redirect(method = "calculate", at = @At(value = "NEW", target = "net/minecraft/core/BlockPos$MutableBlockPos"),
            require = 0)
    private BlockPos.MutableBlockPos aetherium$pos() {
        BlockPos.MutableBlockPos pos = AETHERIUM$POS.get();
        if (pos == null) {
            pos = new BlockPos.MutableBlockPos();
            AETHERIUM$POS.set(pos);
        }
        return pos;
    }
    // @era:mesh-alloc-else none
    // @era:mesh-alloc-end
}
