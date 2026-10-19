package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-hud-begin none
// @era:q-hud-else v16
//~ import com.aetherium.client.McCanvas;
//~ import com.aetherium.gui.GuiCanvas;
//~ import com.aetherium.client.QolFoodIcons;
//~ import com.aetherium.qol.FoodOverlay;
//~ import com.aetherium.qol.Qol;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.Tesselator;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.gui.GuiComponent;
//~ import com.mojang.blaze3d.systems.RenderSystem;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.Gui;
//~ import net.minecraft.client.player.LocalPlayer;
//~ import net.minecraft.world.entity.LivingEntity;
//~ import net.minecraft.world.food.FoodData;
//~ import net.minecraft.world.food.FoodProperties;
//~ import net.minecraft.world.item.ItemStack;
//~ import net.minecraft.world.phys.EntityHitResult;
//~ import net.minecraft.world.phys.HitResult;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:q-hud-end

/**
 * HUD parts of the Quality of life tab (Minecraft 1.16.5): the custom crosshair, and the food
 * bar overlay (AppleSkin-style saturation outline, flashing preview of what the held food restores).
 */
@Mixin(targets = "net.minecraft.client.gui.Gui")
public abstract class QolGuiMixin {
    // @era:q-hud-begin none
    // @era:q-hud-else v16
    //~ private static final int GL_ZERO = 0;
    //~ private static final int GL_ONE = 1;
    //~ private static final int GL_ONE_MINUS_SRC_COLOR = 0x0301;
    //~ private static final int GL_ONE_MINUS_DST_COLOR = 0x0307;

    //~ @Shadow
    //~ private int screenWidth;

    //~ @Shadow
    //~ private int screenHeight;

    //~ @Shadow
    //~ private int tickCount;

    //~ // ------------------------------------------------------------------ crosshair

    //~ /** The crosshair sprite itself (the first blit; the attack indicator below it stays vanilla). */
    //~ @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V", ordinal = 0),
            //~ require = 0)
    //~ private void aetherium$crosshair(final Gui gui, final PoseStack pose, final int x, final int y, final int u,
                                     //~ final int v, final int w, final int h) {
        //~ if (!Qol.crosshair) {
            //~ gui.blit(pose, x, y, u, v, w, h);
            //~ return;
        //~ }
        //~ // Vanilla's sprite is 15x15 at x, y: its middle pixel is the centre.
        //~ final int cx = x + 7;
        //~ final int cy = y + 7;
        //~ final int color = Qol.crosshairTarget && aetherium$aimingAtMob() ? Qol.crosshairTargetArgb : Qol.crosshairArgb;
        //~ RenderSystem.defaultBlendFunc();
        //~ final GuiCanvas canvas = McCanvas.INSTANCE.begin(pose);
        //~ final int[] outline = Qol.crosshairOutlineRects;
        //~ final int shadow = color & 0xFF000000;
        //~ for (int i = 0; i + 3 < outline.length; i += 4) {
            //~ canvas.fill(cx + outline[i], cy + outline[i + 1], cx + outline[i + 2], cy + outline[i + 3], shadow);
        //~ }
        //~ final int[] rects = Qol.crosshairRects;
        //~ for (int i = 0; i + 3 < rects.length; i += 4) {
            //~ canvas.fill(cx + rects[i], cy + rects[i + 1], cx + rects[i + 2], cy + rects[i + 3], color);
        //~ }
        //~ // Back to the state vanilla set up for the attack indicator that follows.
        //~ RenderSystem.enableBlend();
        //~ RenderSystem.blendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);
    //~ }

    //~ private static boolean aetherium$aimingAtMob() {
        //~ final HitResult hit = Minecraft.getInstance().hitResult;
        //~ return hit != null && hit.getType() == HitResult.Type.ENTITY
                //~ && ((EntityHitResult) hit).getEntity() instanceof LivingEntity;
    //~ }

    //~ // ------------------------------------------------------------------ food bar

    //~ private static final int GL_QUADS = 7;

    //~ /**
     //~ * Hunger bar overlay in the style of AppleSkin, drawn over vanilla's bar: the saturation you
     //~ * have as a gold outline around the shanks, and while you hold food, the shanks it would fill
     //~ * and the saturation it would add, flashing. One extra draw for the outline (all pixels in a
     //~ * single batch), two small sprites per previewed shank; nothing at all while it is off.
     //~ */
    //~ @Inject(method = "renderPlayerHealth", at = @At("TAIL"), require = 0)
    //~ private void aetherium$foodBar(final PoseStack pose, final CallbackInfo ci) {
        //~ if (!Qol.foodHud) {
            //~ return;
        //~ }
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ final LocalPlayer player = mc.player;
        //~ if (player == null || player.getVehicle() instanceof LivingEntity) {
            //~ return; // riding a horse: the bar shows the horse's health instead
        //~ }
        //~ final FoodData data = player.getFoodData();
        //~ final int food = data.getFoodLevel();
        //~ final float saturation = data.getSaturationLevel();
        //~ final int right = this.screenWidth / 2 + 91;
        //~ final int top = this.screenHeight - 39;
        //~ final int hearts = (int) Math.ceil((player.getMaxHealth() + (float) Math.ceil(player.getAbsorptionAmount())) / 2.0f);
        //~ FoodOverlay.shake(QolFoodIcons.SHAKE, this.tickCount, (int) Math.ceil(player.getHealth()), hearts, saturation, food);

