package com.aetherium.qol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScreenProjectorTest {
    private static final double W = 480;
    private static final double H = 270;

    @Test
    @DisplayName("Straight ahead lands in the middle; Minecraft's yaw and pitch conventions")
    void directions() {
        final ScreenProjector p = new ScreenProjector();
        p.setCamera(0, 64, 0, 0, 0, 70, W, H); // yaw 0 looks south (+Z)
        assertTrue(p.project(0, 64, 10));
        assertEquals(W / 2, p.x, 1e-9);
        assertEquals(H / 2, p.y, 1e-9);
        assertEquals(10, p.depth, 1e-9);
        assertTrue(p.project(-3, 64, 10), "facing south, west (-X) is on the right");
        assertTrue(p.x > W / 2);
        assertTrue(p.project(0, 70, 10));
        assertTrue(p.y < H / 2, "higher is further up the screen");
        assertFalse(p.project(0, 64, -10), "behind the camera");

        p.setCamera(0, 64, 0, 90, 0, 70, W, H); // yaw 90 looks west (-X)
        assertTrue(p.project(-10, 64, 0));
        assertEquals(W / 2, p.x, 1e-9);
        assertTrue(p.project(-10, 64, -3), "facing west, north (-Z) is on the right");
        assertTrue(p.x > W / 2);

        p.setCamera(0, 64, 0, 0, 90, 70, W, H); // looking straight down
        assertTrue(p.project(0, 54, 0));
        assertEquals(W / 2, p.x, 1e-6);
        assertEquals(H / 2, p.y, 1e-6);
        assertTrue(p.project(0, 54, 3), "looking down with yaw 0, south is the top of the screen");
        assertTrue(p.y < H / 2);
    }

    @Test
    @DisplayName("The vertical field of view reaches exactly the top edge")
    void fieldOfView() {
        final ScreenProjector p = new ScreenProjector();
        p.setCamera(0, 0, 0, 0, 0, 90, W, H);
        assertTrue(p.project(0, 10, 10)); // 45 degrees up = half of a 90 degree FOV
        assertEquals(0, p.y, 1e-9);
        assertTrue(p.onScreen(0));
        assertTrue(p.project(0, 20, 10));
        assertFalse(p.onScreen(4));
    }

    @Test
    @DisplayName("A beam through the camera plane is clipped to the part in front")
    void segment() {
        final ScreenProjector p = new ScreenProjector();
        p.setCamera(0, 64, 0, 0, 0, 70, W, H);
        final double[] out = new double[6];
        assertTrue(p.segment(0, 0, 10, 0, 320, 10, out));
        assertEquals(W / 2, out[0], 1e-9);
        assertEquals(W / 2, out[2], 1e-9);
        assertTrue(out[1] > out[3], "the beam bottom is below its top");
        assertEquals(0, out[4], 1e-12);
        assertEquals(1, out[5], 1e-12);
        assertTrue(p.segment(0, 64, -10, 0, 64, 10, out), "half behind the camera");
        assertEquals(0.5 + ScreenProjector.NEAR / 20, out[4], 1e-9);
        assertFalse(p.segment(0, 0, -10, 0, 320, -10, out), "entirely behind");
    }
}
