package com.aetherium.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.aetherium.util.AetheriumLog;

/**
 * Loads and saves {@link AetheriumConfig} from a JSON file.
 *
 * <p>Guarantees that matter here:</p>
 * <ul>
 *   <li><b>Never lose a user's file.</b> A parse failure keeps a backup of the
 *       bad file next to it ({@code aetherium.json.bad}) and falls back to
 *       defaults, rather than truncating it or crashing the game.</li>
 *   <li><b>Never corrupt a file.</b> Saves go to a temp file plus an atomic
 *       rename; on filesystems without atomic move (FUSE / sdcardfs on Android,
 *       which is the common case for launcher installs) it degrades to a
 *       replace-and-sync and says so in the log.</li>
 *   <li><b>Never block the render thread.</b> {@link #requestSave()} coalesces
 *       to at most one write per 500 ms on the flush thread.</li>
 * </ul>
 */
public final class ConfigStore implements AutoCloseable {
    private static final AetheriumLog LOGGER = AetheriumLog.of(ConfigStore.class);
    private static final String ROOT_KEY = "aetherium";

    private final Path file;
    private final AetheriumConfig config;

    /** Guarded by {@code saveLock}: the render thread requests, the flush thread writes. */
    private final Object saveLock = new Object();
    private volatile boolean saveRequested;
    private volatile long lastSaveEpochMs;
    private volatile boolean closed;

    /** Keys present in the file that this build does not know, preserved for round-trip. */
    private final Map<String, Object> unknownKeys = new LinkedHashMap<>();
    /** Keys this build has that the file did not (new in a version delta). */
    private final List<String> addedKeys = new ArrayList<>();

    public ConfigStore(final Path file, final AetheriumConfig config) {
        this.file = Objects.requireNonNull(file, "file");
        this.config = Objects.requireNonNull(config, "config");
    }

    public Path getFile() {
        return this.file;
    }

    public List<String> getAddedKeys() {
        return this.addedKeys;
    }

    public Map<String, Object> getUnknownKeys() {
        return this.unknownKeys;
    }

    /** Reads the file if present; applies known keys onto {@code config}. */
    public void load() {
        if (!Files.exists(this.file)) {
            LOGGER.info("No config file at {}; writing defaults", this.file);
            this.saveNow();
            return;
        }

        final String text;
        try {
            text = new String(Files.readAllBytes(this.file), StandardCharsets.UTF_8);
        } catch (final IOException error) {
            LOGGER.error("Could not read " + this.file + "; continuing with defaults", error);
            return;
        }

        final Map<String, Object> root;
        try {
            root = Json.parseObject(text);
        } catch (final RuntimeException error) {
            this.quarantineBrokenFile(error);
            return;
        }

        Object body = root.get(ROOT_KEY);
        if (!(body instanceof Map)) {
            // Accept a flat file (people paste fragments from issue threads).
            body = root;
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> flat = Json.flatten(new LinkedHashMap<>((Map<String, Object>) body));
        Object version = root.get("version");
        if (version == null) {
            version = flat.get("version");
        }
        if (version instanceof Long) {
            final int found = Math.toIntExact((Long) version);
            this.config.setFileVersion(found);
            if (found != AetheriumConfig.CURRENT_VERSION) {
                LOGGER.info("Migrating config from schema v{} to v{}", found, AetheriumConfig.CURRENT_VERSION);
                this.migrate(found, flat);
            }
        }

        int applied = 0;
        int rejected = 0;
        for (final Map.Entry<String, Object> entry : flat.entrySet()) {
            final String key = entry.getKey();
            if (key.equals("version")) {
                continue;
            }
            final ConfigValue<?> option = this.config.byKey(key);
            if (option == null) {
                this.unknownKeys.put(key, entry.getValue());
                continue;
            }
            if (option.accept(entry.getValue())) {
                applied++;
            } else {
                rejected++;
                LOGGER.warn("Ignoring value for '{}' — '{}' is not a {} (default {} restored)",
                        key, entry.getValue(), option.getDefault().getClass().getSimpleName(), option.getDefault());
            }
        }
        for (final String key : this.config.all().keySet()) {
            if (!flat.containsKey(key)) {
                this.addedKeys.add(key);
            }
        }
        LOGGER.info("Config loaded: {} applied, {} rejected, {} unknown keys kept for round-trip, {} new options",
                applied, rejected, this.unknownKeys.size(), this.addedKeys.size());
    }

    /** Key renames that keep old user files meaningful across version deltas. */
    private void migrate(final int from, final Map<String, Object> flat) {
        if (from < 2) {
            rename(flat, "render.useSodiumRenderer", "general.enabled");
            rename(flat, "render.backendOverride", "performance.backend");
            rename(flat, "lighting.dynamicLights", "utilities.dynamic_lights.enabled");
            rename(flat, "gamma.gammaEnabled", "utilities.gamma.enabled");
            rename(flat, "gamma.gamma", "utilities.gamma.amount");
        }
        if (from < 3) {
            rename(flat, "android.memoryBudget", "android.memory_budget_mb");
            rename(flat, "performance.maxUploadKb", "performance.upload_budget_kb");
            rename(flat, "performance.hzbLevels", "performance.hzb.levels");
            // v3 removed the separate "no-op toggles" for shader reload; drop them.
            flat.remove("shaders.force_reload");
            flat.remove("shaders.compatibility_reload");
        }
    }

    private static void rename(final Map<String, Object> flat, final String from, final String to) {
        final Object value = flat.remove(from);
        if (value != null) {
            flat.putIfAbsent(to, value);
        }
    }

    private void quarantineBrokenFile(final RuntimeException error) {
        final Path backup = this.file.resolveSibling(this.file.getFileName() + ".bad");
        try {
            Files.copy(this.file, backup, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Config at {} is invalid ({}); the bad file is preserved at {} and defaults are in use",
                    this.file, error.getMessage(), backup);
        } catch (final IOException copyError) {
            LOGGER.error("Config is invalid and could not be backed up to " + backup + "; defaults are in use", copyError);
        }
    }

    /** Coalescing request; safe from any thread including the render thread. */
    public void requestSave() {
        if (this.closed) {
            return;
        }
        synchronized (this.saveLock) {
            this.saveRequested = true;
            this.saveLock.notifyAll();
        }
    }

    /** Called once per frame from the client tick hook. */
    public void tick() {
        if (!this.saveRequested || this.closed) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - this.lastSaveEpochMs < 500L) {
            return;
        }
        this.lastSaveEpochMs = now;
        this.saveRequested = false;
        this.saveNow();
    }

