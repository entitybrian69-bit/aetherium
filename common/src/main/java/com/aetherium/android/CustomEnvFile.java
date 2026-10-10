package com.aetherium.android;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.aetherium.util.AetheriumLog;

/**
 * Reader for a launcher's {@code custom_env.txt}.
 *
 * <p>Format in the wild is one {@code KEY=value} per line, with three
 * complications that real launcher files contain and a naive splitter trips on:
 * {@code export KEY=value} prefixes, {@code #} comments, and values that are
 * themselves {@code KEY=value} fragments quoted with {@code "} (library paths).
 * Lines without {@code =} are ignored rather than failing the parse — Pojav
 * users routinely paste shell snippets in here.</p>
 *
 * <p>Cap: 256 KB and 4096 lines. A launcher env file is tens of lines; if
 * something reports a 900 MB file at this path we refuse to read it instead of
 * OOMing during boot on a 1 GB heap.</p>
 */
public final class CustomEnvFile {
    private static final AetheriumLog LOGGER = AetheriumLog.of(CustomEnvFile.class);
    private static final long MAX_BYTES = 256L * 1024L;
    private static final int MAX_LINES = 4096;

    private CustomEnvFile() {
    }

    public static Map<String, String> parse(final Path path) {
        Objects.requireNonNull(path, "path");
        if (!Files.isReadable(path)) {
            return Collections.emptyMap();
        }
        try {
            if (Files.size(path) > MAX_BYTES) {
                LOGGER.warn("Refusing to read oversized env file {} ({} bytes)", path, Files.size(path));
                return Collections.emptyMap();
            }
            final List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            final Map<String, String> out = new LinkedHashMap<>();
            int count = 0;
            for (String line : lines) {
                if (++count > MAX_LINES) {
                    LOGGER.warn("Env file {} exceeded {} lines; stopping", path, MAX_LINES);
                    break;
                }
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                final int equals = line.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                final String key = line.substring(0, equals).trim();
                String value = line.substring(equals + 1).trim();
                if (value.length() >= 2 && ((value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                        || (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (key.isEmpty()) {
                    continue;
                }
                out.put(key.toUpperCase(Locale.ROOT), value);
            }
            LOGGER.dev("Parsed {} entries from {}", out.size(), path);
            return Collections.unmodifiableMap(out);
        } catch (final IOException error) {
            LOGGER.warn("Could not read env file " + path + "; ignoring it", error);
            return Collections.emptyMap();
        }
    }

    /**
     * Extracts just the renderer-relevant keys. Kept explicit so that a stray
     * {@code PATH=} in the file cannot influence anything.
     */
    public static Map<String, String> extractRendererKeys(final Map<String, String> parsed) {
        final Map<String, String> out = new LinkedHashMap<>();
        for (final String key : new String[]{"POJAV_RENDERER", "ZALITH_RENDERER", "FCL_RENDERER", "AMETHYST_RENDERER",
                "MESA_GL_VERSION_OVERRIDE", "MESA_GLSL_VERSION_OVERRIDE", "POJAVEXEC_EGL", "GALLIUM_HUD",
                "MESA_EXTENSION_OVERRIDE", "DROIDBRIDGE_RENDERER"}) {
            final String value = parsed.get(key);
            if (value != null && !value.isEmpty()) {
                out.put(key, value);
            }
        }
        return out;
    }
}
