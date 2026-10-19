package com.aetherium.mixin.core;

import com.aetherium.client.QolSlotAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes a slot's index inside its container (Quality of life inventory helpers, every version;
 * 1.16.5 has no getContainerSlot()), to tell the hotbar from the rest of the player's inventory.
 */
@Mixin(targets = "net.minecraft.world.inventory.Slot")
public abstract class QolSlotMixin implements QolSlotAccess {
    @Shadow
    @Final
    private int slot;

    @Override
    public int aetherium$containerSlot() {
        return this.slot;
    }
}
