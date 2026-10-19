package com.aetherium.client;

import com.aetherium.qol.FoodOverlay;
import com.aetherium.qol.Qol;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Quality of life HUD drawing shared by every Minecraft version: the custom crosshair and the
 * AppleSkin-style hunger bar overlay. QolGuiMixin (and QolHudMixin on 26.2+, where the HUD moved
 * to its own class) only hook vanilla at the right moment and hand over the draw context; the
 * version-specific draws are in {@link QolHudDraw}. Costs nothing while the features are off.
 */
public final class QolHud {
    private QolHud() {
    }

    /** True when the custom crosshair replaces vanilla's sprite this frame. */
    public static boolean crosshair() {
        return Qol.crosshair;
    }

    /**
     * Draws the custom crosshair where vanilla's 15 x 15 sprite would go (x, y = its top left
     * corner, so its middle pixel is the centre). The attack indicator below it stays vanilla.
     */
    public static void crosshair(final McCanvas canvas, final int x, final int y) {
        final int cx = x + 7;
        final int cy = y + 7;
        final int color = Qol.crosshairTarget && aimingAtMob() ? Qol.crosshairTargetArgb : Qol.crosshairArgb;
        final int[] outline = Qol.crosshairOutlineRects;
        final int shadow = color & 0xFF000000;
        for (int i = 0; i + 3 < outline.length; i += 4) {
            canvas.fill(cx + outline[i], cy + outline[i + 1], cx + outline[i + 2], cy + outline[i + 3], shadow);
        }
        final int[] rects = Qol.crosshairRects;
        for (int i = 0; i + 3 < rects.length; i += 4) {
            canvas.fill(cx + rects[i], cy + rects[i + 1], cx + rects[i + 2], cy + rects[i + 3], color);
        }
        QolHudDraw.afterCrosshair();
    }

    private static boolean aimingAtMob() {
        final HitResult hit = Minecraft.getInstance().hitResult;
        return hit != null && hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() instanceof LivingEntity;
    }

    /**
     * Hunger bar overlay in the style of AppleSkin, drawn over vanilla's bar right after it: the
     * saturation you have as a gold outline around the shanks, and while you hold food, the
     * shanks it would fill and the saturation it would add, flashing. One draw for the outline,
     * two small sprites per previewed shank.
     *
     * @param tickCount the HUD's tick counter (vanilla's shank jiggle is seeded from it)
     */
    public static void food(final McCanvas canvas, final int tickCount) {
        if (!Qol.foodHud) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        if (player == null || player.getVehicle() instanceof LivingEntity) {
            return; // riding a horse: the bar shows the horse's health instead
        }
        final FoodData data = player.getFoodData();
        final int food = data.getFoodLevel();
        final float saturation = data.getSaturationLevel();
        final int right = mc.getWindow().getGuiScaledWidth() / 2 + 91;
        final int top = mc.getWindow().getGuiScaledHeight() - 39;
        final int hearts = (int) Math.ceil((player.getMaxHealth() + (float) Math.ceil(player.getAbsorptionAmount())) / 2.0f);
        final int[] shake = QolFoodIcons.SHAKE;
        FoodOverlay.shake(shake, tickCount, (int) Math.ceil(player.getHealth()), hearts, saturation, food);

        int foodAfter = food;
        float saturationAfter = saturation;
        float flash = 0f;
        if (QolFoodInfo.read(player.getMainHandItem()) || QolFoodInfo.read(player.getOffhandItem())) {
            foodAfter = Math.min(20, food + QolFoodInfo.nutrition);
            saturationAfter = FoodOverlay.saturationAfter(food, saturation, QolFoodInfo.nutrition, QolFoodInfo.modifier);
            flash = FoodOverlay.flash(tickCount, 0f);
        }

        // Held food: the shanks it would fill, flashing over a faint highlight (under the outline).
        if (foodAfter > food && flash > 0.01f) {
            for (int i = food / 2; i < (foodAfter + 1) / 2; i++) {
                final int x = right - i * 8 - 9;
                final int y = top + shake[i];
                QolHudDraw.foodSprite(canvas, QolHudDraw.EMPTY, x, y, flash * 0.25f);
                QolHudDraw.foodSprite(canvas, i * 2 + 1 == foodAfter ? QolHudDraw.HALF : QolHudDraw.FULL, x, y, flash);
            }
        }

        // Saturation: the gold outline, solid for what you have, flashing for what the food adds.
        saturationOutline(canvas, right, top, saturation, Math.max(saturation, saturationAfter), flash);
    }

    private static void saturationOutline(final McCanvas canvas, final int right, final int top, final float saturation,
                                          final float after, final float flash) {
        if (after <= 0f) {
            return;
        }
        final int[] outline = QolFoodIcons.outline();
        final int total = outline.length;
        if (total == 0) {
            return;
        }
        final int flashAlpha = (int) (flash * 255f);
        final int[] shake = QolFoodIcons.SHAKE;
        QolHudDraw.beginPixels(canvas);
        for (int i = 0; i < 10; i++) {
            final int shown = FoodOverlay.pixelsFor(saturation / 2f - i, total);
            final int preview = flashAlpha > 2 ? FoodOverlay.pixelsFor(after / 2f - i, total) : 0;
            final int count = Math.max(shown, preview);
            final int x0 = right - i * 8 - 9;
            final int y0 = top + shake[i];
            for (int k = 0; k < count; k++) {
                final int px = FoodOverlay.sx(outline[k]);
                final int py = FoodOverlay.sy(outline[k]);
                final int a = k < shown ? 255 : flashAlpha;
                QolHudDraw.pixel(x0 + px, y0 + py, a << 24 | FoodOverlay.gold(px, py) & 0xFFFFFF);
            }
        }
        QolHudDraw.endPixels();
    }
}
