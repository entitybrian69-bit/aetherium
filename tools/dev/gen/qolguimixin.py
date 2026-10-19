import sys
sys.path.insert(0, "tools/dev/gen")
from eragen import block

GG = "net.minecraft.client.gui.GuiGraphics"
GX = "net.minecraft.client.gui.GuiGraphicsExtractor"
PS = "com.mojang.blaze3d.vertex.PoseStack"
RL = "net.minecraft.resources.ResourceLocation"
ID = "net.minecraft.resources.Identifier"
RP = "com.mojang.blaze3d.pipeline.RenderPipeline"


def crosshair(method, target, params, fallback, begin):
    return '''    /** The crosshair sprite itself (the first sprite drawn; the attack indicator after it stays vanilla). */
    @Redirect(method = "%s", at = @At(value = "INVOKE",
            target = "%s", ordinal = 0),
            require = 0)
    private void aetherium$crosshair(%s) {
        if (!QolHud.crosshair()) {
            %s;
            return;
        }
        QolHud.crosshair(McCanvas.INSTANCE.begin(%s), x, y);
    }
''' % (method, target, params, fallback, begin)


def food(method, ctx):
    return '''    /** Right after vanilla's health and hunger bars: the AppleSkin-style overlay. */
    @Inject(method = "%s", at = @At("TAIL"), require = 0)
    private void aetherium$foodBar(final %s context, final CallbackInfo ci) {
        QolHud.food(McCanvas.INSTANCE.begin(context), this.tickCount);
    }
''' % (method, ctx)


def variant(cross, foodm, ctx):
    return cross + "\n" + food(foodm, ctx)


ints4 = "final int x, final int y, final int w, final int h"
ints6 = "final int x, final int y, final int u, final int v, final int w, final int h"

V = {}
V["v16|s17"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V",
    "final net.minecraft.client.gui.Gui gui, final %s pose, %s" % (PS, ints6),
    "gui.blit(pose, x, y, u, v, w, h)", "pose"), "renderPlayerHealth", PS)
V["s194"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V",
    "final %s pose, %s" % (PS, ints6),
    "net.minecraft.client.gui.GuiComponent.blit(pose, x, y, u, v, w, h)", "pose"), "renderPlayerHealth", PS)
V["g20"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
    "final %s graphics, final %s texture, %s" % (GG, RL, ints6),
    "graphics.blit(texture, x, y, u, v, w, h)", "graphics"), "renderPlayerHealth", GG)
V["g202|g21"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
    "final %s graphics, final %s sprite, %s" % (GG, RL, ints4),
    "graphics.blitSprite(sprite, x, y, w, h)", "graphics"), "renderPlayerHealth", GG)
V["g212"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V",
    "final %s graphics, final java.util.function.Function<%s, net.minecraft.client.renderer.RenderType> type,\n"
    "                                     final %s sprite, %s" % (GG, RL, RL, ints4),
    "graphics.blitSprite(type, sprite, x, y, w, h)", "graphics"), "renderPlayerHealth", GG)
V["g216"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V",
    "final %s graphics, final %s pipeline, final %s sprite,\n                                     %s" % (GG, RP, RL, ints4),
    "graphics.blitSprite(pipeline, sprite, x, y, w, h)", "graphics"), "renderPlayerHealth", GG)
V["g2111"] = variant(crosshair("renderCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
    "final %s graphics, final %s pipeline, final %s sprite,\n                                     %s" % (GG, RP, ID, ints4),
    "graphics.blitSprite(pipeline, sprite, x, y, w, h)", "graphics"), "renderPlayerHealth", GG)
V["x261"] = variant(crosshair("extractCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
    "final %s graphics, final %s pipeline, final %s sprite,\n                                     %s" % (GX, RP, ID, ints4),
    "graphics.blitSprite(pipeline, sprite, x, y, w, h)", "graphics"), "extractPlayerHealth", GX)

RPX = "com.mojang.renderpearl.api.pipeline.RenderPipeline"
V["x263"] = variant(crosshair("extractCrosshair",
    "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
    "final %s graphics, final %s pipeline, final %s sprite,\n                                     %s" % (GX, RPX, ID, ints4),
    "graphics.blitSprite(pipeline, sprite, x, y, w, h)", "graphics"), "extractPlayerHealth", GX)

order = ["g202|g21", "v16|s17", "s194", "g20", "g212", "g216", "g2111", "x261"]
body = block("q-hud", [(k, V[k]) for k in order] + [("x262|x263|none", "")])

GUI_LABELS = "v16|s17|s194|g20|g202|g21|g212|g216|g2111|x261"
src = '''package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
''' + block("q-hud", [(GUI_LABELS, '''import com.aetherium.client.McCanvas;
import com.aetherium.client.QolHud;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
'''), ("x262|x263|none", "")], indent=0) + '''
/**
 * Custom crosshair and AppleSkin-style hunger bar (Quality of life, every version up to 26.1; on
 * 26.2+ the HUD is its own class, see QolHudMixin). Two hooks in vanilla's HUD: the crosshair
 * sprite is replaced while the custom crosshair is on, and the hunger overlay is drawn right after
 * the health and hunger bars. The drawing is shared ({@link com.aetherium.client.QolHud}); only
 * the hook signatures follow the GUI API of each version.
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class QolGuiMixin {
''' + block("q-hud", [(GUI_LABELS, '''    @Shadow
    private int tickCount;

'''), ("x262|x263|none", "")]) + body + '''}
'''
open("common/src/main/java/com/aetherium/mixin/core/QolGuiMixin.java", "w").write(src)

# 26.2+: the HUD moved to net.minecraft.client.gui.Hud (26.3: RenderPipeline in renderpearl).
HUD_OLD = GUI_LABELS + "|none"
hud = '''package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
''' + block("q-hud", [("x262|x263", '''import com.aetherium.client.FrameHud;
import com.aetherium.client.McCanvas;
import com.aetherium.client.QolHud;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
'''), (HUD_OLD, "")], indent=0) + '''
/**
 * Minecraft 26.2+ moved the in-game HUD out of Gui into its own class, Hud. Same hooks as GuiMixin
 * and QolGuiMixin have on older versions: the frame-time box and Quality of life HUD lines and
 * overlays after the HUD is extracted, the custom crosshair in place of vanilla's sprite, and the
 * hunger overlay after the health and hunger bars.
 */
@Mixin(targets = "net.minecraft.client.gui.Hud")
public abstract class QolHudMixin {
''' + block("q-hud", [("x262|x263", '''    @Shadow
    private int tickCount;

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void aetherium$hud(final net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                               final net.minecraft.client.DeltaTracker delta, final CallbackInfo ci) {
        FrameHud.render(McCanvas.INSTANCE.begin(graphics));
    }
'''), (HUD_OLD, "")]) + "\n" + block("q-hud", [("x262", V["x261"]), ("x263", V["x263"]), (HUD_OLD, "")]) + '''}
'''
open("common/src/main/java/com/aetherium/mixin/core/QolHudMixin.java", "w").write(hud)
