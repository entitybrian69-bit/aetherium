package com.aetherium.perf;

import com.aetherium.config.AetheriumConfig;

/**
 * Live snapshot of the 1.10.0 tuning options, read by the hooks with one volatile load each.
 *
 * <p>Minecraft-free on purpose: the hooks live in mixins (Minecraft 1.16.5 only, see
 * {@code Capabilities.TUNING}) and only read these fields; {@link #configure} is called from
 * {@code ClientHooks.onSettingsApplied}. When Aetherium is off (master switch) every flag falls
 * back to vanilla behaviour.</p>
 *
 * <p>Low-end mode is one switch that tightens every limit at once without touching the user's own
 * values, so turning it off restores exactly what was set before.</p>
 */
public final class Tuning {
    /** Distance beyond which entity details are dropped, as "vanilla": no limit. */
    public static final int VANILLA_DETAIL = 64;
    /** Explosion slider value that means "unlimited" (vanilla). */
    public static final int EXPLOSIONS_UNLIMITED = 32;
    /** Menu frame-rate slider value that means "no cap". */
    public static final int MENU_FPS_OFF = 120;

    public static volatile boolean on;
    public static volatile boolean lowEnd;

    // frame rate
    public static volatile boolean dynamicFps;
    public static volatile int menuFps;
    public static volatile boolean hitchGuard;
    public static volatile boolean loadGc;

    // entities
    /** Squared distance beyond which shadows, armour, held items, frame items and minecart blocks are skipped; MAX = off. */
    public static volatile double detailDistanceSq = Double.MAX_VALUE;
    public static volatile boolean crowdCull;
    public static volatile boolean glint = true;
    public static volatile boolean crystalBeams = true;

    // world
    public static volatile int explosionEmitters = EXPLOSIONS_UNLIMITED;
    public static volatile boolean lightWeather;
    /** 0 = follow Graphics, 1 = fast, 2 = fancy. */
    public static volatile int leaves;
    public static volatile boolean leafCulling;
    /** Whether leaves are currently meshed see-through (set where vanilla decides it, setFancy). */
    public static volatile boolean leavesFancy = true;
    public static volatile boolean dimensionCulling;
    public static volatile boolean lightUpdates;
    public static volatile boolean mapThrottle;
    public static volatile boolean asyncSkulls;

    // integrated server
    public static volatile boolean smoothAutosave;
    public static volatile boolean flightPreload;

    /** Set by the server thread while vanilla's periodic autosave runs (MinecraftServer.tickServer). */
    public static volatile boolean autosaving;

    private Tuning() {
    }

    public static void configure(final AetheriumConfig c, final boolean active) {
        final boolean low = active && c.lowEndMode.get().booleanValue();
        lowEnd = low;
        dynamicFps = active && c.dynamicFps.get().booleanValue();
        int menu = c.menuFps.get().intValue();
        if (low) {
            menu = Math.min(menu, 20);
        }
        menuFps = active && menu < MENU_FPS_OFF ? menu : 0;
        hitchGuard = active && c.hitchGuard.get().booleanValue();
        loadGc = active && c.loadGc.get().booleanValue();

        int detail = c.entityDetailDistance.get().intValue();
        if (low) {
            detail = Math.min(detail, 16);
        }
        detailDistanceSq = active && detail < VANILLA_DETAIL ? (double) detail * detail : Double.MAX_VALUE;
        crowdCull = active && c.crowdCulling.get().booleanValue();
        glint = !active || c.enchantGlint.get().booleanValue();
        crystalBeams = !active || c.endCrystalBeams.get().booleanValue();

        int explosions = c.explosionParticles.get().intValue();
        if (low) {
            explosions = Math.min(explosions, 2);
        }
        explosionEmitters = active ? explosions : EXPLOSIONS_UNLIMITED;
        lightWeather = active && (low || c.lightWeather.get().booleanValue());
        final AetheriumConfig.LeavesMode mode = c.leaves.get();
        leaves = !active ? 0 : low ? 1 : mode == AetheriumConfig.LeavesMode.FAST ? 1
                : mode == AetheriumConfig.LeavesMode.FANCY ? 2 : 0;
        leafCulling = active && c.leafCulling.get().booleanValue();
        dimensionCulling = active && c.dimensionCulling.get().booleanValue();
        lightUpdates = active && c.lightUpdates.get().booleanValue();
        mapThrottle = active && c.mapUpdates.get().booleanValue();
        asyncSkulls = active && c.asyncSkulls.get().booleanValue();
        smoothAutosave = active && c.smoothAutosave.get().booleanValue();
        flightPreload = active && c.flightPreload.get().booleanValue();
        on = active;
    }

    /** True when an entity at this squared distance from the camera should lose its details. */
    public static boolean beyondDetail(final double distanceSq) {
        return distanceSq > detailDistanceSq;
    }
}
