package com.aetherium.lighting;

import com.aetherium.config.AetheriumConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Finds the light sources around the player every few ticks (held torches, dropped glowstone,
 * burning mobs, blazes), publishes them to {@link LightField} and marks the blocks whose light
 * changed for a re-mesh. Minecraft 1.12.2 names; the logic matches the main mod.
 */
public final class DynamicLightTracker {

    private static final LightField.Source[] EMPTY = new LightField.Source[0];

    private final AetheriumConfig config;
    private LightField.Source[] last = EMPTY;
    private int countdown;

    public DynamicLightTracker(final AetheriumConfig config) {
        this.config = config;
    }

    public void clear(final Minecraft minecraft) {
        if (this.last.length > 0 && minecraft != null && minecraft.renderGlobal != null && minecraft.world != null) {
            markDirty(minecraft, LightDiff.changedRegions(this.last, EMPTY));
        }
        this.last = EMPTY;
        LightField.clear();
    }

    public void reset() {
        this.last = EMPTY;
        LightField.clear();
    }

    public int activeSources() {
        return this.last.length;
    }

    public void tick(final Minecraft minecraft, final boolean allowed) {
        final AetheriumConfig.LightMode mode = this.config.dynamicLights.get();
        if (!allowed || mode == AetheriumConfig.LightMode.OFF || minecraft.world == null || minecraft.player == null) {
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
        final double vx = viewer.posX;
        final double vy = viewer.posY;
        final double vz = viewer.posZ;
        final double rangeSq = range * range;
        final List<Candidate> found = new ArrayList<Candidate>();
        final List<Entity> all = minecraft.world.loadedEntityList;
        for (int i = 0, n = all.size(); i < n; i++) {
            final Entity entity = all.get(i);
            if (entity == null || entity.isDead) {
                continue;
            }
            final double distanceSq = entity.getDistanceSq(vx, vy, vz);
            if (distanceSq > rangeSq) {
                continue;
            }
            int level = 0;
            if (held && entity instanceof EntityLivingBase) {
                final EntityLivingBase living = (EntityLivingBase) entity;
                level = Math.max(luminance(living.getHeldItemMainhand()), luminance(living.getHeldItemOffhand()));
            }
            if (entities) {
                if (entity instanceof EntityItem) {
                    level = Math.max(level, luminance(((EntityItem) entity).getItem()));
                } else if (entity instanceof EntityBlaze) {
                    level = Math.max(level, 10);
                }
                if (entity.isBurning()) {
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
            // Block-centre snapping keeps the light still while the source moves inside a block.
            final double x = Math.floor(entity.posX) + 0.5;
            final double y = Math.floor(entity.posY + entity.getEyeHeight()) + 0.5;
            final double z = Math.floor(entity.posZ) + 0.5;
            out[i] = new LightField.Source(x, y, z, candidate.level);
        }
        return Arrays.copyOf(out, count);
    }

    private static void markDirty(final Minecraft minecraft, final List<int[]> regions) {
        if (minecraft.renderGlobal == null) {
            return;
        }
        for (final int[] box : regions) {
            minecraft.renderGlobal.markBlockRangeForRenderUpdate(box[0], box[1], box[2], box[3], box[4], box[5]);
        }
    }

    @SuppressWarnings("deprecation")
    public static int luminance(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        final Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) {
            return 15;
        }
        if (item instanceof ItemBlock) {
            return ((ItemBlock) item).getBlock().getDefaultState().getLightValue();
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
