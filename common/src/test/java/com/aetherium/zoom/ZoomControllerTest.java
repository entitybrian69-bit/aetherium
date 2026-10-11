package com.aetherium.zoom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.zoom.ZoomController.View;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Zume-style zoom: curves, animation, keys, scroll, sensitivity, cinematic camera, third person. */
final class ZoomControllerTest {

    private static ZoomController controller() {
        final ZoomController c = new ZoomController();
        c.configure(new ZoomController.Settings());
        return c;
    }

    @Test
    @DisplayName("zoom: idle changes nothing; holding Zoom eases to ~18 degrees from 70 (OptiFine strength) in 150 ms")
    void holdToZoom() {
        final ZoomController c = controller();
        c.frame(1000, false, false, false, View.FIRST_PERSON);
        assertEquals(70.0, c.fov(70.0, 1000), 0.0);
        assertEquals(1.0, c.sensitivity(1.0, 1000), 0.0);
        assertFalse(c.smoothCamera(false));

        c.frame(2000, true, false, false, View.FIRST_PERSON);
        assertEquals(70.0, c.fov(70.0, 2000), 1e-9, "animation starts at no zoom");
        final double mid = c.fov(70.0, 2075);
        assertTrue(mid < 70.0 && mid > 18.25, "halfway through the ease: " + mid);
        assertEquals(1.0 + 69.0 * 0.25, c.fov(70.0, 2150), 1e-9, "1 + (70 - 1) * 0.5^2");
        assertEquals(1.0 + 69.0 * 0.25, c.fov(70.0, 5000), 1e-9, "settled");
        assertTrue(c.smoothCamera(false), "cinematic camera while zoomed");
        assertEquals(0.4 + 0.6 * 0.5, c.sensitivity(1.0, 5000), 1e-9, "floor + (1 - floor) * z");

        c.frame(6000, false, false, false, View.FIRST_PERSON);
        assertTrue(c.isVisible(6100), "eases back out");
        assertEquals(70.0, c.fov(70.0, 6150), 1e-9);
        assertFalse(c.isVisible(6150));
        assertFalse(c.smoothCamera(false));
    }

    @Test
    @DisplayName("zoom: the ease-out is monotonic and front-loaded (quartic)")
    void easing() {
        final ZoomController c = controller();
        c.frame(0, true, false, false, View.FIRST_PERSON);
        double last = 2.0;
        for (long t = 0; t <= 150; t += 5) {
            final double a = c.amount(t);
            assertTrue(a <= last + 1e-12, "monotonic at " + t);
            last = a;
        }
        assertTrue(c.amount(37) < 0.5 + 0.5 * 0.5, "a quarter of the time covers more than half the way");
    }

    @Test
    @DisplayName("zoom: scroll moves the target 0.08 per notch and is consumed only while zooming")
    void scrolling() {
        final ZoomController c = controller();
        assertFalse(c.scroll(1.0), "not zooming: the hotbar keeps the wheel");
        c.frame(0, true, false, false, View.FIRST_PERSON);
        assertTrue(c.scroll(1.0));
        assertTrue(c.scroll(3.0), "sign only, like one notch");
        c.frame(16, true, false, false, View.FIRST_PERSON);
        assertEquals(0.5 - 2 * 0.08, c.targetAmount(), 1e-9);
        assertTrue(c.scroll(-1.0));
        c.frame(32, true, false, false, View.FIRST_PERSON);
        assertEquals(0.5 - 0.08, c.targetAmount(), 1e-9);
        for (int i = 0; i < 50; i++) {
            c.scroll(1.0);
            c.frame(48 + i, true, false, false, View.FIRST_PERSON);
        }
        assertEquals(0.0, c.targetAmount(), 0.0, "clamped at full zoom");
        assertEquals(1.0, c.fov(70.0, 100_000), 1e-9, "full zoom = minimum FOV");

        final ZoomController.Settings off = new ZoomController.Settings();
        off.scrolling = false;
        c.configure(off);
        assertFalse(c.scroll(1.0));
    }

