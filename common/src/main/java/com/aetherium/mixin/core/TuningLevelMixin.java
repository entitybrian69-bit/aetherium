package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.ChunkInfoOrigin;
//~ import com.aetherium.perf.CrowdCull;
//~ import com.aetherium.perf.HitchGuard;
//~ import com.aetherium.perf.Tuning;
//~ import it.unimi.dsi.fastutil.objects.ObjectList;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.client.renderer.culling.Frustum;
//~ import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.Mob;
//~ import net.minecraft.world.entity.item.ItemEntity;
//~ import net.minecraft.world.level.Level;
//~ import net.minecraft.world.level.lighting.LevelLightEngine;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Constant;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:qol16-end

/**
 * 1.10.0 tuning hooks in {@code LevelRenderer} (Minecraft 1.16.5; {@code Capabilities.TUNING}):
 * crowd culling of entity piles, the client light-update quota, lighter rain and snow, and the
 * Nether roof cull.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class TuningLevelMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Shadow
    //~ private ClientLevel level;

    //~ @Shadow
    //~ private ObjectList<Object> renderChunks;

    //~ /** Once per frame, before any entity is tested. */
    //~ @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    //~ private void aetherium$frame(final CallbackInfo ci) {
        //~ CrowdCull.newFrame();
    //~ }

    //~ /**
     //~ * Crowd culling: an entity that vanilla would draw is skipped once its block space already holds
     //~ * {@link CrowdCull#ITEM_CAP} items or {@link CrowdCull#MOB_CAP} mobs this frame. Named and glowing
     //~ * entities always render.
     //~ */
    //~ @Redirect(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"),
            //~ require = 0)
    //~ private boolean aetherium$crowd(final EntityRenderDispatcher dispatcher, final Entity entity, final Frustum frustum,
                                    //~ final double camX, final double camY, final double camZ) {
        //~ if (!dispatcher.shouldRender(entity, frustum, camX, camY, camZ)) {
            //~ return false;
        //~ }
        //~ if (!Tuning.crowdCull || entity.hasCustomName() || entity.isGlowing()) {
            //~ return true;
        //~ }
        //~ if (entity instanceof ItemEntity) {
            //~ return !CrowdCull.crowded(entity.getX(), entity.getY(), entity.getZ(), false);
        //~ }
        //~ if (entity instanceof Mob && !entity.isVehicle()) {
            //~ return !CrowdCull.crowded(entity.getX(), entity.getY(), entity.getZ(), true);
        //~ }
        //~ return true;
    //~ }

    //~ /**
     //~ * Client light updates: vanilla runs every queued update in one frame (Integer.MAX_VALUE); with
     //~ * the option on they get a per-frame quota, smaller right after a hitch. The rest stay queued.
     //~ */
    //~ @Redirect(method = "renderLevel", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runUpdates(IZZ)I"), require = 0)
    //~ private int aetherium$lightQuota(final LevelLightEngine engine, final int maxUpdates, final boolean skyLight,
                                     //~ final boolean blockLight) {
        //~ return engine.runUpdates(Tuning.lightUpdates ? Math.min(maxUpdates, HitchGuard.lightQuota()) : maxUpdates,
                //~ skyLight, blockLight);
    //~ }

    //~ /** Lighter weather: rain and snow radius 5 (fast graphics) becomes 3. */
    //~ @ModifyConstant(method = "renderSnowAndRain", constant = @Constant(intValue = 5), require = 0)
    //~ private int aetherium$weatherRadiusFast(final int radius) {
        //~ return Tuning.lightWeather ? 3 : radius;
    //~ }

    //~ /** Lighter weather: rain and snow radius 10 (fancy graphics) becomes 6. */
    //~ @ModifyConstant(method = "renderSnowAndRain", constant = @Constant(intValue = 10), require = 0)
    //~ private int aetherium$weatherRadiusFancy(final int radius) {
        //~ return Tuning.lightWeather ? 6 : radius;
    //~ }

    //~ /**
     //~ * Nether roof cull: while the camera is below the bedrock roof, the chunk sections above it
     //~ * (y 128 and up) cannot be seen and are dropped from the visible list. Idempotent, so it is safe
     //~ * whether or not vanilla rebuilt the list this frame.
     //~ */
    //~ @Inject(method = "setupRender", at = @At("TAIL"), require = 0)
    //~ private void aetherium$netherRoof(final Camera camera, final Frustum frustum, final boolean hasCapturedFrustum,
                                      //~ final int frame, final boolean spectator, final CallbackInfo ci) {
        //~ final ClientLevel world = this.level;
        //~ if (!Tuning.dimensionCulling || world == null || world.dimension() != Level.NETHER
                //~ || camera.getPosition().y >= 127.0 || spectator) {
            //~ return;
        //~ }
        //~ final ObjectList<Object> list = this.renderChunks;
        //~ final int n = list.size();
        //~ int kept = 0;
        //~ for (int i = 0; i < n; i++) {
            //~ final Object item = list.get(i);
            //~ if (item instanceof ChunkInfoOrigin && ((ChunkInfoOrigin) item).aetheriumOrigin().getY() >= 128) {
                //~ continue;
            //~ }
            //~ if (kept != i) {
                //~ list.set(kept, item);
            //~ }
            //~ kept++;
        //~ }
        //~ if (kept != n) {
            //~ list.subList(kept, n).clear();
        //~ }
    //~ }
    // @era:qol16-end
}
