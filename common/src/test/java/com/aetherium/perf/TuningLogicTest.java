package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.config.AetheriumConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.10.0 tuning options: the config snapshot, crowd culling, hitch guard and explosion budget. */
final class TuningLogicTest {

    @Test
    @DisplayName("tuning: inactive means exactly vanilla behaviour")
    void inactiveIsVanilla() {
        final AetheriumConfig c = new AetheriumConfig();
        c.lowEndMode.set(Boolean.TRUE);
        c.enchantGlint.set(Boolean.FALSE);
        c.endCrystalBeams.set(Boolean.FALSE);
        Tuning.configure(c, false);
        assertFalse(Tuning.on);
        assertFalse(Tuning.lowEnd);
        assertFalse(Tuning.dynamicFps);
        assertEquals(0, Tuning.menuFps);
        assertEquals(Double.MAX_VALUE, Tuning.detailDistanceSq, 0.0);
        assertFalse(Tuning.beyondDetail(1.0e12), "no distance is beyond the detail distance when off");
        assertTrue(Tuning.glint, "glint stays on when Aetherium is off");
        assertTrue(Tuning.crystalBeams, "beams stay on when Aetherium is off");
        assertEquals(Tuning.EXPLOSIONS_UNLIMITED, Tuning.explosionEmitters);
        assertEquals(0, Tuning.leaves, "leaves follow the Graphics setting");
        assertFalse(Tuning.crowdCull || Tuning.lightUpdates || Tuning.mapThrottle || Tuning.asyncSkulls
                || Tuning.smoothAutosave || Tuning.flightPreload || Tuning.dimensionCulling
                || Tuning.leafCulling || Tuning.lightWeather || Tuning.hitchGuard || Tuning.loadGc);
    }

    @Test
    @DisplayName("tuning: defaults when active")
    void defaults() {
        final AetheriumConfig c = new AetheriumConfig();
        Tuning.configure(c, true);
        assertTrue(Tuning.on);
        assertFalse(Tuning.lowEnd);
        assertTrue(Tuning.dynamicFps);
        assertEquals(30, Tuning.menuFps);
        assertEquals(32.0 * 32.0, Tuning.detailDistanceSq, 0.0);
        assertTrue(Tuning.beyondDetail(33.0 * 33.0));
        assertFalse(Tuning.beyondDetail(31.0 * 31.0));
        assertEquals(4, Tuning.explosionEmitters);
        assertEquals(0, Tuning.leaves);
        assertTrue(Tuning.crowdCull && Tuning.lightUpdates && Tuning.mapThrottle && Tuning.asyncSkulls
                && Tuning.smoothAutosave && Tuning.flightPreload && Tuning.dimensionCulling
                && Tuning.leafCulling && Tuning.hitchGuard && Tuning.loadGc && Tuning.glint && Tuning.crystalBeams);
        assertFalse(Tuning.lightWeather);
    }

    @Test
    @DisplayName("tuning: low-end mode tightens limits without touching the stored values")
    void lowEndMode() {
        final AetheriumConfig c = new AetheriumConfig();
        c.lowEndMode.set(Boolean.TRUE);
        c.menuFps.set(Integer.valueOf(Tuning.MENU_FPS_OFF));
        c.entityDetailDistance.set(Integer.valueOf(Tuning.VANILLA_DETAIL));
        c.explosionParticles.set(Integer.valueOf(Tuning.EXPLOSIONS_UNLIMITED));
        c.leaves.set(AetheriumConfig.LeavesMode.FANCY);
        Tuning.configure(c, true);
        assertTrue(Tuning.lowEnd);
        assertEquals(20, Tuning.menuFps);
        assertEquals(16.0 * 16.0, Tuning.detailDistanceSq, 0.0);
        assertEquals(2, Tuning.explosionEmitters);
        assertEquals(1, Tuning.leaves, "low-end forces fast leaves");
        assertTrue(Tuning.lightWeather);
        assertEquals(Integer.valueOf(Tuning.MENU_FPS_OFF), c.menuFps.get(), "stored value untouched");
        c.lowEndMode.set(Boolean.FALSE);
        Tuning.configure(c, true);
        assertEquals(0, Tuning.menuFps, "120 = no menu cap");
        assertEquals(Double.MAX_VALUE, Tuning.detailDistanceSq, 0.0, "64 = vanilla details");
        assertEquals(Tuning.EXPLOSIONS_UNLIMITED, Tuning.explosionEmitters);
        assertEquals(2, Tuning.leaves);
    }

