package com.aetherium.client;

// @era:t-ocean-floor-begin none
// @era:t-ocean-floor-else v16
//~ import com.aetherium.perf.Tuning;
//~ import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.multiplayer.ClientLevel;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.tags.FluidTags;
//~ import net.minecraft.tags.Tag;
//~ import net.minecraft.world.level.material.Fluid;
//~ import net.minecraft.world.level.ChunkPos;
//~ import net.minecraft.world.level.chunk.LevelChunk;
//~ import net.minecraft.world.level.levelgen.Heightmap;
// @era:t-ocean-floor-end

/**
 * Ocean floor culling (1.12.0, Minecraft 1.16.5, off by default): while the camera is above the
 * sea surface and not in a fluid, chunk sections lying entirely more than {@code MARGIN} blocks
 * below the floor of an ocean chunk are left out of the visibility search, so neither they nor
 * the caves beyond them are drawn or built.
 *
 * <p>Every chunk the server sends is sampled at 16 columns (every 4 blocks): a chunk counts as
 * ocean only when all 16 columns end in water at the top, and its floor is the lowest first
 * non-water block found. A sea-floor opening narrower than the sample spacing can therefore show
 * a gap, which is why the option is off by default. Render thread only.</p>
 */
public final class OceanFloor {
    private OceanFloor() {
    }

    // @era:t-ocean-floor-begin none
    // @era:t-ocean-floor-else v16
    //~ private static final int NOT_OCEAN = Integer.MIN_VALUE;
    //~ private static final int MARGIN = 8;
    //~ private static final int MAX_CHUNKS = 65536;
    //~ private static final Long2IntLinkedOpenHashMap FLOOR = new Long2IntLinkedOpenHashMap();
    //~ /** This frame: option on, camera above the sea surface and not inside a fluid. */
    //~ private static boolean active;

    //~ /** {@code Tag.Named} extends {@code Tag}; widened once, as {@code FluidState.is} takes a {@code Tag}. */
    //~ private static final Tag<Fluid> WATER = fluidTag(FluidTags.WATER);

    //~ @SuppressWarnings("unchecked")
    //~ private static Tag<Fluid> fluidTag(final Object named) {
        //~ return (Tag<Fluid>) named;
    //~ }

    //~ /** A chunk arrived from the server (ClientChunkCache.replaceWithPacketData). */
    //~ public static void capture(final LevelChunk chunk) {
        //~ if (chunk == null || !Tuning.oceanFloorCulling) {
            //~ return;
        //~ }
        //~ final ChunkPos pos = chunk.getPos();
        //~ final long key = ChunkPos.asLong(pos.x, pos.z);
        //~ if (FLOOR.size() >= MAX_CHUNKS) {
            //~ FLOOR.clear();
        //~ }
        //~ try {
            //~ int floor = Integer.MAX_VALUE;
            //~ for (int x = 2; x < 16; x += 4) {
                //~ for (int z = 2; z < 16; z += 4) {
                    //~ final int top = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    //~ // getHeight is the top block on some versions and the free block above it on
                    //~ // others: accept water at either.
                    //~ int y = water(chunk, x, top, z) ? top : top - 1;
                    //~ if (!water(chunk, x, y, z)) {
                        //~ FLOOR.remove(key);
                        //~ return;
                    //~ }
                    //~ while (y > 0 && water(chunk, x, y, z)) {
                        //~ y--;
                    //~ }
                    //~ floor = Math.min(floor, y);
                //~ }
            //~ }
            //~ FLOOR.put(key, floor);
        //~ } catch (final RuntimeException e) {
            //~ FLOOR.remove(key); // missing heightmap or section: simply not culled
        //~ }
    //~ }

    //~ private static boolean water(final LevelChunk chunk, final int x, final int y, final int z) {
        //~ return y >= 0 && y < 256 && chunk.getFluidState(x, y, z).is(WATER);
    //~ }

    //~ /** Once per frame, before the visibility search (LevelRenderer.renderLevel HEAD). */
    //~ public static void frame(final ClientLevel level, final Camera camera) {
        //~ active = Tuning.oceanFloorCulling && level != null && camera != null && !FLOOR.isEmpty()
                //~ && camera.getPosition().y > level.getSeaLevel() + 1
                //~ && camera.getFluidInCamera().isEmpty();
    //~ }

    //~ /** True when the section at this origin lies entirely below an ocean floor (minus the margin). */
    //~ public static boolean below(final BlockPos origin) {
        //~ if (!active) {
            //~ return false;
        //~ }
        //~ final int floor = FLOOR.getOrDefault(ChunkPos.asLong(origin.getX() >> 4, origin.getZ() >> 4), NOT_OCEAN);
        //~ return floor != NOT_OCEAN && origin.getY() + 16 <= floor - MARGIN;
    //~ }

    //~ public static void levelChanged() {
        //~ FLOOR.clear();
        //~ active = false;
    //~ }
    // @era:t-ocean-floor-end
}
