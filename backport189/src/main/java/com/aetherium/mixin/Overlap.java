package com.aetherium.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 1.8.9 stand-in for the main mod's start-up overlap check: the options it guards (compact block
 * states, cached block fluids, ...) do not exist on 1.8.9, so nothing ever steps aside here.
 */
public final class Overlap {

    private Overlap() {
    }

    public static boolean yielded(final String option) {
        return false;
    }

    public static Set<String> yieldedOptions() {
        return Collections.emptySet();
    }

    public static List<String> notes() {
        return Collections.emptyList();
    }

    public static String summary() {
        return "not used on 1.8.9";
    }
}
