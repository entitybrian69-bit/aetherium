package com.aetherium.client;

/**
 * Batched chests (1.17.2): the lid state of a chest or ender chest block entity, readable from
 * the mesh workers (added to both block entity classes on Minecraft 1.16.5).
 */
public interface ChestLid {
    float aetherium$openness();

    float aetherium$previousOpenness();

    int aetherium$openCount();
}
