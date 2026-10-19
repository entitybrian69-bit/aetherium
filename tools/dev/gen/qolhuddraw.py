import sys
sys.path.insert(0, "tools/dev/gen")
from eragen import block

ICON_LABELS = "v16|s17|s194|g20"
ids = block("q-hud", [
    ("g21|g212|g216", '''    private static final net.minecraft.resources.ResourceLocation[] SPRITES = {
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/food_empty"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/food_half"),
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/food_full")};
'''),
    (ICON_LABELS, '''    private static final net.minecraft.resources.ResourceLocation ICONS =
            new net.minecraft.resources.ResourceLocation("textures/gui/icons.png");
    /** Column of each kind in icons.png's food row (y 27): flashing empty, half, full. */
    private static final int[] U = {25, 61, 52};
'''),
    ("g202", '''    private static final net.minecraft.resources.ResourceLocation[] SPRITES = {
            new net.minecraft.resources.ResourceLocation("hud/food_empty"),
            new net.minecraft.resources.ResourceLocation("hud/food_half"),
            new net.minecraft.resources.ResourceLocation("hud/food_full")};
'''),
    ("g2111|x261|x262|x263", '''    private static final net.minecraft.resources.Identifier[] SPRITES = {
            net.minecraft.resources.Identifier.withDefaultNamespace("hud/food_empty"),
            net.minecraft.resources.Identifier.withDefaultNamespace("hud/food_half"),
            net.minecraft.resources.Identifier.withDefaultNamespace("hud/food_full")};
'''),
    ("none", ""),
])

sprite = block("q-hud", [
    ("g21|g202", '''        final net.minecraft.client.gui.GuiGraphics g = canvas.graphics();
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, alpha);
        g.blitSprite(SPRITES[kind], x, y, 9, 9);
        g.setColor(1f, 1f, 1f, 1f);
'''),
    ("v16", '''        net.minecraft.client.Minecraft.getInstance().getTextureManager().bind(ICONS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1f, 1f, 1f, alpha);
        net.minecraft.client.gui.GuiComponent.blit(canvas.pose(), x, y, 0, (float) U[kind], 27f, 9, 9, 256, 256);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
'''),
    ("s17|s194", '''        RenderSystem.setShaderTexture(0, ICONS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        net.minecraft.client.gui.GuiComponent.blit(canvas.pose(), x, y, 0, (float) U[kind], 27f, 9, 9, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
'''),
    ("g20", '''        final net.minecraft.client.gui.GuiGraphics g = canvas.graphics();
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(ICONS, x, y, U[kind], 27, 9, 9);
        g.setColor(1f, 1f, 1f, 1f);
'''),
    ("g212", '''        canvas.graphics().blitSprite(net.minecraft.client.renderer.RenderType::guiTextured, SPRITES[kind], x, y, 9, 9,
                white(alpha));
'''),
    ("g216|g2111|x261|x262|x263", '''        canvas.graphics().blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, SPRITES[kind], x, y, 9, 9,
                white(alpha));
'''),
    ("none", ""),
], indent=8)

state = block("q-hud", [
    ("g21|g202|g20", '''    private static com.mojang.blaze3d.vertex.VertexConsumer consumer;
    private static com.mojang.blaze3d.vertex.PoseStack.Pose pose;
'''),
    ("v16|s17|s194", '''    private static com.mojang.blaze3d.vertex.BufferBuilder buffer;
    private static com.mojang.blaze3d.vertex.PoseStack.Pose pose;
'''),
    ("g212|g216|g2111|x261|x262|x263|none", ""),
])

begin = block("q-hud", [
    ("g21|g202|g20", '''        consumer = canvas.graphics().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());
        pose = canvas.graphics().pose().last();
'''),
    ("v16", '''        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        buffer.begin(GL_QUADS, com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        pose = canvas.pose().last();
'''),
    ("s17|s194", '''        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
        buffer = com.mojang.blaze3d.vertex.Tesselator.getInstance().getBuilder();
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,
                com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
        pose = canvas.pose().last();
'''),
    ("g212|g216|g2111|x261|x262|x263|none", ""),
], indent=8)

