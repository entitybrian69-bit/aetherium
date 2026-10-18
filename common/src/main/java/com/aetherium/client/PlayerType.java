package com.aetherium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * "Is this the player entity type?" without naming {@code EntityType.PLAYER}, which Minecraft
 * 26.2 removed from {@code EntityType}: every player shares the local player's entity type.
 * Render thread only.
 */
public final class PlayerType {
    private PlayerType() {
    }

    /** True for players (and when there is no local player yet, so nothing is hidden by mistake). */
    public static boolean is(final EntityType<?> type) {
        final Object player = Minecraft.getInstance().player;
        return player == null || type == ((Entity) player).getType();
    }
}
