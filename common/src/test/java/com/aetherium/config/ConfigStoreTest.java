package com.aetherium.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The file format and its failure modes. A config store that corrupts a user's file on
 * the first bad launch is the kind of bug that ends a mod's reputation, so the tests here
 * are mostly about things going wrong.
 */
final class ConfigStoreTest {
    @TempDir
    Path directory;

    private Path file() {
        return this.directory.resolve("aetherium.json");
    }

    @Test
    @DisplayName("a missing file is created with the defaults and the schema version")
    void createsDefaults() throws IOException {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
        }
        assertTrue(Files.exists(this.file()), "a first launch must leave a readable file behind");
        final Map<String, Object> root = Json.parseObject(new String(Files.readAllBytes(this.file()), StandardCharsets.UTF_8));
        assertEquals((long) AetheriumConfig.CURRENT_VERSION, ((Number) root.get("version")).longValue());
        // Keys are group-prefixed ("general.enabled"), and the writer groups on the first
        // dot: the GUI, the schema and every delta use the same flat dotted form.
        assertEquals(Boolean.TRUE, Json.path(root, "aetherium.general.enabled"));
        assertTrue(root.containsKey("_comment"), "the file explains itself, because people hand-edit it");
        assertNoTempLeftBehind();
    }

    @Test
    @DisplayName("values round-trip exactly, including the awkward ones")
    void roundTrip() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
            config.enabled.set(false);
            config.hudCorner.set("bottom-right");
            config.backend.set(AetheriumConfig.BackendChoice.GL_CORE);
            config.targetFps.set(144);
            store.saveNow();
        }

        final AetheriumConfig reloaded = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), reloaded)) {
            store.load();
            assertFalse(reloaded.enabled.get());
            assertEquals("bottom-right", reloaded.hudCorner.get());
            assertEquals(AetheriumConfig.BackendChoice.GL_CORE, reloaded.backend.get());
            assertEquals(144, reloaded.targetFps.get().intValue());
        }
    }

    @Test
    @DisplayName("an unknown key in the file is kept, so a downgrade does not lose settings")
    void unknownKeysSurvive() throws IOException {
        Files.write(this.file(), ("{\n"
                + "  \"version\": 3,\n"
                + "  \"aetherium\": {\n"
                + "    \"general.enabled\": true,\n"
                + "    \"general.hud.corner\": \"top-right\",\n"
                + "    \"future.some_option\": 42\n"
                + "  }\n}\n").getBytes(StandardCharsets.UTF_8));

        final AetheriumConfig config = AetheriumConfig.createDefaults();
        final ConfigStore store = new ConfigStore(this.file(), config);
        store.load();
        assertEquals("top-right", config.hudCorner.get());
        assertTrue(store.getUnknownKeys().containsKey("future.some_option"),
                "unknown keys seen: " + store.getUnknownKeys().keySet());
        store.saveNow();
        final Map<String, Object> root = Json.parseObject(new String(Files.readAllBytes(this.file()), StandardCharsets.UTF_8));
        assertEquals(42L, ((Number) Json.path(root, "aetherium.future.some_option")).longValue(),
                "the writer dropped a key it never understood");
        store.close();
    }

    @Test
    @DisplayName("a new option is reported as added, not as an error")
    void addedKeysReported() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            // Force the file to exist with only one key by writing it ourselves first.
            store.saveNow();
        }
        final Map<String, Object> root = readRoot();
        assertTrue(config.all().size() > 1, "there must be more than one option to be 'added'");
        // With a full file present, nothing is new.
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
            assertTrue(store.getAddedKeys().isEmpty(), "unexpected new keys: " + store.getAddedKeys());
        }
        // A file with a single key reports everything else as new - which is what tells
        // the GUI to show the "some options were added" note.
        try {
            Files.write(this.file(), "{\"version\": 3, \"aetherium\": {\"general.enabled\": false}}"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (final IOException error) {
            throw new IllegalStateException("could not stage the file", error);
        }
        final AetheriumConfig second = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), second)) {
            store.load();
            assertFalse(second.enabled.get(), "the one key we wrote must land");
            final List<String> added = store.getAddedKeys();
            assertTrue(added.contains("general.hud.corner"), "a fresh option should be reported as added: " + added);
        }
        assertFalse(root.isEmpty());
    }

    @Test
    @DisplayName("a corrupt file is preserved as .bad and defaults are used")
    void quarantinesCorruptFile() throws IOException {
        final String broken = "{ \"version\": 3, \"aetherium\": { \"general.enabled\": tru";
        Files.write(this.file(), broken.getBytes(StandardCharsets.UTF_8));

        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
            assertTrue(config.enabled.get(), "defaults must be in use after a parse failure");
            final Path backup = this.directory.resolve("aetherium.json.bad");
            assertTrue(Files.exists(backup), "the user's file must be preserved for comparison");
            assertEquals(broken, new String(Files.readAllBytes(backup), StandardCharsets.UTF_8));
            // Deliberate: the original is left alone rather than deleted, so a bad
            // hand-edit is never destroyed. The next save overwrites it with valid JSON.
            assertTrue(Files.exists(this.file()));
        }
    }

    @Test
    @DisplayName("an old schema version migrates the renamed keys")
    void migratesOldVersions() throws IOException {
        Files.write(this.file(), ("{\n"
                + "  \"version\": 1,\n"
                + "  \"aetherium\": {\n"
                + "    \"gamma.gammaEnabled\": true,\n"
                + "    \"gamma.gamma\": 2.5,\n"
                + "    \"render.backendOverride\": \"gl_core\"\n"
                + "  }\n}\n").getBytes(StandardCharsets.UTF_8));

        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
            assertTrue(config.gammaEnabled.get(), "v1 gamma.gammaEnabled must migrate to the new key");
            assertEquals(2.5d, config.gammaAmount.get(), 1.0E-9);
            assertEquals(AetheriumConfig.BackendChoice.GL_CORE, config.backend.get());
            assertEquals(1, config.getFileVersion(), "loading must not pretend the file is v3");
            store.saveNow();
            assertEquals((long) AetheriumConfig.CURRENT_VERSION,
                    ((Number) readRoot().get("version")).longValue(), "the rewrite must be at the current version");
        }
    }

    @Test
    @DisplayName("saves are coalesced to at most one write per half second")
    void coalescesWrites() throws IOException {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            store.load();
            config.targetFps.set(90);
            store.requestSave();
            store.tick();
            final long afterFirst = Files.getLastModifiedTime(this.file()).toMillis();
            final long beforeSecondTick = System.currentTimeMillis();
            config.targetFps.set(91);
            store.requestSave();
            store.tick();
            final long tickDuration = System.currentTimeMillis() - beforeSecondTick;
            if (tickDuration < 400L) {
                // The window is 500 ms; if the box is so slow that we crossed it anyway,
                // the second write is legitimate and the assertion below would be a lie.
                assertEquals(afterFirst, Files.getLastModifiedTime(this.file()).toMillis(),
                        "a second tick inside the coalescing window must not hit the disk again");
                assertEquals(90, ((Number) Json.path(readRoot(), "aetherium.performance.target_fps")).intValue(),
                        "the coalesced write must still carry the requested value");
            }
            config.targetFps.set(92);
            store.saveNow();
            assertEquals(92, config.targetFps.get().intValue());
            assertFalse(config.anyDirty(), "saveNow must clear the dirty flags so shutdown does not rewrite");
        }
    }

    @Test
    @DisplayName("no .tmp file survives a successful write")
    void atomicReplaceLeavesNoTrash() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        try (ConfigStore store = new ConfigStore(this.file(), config)) {
            for (int i = 0; i < 8; i++) {
                config.targetFps.set(30 + i);
                store.saveNow();
            }
        }
        assertNoTempLeftBehind();
    }

    private void assertNoTempLeftBehind() {
        try (var listing = Files.list(this.directory)) {
            assertTrue(listing.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")),
                    "a temp file survived the atomic replace");
        } catch (final IOException error) {
            throw new IllegalStateException("cannot list " + this.directory, error);
        }
    }

    private Map<String, Object> readRoot() {
        try {
            return Json.parseObject(new String(Files.readAllBytes(this.file()), StandardCharsets.UTF_8));
        } catch (final IOException error) {
            throw new IllegalStateException("cannot read " + this.file(), error);
        }
    }
}
