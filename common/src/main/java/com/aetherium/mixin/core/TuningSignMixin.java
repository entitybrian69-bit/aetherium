package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-sign-begin none
// @era:t-sign-else v16
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Matrix4f;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.gui.Font;
//~ import net.minecraft.client.renderer.MultiBufferSource;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.util.FormattedCharSequence;
//~ import net.minecraft.world.level.block.entity.SignBlockEntity;
//~ import net.minecraft.world.phys.Vec3;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-sign-end

/**
 * Sign text culling (1.11.0, Minecraft 1.16.5): beyond the entity detail distance a sign is drawn
 * without its text. Four lines of text cost a glyph quad per character; at that range they are an
 * unreadable smudge, and walls of signs are a classic frame-rate sink.
 */
@Mixin(targets = "net.minecraft.client.renderer.blockentity.SignRenderer")
public abstract class TuningSignMixin {
    // @era:t-sign-begin none
    // @era:t-sign-else v16
    //~ /** Render thread only: the sign being drawn is beyond the detail distance. */
    //~ @Unique
    //~ private static boolean aetherium$far;

    //~ @Inject(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            //~ at = @At("HEAD"), require = 0)
    //~ private void aetherium$distance(final SignBlockEntity sign, final float partialTicks, final PoseStack poseStack,
                                    //~ final MultiBufferSource buffers, final int light, final int overlay,
                                    //~ final CallbackInfo ci) {
        //~ if (Tuning.detailDistanceSq == Double.MAX_VALUE) {
            //~ aetherium$far = false;
            //~ return;
        //~ }
        //~ final Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //~ final BlockPos pos = sign.getBlockPos();
        //~ final double dx = pos.getX() + 0.5 - cam.x;
        //~ final double dy = pos.getY() + 0.5 - cam.y;
        //~ final double dz = pos.getZ() + 0.5 - cam.z;
        //~ aetherium$far = Tuning.beyondDetail(dx * dx + dy * dy + dz * dz);
    //~ }

    //~ @Redirect(method = "render(Lnet/minecraft/world/level/block/entity/SignBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/client/gui/Font;drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLcom/mojang/math/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;ZII)I"),
            //~ require = 0)
    //~ private int aetherium$text(final Font font, final FormattedCharSequence text, final float x, final float y,
                               //~ final int color, final boolean shadow, final Matrix4f pose,
                               //~ final MultiBufferSource buffers, final boolean seeThrough, final int background,
                               //~ final int light) {
        //~ if (aetherium$far) {
            //~ return 0;
        //~ }
        //~ return font.drawInBatch(text, x, y, color, shadow, pose, buffers, seeThrough, background, light);
    //~ }
    // @era:t-sign-end
}
