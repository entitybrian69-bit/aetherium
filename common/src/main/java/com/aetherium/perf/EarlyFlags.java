package com.aetherium.perf;

import com.aetherium.config.EarlyConfig;
import java.util.Map;

/**
 * Options that only take effect at start-up, read straight from {@code aetherium.json} the first
 * time they are needed (before the normal config load, which happens after Minecraft's blocks are
 * registered). Defaults when the file is missing or unreadable; the master switch counts.
 */
public final class EarlyFlags {
    private static volatile Boolean compactBlockStates;

    private EarlyFlags() {
    }

    /** Compact block states (1.17.2): decided once, while blocks register. */
    public static boolean compactBlockStates() {
        Boolean value = compactBlockStates;
        if (value == null) {
            value = Boolean.valueOf(decide(EarlyConfig.read(WorkerThreads.defaultConfigFile()),
                    "compact_block_states", true));
            compactBlockStates = value;
        }
        return value.booleanValue();
    }

    static boolean decide(final Map<String, Object> root, final String name, final boolean fallback) {
        if (root == null) {
            return fallback;
        }
        final Object master = EarlyConfig.value(root, "general", "enabled");
        if (Boolean.FALSE.equals(master)) {
            return false;
        }
        final Object value = EarlyConfig.value(root, "performance", name);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : fallback;
    }

    /** Tests. */
    static void reset() {
        compactBlockStates = null;
    }
}
