package com.aetherium.client;

import com.aetherium.Capabilities;
import com.aetherium.perf.FogCull;
import com.aetherium.perf.RenderToggles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Fog cull of vanilla's visible-section list on 1.17..26.1 (see {@code FogCullMixin}): sections
 * that lie wholly past the distance where this frame's terrain fog is solid are dropped from the
 * list vanilla draws (and schedules rebuilds from). Render thread only.
 */
public final class FogCullHooks {

    private static double camX;
    private static double camY;
    private static double camZ;

    private FogCullHooks() {
    }

    /** Frame start: refresh vanilla's list when the fog limit moved since it was filtered. */
    public static void frame(final Minecraft mc) {
        FogCull.frame();
        if (!Capabilities.FOG_CULL) {
            return;
        }
        final Object renderer = mc.levelRenderer;
        if (renderer instanceof FogCullAccess && FogCull.distanceStale(RenderToggles.smartChunkCulling)) {
            ((FogCullAccess) renderer).aetheriumRefreshFogCull();
        }
    }

    /** Before a filter pass; false when nothing can be culled (then the list is left alone). */
    public static boolean begin() {
        if (!FogCull.beginDistance(RenderToggles.smartChunkCulling)) {
            return false;
        }
        final Camera camera = ClientHooks.mainCamera(Minecraft.getInstance());
        if (camera == null) {
            FogCull.beginDistance(false);
            return false;
        }
        // @era:cam-pos-begin getter
        final Vec3 pos = camera.getPosition();
        // @era:cam-pos-else position
        //~ final Vec3 pos = camera.position();
        // @era:cam-pos-end
        camX = pos.x;
        camY = pos.y;
        camZ = pos.z;
        return true;
    }

    /** A 16-block section at this origin (block coordinates) is wholly inside the fog. */
    public static boolean hidden(final int x, final int y, final int z) {
        final float x0 = (float) (x - camX);
        final float y0 = (float) (y - camY);
        final float z0 = (float) (z - camZ);
        return FogCull.distanceHidden(x0, y0, z0, x0 + 16.0f, y0 + 16.0f, z0 + 16.0f);
    }
}
