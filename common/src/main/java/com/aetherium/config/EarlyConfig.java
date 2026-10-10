package com.aetherium.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Reads {@code aetherium.json} before Minecraft classes load, for the few decisions that must be
 * made while mixins are still being selected. No logging and no exceptions: anything unreadable
 * means "defaults", and the normal {@link ConfigStore} load reports problems later.
 */
public final class EarlyConfig {
    private EarlyConfig() {
    }

    /** @return the parsed document, or null when the file is missing or unreadable */
    public static Map<String, Object> read(final Path file) {
        try {
            if (file == null || !Files.isRegularFile(file)) {
                return null;
            }
            return parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        } catch (final Exception | LinkageError error) {
            return null;
        }
    }

    public static Map<String, Object> parse(final String text) {
        try {
            return Json.parseObject(text);
        } catch (final RuntimeException error) {
            return null;
        }
    }

    /** Schema version of the document, or -1 when absent. */
    public static int version(final Map<String, Object> root) {
        final Object version = root == null ? null : root.get("version");
        return version instanceof Number ? ((Number) version).intValue() : -1;
    }

    /** {@code aetherium.<group>.<name>}; option names may themselves contain dots. */
    public static Object value(final Map<String, Object> root, final String group, final String name) {
        if (root == null) {
            return null;
        }
        final Object body = root.get("aetherium");
        if (!(body instanceof Map)) {
            return null;
        }
        final Object bucket = ((Map<?, ?>) body).get(group);
        return bucket instanceof Map ? ((Map<?, ?>) bucket).get(name) : null;
    }

    /**
     * Whether dynamic lights will be on once this launch's config is loaded. Files older than
     * schema v5 are migrated to Off, so they count as Off here too.
     */
    public static boolean dynamicLightsOn(final Map<String, Object> root) {
        if (version(root) < 5) {
            return false;
        }
        final Object mode = value(root, "effects", "dynamic_lights");
        return mode instanceof String && !"OFF".equalsIgnoreCase(((String) mode).trim());
    }
}
