package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-crowd-begin c17
import com.aetherium.perf.CrowdCull;
import com.aetherium.perf.Tuning;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:t-crowd-else none
// @era:t-crowd-end

/**
 * Crowd culling (1.11.0) on 1.17+: an entity that vanilla would draw is skipped once its block
 * space already holds {@link CrowdCull#ITEM_CAP} items or {@link CrowdCull#MOB_CAP} mobs this
 * frame. Named and glowing entities always render.
 *
 * <p>On 1.16.5 {@code TuningLevelMixin} redirects the call in {@code renderLevel}; from 1.17 the
 * caller moves between versions (renderLevel, collectVisibleEntities, extractVisibleEntities, and
 * out of LevelRenderer on 26.2+), so the result of {@code EntityRenderDispatcher.shouldRender}
 * itself is filtered. Only the level renderer asks it, once per entity and frame; the budget is
 * reset at frame start ({@code ClientHooks.onFrameStart}).</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.EntityRenderDispatcher")
public abstract class TuningCrowdMixin {
    // @era:t-crowd-begin c17
    @Inject(method = "shouldRender", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherium$crowd(final CallbackInfoReturnable<Boolean> cir,
                                 @Local(argsOnly = true) final Entity entity) {
        if (!Tuning.crowdCull || !cir.getReturnValueZ()) {
            return;
        }
        if (entity.hasCustomName() || entity.isCurrentlyGlowing()) {
            return;
        }
        if (entity instanceof ItemEntity) {
            if (CrowdCull.crowded(entity.getX(), entity.getY(), entity.getZ(), false)) {
                cir.setReturnValue(Boolean.FALSE);
            }
        } else if (entity instanceof Mob && !entity.isVehicle()) {
            if (CrowdCull.crowded(entity.getX(), entity.getY(), entity.getZ(), true)) {
                cir.setReturnValue(Boolean.FALSE);
            }
        }
    }
    // @era:t-crowd-else none
    // @era:t-crowd-end
}
