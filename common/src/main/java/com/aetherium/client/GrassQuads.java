package com.aetherium.client;

// @era:t-grass-quads-begin none
// @era:t-grass-quads-else v16
//~ import com.aetherium.perf.Tuning;
//~ import java.util.ArrayList;
//~ import java.util.List;
//~ import net.minecraft.client.renderer.block.model.BakedQuad;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.world.level.block.Blocks;
//~ import net.minecraft.world.level.block.state.BlockState;
// @era:t-grass-quads-end

/**
 * Fast grass (1.12.0, Minecraft 1.16.5, off by default): the sides of a grass block are drawn as
 * two layers, the dirt side and a biome-tinted grass fringe on top of it. With fast grass only
 * the first layer is drawn (its built-in green fringe stays), which halves the side faces of
 * every grass block in view and in each chunk build. Grass tops and all other blocks are
 * unchanged; a model whose sides are all tinted keeps them, so a resource pack can never end up
 * with see-through grass.
 *
 * <p>Filtered lists are cached by the identity of the model's own quad list (a baked model hands
 * out the same list every time). The cache is a small copy-on-write array read without locks by
 * the chunk builder threads. After a resource reload the old lists simply stop matching; once
 * 64 pairs are stored the cache starts over, so stale entries never pile up.</p>
 */
public final class GrassQuads {
    private GrassQuads() {
    }

    // @era:t-grass-quads-begin none
    // @era:t-grass-quads-else v16
    //~ private static final int MAX_PAIRS = 64;

    //~ /** Alternating source list, filtered list. Replaced as a whole, never mutated. */
    //~ private static volatile Object[] cache = new Object[0];

    //~ /** The quads to draw for one face of a block: {@code quads} itself unless fast grass applies. */
    //~ public static List<BakedQuad> filter(final List<BakedQuad> quads, final BlockState state, final Direction side) {
        //~ if (!Tuning.fastGrass || side == null || side == Direction.UP || side == Direction.DOWN || quads.isEmpty()
                //~ || state.getBlock() != Blocks.GRASS_BLOCK) {
            //~ return quads;
        //~ }
        //~ final Object[] pairs = cache;
        //~ for (int i = 0; i < pairs.length; i += 2) {
            //~ if (pairs[i] == quads) {
                //~ @SuppressWarnings("unchecked")
                //~ final List<BakedQuad> hit = (List<BakedQuad>) pairs[i + 1];
                //~ return hit;
            //~ }
        //~ }
        //~ final List<BakedQuad> result = untinted(quads);
        //~ remember(quads, result);
        //~ return result;
    //~ }

    //~ /** The untinted quads, or {@code quads} itself when none or all of them are tinted. */
    //~ private static List<BakedQuad> untinted(final List<BakedQuad> quads) {
        //~ int tinted = 0;
        //~ for (int i = 0; i < quads.size(); i++) {
            //~ if (quads.get(i).isTinted()) {
                //~ tinted++;
            //~ }
        //~ }
        //~ if (tinted == 0 || tinted == quads.size()) {
            //~ return quads;
        //~ }
        //~ final List<BakedQuad> kept = new ArrayList<>(quads.size() - tinted);
        //~ for (int i = 0; i < quads.size(); i++) {
            //~ if (!quads.get(i).isTinted()) {
                //~ kept.add(quads.get(i));
            //~ }
        //~ }
        //~ return kept;
    //~ }

    //~ private static synchronized void remember(final Object source, final Object filtered) {
        //~ final Object[] old = cache;
        //~ for (int i = 0; i < old.length; i += 2) {
            //~ if (old[i] == source) {
                //~ return;
            //~ }
        //~ }
        //~ final Object[] next;
        //~ if (old.length >= MAX_PAIRS * 2) {
            //~ next = new Object[] {source, filtered}; // a resource reload left stale lists: start over
        //~ } else {
            //~ next = new Object[old.length + 2];
            //~ System.arraycopy(old, 0, next, 0, old.length);
            //~ next[old.length] = source;
            //~ next[old.length + 1] = filtered;
        //~ }
        //~ cache = next;
    //~ }

    //~ /** Forgets every cached list (resource reload or tests). */
    //~ public static void clear() {
        //~ cache = new Object[0];
    //~ }
    // @era:t-grass-quads-end
}
