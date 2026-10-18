package com.aetherium.client;

import com.aetherium.Capabilities;
import com.aetherium.perf.FogCull;
import com.aetherium.perf.Tuning;
import com.aetherium.perf.RenderToggles;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
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
    /** Nether roof cull in this filter pass (1.12.0 dimension culling, 1.17+). */
    private static boolean roof;
    /** The roof state the current list was filtered with. */
    private static boolean roofUsed;

    private FogCullHooks() {
    }

    /** Frame start: refresh vanilla's list when the fog limit moved since it was filtered. */
    public static void frame(final Minecraft mc) {
        FogCull.frame();
        if (!Capabilities.FOG_CULL) {
            return;
        }
        final Object renderer = mc.levelRenderer;
        if (renderer instanceof FogCullAccess
                && (FogCull.distanceStale(RenderToggles.smartChunkCulling) || roofWanted(mc) != roofUsed)) {
            ((FogCullAccess) renderer).aetheriumRefreshFogCull();
        }
    }

    /** Before a filter pass; false when nothing can be culled (then the list is left alone). */
    public static boolean begin() {
        final Minecraft mc = Minecraft.getInstance();
        roof = roofWanted(mc);
        roofUsed = roof;
        if (!FogCull.beginDistance(RenderToggles.smartChunkCulling) && !roof) {
            return false;
        }
        final Camera camera = ClientHooks.mainCamera(mc);
        if (camera == null) {
            FogCull.beginDistance(false);
            roof = false;
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

    /**
     * Nether roof cull (dimension culling): while the camera is below the bedrock roof (y 127) of
     * the Nether, sections from y 128 up cannot be seen. Spectators can look through the roof.
     */
    private static boolean roofWanted(final Minecraft mc) {
        if (!Tuning.dimensionCulling) {
            return false;
        }
        final ClientLevel level = mc.level;
        if (level == null || level.dimension() != Level.NETHER || mc.player == null || mc.player.isSpectator()) {
            return false;
        }
        final Camera camera = ClientHooks.mainCamera(mc);
        if (camera == null) {
            return false;
        }
        // @era:cam-pos-begin getter
        return camera.getPosition().y < 127.0;
        // @era:cam-pos-else position
        //~ return camera.position().y < 127.0;
        // @era:cam-pos-end
    }

    /** A 16-block section at this origin (block coordinates) is wholly inside the fog. */
    public static boolean hidden(final int x, final int y, final int z) {
        if (roof && y >= 128) {
            return true;
        }
        final float x0 = (float) (x - camX);
        final float y0 = (float) (y - camY);
        final float z0 = (float) (z - camZ);
        return FogCull.distanceHidden(x0, y0, z0, x0 + 16.0f, y0 + 16.0f, z0 + 16.0f);
    }
}
