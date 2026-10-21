package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.aetherium.perf.ViewFrustum;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.util.Mth;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.LightningBolt;
//~ import net.minecraft.world.entity.LivingEntity;
//~ import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
//~ import net.minecraft.world.entity.boss.wither.WitherBoss;
//~ import net.minecraft.world.entity.item.FallingBlockEntity;
//~ import net.minecraft.world.entity.item.PrimedTnt;
//~ import net.minecraft.world.entity.player.Player;
//~ import net.minecraft.world.entity.projectile.EyeOfEnder;
//~ import net.minecraft.world.entity.projectile.Projectile;
//~ import net.minecraft.world.entity.vehicle.AbstractMinecart;
//~ import net.minecraft.world.entity.vehicle.Boat;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:boost16-end

/**
 * Skip unseen entity updates (1.17.2, Minecraft 1.16.5): the client runs every loaded entity's
 * full update each tick (animation, smoothing, effects, client-side physics) although most of
 * them are off screen. With the option on, an entity that was outside last frame's view and more
 * than 8 blocks from the camera skips that update; a living entity jumps to the position and
 * rotation the server last sent instead of gliding there. Its age still advances (vanilla counts
 * it before the update), and the server stays in charge of everything that matters.
 *
 * <p>Always updated: players, the camera entity, anything carrying passengers, bosses, projectiles,
 * minecarts, boats, falling blocks, lit TNT (fuse flashing), lightning (thunder sound) and eyes of
 * ender, whose client-side updates are visible or audible from anywhere.</p>
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel")
public abstract class TuningEntityTickMixin {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Redirect(method = "tickNonPassenger",
            //~ at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"), require = 0)
    //~ private void aetherium$tickIfSeen(final Entity entity) {
        //~ if (Tuning.entityTickCulling && aetherium$skip(entity)) {
            //~ if (entity instanceof TuningLerpAccessor) {
                //~ aetherium$snap(entity, (TuningLerpAccessor) entity);
            //~ }
            //~ return;
        //~ }
        //~ entity.tick();
    //~ }

    //~ @Unique
    //~ private static boolean aetherium$skip(final Entity entity) {
        //~ final ViewFrustum frustum = ViewFrustum.FRAME;
        //~ if (!frustum.valid() || entity instanceof Player || entity.isVehicle() || entity instanceof Projectile
                //~ || entity instanceof AbstractMinecart || entity instanceof Boat || entity instanceof EnderDragon
                //~ || entity instanceof WitherBoss || entity instanceof FallingBlockEntity || entity instanceof PrimedTnt
                //~ || entity instanceof LightningBolt || entity instanceof EyeOfEnder) {
            //~ return false;
        //~ }
        //~ final Minecraft minecraft = Minecraft.getInstance();
        //~ if (entity == minecraft.getCameraEntity()) {
            //~ return false;
        //~ }
        //~ final Camera camera = minecraft.gameRenderer.getMainCamera();
        //~ if (camera == null || !camera.isInitialized()) {
            //~ return false;
        //~ }
        //~ final Vec3 eye = camera.getPosition();
        //~ final float height = entity.getBbHeight();
        //~ final float width = entity.getBbWidth();
        //~ final double dx = entity.getX() - eye.x;
        //~ final double dy = entity.getY() + height * 0.5 - eye.y;
        //~ final double dz = entity.getZ() - eye.z;
        //~ if (dx * dx + dy * dy + dz * dz <= 64.0) {
            //~ return false;
        //~ }
        //~ // Half diagonal of the box, plus room for limbs, held items and smoothing.
        //~ final float radius = 0.5f * (float) Math.sqrt(2.0f * width * width + height * height) + 3.0f;
        //~ return !frustum.sphereVisible((float) dx, (float) dy, (float) dz, radius);
    //~ }

    //~ /** What the skipped smoothing steps would have reached: the server's last position and rotation. */
    //~ @Unique
    //~ private static void aetherium$snap(final Entity entity, final TuningLerpAccessor lerp) {
        //~ if (lerp.aetherium$lerpSteps() > 0) {
            //~ final float yRot = (float) (entity.yRot + Mth.wrapDegrees(lerp.aetherium$lerpYRot() - entity.yRot));
            //~ final float xRot = (float) lerp.aetherium$lerpXRot();
            //~ lerp.aetherium$setLerpSteps(0);
            //~ entity.setPos(lerp.aetherium$lerpX(), lerp.aetherium$lerpY(), lerp.aetherium$lerpZ());
            //~ entity.yRot = yRot % 360.0f;
            //~ entity.xRot = xRot % 360.0f;
        //~ }
        //~ if (lerp.aetherium$lerpHeadSteps() > 0 && entity instanceof LivingEntity) {
            //~ final LivingEntity living = (LivingEntity) entity;
            //~ living.yHeadRot = (float) (living.yHeadRot + Mth.wrapDegrees(lerp.aetherium$lerpHeadRot() - living.yHeadRot));
            //~ lerp.aetherium$setLerpHeadSteps(0);
        //~ }
    //~ }
    // @era:boost16-end
}
