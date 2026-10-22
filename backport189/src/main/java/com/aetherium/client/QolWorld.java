package com.aetherium.client;

import com.aetherium.qol.DamageNumbers;
import com.aetherium.qol.Qol;
import com.aetherium.qol.Waypoints;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

/**
 * Waypoint beams and labels and damage numbers, drawn in the world after it is rendered
 * (RenderWorldLastEvent; Quality of life on 1.12.2). Same look as the main mod's 1.16.5 world
 * renderer: one batched draw for all beams, labels that keep their size on screen and stay
 * visible through walls (a faint copy without depth test under the depth-tested one).
 */
public final class QolWorld {
    private static final float BEAM_HALF_WIDTH = 0.12f;

    private QolWorld() {
    }

    public static void render(final float partialTicks) {
        final boolean waypoints = Qol.waypoints || Qol.deathPoint;
        final boolean damage = Qol.damageNumbers && !QolClient.DAMAGE.isIdle();
        final Minecraft mc = Minecraft.getMinecraft();
        if (!waypoints && !damage || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        final RenderManager rm = mc.getRenderManager();
        final double cx = rm.viewerPosX;
        final double cy = rm.viewerPosY;
        final double cz = rm.viewerPosZ;
        final boolean frontView = mc.gameSettings.thirdPersonView == 2;
        GlStateManager.pushMatrix();
        try {
            if (waypoints) {
                final double maxDistance = Math.max(24.0, mc.gameSettings.renderDistanceChunks * 16 * 0.9);
                renderWaypoints(mc, rm, frontView, cx, cy, cz, maxDistance);
            }
            if (damage) {
                renderDamage(mc, rm, frontView, cx, cy, cz);
            }
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableTexture2D();
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
        }
    }

    private static void renderWaypoints(final Minecraft mc, final RenderManager rm, final boolean frontView,
                                        final double cx, final double cy, final double cz, final double maxDistance) {
        final List<Waypoints.Waypoint> list = QolClient.waypoints().all();
        final int n = list.size();
        if (n == 0) {
            return;
        }
        final String here = QolClient.dimension(mc);
        final double[] xs = new double[n];
        final double[] zs = new double[n];
        final boolean[] shown = new boolean[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            final Waypoints.Waypoint w = list.get(i);
            if (!w.visible || !(Qol.waypoints || w.death) || !w.dimension.equals(here) && !Qol.crossDimension) {
                continue;
            }
            final double x = Waypoints.convert(w.x + 0.5, w.dimension, here);
            final double z = Waypoints.convert(w.z + 0.5, w.dimension, here);
            if (Double.isNaN(x) || Double.isNaN(z)) {
                continue;
            }
            xs[i] = x;
            zs[i] = z;
            shown[i] = true;
            count++;
        }
        if (count == 0) {
            return;
        }
        GlStateManager.disableLighting();
        if (Qol.waypointBeams) {
            GlStateManager.disableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.disableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(false);
            final Tessellator tessellator = Tessellator.getInstance();
            final WorldRenderer buffer = tessellator.getWorldRenderer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < n; i++) {
                if (!shown[i]) {
                    continue;
                }
                double dx = xs[i] - cx;
                double dz = zs[i] - cz;
                final double flat = Math.sqrt(dx * dx + dz * dz);
                if (flat > maxDistance) {
                    dx *= maxDistance / flat;
                    dz *= maxDistance / flat;
                }
                beam(buffer, dx, -cy, 256.0 - cy, dz, list.get(i).argb());
            }
            tessellator.draw();
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
        }

        final FontRenderer font = mc.fontRendererObj;
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        for (int i = 0; i < n; i++) {
            if (!shown[i]) {
                continue;
            }
            final Waypoints.Waypoint w = list.get(i);
            final double wy = w.dimension.equals(here) ? w.y + 1.5 : cy;
            double dx = xs[i] - cx;
            double dy = wy - cy;
            double dz = zs[i] - cz;
            final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 2.0) {
                continue;
            }
            if (distance > maxDistance) {
                final double k = maxDistance / distance;
                dx *= k;
                dy *= k;
                dz *= k;
            }
            final String label = w.name + " (" + (int) Math.round(distance) + " m)";
            // Constant size on screen: the scale grows with distance.
            final float scale = (float) (0.0028 * Math.max(6.0, Math.min(distance, maxDistance)));
            billboard(rm, frontView, dx, dy, dz, scale);
            final int x = -font.getStringWidth(label) / 2;
            GlStateManager.disableDepth();
            GlStateManager.depthMask(false);
            GlStateManager.disableTexture2D();
            final Tessellator tessellator = Tessellator.getInstance();
            final WorldRenderer buffer = tessellator.getWorldRenderer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            quad(buffer, x - 1, -1, -x + 1, 8, 0x60000000);
            tessellator.draw();
            GlStateManager.enableTexture2D();
            font.drawString(label, x, 0, 0x20FFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            font.drawString(label, x, 0, 0xFF000000 | w.argb());
            GlStateManager.popMatrix();
        }
    }

