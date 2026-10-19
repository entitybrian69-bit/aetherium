package com.aetherium.qol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Plans an inventory sort one move at a time (Quality of life, Sort button).
 *
 * <p>The planner never keeps its own copy of the inventory: each call looks at the slots as they
 * are right now and returns the single next move, or {@code null} when the slots are sorted. The
 * caller performs the move with ordinary inventory clicks and asks again, so a slot changed by the
 * server or by another player half-way through simply changes the next answer instead of
 * desynchronising a precomputed plan.</p>
 *
 * <p>Two phases, in order: first partial stacks of the same item are merged into the earliest
 * one, then stacks are put in order with swaps. Merging first means every item has at most one
 * partial stack, which the order puts after the full stacks of that item; a "swap" of a full and
 * a partial stack of the same item then works with plain clicks (vanilla tops the partial stack
 * up and leaves the rest on the cursor, which goes into the emptied slot).</p>
 */
public final class SortPlanner {

    /** What the planner needs to know about a range of slots (indices 0..size-1). */
    public interface Slots {
        int size();

        boolean isEmpty(int slot);

        /** Same item and same tags, so the two stacks can be merged. */
        boolean sameItem(int a, int b);

        int count(int slot);

        int maxCount(int slot);

        /** Sort order of two non-empty slots by item only (count is handled by the planner). */
        int compareItems(int a, int b);
    }

    /** One move: merge {@code from} into {@code to}, or swap the stacks of the two slots. */
    public static final class Move {
        public final boolean merge;
        public final int from;
        public final int to;

        Move(final boolean merge, final int from, final int to) {
            this.merge = merge;
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return (this.merge ? "merge " : "swap ") + this.from + " -> " + this.to;
        }
    }

    private SortPlanner() {
    }

    /** The next move, or null when the slots are merged and in order. */
    public static Move next(final Slots slots) {
        final Move merge = nextMerge(slots);
        return merge != null ? merge : nextSwap(slots);
    }

    static Move nextMerge(final Slots slots) {
        final int n = slots.size();
        for (int to = 0; to < n; to++) {
            if (slots.isEmpty(to) || slots.count(to) >= slots.maxCount(to)) {
                continue;
            }
            for (int from = to + 1; from < n; from++) {
                if (!slots.isEmpty(from) && slots.count(from) < slots.maxCount(from) && slots.sameItem(to, from)) {
                    return new Move(true, from, to);
                }
            }
        }
        return null;
    }

    static Move nextSwap(final Slots slots) {
        final int n = slots.size();
        final List<Integer> order = new ArrayList<Integer>(n);
        for (int i = 0; i < n; i++) {
            if (!slots.isEmpty(i)) {
                order.add(Integer.valueOf(i));
            }
        }
        Collections.sort(order, new Comparator<Integer>() {
            @Override
            public int compare(final Integer a, final Integer b) {
                final int byItem = slots.compareItems(a.intValue(), b.intValue());
                if (byItem != 0) {
                    return byItem;
                }
                final int byCount = Integer.compare(slots.count(b.intValue()), slots.count(a.intValue()));
                return byCount != 0 ? byCount : Integer.compare(a.intValue(), b.intValue());
            }
        });
        for (int p = 0; p < n; p++) {
            if (p >= order.size()) {
                // Everything non-empty is already in front; the rest must be empty.
                return null;
            }
            final int wanted = order.get(p).intValue();
            if (equivalent(slots, p, wanted)) {
                continue;
            }
            for (int q = p + 1; q < n; q++) {
                if (equivalent(slots, q, wanted)) {
                    return new Move(false, q, p);
                }
            }
            return null; // cannot happen with a consistent view; stop instead of looping
        }
        return null;
    }

    private static boolean equivalent(final Slots slots, final int a, final int b) {
        if (slots.isEmpty(a) || slots.isEmpty(b)) {
            return slots.isEmpty(a) && slots.isEmpty(b);
        }
        return a == b || slots.sameItem(a, b) && slots.count(a) == slots.count(b);
    }
}
