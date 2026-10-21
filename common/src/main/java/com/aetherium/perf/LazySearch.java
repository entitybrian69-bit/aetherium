package com.aetherium.perf;

/**
 * Search index on demand (1.17.2): implemented by the search trees of the creative inventory and
 * the recipe book (through a mixin) so a subclass hook can ask the base class to build its index.
 */
public interface LazySearch {
    /** Builds the index now if items were added or a rebuild was requested since the last search. */
    void aetherium$ensureBuilt();
}
