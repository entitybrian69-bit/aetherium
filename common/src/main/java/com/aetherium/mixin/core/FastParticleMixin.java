package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:particle-fast-begin inner
import com.aetherium.perf.Billboard;
import com.aetherium.perf.RenderToggles;
import com.aetherium.perf.ViewFrustum;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:particle-fast-else v16|quat
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
// @era:particle-fast-else joml
//~ import com.aetherium.perf.Billboard;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.ViewFrustum;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.client.particle.Particle;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.joml.Quaternionf;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:particle-fast-else facing
//~ import com.aetherium.perf.Billboard;
//~ import com.aetherium.perf.RenderToggles;
//~ import com.aetherium.perf.ViewFrustum;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.client.particle.Particle;
//~ import net.minecraft.client.particle.SingleQuadParticle;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.joml.Quaternionf;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:particle-fast-else none
// @era:particle-fast-end

/**
 * Fast particles. Vanilla's {@code SingleQuadParticle} allocates vectors (and quaternions) for
 * every particle every frame, and draws particles behind the camera too. Here a particle whose
 * quad cannot reach the screen (sphere against this frame's frustum) is skipped, and the four
 * corners come from {@code Billboard} (the same rotation, computed directly into a reused array),
 * written in vanilla's vertex order with vanilla's UVs, colour and light. Overrides of
 * {@code getQuadSize}, {@code getU0}... and {@code getLightColor} are honoured.
 *
 * <p>1.16.5..1.20.6 replace {@code render(VertexConsumer, Camera, float)} (1.20.3+: only for the
 * default camera-facing mode). 1.21..1.21.8 replace the inner
 * {@code renderRotatedQuad(VertexConsumer, Quaternionf, x, y, z, partialTick)}, so vanilla (and
 * any subclass) still picks the rotation and position; the corners there run in reverse order.
 * 1.21.9+ extract particle render states instead and are left to vanilla.</p>
 */
@Mixin(targets = "net.minecraft.client.particle.SingleQuadParticle")
// @era:particle-fast-begin v16|quat|joml|facing|inner
public abstract class FastParticleMixin extends Particle {
// @era:particle-fast-else none
//~ public abstract class FastParticleMixin {
// @era:particle-fast-end

    // @era:particle-fast-begin v16|quat|joml|facing|inner
    @Unique
    private static final float[] AETHERIUM$CORNERS = new float[12];

    protected FastParticleMixin(final ClientLevel level, final double x, final double y, final double z) {
        super(level, x, y, z);
    }

    @Shadow
    public abstract float getQuadSize(float partialTick);

    @Shadow
    protected abstract float getU0();

    @Shadow
    protected abstract float getU1();

    @Shadow
    protected abstract float getV0();

    @Shadow
    protected abstract float getV1();
    // @era:particle-fast-else none
    // @era:particle-fast-end

