package com.aetherium.qol;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-type particle toggles. Particle types are sorted into categories by their registry name;
 * the Minecraft glue resolves each type's name once and caches the category by identity.
 */
public final class ParticleFilter {
    public static final int NONE = -1;
    public static final int EXPLOSIONS = 0;
    public static final int WEATHER = 1;
    public static final int SMOKE = 2;
    public static final int POTIONS = 3;
    public static final int COMBAT = 4;
    public static final int FIRE = 5;
    public static final int DRIPS = 6;
    public static final int PORTALS = 7;
    public static final int AMBIENT = 8;
    public static final int TOTEM = 9;
    public static final int COUNT = 10;

    private static final Map<String, Integer> BY_NAME = new HashMap<String, Integer>();
    private static final boolean[] ENABLED = new boolean[COUNT];
    private static volatile boolean anyDisabled;
    private static volatile boolean blockBreaking = true;

    static {
        java.util.Arrays.fill(ENABLED, true);
        put(EXPLOSIONS, "explosion", "explosion_emitter", "firework", "flash");
        put(WEATHER, "rain", "splash");
        put(SMOKE, "smoke", "large_smoke", "campfire_cosy_smoke", "campfire_signal_smoke", "cloud", "poof", "sneeze");
        put(POTIONS, "effect", "entity_effect", "ambient_entity_effect", "instant_effect", "witch", "dragon_breath");
        put(COMBAT, "crit", "enchanted_hit", "sweep_attack", "damage_indicator");
        put(FIRE, "flame", "soul_fire_flame", "lava", "soul");
        put(DRIPS, "dripping_water", "falling_water", "dripping_lava", "falling_lava", "landing_lava",
                "dripping_honey", "falling_honey", "landing_honey", "falling_nectar", "dripping_obsidian_tear",
                "falling_obsidian_tear", "landing_obsidian_tear", "bubble", "bubble_pop", "bubble_column_up",
                "current_down", "dripping_dripstone_water", "falling_dripstone_water", "dripping_dripstone_lava",
                "falling_dripstone_lava", "falling_spore_blossom");
        put(PORTALS, "portal", "reverse_portal");
        put(AMBIENT, "ash", "white_ash", "crimson_spore", "warped_spore", "mycelium", "underwater", "spore_blossom_air");
        put(TOTEM, "totem_of_undying");
    }

    private ParticleFilter() {
    }

    private static void put(final int category, final String... names) {
        for (final String name : names) {
            BY_NAME.put(name, Integer.valueOf(category));
        }
    }

    /** Category of a particle type by its registry name ("minecraft:flame" or "flame"); NONE if not sorted. */
    public static int categoryOf(final String id) {
        if (id == null) {
            return NONE;
        }
        final String path = id.startsWith("minecraft:") ? id.substring(10) : id.indexOf(':') >= 0 ? null : id;
        if (path == null) {
            return NONE;
        }
        final Integer category = BY_NAME.get(path);
        return category == null ? NONE : category.intValue();
    }

    public static void set(final int category, final boolean on) {
        synchronized (ENABLED) {
            ENABLED[category] = on;
            boolean any = false;
            for (final boolean e : ENABLED) {
                any |= !e;
            }
            anyDisabled = any;
        }
    }

    /** False while every category is on: the hook then skips the lookup entirely. */
    public static boolean anyDisabled() {
        return anyDisabled;
    }

    public static boolean allowed(final int category) {
        return category < 0 || ENABLED[category];
    }

    public static void setBlockBreaking(final boolean on) {
        blockBreaking = on;
    }

    public static boolean blockBreaking() {
        return blockBreaking;
    }
}
