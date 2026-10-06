package dev.aetherium.client.render;

import dev.aetherium.config.AetheriumConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Entity culling beyond vanilla's frustum test. NORMAL: size-scaled distance cull. AGGRESSIVE: plus block raycast
 * occlusion (result cached per entity for a few ticks). EXTREME: plus aggressive distance and no re-checks for
 * entities that were hidden recently.
 */
public final class EntityCuller {
    private static final Map<Entity, long[]> cache = new WeakHashMap<>(); // [lastCheckTick, hiddenFlag]
    private static volatile AetheriumConfig.EntityCullingMode mode = AetheriumConfig.EntityCullingMode.NORMAL;

    private EntityCuller() {}

    public static void applyConfig(AetheriumConfig cfg) { mode = cfg.performance.entityCulling; }

    public static boolean shouldSkip(Entity e, double camX, double camY, double camZ) {
        if (mode == AetheriumConfig.EntityCullingMode.OFF) return false;
        Minecraft mc = Minecraft.getInstance();
        if (e == mc.getCameraEntity() || e instanceof Player || e.isCurrentlyGlowing() || e.hasCustomName()) return false;

        AABB bb = e.getBoundingBox();
        double size = Math.max(bb.getXsize(), Math.max(bb.getYsize(), bb.getZsize()));
        double d2 = e.distanceToSqr(camX, camY, camZ);
        double maxDist = switch (mode) {
            case NORMAL -> 96 + size * 48;
            case AGGRESSIVE -> 72 + size * 40;
            case EXTREME -> 56 + size * 32;
            default -> Double.MAX_VALUE;
        };
        if (d2 > maxDist * maxDist) return true;
        if (mode == AetheriumConfig.EntityCullingMode.NORMAL || d2 < 16) return false;

        long tick = mc.level == null ? 0 : mc.level.getGameTime();
        int recheck = mode == AetheriumConfig.EntityCullingMode.EXTREME ? 10 : 4;
        long[] c;
        synchronized (cache) { c = cache.computeIfAbsent(e, k -> new long[]{Long.MIN_VALUE, 0}); }
        if (tick - c[0] < recheck) return c[1] == 1;

        boolean hidden = occluded(e, bb, new Vec3(camX, camY, camZ));
        c[0] = tick; c[1] = hidden ? 1 : 0;
        return hidden;
    }

    private static boolean occluded(Entity e, AABB bb, Vec3 cam) {
        if (e.level() == null) return false;
        Vec3[] targets = {
                bb.getCenter(),
                new Vec3(bb.minX, bb.maxY, bb.minZ), new Vec3(bb.maxX, bb.maxY, bb.maxZ),
                new Vec3(bb.minX, bb.minY, bb.maxZ), new Vec3(bb.maxX, bb.minY, bb.minZ)
        };
        int samples = mode == AetheriumConfig.EntityCullingMode.EXTREME ? 3 : targets.length;
        for (int i = 0; i < samples; i++) {
            HitResult hit = e.level().clip(new ClipContext(cam, targets[i], ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e));
            if (hit.getType() == HitResult.Type.MISS) return false;
            if (hit.getLocation().distanceToSqr(targets[i]) < 1.0) return false;
        }
        return !(e instanceof LivingEntity le && le.hurtTime > 0);
    }
}
