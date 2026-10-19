package com.aetherium.mixin.core;

import com.aetherium.client.QolSlotAccess;
import org.spongepowered.asm.mixin.Mixin;
// @era:q-inv-begin none
// @era:q-inv-else v16
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
// @era:q-inv-end

/**
 * Exposes a slot's index inside its container (Quality of life inventory helpers, Minecraft
 * 1.16.5), to tell the hotbar from the rest of the player's inventory.
 */
@Mixin(targets = "net.minecraft.world.inventory.Slot")
public abstract class QolSlotMixin implements QolSlotAccess {
    // @era:q-inv-begin none
    @Override
    public int aetherium$containerSlot() {
        return -1;
    }
    // @era:q-inv-else v16
    //~ @Shadow
    //~ @Final
    //~ private int slot;

    //~ @Override
    //~ public int aetherium$containerSlot() {
        //~ return this.slot;
    //~ }
    // @era:q-inv-end
}