    // @era:particle-fast-begin inner
    @Inject(method = "renderRotatedQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Quaternionf;FFFF)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void aetherium$fastQuad(final VertexConsumer consumer, final Quaternionf q, final float px,
                                    final float py, final float pz, final float partialTick, final CallbackInfo ci) {
        if (!RenderToggles.fastParticles) {
            return;
        }
        final float size = this.getQuadSize(partialTick);
        ci.cancel();
        // Corners are size * sqrt(2) from the centre.
        if (!ViewFrustum.FRAME.sphereVisible(px, py, pz, size * 1.4143f + 0.01f)) {
            return;
        }
        final float[] c = AETHERIUM$CORNERS;
        // The roll (if any) is already part of q here.
        Billboard.corners(q.x, q.y, q.z, q.w, 0.0f, size, c);
        final float u0 = this.getU0();
        final float u1 = this.getU1();
        final float v0 = this.getV0();
        final float v1 = this.getV1();
        final int light = this.getLightColor(partialTick);
        final float r = this.rCol;
        final float g = this.gCol;
        final float b = this.bCol;
        final float a = this.alpha;
        // 1.21 order: (1, -1), (1, 1), (-1, 1), (-1, -1) = Billboard corners 3, 2, 1, 0.
        consumer.addVertex(c[9] + px, c[10] + py, c[11] + pz).setUv(u1, v1).setColor(r, g, b, a).setLight(light);
        consumer.addVertex(c[6] + px, c[7] + py, c[8] + pz).setUv(u1, v0).setColor(r, g, b, a).setLight(light);
        consumer.addVertex(c[3] + px, c[4] + py, c[5] + pz).setUv(u0, v0).setColor(r, g, b, a).setLight(light);
        consumer.addVertex(c[0] + px, c[1] + py, c[2] + pz).setUv(u0, v1).setColor(r, g, b, a).setLight(light);
    }
    // @era:particle-fast-else v16|quat
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
        //~ this.aetherium$emit(consumer, c, px, py, pz, partialTick);
    //~ }
    // @era:particle-fast-else joml
    //~ @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$fastRender(final VertexConsumer consumer, final Camera camera, final float partialTick,
                                      //~ final CallbackInfo ci) {
        //~ if (!RenderToggles.fastParticles) {
            //~ return;
        //~ }
        //~ final Vec3 cam = camera.getPosition();
        //~ final double t = partialTick;
        //~ final float px = (float) (this.xo + t * (this.x - this.xo) - cam.x());
        //~ final float py = (float) (this.yo + t * (this.y - this.yo) - cam.y());
        //~ final float pz = (float) (this.zo + t * (this.z - this.zo) - cam.z());
        //~ final float size = this.getQuadSize(partialTick);
        //~ ci.cancel();
        //~ if (!ViewFrustum.FRAME.sphereVisible(px, py, pz, size * 1.4143f + 0.01f)) {
            //~ return;
        //~ }
        //~ final Quaternionf q = camera.rotation();
        //~ final float roll = this.roll == 0.0f ? 0.0f : this.oRoll + partialTick * (this.roll - this.oRoll);
        //~ final float[] c = AETHERIUM$CORNERS;
        //~ Billboard.corners(q.x, q.y, q.z, q.w, roll, size, c);
        //~ this.aetherium$emit(consumer, c, px, py, pz, partialTick);
    //~ }
    // @era:particle-fast-else facing
    //~ @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    //~ private void aetherium$fastRender(final VertexConsumer consumer, final Camera camera, final float partialTick,
                                      //~ final CallbackInfo ci) {
        //~ // Other facing modes (LOOKAT_Y, custom) keep vanilla's rotation.
        //~ if (!RenderToggles.fastParticles
                //~ || ((SingleQuadParticle) (Object) this).getFacingCameraMode() != SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ) {
            //~ return;
        //~ }
        //~ final Vec3 cam = camera.getPosition();
        //~ final double t = partialTick;
        //~ final float px = (float) (this.xo + t * (this.x - this.xo) - cam.x());
        //~ final float py = (float) (this.yo + t * (this.y - this.yo) - cam.y());
        //~ final float pz = (float) (this.zo + t * (this.z - this.zo) - cam.z());
        //~ final float size = this.getQuadSize(partialTick);
        //~ ci.cancel();
        //~ if (!ViewFrustum.FRAME.sphereVisible(px, py, pz, size * 1.4143f + 0.01f)) {
            //~ return;
        //~ }
        //~ final Quaternionf q = camera.rotation();
        //~ final float roll = this.roll == 0.0f ? 0.0f : this.oRoll + partialTick * (this.roll - this.oRoll);
        //~ final float[] c = AETHERIUM$CORNERS;
        //~ Billboard.corners(q.x, q.y, q.z, q.w, roll, size, c);
        //~ this.aetherium$emit(consumer, c, px, py, pz, partialTick);
    //~ }
    // @era:particle-fast-else none
    // @era:particle-fast-end

    // @era:particle-fast-begin inner|none
    // @era:particle-fast-else v16|quat|joml|facing
    //~ /** Vanilla's 1.16.5..1.20.6 order: (-1, -1), (-1, 1), (1, 1), (1, -1). */
    //~ @Unique
    //~ private void aetherium$emit(final VertexConsumer consumer, final float[] c, final float px, final float py,
                                //~ final float pz, final float partialTick) {
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
    // @era:particle-fast-end
}
