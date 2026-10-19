package com.aetherium.client;

// @era:food-api-begin component
import net.minecraft.core.component.DataComponents;
// @era:food-api-else item
// @era:food-api-end
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * What a food item gives, read the way this Minecraft version stores it: on the item before
 * 1.20.5, as the {@code FOOD} data component (with an absolute saturation) from 1.20.5. Shared by
 * the food tooltip and the hunger bar preview. Render or main thread only (one reused result).
 */
public final class QolFoodInfo {
    /** Hunger points (half shanks) of the last food read. */
    public static int nutrition;
    /** Saturation modifier of the last food read (saturation added = nutrition * modifier * 2). */
    public static float modifier;

    private QolFoodInfo() {
    }

    /** Reads the stack's food values into {@link #nutrition} and {@link #modifier}; false if it is not food. */
    public static boolean read(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        // @era:food-api-begin component
        final FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return false;
        }
        nutrition = food.nutrition();
        modifier = nutrition > 0 ? food.saturation() / (nutrition * 2f) : 0f;
        // @era:food-api-else item
        //~ if (!stack.getItem().isEdible()) {
            //~ return false;
        //~ }
        //~ final FoodProperties food = stack.getItem().getFoodProperties();
        //~ if (food == null) {
            //~ return false;
        //~ }
        //~ nutrition = food.getNutrition();
        //~ modifier = food.getSaturationModifier();
        // @era:food-api-end
        return true;
    }
}
