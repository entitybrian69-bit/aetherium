package com.aetherium.qol;

/**
 * Accurate block placement (Quality of life), the rule only; the Minecraft hook asks it once per
 * tick while the Use key is held.
 *
 * <p>Vanilla repeats a held Use every 4 ticks, whatever the crosshair is doing, so walking along a
 * floor while holding right-click leaves gaps (the crosshair skipped a spot between two repeats)
 * or tries the same spot twice. Here a repeat may happen early, as soon as the spot the block would
 * go to is a new one. Three limits keep it a timing fix rather than a faster building speed:</p>
 * <ul>
 *   <li>building onto the block that was just placed (a pillar, or a line out from your own last
 *       block) keeps vanilla's 4-tick rhythm, so held towers and bridges are exactly vanilla;</li>
 *   <li>the same spot is never tried twice in a row early;</li>
 *   <li>at least {@link #MIN_TICKS} ticks pass between two placements (10 per second at most).</li>
 * </ul>
 *
 * <p>Positions are packed block positions ({@code BlockPos.asLong}); {@link #NONE} means none.</p>
 */
public final class AccuratePlacement {
    public static final long NONE = Long.MIN_VALUE;
    /** Fastest early repeat: one placement every 2 ticks (vanilla's held repeat is every 4). */
    public static final int MIN_TICKS = 2;

    private long lastTarget = NONE;
    private int ticksSincePlace = Integer.MAX_VALUE / 2;

    /** Once per client tick, before the hook decides. */
    public void tick() {
        if (this.ticksSincePlace < Integer.MAX_VALUE / 2) {
            this.ticksSincePlace++;
        }
    }

    /** The Use key was released or the player stopped holding a block: start fresh. */
    public void reset() {
        this.lastTarget = NONE;
        this.ticksSincePlace = Integer.MAX_VALUE / 2;
    }

    /**
     * True when the held Use may place now instead of waiting for vanilla's repeat timer.
     *
     * @param clicked the block the crosshair is on
     * @param target  the block space a block would be placed in ({@code clicked} itself when it is
     *                replaceable, like tall grass, otherwise its neighbour on the clicked face)
     */
    public boolean placeEarly(final long clicked, final long target) {
        if (clicked == NONE || target == NONE) {
            return false;
        }
        if (this.ticksSincePlace < MIN_TICKS) {
            return false;
        }
        if (target == this.lastTarget) {
            return false;
        }
        // Building onto the block placed last: vanilla rhythm.
        return clicked != this.lastTarget;
    }

    /** A placement attempt is being made for {@code target} (early or by vanilla's own timer). */
    public void placed(final long target) {
        this.lastTarget = target;
        this.ticksSincePlace = 0;
    }

    long lastTarget() {
        return this.lastTarget;
    }
}
