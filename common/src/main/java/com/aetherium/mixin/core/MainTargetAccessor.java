package com.aetherium.mixin.core;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
// @era:render-scale-begin target|r17|r212|r215
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
// @era:render-scale-else none
// @era:render-scale-end

/** {@code Minecraft.mainRenderTarget}, swapped by render scale around the world pass (ScaledRenderTarget). */
@Mixin(Minecraft.class)
public interface MainTargetAccessor {

    // @era:render-scale-begin target|r17|r212|r215
    @Accessor("mainRenderTarget")
    RenderTarget aetheriumMainTarget();

    @Mutable
    @Accessor("mainRenderTarget")
    void aetheriumSetMainTarget(RenderTarget target);
    // @era:render-scale-else none
    // @era:render-scale-end
}
