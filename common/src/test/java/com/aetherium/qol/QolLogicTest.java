package com.aetherium.qol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.5.0 fog/sky settings, particle categories and freelook. */
final class QolLogicTest {

    private static void vanillaScenery() {
        Scenery.distanceFog = true;
        Scenery.fogStartPercent = Scenery.VANILLA_FOG_START;
        Scenery.waterFogPercent = 100;
        Scenery.clearLava = false;
        Scenery.cloudHeight = 0;
    }

    @Test
    @DisplayName("fog: vanilla settings change nothing; each option changes only its own medium")
    void fogControls() {
        vanillaScenery();
        final float[] out = new float[3];
        assertTrue(Scenery.fogVanilla());
        assertFalse(Scenery.adjustFog(Scenery.AIR, false, 80f, 60f, 80f, 0.1f, out));
        assertEquals(60f, out[Scenery.START], 0f);

        Scenery.fogStartPercent = 90;
        assertTrue(Scenery.adjustFog(Scenery.AIR, false, 80f, 60f, 80f, 0.1f, out));
        assertEquals(72f, out[Scenery.START], 1e-4f);
        assertEquals(80f, out[Scenery.END], 0f);
        Scenery.fogStartPercent = 100;
        Scenery.adjustFog(Scenery.AIR, false, 80f, 60f, 80f, 0.1f, out);
        assertTrue(out[Scenery.START] < out[Scenery.END], "start stays below end");
        assertFalse(Scenery.adjustFog(Scenery.AIR, true, 80f, 4f, 40f, 0.1f, out), "Nether fog keeps its shape");
        assertFalse(Scenery.adjustFog(Scenery.WATER, false, 80f, 0f, 0f, 0.05f, out), "start % is air only");

        Scenery.distanceFog = false;
        assertTrue(Scenery.adjustFog(Scenery.AIR, true, 80f, 4f, 40f, 0.1f, out));
        assertTrue(out[Scenery.START] > 80f && out[Scenery.END] > out[Scenery.START], "no fog anywhere in view");

        vanillaScenery();
        Scenery.waterFogPercent = 40;
        assertTrue(Scenery.adjustFog(Scenery.WATER, false, 80f, 0f, 0f, 0.05f, out));
        assertEquals(0.02f, out[Scenery.DENSITY], 1e-6f);
        assertFalse(Scenery.adjustFog(Scenery.LAVA, false, 80f, 0.25f, 1f, 0.1f, out), "clear lava is separate");

        vanillaScenery();
        Scenery.clearLava = true;
        assertTrue(Scenery.adjustFog(Scenery.LAVA, false, 80f, 0.25f, 1f, 0.1f, out));
        assertEquals(0f, out[Scenery.START], 0f);
        assertEquals(Scenery.CLEAR_LAVA_END, out[Scenery.END], 0f);
        assertTrue(Scenery.adjustFog(Scenery.LAVA, false, 4f, 0.25f, 1f, 0.1f, out));
        assertEquals(4f, out[Scenery.END], 0f, "never beyond the render distance");
        vanillaScenery();
    }

    @Test
    @DisplayName("cloud height: 0 keeps the dimension's height; dimensions without clouds stay without")
    void cloudHeight() {
        vanillaScenery();
        assertEquals(128f, Scenery.cloudHeight(128f), 0f);
        Scenery.cloudHeight = 192;
        assertEquals(192f, Scenery.cloudHeight(128f), 0f);
        assertTrue(Float.isNaN(Scenery.cloudHeight(Float.NaN)));
        vanillaScenery();
    }

    @Test
    @DisplayName("particles: registry names map to categories; toggles are tracked")
    void particleCategories() {
        assertEquals(ParticleFilter.EXPLOSIONS, ParticleFilter.categoryOf("minecraft:explosion"));
        assertEquals(ParticleFilter.FIRE, ParticleFilter.categoryOf("minecraft:flame"));
        assertEquals(ParticleFilter.WEATHER, ParticleFilter.categoryOf("rain"));
        assertEquals(ParticleFilter.TOTEM, ParticleFilter.categoryOf("minecraft:totem_of_undying"));
        assertEquals(ParticleFilter.AMBIENT, ParticleFilter.categoryOf("minecraft:white_ash"));
        assertEquals(ParticleFilter.DRIPS, ParticleFilter.categoryOf("minecraft:dripping_obsidian_tear"));
        assertEquals(ParticleFilter.NONE, ParticleFilter.categoryOf("minecraft:block"), "block dust has its own switch");
        assertEquals(ParticleFilter.NONE, ParticleFilter.categoryOf("othermod:flame"), "other namespaces are left alone");
        assertEquals(ParticleFilter.NONE, ParticleFilter.categoryOf(null));

        for (int c = 0; c < ParticleFilter.COUNT; c++) {
            ParticleFilter.set(c, true);
        }
        assertFalse(ParticleFilter.anyDisabled());
        ParticleFilter.set(ParticleFilter.SMOKE, false);
        assertTrue(ParticleFilter.anyDisabled());
        assertFalse(ParticleFilter.allowed(ParticleFilter.SMOKE));
        assertTrue(ParticleFilter.allowed(ParticleFilter.FIRE));
        assertTrue(ParticleFilter.allowed(ParticleFilter.NONE));
        ParticleFilter.set(ParticleFilter.SMOKE, true);
        assertFalse(ParticleFilter.anyDisabled());
    }

    @Test
    @DisplayName("freelook: hold and toggle modes, free camera turns like vanilla and clamps pitch")
    void freelook() {
        final Freelook hold = new Freelook();
        assertEquals(Freelook.NO_CHANGE, hold.update(false, false));
        assertEquals(Freelook.STARTED, hold.update(true, false));
        assertEquals(Freelook.NO_CHANGE, hold.update(true, false));
        assertEquals(Freelook.STOPPED, hold.update(false, false));

        final Freelook toggle = new Freelook();
        assertEquals(Freelook.STARTED, toggle.update(true, true));
        assertEquals(Freelook.NO_CHANGE, toggle.update(true, true), "holding does not toggle again");
        assertEquals(Freelook.NO_CHANGE, toggle.update(false, true));
        assertTrue(toggle.active());
        assertEquals(Freelook.STOPPED, toggle.update(true, true));
        assertFalse(toggle.active());

        final Freelook cam = new Freelook();
        cam.begin(90f, 10f);
        cam.turn(100.0, 0.0);
        assertEquals(105f, cam.yaw(), 1e-4f);
        cam.turn(0.0, 1000.0);
        assertEquals(90f, cam.pitch(), 0f);
        cam.turn(0.0, -2000.0);
        assertEquals(-90f, cam.pitch(), 0f);
    }
}
