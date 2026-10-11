package com.aetherium.mixin.core;

import com.aetherium.client.ClientHooks;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Occlusion culling for entities: after vanilla's frustum test said "visible", skip the entity
 * anyway when every section its culling box touches is missing from vanilla's visible-section
 * list (the same list that decides which terrain is drawn).
 *
 * <p>The hook sits in the base {@code EntityRenderer.shouldRender}, so renderers that override it
 * without calling super (ender dragon, lightning) are never affected, and renderers that extend it
 * (mob leashes, guardian beams) still run their own "visible anyway" checks after ours.</p>
 *
 * <p>Every version: vanilla only frustum-tests entities (1.21.11+ also waits for the section's
 * fade-in), it never asks whether the section is occluded.</p>
 */
@Mixin(EntityRenderer.class)
public abstract class EntityOcclusionMixin {

    // @era:occlusion-box-begin entity
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$occlusion(final Entity entity, final Frustum frustum, final double camX, final double camY,
                                     final double camZ, final CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && !entity.noCulling
                && ClientHooks.hiddenBehindTerrain(entity, entity.getBoundingBoxForCulling(), camX, camY, camZ)) {
            cir.setReturnValue(Boolean.FALSE);
        }
    }
    // @era:occlusion-box-else renderer
    //~ @Shadow
    //~ protected abstract boolean affectedByCulling(Entity entity);
    //~ @Shadow
    //~ protected abstract AABB getBoundingBoxForCulling(Entity entity);
    //~ @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
    //~ private void aetherium$occlusion(final Entity entity, final Frustum frustum, final double camX, final double camY,
                                     //~ final double camZ, final CallbackInfoReturnable<Boolean> cir) {
        //~ if (Boolean.TRUE.equals(cir.getReturnValue()) && this.affectedByCulling(entity)
                //~ && ClientHooks.hiddenBehindTerrain(entity, this.getBoundingBoxForCulling(entity), camX, camY, camZ)) {
            //~ cir.setReturnValue(Boolean.FALSE);
        //~ }
    //~ }
    // @era:occlusion-box-else renderer-tick
    //~ @Shadow
    //~ protected abstract boolean affectedByCulling(Entity entity);

    //~ @Shadow
    //~ protected abstract AABB getBoundingBoxForCulling(Entity entity, float partialTick);

    //~ @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
    //~ private void aetherium$occlusion(final Entity entity, final Frustum frustum, final double camX, final double camY,
                                     //~ final double camZ, final float partialTick, final CallbackInfoReturnable<Boolean> cir) {
        //~ if (Boolean.TRUE.equals(cir.getReturnValue()) && this.affectedByCulling(entity)
                //~ && ClientHooks.hiddenBehindTerrain(entity, this.getBoundingBoxForCulling(entity, partialTick), camX, camY, camZ)) {
            //~ cir.setReturnValue(Boolean.FALSE);
        //~ }
    //~ }
    // @era:occlusion-box-end
}
