package com.aetherium.lighting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.aetherium.config.AetheriumConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Collects light sources on the client thread and publishes them to {@link LightField}.
 *
 * <p>Cost model, which is the whole point of the design:</p>
 * <ul>
 *   <li>FAST scans at 4 Hz, FANCY every tick; the scan is a distance check per entity.</li>
 *   <li>Sources are snapped to block centres, so chunk sections are only re-meshed
 *       when a source crosses a block boundary or changes brightness
 *       ({@link LightDiff}); standing still costs zero rebuilds.</li>
 *   <li>The source count is capped (12 FAST, 32 FANCY) and FAST caps the
 *       emitted level at 12, which keeps each rebuild to a handful of sections.</li>
 * </ul>
 */
public final class DynamicLightTracker {
    private static final LightField.Source[] EMPTY = new LightField.Source[0];

    private final AetheriumConfig config;
    private LightField.Source[] last = EMPTY;
    private int countdown;

    public DynamicLightTracker(final AetheriumConfig config) {
        this.config = config;
    }

    /** Drops every source and re-meshes where they were. */
    public void clear(final Minecraft minecraft) {
        if (this.last.length > 0 && minecraft != null && minecraft.levelRenderer != null && minecraft.level != null) {
            markDirty(minecraft, LightDiff.changedRegions(this.last, EMPTY));
        }
        this.last = EMPTY;
        LightField.clear();
    }

    /** Forget sources without touching the renderer (world unload). */
    public void reset() {
        this.last = EMPTY;
        LightField.clear();
    }

    public int activeSources() {
        return this.last.length;
    }

    public void tick(final Minecraft minecraft, final boolean allowed) {
        final AetheriumConfig.LightMode mode = this.config.dynamicLights.get();
        if (!allowed || mode == AetheriumConfig.LightMode.OFF || minecraft.level == null || minecraft.player == null) {
            if (this.last.length > 0) {
                clear(minecraft);
            }
            return;
        }
        if (--this.countdown > 0) {
            return;
        }
        final boolean fancy = mode == AetheriumConfig.LightMode.FANCY;
        this.countdown = fancy ? 1 : 5;
        final double range = fancy ? 48.0 : 32.0;
        final int maxLevel = fancy ? 15 : 12;
        final int maxSources = fancy ? 32 : 12;
        final boolean held = this.config.dynamicLightsHeld.get().booleanValue();
        final boolean entities = this.config.dynamicLightsEntities.get().booleanValue();

        final Entity viewer = minecraft.player;
        final double vx = viewer.getX();
        final double vy = viewer.getY();
        final double vz = viewer.getZ();
        final double rangeSq = range * range;
        final List<Candidate> found = new ArrayList<Candidate>();

        final Iterable<Entity> all = minecraft.level.entitiesForRendering();
        for (final Entity entity : all) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            final double distanceSq = entity.distanceToSqr(vx, vy, vz);
            if (distanceSq > rangeSq) {
                continue;
            }
            int level = 0;
            if (held && entity instanceof LivingEntity) {
                final LivingEntity living = (LivingEntity) entity;
                level = Math.max(luminance(living.getMainHandItem()), luminance(living.getOffhandItem()));
            }
            if (entities) {
                if (entity instanceof ItemEntity) {
                    level = Math.max(level, luminance(((ItemEntity) entity).getItem()));
                } else if (entity instanceof Blaze) {
                    level = Math.max(level, 10);
                }
                if (entity.isOnFire()) {
                    level = 15;
                }
            }
            if (level <= 0) {
                continue;
            }
            found.add(new Candidate(entity, Math.min(level, maxLevel), distanceSq));
        }

        final LightField.Source[] next = build(found, maxSources);
        final List<int[]> regions = LightDiff.changedRegions(this.last, next);
        if (!regions.isEmpty()) {
            markDirty(minecraft, regions);
        }
        this.last = next;
        LightField.publish(next);
    }

    private static LightField.Source[] build(final List<Candidate> found, final int maxSources) {
        if (found.isEmpty()) {
            return EMPTY;
        }
        if (found.size() > maxSources) {
            found.sort(new Comparator<Candidate>() {
                @Override
                public int compare(final Candidate a, final Candidate b) {
                    return Double.compare(a.distanceSq, b.distanceSq);
                }
            });
        }
        final int count = Math.min(maxSources, found.size());
        final LightField.Source[] out = new LightField.Source[count];
        for (int i = 0; i < count; i++) {
            final Candidate candidate = found.get(i);
            final Entity entity = candidate.entity;
            // Block-centre snapping: see the class comment.
            final double x = Math.floor(entity.getX()) + 0.5;
            final double y = Math.floor(entity.getEyeY()) + 0.5;
            final double z = Math.floor(entity.getZ()) + 0.5;
            out[i] = new LightField.Source(x, y, z, candidate.level);
        }
        return Arrays.copyOf(out, count);
    }

    private static void markDirty(final Minecraft minecraft, final List<int[]> regions) {
        // @era:dirty-begin blocks
        if (minecraft.levelRenderer == null) {
            return;
        }
        for (final int[] box : regions) {
            minecraft.levelRenderer.setBlocksDirty(box[0], box[1], box[2], box[3], box[4], box[5]);
        }
        // @era:dirty-else sections
        //~ if (minecraft.level == null) {
        //~     return;
        //~ }
        //~ for (final int[] box : regions) {
        //~     for (int sx = box[0] >> 4; sx <= box[3] >> 4; sx++) {
        //~         for (int sy = box[1] >> 4; sy <= box[4] >> 4; sy++) {
        //~             for (int sz = box[2] >> 4; sz <= box[5] >> 4; sz++) {
        //~                 minecraft.level.setSectionDirtyWithNeighbors(sx, sy, sz);
        //~             }
        //~         }
        //~     }
        //~ }
        // @era:dirty-end
    }

    /** Light level emitted by an item stack, 0 when it emits none. */
    public static int luminance(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        final Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) {
            return 15;
        }
        if (item instanceof BlockItem) {
            return ((BlockItem) item).getBlock().defaultBlockState().getLightEmission();
        }
        return 0;
    }

    private static final class Candidate {
        final Entity entity;
        final int level;
        final double distanceSq;

        Candidate(final Entity entity, final int level, final double distanceSq) {
            this.entity = entity;
            this.level = level;
            this.distanceSq = distanceSq;
        }
    }
}