pixel = block("q-hud", [
    ("g21", '''        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        final int a = argb >>> 24;
        consumer.addVertex(pose.pose(), x, y + 1f, 0f).setColor(r, g, b, a);
        consumer.addVertex(pose.pose(), x + 1f, y + 1f, 0f).setColor(r, g, b, a);
        consumer.addVertex(pose.pose(), x + 1f, y, 0f).setColor(r, g, b, a);
        consumer.addVertex(pose.pose(), x, y, 0f).setColor(r, g, b, a);
'''),
    ("g202|g20", '''        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        final int a = argb >>> 24;
        consumer.vertex(pose.pose(), x, y + 1f, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), x + 1f, y + 1f, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), x + 1f, y, 0f).color(r, g, b, a).endVertex();
        consumer.vertex(pose.pose(), x, y, 0f).color(r, g, b, a).endVertex();
'''),
    ("v16|s17|s194", '''        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        final int a = argb >>> 24;
        buffer.vertex(pose.pose(), x, y + 1f, 0f).color(r, g, b, a).endVertex();
        buffer.vertex(pose.pose(), x + 1f, y + 1f, 0f).color(r, g, b, a).endVertex();
        buffer.vertex(pose.pose(), x + 1f, y, 0f).color(r, g, b, a).endVertex();
        buffer.vertex(pose.pose(), x, y, 0f).color(r, g, b, a).endVertex();
'''),
    ("g212|g216|g2111|x261|x262|x263", '''        // The GUI batches fills itself on this version: one draw for all of them.
        canvas.fill(x, y, x + 1, y + 1, argb);
'''),
    ("none", ""),
], indent=8)

end = block("q-hud", [
    ("g21|g202|g20", '''        canvas.graphics().flush();
        consumer = null;
        pose = null;
'''),
    ("v16", '''        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
        RenderSystem.enableTexture();
        buffer = null;
        pose = null;
'''),
    ("s17|s194", '''        com.mojang.blaze3d.vertex.Tesselator.getInstance().end();
        buffer = null;
        pose = null;
'''),
    ("g212|g216|g2111|x261|x262|x263|none", ""),
], indent=8)

after = block("q-hud", [
    ("v16|s17|s194|g20|g202|g21", '''        // Back to the inverting blend vanilla set up for the attack indicator drawn next.
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);
'''),
    ("g212|g216|g2111|x261|x262|x263|none", ""),
], indent=8)

imports = block("q-hud", [
    ("v16|s17|s194|g20|g202|g21", "import com.mojang.blaze3d.systems.RenderSystem;\n"),
    ("g212|g216|g2111|x261|x262|x263|none", ""),
], indent=0)

src = '''package com.aetherium.client;

''' + imports + '''
/**
 * The two vanilla draws the Quality of life HUD needs beyond the canvas's rectangles and text, in
 * the API of each Minecraft version: a hunger shank sprite with transparency (the held-food
 * preview), and a batch of single pixels (the saturation outline) sent as one draw. Before 1.21.2
 * every vanilla fill is its own draw call, so the pixels go into one buffer by hand there; later
 * versions batch GUI fills themselves. Render thread only, no allocation.
 */
public final class QolHudDraw {
    /** Sprite kinds for {@link #foodSprite}. */
    public static final int EMPTY = 0;
    public static final int HALF = 1;
    public static final int FULL = 2;

    private static final int GL_QUADS = 7;
    private static final int GL_ZERO = 0;
    private static final int GL_ONE = 1;
    private static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    private static final int GL_ONE_MINUS_DST_COLOR = 0x0307;

''' + ids + '''
    private static McCanvas canvas;
''' + state + '''
    private QolHudDraw() {
    }

    /** A hunger shank (vanilla's sprite or the resource pack's) at x, y with this opacity. */
    public static void foodSprite(final McCanvas canvas, final int kind, final int x, final int y, final float alpha) {
''' + sprite + '''    }

    /** Starts a batch of {@link #pixel} calls (no text or sprites until {@link #endPixels}). */
    public static void beginPixels(final McCanvas target) {
        canvas = target;
''' + begin + '''    }

    public static void pixel(final int x, final int y, final int argb) {
''' + pixel + '''    }

    public static void endPixels() {
''' + end + '''        canvas = null;
    }

    /** After the custom crosshair replaced vanilla's sprite: the GL state vanilla expects next. */
    public static void afterCrosshair() {
''' + after + '''    }

    private static int white(final float alpha) {
        return (Math.max(0, Math.min(255, (int) (alpha * 255f))) << 24) | 0xFFFFFF;
    }
}
'''
open("common/src/main/java/com/aetherium/client/QolHudDraw.java", "w").write(src)
