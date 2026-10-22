package com.aetherium.perf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Which tuning options (Performance and Quality rows) work on Minecraft 1.12.2 (Aetherium
 * Backport's own copy of the main mod's per-version table). Every other tuning row is shown
 * greyed out as "Not available on this version": its hook is a 1.13+ system that 1.12.2 does not
 * have (the 1.14 light engine, the 1.15 chunk builder, 1.16 block models...), or vanilla 1.12.2
 * already behaves that way.
 */
public final class TuningSupport {
    private static final Set<String> OPTIONS = new HashSet<String>(Arrays.asList(
            "perf.low_end_mode",
            "perf.dynamic_fps",
            "perf.menu_fps",
            "perf.hitch_guard",
            "perf.entity_detail_distance",
            "perf.explosion_particles",
            "perf.map_updates",
            "perf.redstone_particle_limit",
            "perf.block_entity_limit",
            "quality.light_weather",
            "quality.leaves",
            "quality.leaf_culling",
            "quality.enchant_glint",
            "quality.end_crystal_beams"));

    private TuningSupport() {
    }

    /** The option keys that work here. */
    public static String[] optionKeys() {
        return OPTIONS.toArray(new String[0]);
    }

    /** Whether the tuning row {@code key} works on 1.12.2. */
    public static boolean option(final String key) {
        return OPTIONS.contains(key);
    }
}