    private static void renderDamage(final Minecraft mc, final RenderManager rm, final boolean frontView,
                                     final double cx, final double cy, final double cz) {
        final DamageNumbers numbers = QolClient.DAMAGE;
        final long now = System.currentTimeMillis();
        final FontRenderer font = mc.fontRendererObj;
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableDepth();
        for (int i = 0; i < numbers.capacity(); i++) {
            final DamageNumbers.Popup p = numbers.popup(i);
            if (!p.isLive()) {
                continue;
            }
            final float age = p.age(now);
            if (age >= 1f) {
                continue;
            }
            final int alpha = age < 0.7f ? 255 : (int) (255 * (1f - (age - 0.7f) / 0.3f));
            if (alpha < 8) {
                continue;
            }
            final String text = p.text();
            final int rgb = p.heal ? 0x55FF55 : 0xFF5555;
            final float scale = 0.025f * (1f + 0.25f * (1f - age));
            billboard(rm, frontView, p.x - cx, p.y + 0.25 + age * 0.7 - cy, p.z - cz, scale);
            font.drawStringWithShadow(text, -font.getStringWidth(text) / 2f, 0f, alpha << 24 | rgb);
            GlStateManager.popMatrix();
        }
    }

    /** Pushes a matrix at (x, y, z) relative to the camera that faces the viewer, like a name tag. */
    private static void billboard(final RenderManager rm, final boolean frontView, final double x, final double y,
                                  final double z, final float scale) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(-rm.playerViewY, 0f, 1f, 0f);
        GlStateManager.rotate((frontView ? -1f : 1f) * rm.playerViewX, 1f, 0f, 0f);
        GlStateManager.scale(-scale, -scale, scale);
    }

    private static void quad(final WorldRenderer buffer, final double x0, final double y0, final double x1,
                             final double y1, final int argb) {
        final int a = argb >>> 24;
        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        buffer.pos(x0, y0, 0).color(r, g, b, a).endVertex();
        buffer.pos(x0, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y0, 0).color(r, g, b, a).endVertex();
    }

    /** Two crossed vertical quads around the waypoint's column, fading out upwards. */
    private static void beam(final WorldRenderer buffer, final double x, final double y0, final double y1,
                             final double z, final int argb) {
        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        final int a = 0x70;
        final double h = BEAM_HALF_WIDTH;
        buffer.pos(x - h, y0, z).color(r, g, b, a).endVertex();
        buffer.pos(x + h, y0, z).color(r, g, b, a).endVertex();
        buffer.pos(x + h, y1, z).color(r, g, b, 0).endVertex();
        buffer.pos(x - h, y1, z).color(r, g, b, 0).endVertex();
        buffer.pos(x, y0, z - h).color(r, g, b, a).endVertex();
        buffer.pos(x, y0, z + h).color(r, g, b, a).endVertex();
        buffer.pos(x, y1, z + h).color(r, g, b, 0).endVertex();
        buffer.pos(x, y1, z - h).color(r, g, b, 0).endVertex();
    }
}
