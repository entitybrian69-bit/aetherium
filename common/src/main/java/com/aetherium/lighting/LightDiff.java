package com.aetherium.lighting;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which world regions must be re-meshed when the light sources change.
 *
 * <p>Sources are compared by block position and level only. Because the tracker
 * snaps every source to its block centre, an entity walking inside one block
 * produces no rebuilds at all; crossing a block boundary rebuilds the region
 * around the old and the new position, and nothing else. Pure Java so the
 * behaviour is covered by unit tests.</p>
 */
public final class LightDiff {
    private LightDiff() {
    }

    /**
     * @return boxes {minX, minY, minZ, maxX, maxY, maxZ} (inclusive block coordinates)
     *         covering every source that appeared, disappeared, moved or changed level
     */
    public static List<int[]> changedRegions(final LightField.Source[] before, final LightField.Source[] after) {
        final List<int[]> out = new ArrayList<int[]>();
        for (final LightField.Source old : before) {
            if (!containsSame(after, old)) {
                out.add(region(old));
            }
        }
        for (final LightField.Source now : after) {
            if (!containsSame(before, now)) {
                out.add(region(now));
            }
        }
        return out;
    }

    static boolean containsSame(final LightField.Source[] set, final LightField.Source probe) {
        for (final LightField.Source candidate : set) {
            if (candidate.level == probe.level && candidate.blockX() == probe.blockX()
                    && candidate.blockY() == probe.blockY() && candidate.blockZ() == probe.blockZ()) {
                return true;
            }
        }
        return false;
    }

    static int[] region(final LightField.Source source) {
        final int reach = Math.max(1, source.level - 1);
        return new int[]{
                source.blockX() - reach, source.blockY() - reach, source.blockZ() - reach,
                source.blockX() + reach, source.blockY() + reach, source.blockZ() + reach
        };
    }
}
