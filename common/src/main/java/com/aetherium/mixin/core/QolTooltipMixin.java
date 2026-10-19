package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:q-food-begin on
import com.aetherium.client.ClientHooks;
import com.aetherium.client.QolFoodInfo;
import com.aetherium.qol.Qol;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:q-food-else none
// @era:q-food-end

/**
 * Food tooltip (Quality of life, every version): edible items get one gold line with the hunger
 * and saturation they restore, right under the name. Reads the item's food values the way the
 * version stores them ({@link com.aetherium.client.QolFoodInfo}); the hook takes no arguments of
 * getTooltipLines, whose parameters changed in 1.20.5, so one handler serves every version.
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack")
public abstract class QolTooltipMixin {
    // @era:q-food-begin on
    @Inject(method = "getTooltipLines", at = @At("RETURN"), require = 0)
    private void aetherium$foodLine(final CallbackInfoReturnable<List<Component>> cir) {
        if (!Qol.foodTooltip || !QolFoodInfo.read((ItemStack) (Object) this)) {
            return;
        }
        final List<Component> lines = cir.getReturnValue();
        if (lines == null) {
            return;
        }
        final int nutrition = QolFoodInfo.nutrition;
        final String text = "Hunger +" + nutrition + " (" + Qol.oneDecimal(nutrition / 2f) + " shanks)  \u00b7  Saturation +"
                + Qol.oneDecimal(Qol.saturation(nutrition, QolFoodInfo.modifier));
        try {
            lines.add(Math.min(1, lines.size()), ClientHooks.literal(text).withStyle(ChatFormatting.GOLD));
        } catch (final UnsupportedOperationException immutable) {
            // A mod returned a fixed list; leave it alone.
        }
    }
    // @era:q-food-else none
    // @era:q-food-end
}