    @Test
    @DisplayName("zoom: Zoom In / Zoom Out keys change the target by speed * 0.0001 per millisecond")
    void keys() {
        final ZoomController c = controller();
        c.frame(0, true, false, false, View.FIRST_PERSON);
        c.frame(100, true, true, false, View.FIRST_PERSON);
        assertEquals(0.5 - 20 * 0.0001 * 100, c.targetAmount(), 1e-9);
        c.frame(200, true, false, true, View.FIRST_PERSON);
        assertEquals(0.5, c.targetAmount(), 1e-9);
        c.frame(300, true, true, true, View.FIRST_PERSON);
        assertEquals(0.5, c.targetAmount(), 1e-9, "both keys cancel out");
        c.frame(10_000, true, true, false, View.FIRST_PERSON);
        assertEquals(0.5 - 20 * 0.0001 * ZoomController.MAX_FRAME_MS, c.targetAmount(), 1e-9, "a hitch is capped");
    }

    @Test
    @DisplayName("zoom: toggle mode flips on each press; a fresh zoom starts at the default again")
    void toggle() {
        final ZoomController c = controller();
        final ZoomController.Settings s = new ZoomController.Settings();
        s.toggle = true;
        s.smoothnessMs = 0;
        c.configure(s);
        c.frame(0, true, false, false, View.FIRST_PERSON);
        assertTrue(c.isZooming());
        c.frame(10, false, false, false, View.FIRST_PERSON);
        assertTrue(c.isZooming(), "release does not stop a toggled zoom");
        c.scroll(1.0);
        c.frame(20, false, false, false, View.FIRST_PERSON);
        assertEquals(0.42, c.targetAmount(), 1e-9);
        c.frame(30, true, false, false, View.FIRST_PERSON);
        assertFalse(c.isZooming());
        assertEquals(70.0, c.fov(70.0, 30), 0.0, "instant with smoothness 0");
        c.frame(40, false, false, false, View.FIRST_PERSON);
        c.frame(50, true, false, false, View.FIRST_PERSON);
        assertEquals(0.5, c.targetAmount(), 1e-9);
    }

    @Test
    @DisplayName("zoom: third person changes the camera distance around vanilla's 4 blocks, not the FOV")
    void thirdPerson() {
        final ZoomController c = controller();
        final double rest = c.thirdPersonRest();
        assertEquals(4.0, 0.5 + 14.5 * rest * rest, 1e-9, "resting amount maps to 4 blocks");
        c.frame(0, true, false, false, View.THIRD_PERSON_BACK);
        assertTrue(c.isZooming());
        assertEquals(4.0, c.cameraDistance(4.0, 0), 1e-9, "starts at vanilla distance");
        final double far = c.cameraDistance(4.0, 1000);
        assertTrue(far > 4.0 && far <= 15.0, "behind the player a fresh zoom pulls back: " + far);
        assertEquals(70.0, c.fov(70.0, 1000), 0.0, "no FOV change in third person");
        assertEquals(1.0, c.sensitivity(1.0, 1000), 0.0);
        assertFalse(c.smoothCamera(false));
        c.frame(10, false, false, false, View.THIRD_PERSON_BACK);
        assertTrue(c.isZooming(), "third person toggles by default");
        c.frame(20, true, false, false, View.THIRD_PERSON_BACK);
        assertFalse(c.isZooming());
        assertEquals(4.0, c.cameraDistance(4.0, 5000), 1e-9, "settles back at vanilla");

        final ZoomController front = controller();
        front.frame(0, true, false, false, View.THIRD_PERSON_FRONT);
        assertTrue(front.cameraDistance(4.0, 1000) < 4.0, "facing the player a fresh zoom moves closer");

        final ZoomController.Settings fovEverywhere = new ZoomController.Settings();
        fovEverywhere.maxThirdPersonDistance = 0;
        final ZoomController fovZoom = new ZoomController();
        fovZoom.configure(fovEverywhere);
        fovZoom.frame(0, true, false, false, View.THIRD_PERSON_BACK);
        assertEquals(4.0, fovZoom.cameraDistance(4.0, 1000), 0.0);
        assertTrue(fovZoom.fov(70.0, 1000) < 70.0, "distance 0: the FOV zooms in third person too");
    }

    @Test
    @DisplayName("zoom: reset drops all state (world change)")
    void reset() {
        final ZoomController c = controller();
        c.frame(0, true, false, false, View.FIRST_PERSON);
        c.reset();
        assertFalse(c.isZooming());
        assertFalse(c.isVisible(10));
        assertEquals(70.0, c.fov(70.0, 10), 0.0);
    }
}
