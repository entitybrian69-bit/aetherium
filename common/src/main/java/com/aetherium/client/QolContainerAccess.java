package com.aetherium.client;

/**
 * Added to container screens by {@code QolContainerScreenMixin} (Quality of life): the slot under
 * the mouse, typed as Object so this interface compiles on every Minecraft version.
 */
public interface QolContainerAccess {
    /** The hovered {@code Slot}, or null. */
    Object aetherium$hoveredSlot();
}
