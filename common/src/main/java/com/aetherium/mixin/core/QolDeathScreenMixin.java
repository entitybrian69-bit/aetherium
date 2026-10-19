package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-world-begin none
// @era:q-world-else v16
//~ import com.aetherium.client.QolClient;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.GuiComponent;
//~ import net.minecraft.client.gui.screens.Screen;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-world-end

/**
 * Death point (Quality of life, Minecraft 1.16.5): the death screen shows where you died, under
 * the score. The spot is also saved as the Death waypoint (QolClient).
 */
@Mixin(targets = "net.minecraft.client.gui.screens.DeathScreen")
public abstract class QolDeathScreenMixin {
    // @era:q-world-begin none
    // @era:q-world-else v16
    //~ @Inject(method = "render", at = @At("TAIL"), require = 0)
    //~ private void aetherium$deathPoint(final PoseStack pose, final int mouseX, final int mouseY, final float partialTick,
                                      //~ final CallbackInfo ci) {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ final String line = QolClient.deathLine(mc);
        //~ if (line != null) {
            //~ GuiComponent.drawCenteredString(pose, mc.font, line, ((Screen) (Object) this).width / 2, 112, 0xFFFFAA55);
        //~ }
    //~ }
    // @era:q-world-end
}