    @Test
    @DisplayName("crowd culling: per-cell caps, items and mobs counted apart, reset every frame")
    void crowdCull() {
        CrowdCull.newFrame();
        for (int i = 0; i < CrowdCull.ITEM_CAP; i++) {
            assertFalse(CrowdCull.crowded(10.2, 64.0, -5.7, false), "item " + i);
        }
        assertTrue(CrowdCull.crowded(10.9, 64.5, -5.1, false), "same block, one over the item cap");
        for (int i = 0; i < CrowdCull.MOB_CAP; i++) {
            assertFalse(CrowdCull.crowded(10.5, 64.0, -5.5, true), "mobs are counted separately (" + i + ")");
        }
        assertTrue(CrowdCull.crowded(10.5, 64.0, -5.5, true));
        assertFalse(CrowdCull.crowded(11.5, 64.0, -5.5, false), "neighbouring block has its own count");
        CrowdCull.newFrame();
        assertFalse(CrowdCull.crowded(10.2, 64.0, -5.7, false), "counts reset every frame");
    }

    @Test
    @DisplayName("hitch guard: a long frame starts a 20-frame recovery with a small light quota")
    void hitchGuard() {
        HitchGuard.resetForTests();
        final double avg = 16_000_000.0;
        long t = 1_000_000_000L;
        assertFalse(HitchGuard.frame(t, avg));
        t += 16_000_000L;
        assertFalse(HitchGuard.frame(t, avg));
        assertEquals(16384, HitchGuard.lightQuota());
        t += 30_000_000L; // 2x average but under 40 ms: not a hitch
        assertFalse(HitchGuard.frame(t, avg));
        t += 80_000_000L;
        assertTrue(HitchGuard.frame(t, avg));
        assertTrue(HitchGuard.recovering());
        assertEquals(1024, HitchGuard.lightQuota());
        for (int i = 0; i < HitchGuard.RECOVERY_FRAMES; i++) {
            t += 16_000_000L;
            HitchGuard.frame(t, avg);
        }
        assertFalse(HitchGuard.recovering());
        t += 10_000_000_000L; // the game was paused / suspended: not a hitch
        assertFalse(HitchGuard.frame(t, avg));
        HitchGuard.resetForTests();
    }

    @Test
    @DisplayName("explosion budget: N big clouds per tick, the budget resets on the next tick")
    void explosionBudget() {
        final long tick = 123_456L;
        for (int i = 0; i < 4; i++) {
            assertTrue(ExplosionBudget.allowEmitter(tick, 4));
        }
        assertFalse(ExplosionBudget.allowEmitter(tick, 4));
        assertTrue(ExplosionBudget.allowEmitter(tick + 1, 4));
        assertFalse(ExplosionBudget.allowEmitter(tick + 2, 0), "0 = small puffs only");
        assertTrue(ExplosionBudget.allowEmitter(tick + 2, Tuning.EXPLOSIONS_UNLIMITED));
    }

    @Test
    @DisplayName("lazy data fixers: the startup executor never runs its tasks")
    void noOpExecutor() {
        final boolean[] ran = {false};
        NoOpExecutor.INSTANCE.execute(new Runnable() {
            @Override
            public void run() {
                ran[0] = true;
            }
        });
        assertFalse(ran[0]);
    }
}
