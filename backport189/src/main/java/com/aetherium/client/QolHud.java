package com.aetherium.client;

import com.aetherium.qol.FoodOverlay;
import com.aetherium.qol.Qol;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.GuiIngameForge;
import org.lwjgl.opengl.GL11;

/**
 * Quality of life HUD on 1.12.2: the custom crosshair, the AppleSkin-style hunger bar overlay and
 * the death coordinates line. Same look as the main mod. Colour rectangles and outline pixels go
 * through one batched draw each, so the overlay costs a handful of draw calls at most.
 */
public final class QolHud {
    // 1.12.2 icons.png: the hunger shank sprites (row v = 27).
    private static final int FOOD_V = 27;
    private static final int FOOD_EMPTY_U = 16;
    private static final int FOOD_FULL_U = 52;
    private static final int FOOD_HALF_U = 61;

    private static final int[] SHAKE = new int[10];
    private static volatile int[] outline;

    private static int foodNutrition;
    private static float foodModifier;

    private QolHud() {
    }

    // ------------------------------------------------------------------ crosshair

    /**
     * Whether the custom crosshair replaces vanilla's this frame: only when vanilla would draw its
     * crosshair at all (first person, no debug axes, not a spectator without a target).
     */
    public static boolean replacesCrosshair(final Minecraft mc) {
        if (!Qol.crosshair || mc.thePlayer == null || mc.gameSettings.thirdPersonView != 0) {
            return false;
        }
        if (mc.gameSettings.showDebugInfo && !mc.gameSettings.hideGUI && !mc.thePlayer.hasReducedDebug()
                && !mc.gameSettings.reducedDebugInfo) {
            return false;
        }
        return mc.playerController == null || !mc.playerController.isSpectator() || mc.pointedEntity != null;
    }

    /** Draws the custom crosshair in the middle of the screen, then vanilla's attack indicator. */
    public static void crosshair(final Minecraft mc, final ScaledResolution res) {
        final int sw = res.getScaledWidth();
        final int sh = res.getScaledHeight();
        final int cx = sw / 2 - 7 + 7;
        final int cy = sh / 2 - 7 + 7;
        final int color = Qol.crosshairTarget && aimingAtMob(mc) ? Qol.crosshairTargetArgb : Qol.crosshairArgb;
        final int shadow = color & 0xFF000000;
        final int[] outlineRects = Qol.crosshairOutlineRects;
        final int[] rects = Qol.crosshairRects;
        beginQuads();
        for (int i = 0; i + 3 < outlineRects.length; i += 4) {
            rect(cx + outlineRects[i], cy + outlineRects[i + 1], cx + outlineRects[i + 2], cy + outlineRects[i + 3], shadow);
        }
        for (int i = 0; i + 3 < rects.length; i += 4) {
            rect(cx + rects[i], cy + rects[i + 1], cx + rects[i + 2], cy + rects[i + 3], color);
        }
        endQuads();
        attackIndicator(mc, sw, sh);
    }

    private static boolean aimingAtMob(final Minecraft mc) {
        final MovingObjectPosition hit = mc.objectMouseOver;
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit instanceof EntityLivingBase;
    }

    /** 1.8.9 has no attack cooldown, so there is no attack indicator to redraw with the crosshair. */
    private static void attackIndicator(final Minecraft mc, final int sw, final int sh) {
    }

    // ------------------------------------------------------------------ food