        //~ final FoodProperties held = aetherium$heldFood(player);
        //~ int foodAfter = food;
        //~ float saturationAfter = saturation;
        //~ float flash = 0f;
        //~ if (held != null) {
            //~ foodAfter = Math.min(20, food + held.getNutrition());
            //~ saturationAfter = FoodOverlay.saturationAfter(food, saturation, held.getNutrition(), held.getSaturationModifier());
            //~ flash = FoodOverlay.flash(this.tickCount, 0f);
        //~ }

        //~ // Held food: the shanks it would fill, flashing over a faint highlight (under the outline).
        //~ if (foodAfter > food && flash > 0.01f) {
            //~ mc.getTextureManager().bind(GuiComponent.GUI_ICONS_LOCATION);
            //~ RenderSystem.enableBlend();
            //~ RenderSystem.defaultBlendFunc();
            //~ final Gui gui = (Gui) (Object) this;
            //~ for (int i = food / 2; i < (foodAfter + 1) / 2; i++) {
                //~ final int x = right - i * 8 - 9;
                //~ final int y = top + QolFoodIcons.SHAKE[i];
                //~ RenderSystem.color4f(1f, 1f, 1f, flash * 0.25f);
                //~ gui.blit(pose, x, y, 25, 27, 9, 9);
                //~ RenderSystem.color4f(1f, 1f, 1f, flash);
                //~ gui.blit(pose, x, y, i * 2 + 1 == foodAfter ? 61 : 52, 27, 9, 9);
            //~ }
            //~ RenderSystem.color4f(1f, 1f, 1f, 1f);
        //~ }

        //~ // Saturation: the gold outline, solid for what you have, flashing for what the food adds.
        //~ aetherium$saturation(pose, right, top, saturation, Math.max(saturation, saturationAfter), flash);
        //~ RenderSystem.disableBlend();
    //~ }

    //~ private static void aetherium$saturation(final PoseStack pose, final int right, final int top, final float saturation,
                                             //~ final float after, final float flash) {
        //~ if (after <= 0f) {
            //~ return;
        //~ }
        //~ final int[] outline = QolFoodIcons.outline();
        //~ final int total = outline.length;
        //~ if (total == 0) {
            //~ return;
        //~ }
        //~ final int flashAlpha = (int) (flash * 255f);
        //~ RenderSystem.enableBlend();
        //~ RenderSystem.defaultBlendFunc();
        //~ RenderSystem.disableTexture();
        //~ final Tesselator tesselator = Tesselator.getInstance();
        //~ final BufferBuilder buffer = tesselator.getBuilder();
        //~ buffer.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
        //~ final Matrix4f m = pose.last().pose();
        //~ for (int i = 0; i < 10; i++) {
            //~ final int shown = FoodOverlay.pixelsFor(saturation / 2f - i, total);
            //~ final int preview = flashAlpha > 2 ? FoodOverlay.pixelsFor(after / 2f - i, total) : 0;
            //~ final int count = Math.max(shown, preview);
            //~ final int x0 = right - i * 8 - 9;
            //~ final int y0 = top + QolFoodIcons.SHAKE[i];
            //~ for (int k = 0; k < count; k++) {
                //~ final int px = FoodOverlay.sx(outline[k]);
                //~ final int py = FoodOverlay.sy(outline[k]);
                //~ final int rgb = FoodOverlay.gold(px, py);
                //~ final int a = k < shown ? 255 : flashAlpha;
                //~ final int r = rgb >> 16 & 0xFF;
                //~ final int g = rgb >> 8 & 0xFF;
                //~ final int b = rgb & 0xFF;
                //~ final float x = x0 + px;
                //~ final float y = y0 + py;
                //~ buffer.vertex(m, x, y + 1f, 0f).color(r, g, b, a).endVertex();
                //~ buffer.vertex(m, x + 1f, y + 1f, 0f).color(r, g, b, a).endVertex();
                //~ buffer.vertex(m, x + 1f, y, 0f).color(r, g, b, a).endVertex();
                //~ buffer.vertex(m, x, y, 0f).color(r, g, b, a).endVertex();
            //~ }
        //~ }
        //~ tesselator.end();
        //~ RenderSystem.enableTexture();
    //~ }

    //~ /** The food in the main hand, else the off hand, or null. */
    //~ private static FoodProperties aetherium$heldFood(final LocalPlayer player) {
        //~ ItemStack stack = player.getMainHandItem();
        //~ if (!stack.getItem().isEdible()) {
            //~ stack = player.getOffhandItem();
            //~ if (!stack.getItem().isEdible()) {
                //~ return null;
            //~ }
        //~ }
        //~ return stack.getItem().getFoodProperties();
    //~ }
    // @era:q-hud-end
}
