package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-food-begin none
// @era:q-food-else v16
//~ import com.aetherium.qol.Qol;
//~ import java.util.List;
//~ import net.minecraft.ChatFormatting;
//~ import net.minecraft.network.chat.Component;
//~ import net.minecraft.network.chat.TextComponent;
//~ import net.minecraft.world.entity.player.Player;
//~ import net.minecraft.world.food.FoodProperties;
//~ import net.minecraft.world.item.Item;
//~ import net.minecraft.world.item.ItemStack;
//~ import net.minecraft.world.item.TooltipFlag;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:q-food-end

/**
 * Food info (Quality of life, Minecraft 1.16.5): food tooltips get a line with the hunger and
 * saturation the food restores, read from the item's own food properties (so modded foods work).
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack")
public abstract class QolTooltipMixin {
    // @era:q-food-begin none
    // @era:q-food-else v16
    //~ @Inject(method = "getTooltipLines", at = @At("RETURN"), require = 0)
    //~ private void aetherium$foodLine(final Player player, final TooltipFlag flag,
                                    //~ final CallbackInfoReturnable<List<Component>> cir) {
        //~ if (!Qol.foodTooltip) {
            //~ return;
        //~ }
        //~ final Item item = ((ItemStack) (Object) this).getItem();
        //~ if (!item.isEdible()) {
            //~ return;
        //~ }
        //~ final FoodProperties food = item.getFoodProperties();
        //~ final List<Component> lines = cir.getReturnValue();
        //~ if (food == null || lines == null) {
            //~ return;
        //~ }
        //~ final int nutrition = food.getNutrition();
        //~ final String text = "Hunger +" + nutrition + " (" + Qol.oneDecimal(nutrition / 2f) + " shanks)  \u00b7  Saturation +"
                //~ + Qol.oneDecimal(Qol.saturation(nutrition, food.getSaturationModifier()));
        //~ try {
            //~ lines.add(Math.min(1, lines.size()), new TextComponent(text).withStyle(ChatFormatting.GOLD));
        //~ } catch (final UnsupportedOperationException immutable) {
            //~ // A mod returned a fixed list; leave it alone.
        //~ }
    //~ }
    // @era:q-food-end
}
