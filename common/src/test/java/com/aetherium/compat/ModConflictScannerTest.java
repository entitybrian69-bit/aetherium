package com.aetherium.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.aetherium.compat.ModConflictScanner.Ownership;
import com.aetherium.compat.ModConflictScanner.Severity;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.platform.PlatformAdapter;

/**
 * The conflict table is the difference between "it just works" and a startup crash for
 * most users, so these tests treat the table as a product surface: what is detected, what
 * severity it maps to, what gets switched off, and what the user is told.
 */
final class ModConflictScannerTest {
    /** In-memory stand-in for a loader: the scanner only ever asks these seven questions. */
    private static final class FakePlatform implements PlatformAdapter {
        private final Set<String> loaded = new HashSet<>();
        private final String version;

        FakePlatform(final String version, final String... mods) {
            this.version = version;
            for (final String mod : mods) {
                this.loaded.add(mod);
            }
        }

        @Override
        public String platformName() {
            return "test";
        }

        @Override
        public Path gameDirectory() {
            return Path.of(".");
        }

        @Override
        public Path configDirectory() {
            return Path.of(".");
        }

        @Override
        public boolean isModLoaded(final String modId) {
            return this.loaded.contains(modId);
        }

        @Override
        public Optional<String> getModVersion(final String modId) {
            return this.loaded.contains(modId) ? Optional.of("9.9.9") : Optional.empty();
        }

        @Override
        public String minecraftVersion() {
            return this.version;
        }

        @Override
        public boolean isClient() {
            return true;
        }
    }

