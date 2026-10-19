package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hud-begin v16
import com.aetherium.client.McCanvas;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.qol.Qol;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hud-else none
// @era:q-hud-end

/**
 * HUD parts of the Quality of life tab (Minecraft 1.16.5): the custom crosshair, and the food
 * bar additions (saturation tint, preview of what the held food restores).
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class QolGuiMixin {
    // @era:q-hud-begin v16
    @Shadow
    private int screenWidth;

    @Shadow
    private int screenHeight;

    @Shadow
    private int tickCount;

    // ------------------------------------------------------------------ crosshair

    /** The crosshair sprite itself (the first blit; the attack indicator below it stays vanilla). */
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", ordinal = 0),
            require = 0)
    private void aetherium$crosshair(final Gui gui, final PoseStack pose, final int x, final int y, final int u,
                                     final int v, final int w, final int h) {
        if (!Qol.crosshair) {
            gui.blit(pose, x, y, u, v, w, h);
            return;
        }
        // Vanilla's sprite is 15x15 at x, y: its middle pixel is the centre.
        final int cx = x + 7;
        final int cy = y + 7;
        final int color = Qol.crosshairTarget && aetherium$aimingAtMob() ? Qol.crosshairTargetArgb : Qol.crosshairArgb;
        RenderSystem.defaultBlendFunc();
        final GuiCanvas canvas = McCanvas.INSTANCE.begin(pose);
        final int[] outline = Qol.crosshairOutlineRects;
        final int shadow = color & 0xFF000000;
        for (int i = 0; i + 3 < outline.length; i += 4) {
            canvas.fill(cx + outline[i], cy + outline[i + 1], cx + outline[i + 2], cy + outline[i + 3], shadow);
        }
        final int[] rects = Qol.crosshairRects;
        for (int i = 0; i + 3 < rects.length; i += 4) {
            canvas.fill(cx + rects[i], cy + rects[i + 1], cx + rects[i + 2], cy + rects[i + 3], color);
        }
        // Back to the state vanilla set up for the attack indicator that follows.
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static boolean aetherium$aimingAtMob() {
        final HitResult hit = Minecraft.getInstance().hitResult;
        return hit != null && hit.getType() == HitResult.Type.ENTITY
                && ((EntityHitResult) hit).getEntity() instanceof LivingEntity;
    }

    // ------------------------------------------------------------------ food bar

    @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    private void aetherium$foodBar(final PoseStack pose, final CallbackInfo ci) {
        if (!Qol.foodHud) {
            return;
        }
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getVehicle() instanceof LivingEntity) {
            return; // riding a horse: the bar shows the horse's health instead
        }
        final FoodData data = player.getFoodData();
        final int food = data.getFoodLevel();
        final float saturation = data.getSaturationLevel();
        final int right = this.screenWidth / 2 + 91;
        final int top = this.screenHeight - 39;
        final GuiCanvas canvas = McCanvas.INSTANCE.begin(pose);

        // Saturation: a gold tint over as many shanks as there are saturation points (2 per shank).
        for (int i = 0; i < 10 && saturation > i * 2f; i++) {
            final int x = right - i * 8 - 9;
            final boolean half = saturation - i * 2f < 1.5f;
            canvas.fill(half ? x + 5 : x + 1, top + 1, x + 8, top + 8, 0x66FFC21A);
            canvas.fill(half ? x + 5 : x + 1, top + 8, x + 8, top + 9, 0xCCFFC21A);
        }

        // Held food: the shanks it would fill, pulsing.
        final int nutrition = aetherium$heldNutrition(player);
        if (nutrition <= 0 || food >= 20) {
            return;
        }
        final int after = Math.min(20, food + nutrition);
        final float alpha = 0.35f + 0.3f * (float) Math.sin((this.tickCount % 40) / 40.0 * Math.PI * 2.0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1f, 1f, 1f, 0.35f + alpha);
        final Gui gui = (Gui) (Object) this;
        for (int i = 0; i < 10; i++) {
            final int full = i * 2 + 2;
            if (food >= full || after < full - 1) {
                continue;
            }
            final int x = right - i * 8 - 9;
            gui.blit(pose, x, top, after >= full ? 52 : 61, 27, 9, 9);
        }
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private static int aetherium$heldNutrition(final LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!stack.getItem().isEdible()) {
            stack = player.getOffhandItem();
            if (!stack.getItem().isEdible()) {
                return 0;
            }
        }
        final FoodProperties food = stack.getItem().getFoodProperties();
        return food == null ? 0 : food.getNutrition();
    }
    // @era:q-hud-else none
    // @era:q-hud-end
}