    /** Reads a food item's nutrition and saturation modifier into the statics; false for non-food. */
    public static boolean readFood(final ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemFood)) {
            return false;
        }
        final ItemFood food = (ItemFood) stack.getItem();
        foodNutrition = food.getHealAmount(stack);
        foodModifier = food.getSaturationModifier(stack);
        return true;
    }

    /** The tooltip line for a food item, or null (same text as the main mod). */
    public static String foodTooltip(final ItemStack stack) {
        if (!Qol.foodTooltip || !readFood(stack)) {
            return null;
        }
        final int nutrition = foodNutrition;
        return "Hunger +" + nutrition + " (" + Qol.oneDecimal(nutrition / 2f) + " shanks)  \u00b7  Saturation +"
                + Qol.oneDecimal(Qol.saturation(nutrition, foodModifier));
    }

    /**
     * Hunger bar overlay in the style of AppleSkin, drawn right after vanilla's bar (Forge's
     * FOOD overlay Post): the saturation you have as a gold outline around the shanks, and while
     * you hold food, the shanks it would fill and the saturation it would add, flashing.
     */
    public static void food(final Minecraft mc, final ScaledResolution res) {
        if (!Qol.foodHud) {
            return;
        }
        final EntityPlayerSP player = mc.thePlayer;
        if (player == null || player.ridingEntity instanceof EntityLivingBase) {
            return;
        }
        final int food = player.getFoodStats().getFoodLevel();
        final float saturation = player.getFoodStats().getSaturationLevel();
        final int right = res.getScaledWidth() / 2 + 91;
        // GuiIngameForge.renderFood: top = height - right_height, then right_height += 10.
        final int top = res.getScaledHeight() - GuiIngameForge.right_height + 10;
        final int tickCount = mc.ingameGUI.getUpdateCounter();
        final int hearts = (int) Math.ceil((player.getMaxHealth() + (float) Math.ceil(player.getAbsorptionAmount())) / 2.0f);
        FoodOverlay.shake(SHAKE, tickCount, (int) Math.ceil(player.getHealth()), hearts, saturation, food);

        int foodAfter = food;
        float saturationAfter = saturation;
        float flash = 0f;
        if (readFood(player.getHeldItem())) {
            foodAfter = Math.min(20, food + foodNutrition);
            saturationAfter = FoodOverlay.saturationAfter(food, saturation, foodNutrition, foodModifier);
            flash = FoodOverlay.flash(tickCount, 0f);
        }

        if (foodAfter > food && flash > 0.01f) {
            mc.getTextureManager().bindTexture(Gui.icons);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            for (int i = food / 2; i < (foodAfter + 1) / 2; i++) {
                final int x = right - i * 8 - 9;
                final int y = top + SHAKE[i];
                GlStateManager.color(1f, 1f, 1f, flash * 0.25f);
                mc.ingameGUI.drawTexturedModalRect(x, y, FOOD_EMPTY_U, FOOD_V, 9, 9);
                GlStateManager.color(1f, 1f, 1f, flash);
                mc.ingameGUI.drawTexturedModalRect(x, y, i * 2 + 1 == foodAfter ? FOOD_HALF_U : FOOD_FULL_U, FOOD_V, 9, 9);
            }
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        saturationOutline(mc, right, top, saturation, Math.max(saturation, saturationAfter), flash);
    }

    private static void saturationOutline(final Minecraft mc, final int right, final int top, final float saturation,
                                          final float after, final float flash) {
        if (after <= 0f) {
            return;
        }
        final int[] pixels = outline(mc);
        final int total = pixels.length;
        if (total == 0) {
            return;
        }
        final int flashAlpha = (int) (flash * 255f);
        beginQuads();
        for (int i = 0; i < 10; i++) {
            final int shown = FoodOverlay.pixelsFor(saturation / 2f - i, total);
            final int preview = flashAlpha > 2 ? FoodOverlay.pixelsFor(after / 2f - i, total) : 0;
            final int count = Math.max(shown, preview);
            final int x0 = right - i * 8 - 9;
            final int y0 = top + SHAKE[i];
            for (int k = 0; k < count; k++) {
                final int px = FoodOverlay.sx(pixels[k]);
                final int py = FoodOverlay.sy(pixels[k]);
                final int a = k < shown ? 255 : flashAlpha;
                rect(x0 + px, y0 + py, x0 + px + 1, y0 + py + 1, a << 24 | FoodOverlay.gold(px, py) & 0xFFFFFF);
            }
        }
        endQuads();
    }

    /** Ordered outline pixels of the (resource pack's) full shank sprite, traced once. */
    private static int[] outline(final Minecraft mc) {
        int[] result = outline;
        if (result == null) {
            result = FoodOverlay.outline(silhouette(mc));
            if (result.length == 0) {
                result = FoodOverlay.outline(FoodOverlay.fallbackSilhouette());
            }
            outline = result;
        }
        return result;
    }

    /** Forgets the traced outline (resource packs changed). */
    public static void invalidate() {
        outline = null;
    }

    private static boolean[] silhouette(final Minecraft mc) {
        try (InputStream in = mc.getResourceManager().getResource(Gui.icons).getInputStream()) {
            final BufferedImage image = ImageIO.read(in);
            if (image == null) {
                return FoodOverlay.fallbackSilhouette();
            }
            final int scale = Math.max(1, image.getWidth() / 256);
            final int size = FoodOverlay.SIZE;
            final boolean[] solid = new boolean[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    final int sx = (FOOD_FULL_U + x) * scale;
                    final int sy = (FOOD_V + y) * scale;
                    if (sx < image.getWidth() && sy < image.getHeight()) {
                        solid[y * size + x] = (image.getRGB(sx, sy) >>> 24) > 0;
                    }
                }
            }
            return solid;
        } catch (final Exception | LinkageError e) {
            return FoodOverlay.fallbackSilhouette();
        }
    }

    // ------------------------------------------------------------------ death screen

    /** The "You died at ..." line under the death screen's score (same spot as the main mod). */
    public static void deathLine(final Minecraft mc, final int width) {
        final String line = QolClient.deathLine(mc);
        if (line == null) {
            return;
        }
        final int x = width / 2 - mc.fontRendererObj.getStringWidth(line) / 2;
        mc.fontRendererObj.drawString(line, x + 1, 113, 0xFF3F2A15);
        mc.fontRendererObj.drawString(line, x, 112, 0xFFFFAA55);
    }

    // ------------------------------------------------------------------ batched rectangles

    private static void beginQuads() {
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator.getInstance().getWorldRenderer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
    }

    private static void rect(final int x0, final int y0, final int x1, final int y1, final int argb) {
        final WorldRenderer buffer = Tessellator.getInstance().getWorldRenderer();
        final int a = argb >>> 24;
        final int r = argb >> 16 & 0xFF;
        final int g = argb >> 8 & 0xFF;
        final int b = argb & 0xFF;
        buffer.pos(x0, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.pos(x1, y0, 0).color(r, g, b, a).endVertex();
        buffer.pos(x0, y0, 0).color(r, g, b, a).endVertex();
    }

    private static void endQuads() {
        Tessellator.getInstance().draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
    }
}