    public void saveNow() {
        final Map<String, Object> grouped = this.serialize();
        final String text = Json.write(grouped);
        try {
            final Path parent = this.file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
            Files.write(temp, text.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, this.file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (final AtomicMoveNotSupportedException unsupported) {
                // sdcardfs/FUSE on Android: no atomic rename. Replace non-atomically
                // but flush the temp file first (Files.write already closed it).
                LOGGER.dev("Atomic move unsupported on {}; using a direct replace", this.file.getFileSystem());
                Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
            }
            this.config.clearDirty();
        } catch (final IOException error) {
            LOGGER.error("Failed to write " + this.file, error);
        }
    }

    /** @return a nested {@code group -> {name -> value, ...}} document */
    public Map<String, Object> serialize() {
        final Map<String, Object> flat = new LinkedHashMap<>();
        for (final Map.Entry<String, ConfigValue<?>> entry : this.config.all().entrySet()) {
            flat.put(entry.getKey(), entry.getValue().serializeForJson());
        }
        flat.putAll(this.unknownKeys);

        final Map<String, Object> grouped = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : flat.entrySet()) {
            final int dot = entry.getKey().indexOf('.');
            if (dot < 0) {
                grouped.put(entry.getKey(), entry.getValue());
                continue;
            }
            final String group = entry.getKey().substring(0, dot);
            final String name = entry.getKey().substring(dot + 1);
            @SuppressWarnings("unchecked")
            final Map<String, Object> bucket = (Map<String, Object>) grouped.computeIfAbsent(group, key -> new LinkedHashMap<String, Object>());
            bucket.put(name, entry.getValue());
        }

        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("_comment", "Aetherium config. Comments in this file are allowed (// and /* */). See CONFIG_SCHEMA.json.");
        root.put("version", (long) AetheriumConfig.CURRENT_VERSION);
        root.put(ROOT_KEY, grouped);
        return root;
    }

    /** Flushes on shutdown so a crash during play does not eat the last change. */
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.config.anyDirty() || this.saveRequested) {
            this.saveNow();
        }
    }
}
