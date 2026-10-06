package dev.aetherium.compat;

import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.platform.Services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detects rendering-engine mods that replace the same pipeline. Loaders cannot unload a mod at runtime, so the
 * only safe "disable" is to put Aetherium into Compatibility Mode (no pipeline takeover) and tell the user clearly.
 */
public final class ConflictDetector {
    private static final Map<String, String> CONFLICTS = Map.of(
            "sodium", "Sodium",
            "embeddium", "Embeddium",
            "rubidium", "Rubidium",
            "vulkanmod", "VulkanMod",
            "optifine", "OptiFine",
            "optifabric", "OptiFabric",
            "nvidium", "Nvidium",
            "oculus", "Oculus",
            "iris", "Iris"
    );
    private static final List<String> found = new ArrayList<>();

    private ConflictDetector() {}

    public static void scanAndReport() {
        found.clear();
        for (Map.Entry<String, String> e : CONFLICTS.entrySet()) {
            if (Services.PLATFORM.isModLoaded(e.getKey())) found.add(e.getValue());
        }
        // Iris / Oculus are compat partners, not conflicts; keep them out of the forced-compat decision.
        List<String> hard = new ArrayList<>(found);
        hard.remove("Iris");
        hard.remove("Oculus");
        if (hard.isEmpty()) return;

        Aetherium.LOGGER.warn("[Aetherium] Conflicting renderer mods detected: {}", hard);
        AetheriumConfig cfg = AetheriumConfig.get();
        if (cfg.advanced.disableConflictingMods && cfg.advanced.engineMode != AetheriumConfig.EngineMode.COMPATIBILITY) {
            cfg.advanced.engineMode = AetheriumConfig.EngineMode.COMPATIBILITY;
            cfg.save();
            Aetherium.LOGGER.warn("[Aetherium] Engine forced into Compatibility Mode to avoid a crash. Remove {} to use the Aetherium engine.", hard);
        }
    }

    public static List<String> detected() { return List.copyOf(found); }
    public static boolean hasHardConflict() {
        return found.stream().anyMatch(n -> !n.equals("Iris") && !n.equals("Oculus"));
    }
}