    @Test
    @DisplayName("a clean mod list detects nothing and touches nothing")
    void nothingDetected() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1"));
        scanner.scan(config);
        assertFalse(scanner.hasAnything(), "found: " + scanner.summarize());
        assertFalse(scanner.requiresIncompatible());
        assertTrue(config.enabled.get(), "an empty scan must not disable the renderer");
        assertEquals(AetheriumConfig.LightMode.FAST, config.dynamicLights.get(), "an empty scan must not touch features");
        assertTrue(scanner.getDetected().isEmpty());
        assertTrue(scanner.getDetectedVersions().isEmpty());
        assertNotNull(scanner.summarize());
    }

    @Test
    @DisplayName("Sodium takes over dynamic lights only; everything else keeps working")
    void sodiumDelegatesLights() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "sodium"));
        scanner.scan(config);
        assertFalse(scanner.requiresIncompatible(), "Sodium must not make Aetherium stand down");
        assertEquals(Severity.DELEGATE, scanner.getWorstSeverity());
        assertEquals(Ownership.OTHER_RENDERER, scanner.getOwnership());
        final List<String> versions = scanner.getDetectedVersions();
        assertEquals(1, versions.size());
        assertTrue(versions.get(0).startsWith("sodium 9.9.9"), "unexpected version string: " + versions.get(0));
        assertTrue(scanner.summarize().toLowerCase().contains("sodium"), "the summary must name the mod: " + scanner.summarize());
        assertEquals(AetheriumConfig.LightMode.OFF, config.dynamicLights.get(),
                "Sodium meshes chunks itself, so our light hook would never be read");
        assertTrue(config.entityCulling.get(), "culling is independent of the mesher and stays on");
        assertFalse(scanner.adviceForUser().isPresent(), "a delegate needs no user action");
        assertTrue(scanner.asReport().contains("Sodium"), "the crash-report section must name the mod");
    }

    @Test
    @DisplayName("a hard conflict comes with an instruction for the user")
    void hardConflictHasAdvice() {
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "vulkanmod"));
        scanner.scan(AetheriumConfig.createDefaults());
        assertTrue(scanner.requiresIncompatible());
        assertEquals(Severity.HARD, scanner.getWorstSeverity());
        assertTrue(scanner.adviceForUser().isPresent(), "a hard conflict must come with an instruction");
        assertTrue(scanner.adviceForUser().get().length() > 20,
                "the user needs a sentence, not a word: " + scanner.adviceForUser());
    }

    @Test
    @DisplayName("a fork under its own id is detected through the alias list")
    void forksMatchByAlias() {
        // The user installs Rubidium, whose mod id is not "embeddium". Detection has to
        // follow the fork, or Aetherium hooks the renderer underneath it.
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "rubidium"));
        scanner.scan(config);
        assertFalse(scanner.requiresIncompatible(), "a Sodium fork is treated exactly like Sodium");
        assertEquals(AetheriumConfig.LightMode.OFF, config.dynamicLights.get());
        assertEquals(1, scanner.getDetected().size(), "the fork should collapse into one row, not one per alias");
        assertEquals("embeddium", scanner.getDetected().get(0).modId());
        assertEquals("rubidium", scanner.getDetected().get(0).matchedId(new FakePlatform("1.21.1", "rubidium")));
        for (final String alias : scanner.getDetected().get(0).alternativeIds()) {
            assertFalse(alias.contains("."), "alternativeIds holds mod ids, not package names: " + alias);
        }
    }

    @Test
    @DisplayName("delegatable overlaps turn Aetherium's matching feature off")
    void delegatesTurnFeaturesOff() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        assertTrue(config.entityCulling.get(), "precondition: Aetherium's own culling starts on");
        final ModConflictScanner scanner = new ModConflictScanner(
                new FakePlatform("1.21.1", "entityculling-fabric", "lambdynamiclights"));
        scanner.scan(config);
        assertFalse(scanner.requiresIncompatible(), "neither of these is fatal");
        assertFalse(config.entityCulling.get(), "Entity Culling owns culling, so ours must go off");
        assertEquals(AetheriumConfig.LightMode.OFF, config.dynamicLights.get(),
                "LambDynamicLights owns light sources, so ours must go off");
        assertEquals(Severity.DELEGATE, scanner.getWorstSeverity());
        assertEquals(Ownership.AETHERIUM_RENDERER, scanner.getOwnership(), "we still own the renderer");
    }

    @Test
    @DisplayName("a brightness mod turns our fullbright off")
    void gammaDelegateDisablesFullbright() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        config.fullbright.set(true);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "gamma_utils"));
        scanner.scan(config);
        assertFalse(config.fullbright.get(), "two gamma overrides fight over the same option");
    }

    @Test
    @DisplayName("with auto-delegate off, the scanner reports and changes nothing")
    void autoDelegateDisabled() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        config.conflictAutoDelegate.set(false);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "entityculling"));
        scanner.scan(config);
        assertTrue(config.entityCulling.get(), "the user said no automatic changes, so none may happen");
        assertTrue(scanner.hasAnything(), "but the report must still be there for the HUD notice");
    }

    @Test
    @DisplayName("a shader mod is informational, not a conflict")
    void shaderModIsInformational() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "iris"));
        scanner.scan(config);
        assertFalse(scanner.requiresIncompatible());
        assertEquals(Severity.INFO, scanner.getWorstSeverity(), "a shader mod must not degrade the report: " + scanner.summarize());
        assertEquals(Ownership.SHADER_OWNER, scanner.getOwnership());
        assertEquals(AetheriumConfig.LightMode.FAST, config.dynamicLights.get(), "an informational row changes no feature");
    }

    @Test
    @DisplayName("the worst severity wins when several mods are present")
    void worstSeverityWins() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(
                new FakePlatform("1.21.1", "entityculling", "sodium", "optifine", "iris"));
        scanner.scan(config);
        assertEquals(Severity.HARD, scanner.getWorstSeverity());
        assertEquals(4, scanner.getDetected().size(), "every known mod must appear in the report");
        assertTrue(scanner.requiresIncompatible());
        // A HARD conflict is reported and the delegate pass is skipped: with the renderer
        // standing down anyway, mutating 20 options would only confuse the user's file.
        assertTrue(config.entityCulling.get(), "features are left alone when the mod is standing down");
    }

    @Test
    @DisplayName("scanning twice does not double-report or re-apply delegates")
    void scanIsIdempotent() {
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        // Dynamic lights default to OFF; turn them on so "the scan switched them off" is observable.
        config.dynamicLights.set(AetheriumConfig.LightMode.FAST);
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "entityculling"));
        scanner.scan(config);
        final int first = scanner.getDetected().size();
        config.entityCulling.set(true);
        scanner.scan(config);
        assertEquals(first, scanner.getDetected().size(), "a rescan must not duplicate rows");
        assertTrue(config.entityCulling.get(), "a rescan is a no-op, so it must not silently re-delegate");
    }

    @Test
    @DisplayName("every row that matched carries display name, advice and severity")
    void matchedRowsAreComplete() {
        // A row without advice is a HUD notice that tells the user nothing, so every
        // detection path is checked for the fields the GUI renders from.
        final ModConflictScanner scanner = new ModConflictScanner(new FakePlatform("1.21.1", "sodium"));
        scanner.scan(AetheriumConfig.createDefaults());
        final List<ModConflictScanner.KnownConflict> rows = scanner.getDetected();
        assertEquals(1, rows.size());
        final ModConflictScanner.KnownConflict sodium = rows.get(0);
        assertNotNull(sodium.advice());
        assertFalse(sodium.advice().isBlank());
        assertNotNull(sodium.displayName());
        assertNotNull(sodium.severity());
        assertNotNull(sodium.ownership());
        for (final String alias : sodium.alternativeIds()) {
            assertFalse(alias.contains("."), "alternativeIds holds mod ids, not package names: " + alias);
            assertFalse(alias.equals(sodium.modId()), "an alias identical to the id is noise: " + alias);
        }
    }

}
