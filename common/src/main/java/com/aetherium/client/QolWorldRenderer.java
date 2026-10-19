package com.aetherium.client;

// @era:q-world-begin hud
// @era:q-world-else v16
//~ import com.aetherium.qol.DamageNumbers;
//~ import com.aetherium.qol.Qol;
//~ import com.aetherium.qol.Waypoints;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.Tesselator;
//~ import com.mojang.math.Matrix4f;
//~ import java.util.List;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.Font;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.world.phys.Vec3;
// @era:q-world-end

/**
 * World-space drawing of the Quality of life tab: waypoint beams and labels, and damage numbers.
 * Called once per frame at the end of the level pass, when the pose holds only the camera
 * rotation; positions are made camera-relative here. Draws nothing (one field read) while the
 * features are off.
 */
public final class QolWorldRenderer {
    private QolWorldRenderer() {
    }

    // @era:q-world-begin hud
    // @era:q-world-else v16
    //~ private static final int FULL_BRIGHT = 0xF000F0;
    //~ private static final float BEAM_HALF_WIDTH = 0.12f;
    //~ private static final int GL_QUADS = 7;

    //~ /** LevelRenderer.renderLevel TAIL. */
    //~ public static void render(final PoseStack pose, final Camera camera) {
        //~ final boolean waypoints = Qol.waypoints || Qol.deathPoint;
        //~ final boolean damage = Qol.damageNumbers && !QolClient.DAMAGE.isIdle();
        //~ if (!waypoints && !damage) {
            //~ return;
        //~ }
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ if (mc.level == null || mc.player == null) {
            //~ return;
        //~ }
        //~ final Vec3 cam = camera.getPosition();
        //~ // Keep everything inside the far plane: beyond this, things are drawn at this distance in their direction.
        //~ final double maxDistance = Math.max(24.0, mc.gameRenderer.getRenderDistance() * 0.9);
        //~ RenderSystem.disableFog();
        //~ if (waypoints) {
            //~ renderWaypoints(mc, pose, camera, cam, maxDistance);
        //~ }
        //~ if (damage) {
            //~ renderDamage(mc, pose, camera, cam);
        //~ }
    //~ }

    //~ // ------------------------------------------------------------------ waypoints

    //~ private static void renderWaypoints(final Minecraft mc, final PoseStack pose, final Camera camera, final Vec3 cam,
                                        //~ final double maxDistance) {
        //~ final Waypoints store = QolClient.waypoints();
        //~ final List<Waypoints.Waypoint> list = store.all();
        //~ if (list.isEmpty()) {
            //~ return;
        //~ }
        //~ final String here = QolClient.dimension(mc);
        //~ final int n = list.size();
        //~ final double[] xs = new double[n];
        //~ final double[] zs = new double[n];
        //~ final boolean[] shown = new boolean[n];
        //~ int count = 0;
        //~ for (int i = 0; i < n; i++) {
            //~ final Waypoints.Waypoint w = list.get(i);
            //~ if (!w.visible || !(Qol.waypoints || w.death)) {
                //~ continue;
            //~ }
            //~ if (!w.dimension.equals(here) && !Qol.crossDimension) {
                //~ continue;
            //~ }
            //~ final double x = Waypoints.convert(w.x + 0.5, w.dimension, here);
            //~ final double z = Waypoints.convert(w.z + 0.5, w.dimension, here);
            //~ if (Double.isNaN(x) || Double.isNaN(z)) {
                //~ continue;
            //~ }
            //~ xs[i] = x;
            //~ zs[i] = z;
            //~ shown[i] = true;
            //~ count++;
        //~ }
        //~ if (count == 0) {
            //~ return;
        //~ }
        //~ final Matrix4f matrix = pose.last().pose();

        //~ if (Qol.waypointBeams) {
            //~ RenderSystem.disableTexture();
            //~ RenderSystem.disableCull();
            //~ RenderSystem.disableAlphaTest();
            //~ RenderSystem.enableBlend();
            //~ RenderSystem.defaultBlendFunc();
            //~ RenderSystem.enableDepthTest();
            //~ RenderSystem.depthMask(false);
            //~ final Tesselator tesselator = Tesselator.getInstance();
            //~ final BufferBuilder buffer = tesselator.getBuilder();
            //~ buffer.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
            //~ for (int i = 0; i < n; i++) {
                //~ if (!shown[i]) {
                    //~ continue;
                //~ }
                //~ double dx = xs[i] - cam.x;
                //~ double dz = zs[i] - cam.z;
                //~ final double flat = Math.sqrt(dx * dx + dz * dz);
                //~ if (flat > maxDistance) {
                    //~ dx *= maxDistance / flat;
                    //~ dz *= maxDistance / flat;
                //~ }
                //~ final int argb = list.get(i).argb();
                //~ beam(buffer, matrix, (float) dx, (float) (0.0 - cam.y), (float) (256.0 - cam.y), (float) dz, argb);
            //~ }
            //~ tesselator.end();
            //~ RenderSystem.depthMask(true);
            //~ RenderSystem.enableCull();
            //~ RenderSystem.enableTexture();
            //~ RenderSystem.enableAlphaTest();
        //~ }

        //~ final Font font = mc.font;
        //~ final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        //~ for (int i = 0; i < n; i++) {
            //~ if (!shown[i]) {
                //~ continue;
            //~ }
            //~ final Waypoints.Waypoint w = list.get(i);
            //~ final double wy = w.dimension.equals(here) ? w.y + 1.5 : cam.y;
            //~ double dx = xs[i] - cam.x;
            //~ double dy = wy - cam.y;
            //~ double dz = zs[i] - cam.z;
            //~ final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            //~ if (distance < 2.0) {
                //~ continue;
            //~ }
            //~ if (distance > maxDistance) {
                //~ final double k = maxDistance / distance;
                //~ dx *= k;
                //~ dy *= k;
                //~ dz *= k;
            //~ }
            //~ final double shownDistance = Math.min(distance, maxDistance);
            //~ final String label = w.name + " (" + (int) Math.round(distance) + " m)";
            //~ pose.pushPose();
            //~ pose.translate(dx, dy, dz);
            //~ pose.mulPose(camera.rotation());
            //~ // Constant size on screen: the scale grows with distance.
            //~ final float scale = (float) (0.0028 * Math.max(6.0, shownDistance));
            //~ pose.scale(-scale, -scale, scale);
            //~ final Matrix4f labelMatrix = pose.last().pose();
            //~ final float x = -font.width(label) / 2f;
            //~ font.drawInBatch(label, x, 0f, 0x20FFFFFF, false, labelMatrix, buffers, true, 0x60000000, FULL_BRIGHT);
            //~ font.drawInBatch(label, x, 0f, 0xFF000000 | w.argb(), false, labelMatrix, buffers, false, 0, FULL_BRIGHT);
            //~ pose.popPose();
        //~ }
        //~ buffers.endBatch();
    //~ }

    //~ /** Two crossed vertical quads around the waypoint's column. */
    //~ private static void beam(final BufferBuilder buffer, final Matrix4f m, final float x, final float y0, final float y1,
                             //~ final float z, final int argb) {
        //~ final int r = argb >> 16 & 0xFF;
        //~ final int g = argb >> 8 & 0xFF;
        //~ final int b = argb & 0xFF;
        //~ final int a = 0x70;
        //~ final float h = BEAM_HALF_WIDTH;
        //~ buffer.vertex(m, x - h, y0, z).color(r, g, b, a).endVertex();
        //~ buffer.vertex(m, x + h, y0, z).color(r, g, b, a).endVertex();
        //~ buffer.vertex(m, x + h, y1, z).color(r, g, b, 0).endVertex();
        //~ buffer.vertex(m, x - h, y1, z).color(r, g, b, 0).endVertex();
        //~ buffer.vertex(m, x, y0, z - h).color(r, g, b, a).endVertex();
        //~ buffer.vertex(m, x, y0, z + h).color(r, g, b, a).endVertex();
        //~ buffer.vertex(m, x, y1, z + h).color(r, g, b, 0).endVertex();
        //~ buffer.vertex(m, x, y1, z - h).color(r, g, b, 0).endVertex();
    //~ }

    //~ // ------------------------------------------------------------------ damage numbers

    //~ private static void renderDamage(final Minecraft mc, final PoseStack pose, final Camera camera, final Vec3 cam) {
        //~ final DamageNumbers numbers = QolClient.DAMAGE;
        //~ final long now = System.currentTimeMillis();
        //~ final Font font = mc.font;
        //~ final MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        //~ boolean any = false;
        //~ for (int i = 0; i < numbers.capacity(); i++) {
            //~ final DamageNumbers.Popup p = numbers.popup(i);
            //~ if (!p.isLive()) {
                //~ continue;
            //~ }
            //~ final float age = p.age(now);
            //~ if (age >= 1f) {
                //~ continue;
            //~ }
            //~ final int alpha = age < 0.7f ? 255 : (int) (255 * (1f - (age - 0.7f) / 0.3f));
            //~ if (alpha < 8) {
                //~ continue;
            //~ }
            //~ final String text = p.text();
            //~ final int rgb = p.heal ? 0x55FF55 : 0xFF5555;
            //~ pose.pushPose();
            //~ pose.translate(p.x - cam.x, p.y + 0.25 + age * 0.7 - cam.y, p.z - cam.z);
            //~ pose.mulPose(camera.rotation());
            //~ final float scale = 0.025f * (1f + 0.25f * (1f - age));
            //~ pose.scale(-scale, -scale, scale);
            //~ final float x = -font.width(text) / 2f;
            //~ font.drawInBatch(text, x, 0f, alpha << 24 | rgb, true, pose.last().pose(), buffers, false, 0, FULL_BRIGHT);
            //~ pose.popPose();
            //~ any = true;
        //~ }
        //~ if (any) {
            //~ buffers.endBatch();
        //~ }
    //~ }
    // @era:q-world-end
}
