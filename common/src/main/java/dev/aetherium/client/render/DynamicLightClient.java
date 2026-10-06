package dev.aetherium.client.render;

import dev.aetherium.light.DynamicLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Feeds entity/item positions into the engine each frame and schedules the section rebuilds it asks for. */
public final class DynamicLightClient {
    private DynamicLightClient() {}

    public static void tick(ClientLevel level, double camX, double camY, double camZ) {
        DynamicLightEngine engine = DynamicLightEngine.get();
        if (!engine.isEnabled()) return;
        long now = System.nanoTime();
        if (engine.shouldUpdate(now)) {
            Minecraft mc = Minecraft.getInstance();
            for (Entity e : level.entitiesForRendering()) {
                if (e.distanceToSqr(camX, camY, camZ) > 64 * 64) { engine.remove(e.getId()); continue; }
                if (e == mc.player && !engine.lightsSelf()) { engine.remove(e.getId()); continue; }
                int lum = luminance(e);
                if (lum <= 0) { engine.remove(e.getId()); continue; }
                engine.update(e.getId(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), lum, color(e));
            }
        }
        engine.flushDirtySections(key -> mc().levelRenderer.setSectionDirty(
                DynamicLightEngine.sectionX(key), DynamicLightEngine.sectionY(key), DynamicLightEngine.sectionZ(key)));
    }

    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static int luminance(Entity e) {
        DynamicLightEngine engine = DynamicLightEngine.get();
        if (e.isOnFire()) return 15;
        if (e instanceof ItemEntity item) return engine.lightsItems() ? stackLuminance(item.getItem()) : 0;
        if (e instanceof LivingEntity living) {
            if (!engine.lightsEntities() && !(e instanceof Player)) return 0;
            int l = Math.max(stackLuminance(living.getMainHandItem()), stackLuminance(living.getOffhandItem()));
            if (living.getType().toString().contains("blaze") || living.getType().toString().contains("magma_cube")) l = Math.max(l, 10);
            if (living.getType().toString().contains("glow_squid")) l = Math.max(l, 12);
            return l;
        }
        if (e instanceof Projectile) {
            String t = e.getType().toString();
            if (t.contains("fireball") || t.contains("blaze")) return 14;
        }
        return 0;
    }

    public static int stackLuminance(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(Items.TORCH)) return 14;
        if (stack.is(Items.SOUL_TORCH)) return 10;
        if (stack.is(Items.REDSTONE_TORCH)) return 7;
        if (stack.is(Items.LAVA_BUCKET)) return 15;
        if (stack.is(Items.GLOW_INK_SAC) || stack.is(Items.GLOW_BERRIES)) return 8;
        if (stack.is(Items.NETHER_STAR)) return 12;
        if (stack.getItem() instanceof BlockItem bi) {
            BlockState s = bi.getBlock().defaultBlockState();
            int l = s.getLightEmission();
            if (l == 0 && bi.getBlock() == Blocks.SEA_LANTERN) l = 15;
            return l;
        }
        return 0;
    }

    public static int color(Entity e) {
        if (e.isOnFire()) return 0xFF9A3C;
        ItemStack held = e instanceof LivingEntity le ? (stackLuminance(le.getMainHandItem()) >= stackLuminance(le.getOffhandItem()) ? le.getMainHandItem() : le.getOffhandItem())
                : e instanceof ItemEntity ie ? ie.getItem() : ItemStack.EMPTY;
        if (held.is(Items.SOUL_TORCH) || held.is(Items.SOUL_LANTERN)) return 0x5FD6FF;
        if (held.is(Items.REDSTONE_TORCH)) return 0xFF3B3B;
        if (held.is(Items.LAVA_BUCKET) || held.is(Items.MAGMA_BLOCK)) return 0xFF7A1F;
        if (held.is(Items.GLOW_INK_SAC) || held.is(Items.GLOW_BERRIES)) return 0x9CFFD0;
        if (held.is(Items.SEA_LANTERN)) return 0xBFFFF2;
        if (held.is(Items.END_ROD)) return 0xF4E8FF;
        if (held.is(Items.SHROOMLIGHT)) return 0xFFB366;
        if (held.getItem() instanceof BlockItem bi) {
            Block b = bi.getBlock();
            if (b == Blocks.AMETHYST_CLUSTER) return 0xC08BFF;
            if (b == Blocks.CRYING_OBSIDIAN) return 0xA060FF;
        }
        return 0xFFE2B0; // warm torch default
    }

    /** Combine dynamic light into a packed light value (block nibble in bits 4..7). */
    public static int combinePackedLight(int packed, BlockPos pos) {
        DynamicLightEngine engine = DynamicLightEngine.get();
        if (!engine.isEnabled()) return packed;
        double dyn = engine.sampleLevel(pos.getX(), pos.getY(), pos.getZ());
        if (dyn <= 0) return packed;
        int block = (packed >> 4) & 0xF;
        int newBlock = (int) Math.ceil(dyn);
        if (newBlock <= block) return packed;
        return (packed & ~0xF0) | (Math.min(15, newBlock) << 4);
    }
}
