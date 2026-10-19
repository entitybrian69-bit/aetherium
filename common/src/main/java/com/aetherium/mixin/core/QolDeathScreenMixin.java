package com.aetherium.mixin.core;

import com.aetherium.client.McCanvas;
import com.aetherium.client.QolClient;
import com.aetherium.gui.GuiCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Death point (Quality of life, every version): the death screen shows where you died, under the
 * score line, so you can find your items again. Drawn through the shared canvas; only the render
 * method's draw context follows the GUI API of the version.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.DeathScreen")
public abstract class QolDeathScreenMixin {
    // @era:gui-begin graphics
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void aetherium$deathPoint(final net.minecraft.client.gui.GuiGraphics context, final int mouseX, final int mouseY,
                                      final float partialTick, final CallbackInfo ci) {
        aetherium$draw(McCanvas.INSTANCE.begin(context));
    }
    // @era:gui-else stack
    //~ @Inject(method = "render", at = @At("TAIL"), require = 0)
    //~ private void aetherium$deathPoint(final com.mojang.blaze3d.vertex.PoseStack context, final int mouseX, final int mouseY,
                                      //~ final float partialTick, final CallbackInfo ci) {
        //~ aetherium$draw(McCanvas.INSTANCE.begin(context));
    //~ }
    // @era:gui-else extractor
    //~ @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    //~ private void aetherium$deathPoint(final net.minecraft.client.gui.GuiGraphicsExtractor context, final int mouseX, final int mouseY,
                                      //~ final float partialTick, final CallbackInfo ci) {
        //~ aetherium$draw(McCanvas.INSTANCE.begin(context));
    //~ }
    // @era:gui-end

    private void aetherium$draw(final GuiCanvas canvas) {
        final String line = QolClient.deathLine(Minecraft.getInstance());
        if (line == null) {
            return;
        }
        final int x = ((Screen) (Object) this).width / 2 - canvas.textWidth(line) / 2;
        canvas.text(line, x + 1, 113, 0xFF3F2A15); // vanilla's text shadow: the colour at a quarter
        canvas.text(line, x, 112, 0xFFFFAA55);
    }
}
