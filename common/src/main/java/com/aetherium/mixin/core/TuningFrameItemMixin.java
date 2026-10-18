package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-frame-item-begin v16|f17|f212
import com.aetherium.client.TuningClient;
import com.aetherium.perf.Tuning;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-frame-item-else none
// @era:t-frame-item-end

/**
 * Entity detail distance (1.10.0, Minecraft 1.16.5): beyond the distance an item frame is drawn
 * empty (the frame itself still renders). Walls of item frames and map art cost a full item or map
 * render each; far away they are a few pixels.
 *
 * <p>Only the second {@code getItem()} call of {@code ItemFrameRenderer.render} is replaced: the
 * first one picks the frame model (map or normal frame) and stays vanilla.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.ItemFrameRenderer")
public abstract class TuningFrameItemMixin {
    // @era:t-frame-item-begin f17
    @Redirect(method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/decoration/ItemFrame;getItem()Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack aetherium$frameItem(final ItemFrame frame) {
        if (Tuning.detailDistanceSq != Double.MAX_VALUE && Tuning.beyondDetail(TuningClient.distanceToCamera(frame))) {
            return ItemStack.EMPTY;
        }
        return frame.getItem();
    }
    // @era:t-frame-item-else none
    // @era:t-frame-item-else v16
    //~ @Redirect(method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/world/entity/decoration/ItemFrame;getItem()Lnet/minecraft/world/item/ItemStack;",
                    //~ ordinal = 1),
            //~ require = 0)
    //~ private ItemStack aetherium$frameItem(final ItemFrame frame) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && Tuning.beyondDetail(TuningClient.distanceToCamera(frame))) {
            //~ return ItemStack.EMPTY;
        //~ }
        //~ return frame.getItem();
    //~ }
    // @era:t-frame-item-else f212
    //~ @Redirect(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
            //~ at = @At(value = "INVOKE",
                    //~ target = "Lnet/minecraft/world/entity/decoration/ItemFrame;getItem()Lnet/minecraft/world/item/ItemStack;"),
            //~ require = 0)
    //~ private ItemStack aetherium$frameItem(final ItemFrame frame) {
        //~ if (Tuning.detailDistanceSq != Double.MAX_VALUE && Tuning.beyondDetail(TuningClient.distanceToCamera(frame))) {
            //~ return ItemStack.EMPTY;
        //~ }
        //~ return frame.getItem();
    //~ }
    // @era:t-frame-item-end
}
