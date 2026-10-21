package com.aetherium.client;

/**
 * Batched chests (1.17.2): rebuilds the chunk section holding a chest (and the other half of a
 * double chest) so the chest moves into or out of the section mesh. Added to the level renderer
 * on Minecraft 1.16.5; main thread.
 */
public interface ChestSections {
    void aetherium$chestChanged(int x, int y, int z);
}
