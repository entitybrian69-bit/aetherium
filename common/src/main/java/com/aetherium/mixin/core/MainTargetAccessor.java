package com.aetherium.mixin.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
// @era:render-scale-begin target|r17|r212|r215|g262|g263
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
// @era:render-scale-else none
// @era:render-scale-end

/**
 * {@code Minecraft.mainRenderTarget} ({@code GameRenderer.mainRenderTarget} on 26.2+), swapped by
 * render scale around the world pass (ScaledRenderTarget).
 */
// @era:render-scale-begin target|r17|r212|r215|none
@Mixin(Minecraft.class)
// @era:render-scale-else g262|g263
//~ @Mixin(GameRenderer.class)
// @era:render-scale-end
public interface MainTargetAccessor {

    // @era:render-scale-begin target|r17|r212|r215|g262|g263
    @Accessor("mainRenderTarget")
    RenderTarget aetheriumMainTarget();

    @Mutable
    @Accessor("mainRenderTarget")
    void aetheriumSetMainTarget(RenderTarget target);
    // @era:render-scale-else none
    // @era:render-scale-end
}
