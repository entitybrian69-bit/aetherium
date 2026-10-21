package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.world.entity.Entity;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:boost16-end

/** HUD refresh rate (1.17.2, Minecraft 1.16.5): the HUD parts that are drawn live every frame. */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public interface TuningHudInvoker {
    // @era:boost16-begin none
    // @era:boost16-else v16
    //~ @Invoker("renderVignette")
    //~ void aetherium$renderVignette(Entity entity);

    //~ @Invoker("renderCrosshair")
    //~ void aetherium$renderCrosshair(PoseStack pose);
    // @era:boost16-end
}
