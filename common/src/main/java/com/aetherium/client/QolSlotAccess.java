package com.aetherium.client;

/**
 * Added to inventory slots by {@code QolSlotMixin} (Quality of life): the slot's index inside its
 * own container, which Minecraft 1.16.5 keeps private.
 */
public interface QolSlotAccess {
    int aetherium$containerSlot();
}
