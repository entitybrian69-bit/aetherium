package com.aetherium.client;

import com.aetherium.perf.RebuildCoalescer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Rebuild coalescing on 1.17+ (1.16.5 uses {@code QolHooks.deferRebuild}): the decision is
 * {@link RebuildCoalescer#defer}; this class supplies the section's dirty timestamps (stamped by
 * {@code RenderChunkDirtyMixin}) and its distance from the camera. Called only for sections that
 * are dirty this frame, so reading the camera here costs nothing on quiet frames.
 */
public final class CoalesceHooks {
    private CoalesceHooks() {
    }

    /** The dirty check of the section compile loop, for a dirty section: true to leave it for a later frame. */
    public static boolean defer(final Object section, final BlockPos origin, final boolean fromPlayer,
                                final boolean compiled) {
        if (!RebuildCoalescer.enabled() || fromPlayer || !(section instanceof RenderChunkDirtyAccess)) {
            return false;
        }
        final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        if (camera == null) {
            return false;
        }
        // @era:cam-pos-begin getter
        final Vec3 cam = camera.getPosition();
        // @era:cam-pos-else position
        //~ final Vec3 cam = camera.position();
        // @era:cam-pos-end
        final double dx = origin.getX() + 8.0 - cam.x;
        final double dy = origin.getY() + 8.0 - cam.y;
        final double dz = origin.getZ() + 8.0 - cam.z;
        final RenderChunkDirtyAccess times = (RenderChunkDirtyAccess) section;
        return RebuildCoalescer.defer(System.nanoTime(), times.aetheriumFirstDirty(), times.aetheriumLastDirty(),
                false, compiled, dx * dx + dy * dy + dz * dz);
    }
}
