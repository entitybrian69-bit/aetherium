package com.aetherium.client;

import com.aetherium.gui.GuiCanvas;
import com.aetherium.qol.DamageNumbers;
import com.aetherium.qol.Qol;
import com.aetherium.qol.QolSupport;
import com.aetherium.qol.ScreenProjector;
import com.aetherium.qol.Waypoints;

import java.util.List;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * Waypoint beams and labels and damage numbers, drawn on the HUD from world points projected with
 * the camera ({@link ScreenProjector}) instead of inside the world renderer. The world renderer
 * changes shape in almost every Minecraft version (and moved to render-state extraction from 1.21.9),
 * while the HUD canvas and the camera are stable, so one implementation serves every version from
 * 1.17 on; 1.16.5 keeps its in-world renderer ({@link QolSupport#WORLD_IN_LEVEL}).
 *
 * <p>Costs nothing when the features are off. With them on: a few rectangles and one string per
 * visible waypoint, one string per live damage number; no allocation per frame.
 */
public final class QolOverlay {
    private static final ScreenProjector PROJECTOR = new ScreenProjector();
    private static final double[] SEGMENT = new double[6];
    private static final double BEAM_BOTTOM = -64.0;
    private static final double BEAM_TOP = 320.0;
    private static final double BEAM_HALF_WIDTH = 0.12;
    private static final int BEAM_ALPHA = 0x70;

    private QolOverlay() {
    }

    public static void render(final GuiCanvas canvas) {
        if (QolSupport.WORLD_IN_LEVEL) {
            return;
        }
        final boolean waypoints = Qol.waypoints || Qol.deathPoint;
        final boolean damage = Qol.damageNumbers && !QolClient.DAMAGE.isIdle();
        if (!waypoints && !damage) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        final Camera camera = ClientHooks.mainCamera(mc);
        if (camera == null || !camera.isInitialized()) {
            return;
        }
        final Vec3 cam = ClientHooks.cameraPosition(camera);
        final int width = mc.getWindow().getGuiScaledWidth();
        final int height = mc.getWindow().getGuiScaledHeight();
        PROJECTOR.setCamera(cam.x, cam.y, cam.z, ClientHooks.cameraYaw(camera), ClientHooks.cameraPitch(camera),
                ZoomKeys.lastWorldFov, width, height);
        if (waypoints) {
            renderWaypoints(mc, canvas, cam, height);
        }
        if (damage) {
            renderDamage(canvas);
        }
    }

    private static void renderWaypoints(final Minecraft mc, final GuiCanvas canvas, final Vec3 cam, final int height) {
        final List<Waypoints.Waypoint> list = QolClient.waypoints().all();
        if (list.isEmpty()) {
            return;
        }
        final String here = QolClient.dimension(mc);
        final ScreenProjector p = PROJECTOR;
        for (int i = 0, n = list.size(); i < n; i++) {
            final Waypoints.Waypoint w = list.get(i);
            if (!w.visible || !(Qol.waypoints || w.death)) {
                continue;
            }
            final boolean sameDimension = w.dimension.equals(here);
            if (!sameDimension && !Qol.crossDimension) {
                continue;
            }
            final double x = Waypoints.convert(w.x + 0.5, w.dimension, here);
            final double z = Waypoints.convert(w.z + 0.5, w.dimension, here);
            if (Double.isNaN(x) || Double.isNaN(z)) {
                continue;
            }
            final int rgb = w.argb() & 0xFFFFFF;
            if (Qol.waypointBeams) {
                beam(canvas, x, z, cam.y, rgb, height);
            }
            final double wy = sameDimension ? w.y + 1.5 : cam.y;
            final double dx = x - cam.x;
            final double dy = wy - cam.y;
            final double dz = z - cam.z;
            final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 2.0 || !p.project(x, wy, z) || !p.onScreen(0)) {
                continue;
            }
            final String label = w.name + " (" + (int) Math.round(distance) + " m)";
            final int tw = canvas.textWidth(label);
            final int lx = (int) Math.round(p.x) - tw / 2;
            final int ly = (int) Math.round(p.y) - 5;
            canvas.fill(lx - 2, ly - 2, lx + tw + 2, ly + 10, 0x60000000);
            canvas.text(label, lx, ly, 0xFF000000 | rgb);
        }
    }

    /**
     * A vertical beam from the bottom of the world to the top, fading upwards: its projection is a
     * straight screen line, drawn as a short run of rectangles (more of them the more it leans).
     */
    private static void beam(final GuiCanvas canvas, final double x, final double z, final double eyeY, final int rgb,
                             final int height) {
        final ScreenProjector p = PROJECTOR;
        final double[] s = SEGMENT;
        if (!p.segment(x, BEAM_BOTTOM, z, x, BEAM_TOP, z, s)) {
            return;
        }
        final double x0 = s[0];
        final double y0 = s[1];
        final double x1 = s[2];
        final double y1 = s[3];
        final double t0 = s[4];
        final double t1 = s[5];
        if (Math.max(y0, y1) < 0 || Math.min(y0, y1) > height) {
            return;
        }
        // Width from the depth of the column at eye height, its nearest visible point.
        final double pixels = BEAM_HALF_WIDTH * 2.0 * p.pixelsPerBlock(p.forward(x, eyeY, z));
        final int w = (int) Math.max(2, Math.min(8, Math.round(pixels)));
        final int steps = (int) Math.max(8, Math.min(32, Math.ceil(Math.abs(x1 - x0) / 2.0)));
        for (int k = 0; k < steps; k++) {
            final double a = (double) k / steps;
            final double b = (double) (k + 1) / steps;
            final double ya = y0 + (y1 - y0) * a;
            final double yb = y0 + (y1 - y0) * b;
            final double xm = x0 + (x1 - x0) * (a + b) * 0.5;
            final double t = t0 + (t1 - t0) * (a + b) * 0.5; // 0 at the bottom of the world, 1 at the top
            final int alpha = (int) (BEAM_ALPHA * (1.0 - t));
            if (alpha < 4) {
                continue;
            }
            final int top = (int) Math.floor(Math.min(ya, yb));
            final int bottom = (int) Math.ceil(Math.max(ya, yb));
            final int left = (int) Math.round(xm - w / 2.0);
            canvas.fill(left, top, left + w, Math.max(bottom, top + 1), alpha << 24 | rgb);
        }
    }

    private static void renderDamage(final GuiCanvas canvas) {
        final DamageNumbers numbers = QolClient.DAMAGE;
        final long now = System.currentTimeMillis();
        final ScreenProjector p = PROJECTOR;
        for (int i = 0; i < numbers.capacity(); i++) {
            final DamageNumbers.Popup popup = numbers.popup(i);
            if (!popup.isLive()) {
                continue;
            }
            final float age = popup.age(now);
            if (age >= 1f) {
                continue;
            }
            final int alpha = age < 0.7f ? 255 : (int) (255 * (1f - (age - 0.7f) / 0.3f));
            if (alpha < 8 || !p.project(popup.x, popup.y + 0.25 + age * 0.7, popup.z) || !p.onScreen(0)) {
                continue;
            }
            final String text = popup.text();
            final int rgb = popup.heal ? 0x55FF55 : 0xFF5555;
            final int tx = (int) Math.round(p.x) - canvas.textWidth(text) / 2;
            final int ty = (int) Math.round(p.y) - 4;
            canvas.text(text, tx + 1, ty + 1, (alpha / 2) << 24);
            canvas.text(text, tx, ty, alpha << 24 | rgb);
        }
    }
}
