package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.Billboard;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.ViewFrustum;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import com.mojang.math.Quaternion;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.client.particle.Particle;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * Fast particles (1.16.5). Vanilla's {@code SingleQuadParticle.render} allocates an array and
 * five vectors and copies the camera quaternion (twice with roll) for every particle every frame,
 * and draws particles behind the camera too. Here a particle whose quad cannot reach the screen
 * (sphere against this frame's frustum) is skipped, and the four corners come from
 * {@code Billboard} (the same rotation, computed directly into a reused array), written in
 * vanilla's vertex order with vanilla's UVs, colour and light. Subclasses that override
 * {@code render} and call {@code super} get the same path; overrides of {@code getQuadSize},
 * {@code getU0}... and {@code getLightColor} are honoured. Vanilla already draws each particle
 * sheet as one batch. Applied only where {@code Capabilities.STREAMING} is true.
 */
@Mixin(targets = "net.minecraft.client.particle.SingleQuadParticle")
// @era:qol16-begin none
public abstract class FastParticleMixin {
// @era:qol16-else v16
//~ public abstract class FastParticleMixin extends Particle {
// @era:qol16-end
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ /** Render thread only: 4 corners x (x, y, z). */
    //~ @Unique
    //~ private static final float[] AETHERIUM$CORNERS = new float[12];

    //~ protected FastParticleMixin(final ClientLevel level, final double x, final double y, final double z) {
        //~ super(level, x, y, z);
    //~ }

    //~ @Shadow
    //~ public abstract float getQuadSize(float partialTick);

    //~ @Shadow
    //~ protected abstract float getU0();

    //~ @Shadow
    //~ protected abstract float getU1();

    //~ @Shadow
    //~ protected abstract float getV0();

    //~ @Shadow
    //~ protected abstract float getV1();

    //~ @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$fastRender(final VertexConsumer consumer, final Camera camera, final float partialTick,
                                      //~ final CallbackInfo ci) {
        //~ if (!RenderToggles.fastParticles) {
            //~ return;
        //~ }
        //~ final Vec3 cam = camera.getPosition();
        //~ // Mth.lerp(delta, start, end) = start + delta * (end - start), as vanilla interpolates.
        //~ final double t = partialTick;
        //~ final float px = (float) (this.xo + t * (this.x - this.xo) - cam.x());
        //~ final float py = (float) (this.yo + t * (this.y - this.yo) - cam.y());
        //~ final float pz = (float) (this.zo + t * (this.z - this.zo) - cam.z());
        //~ final float size = this.getQuadSize(partialTick);
        //~ ci.cancel();
        //~ // Corners are size * sqrt(2) from the centre.
        //~ if (!ViewFrustum.FRAME.sphereVisible(px, py, pz, size * 1.4143f + 0.01f)) {
            //~ return;
        //~ }
        //~ final Quaternion q = camera.rotation();
        //~ final float roll = this.roll == 0.0f ? 0.0f : this.oRoll + partialTick * (this.roll - this.oRoll);
        //~ final float[] c = AETHERIUM$CORNERS;
        //~ Billboard.corners(q.i(), q.j(), q.k(), q.r(), roll, size, c);
        //~ final float u0 = this.getU0();
        //~ final float u1 = this.getU1();
        //~ final float v0 = this.getV0();
        //~ final float v1 = this.getV1();
        //~ final int light = this.getLightColor(partialTick);
        //~ final float r = this.rCol;
        //~ final float g = this.gCol;
        //~ final float b = this.bCol;
        //~ final float a = this.alpha;
        //~ consumer.vertex(c[0] + px, c[1] + py, c[2] + pz).uv(u1, v1).color(r, g, b, a).uv2(light).endVertex();
        //~ consumer.vertex(c[3] + px, c[4] + py, c[5] + pz).uv(u1, v0).color(r, g, b, a).uv2(light).endVertex();
        //~ consumer.vertex(c[6] + px, c[7] + py, c[8] + pz).uv(u0, v0).color(r, g, b, a).uv2(light).endVertex();
        //~ consumer.vertex(c[9] + px, c[10] + py, c[11] + pz).uv(u0, v1).color(r, g, b, a).uv2(light).endVertex();
    //~ }
    // @era:qol16-end
}
