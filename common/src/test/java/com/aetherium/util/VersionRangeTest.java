package com.aetherium.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The range grammar the porting system is written in. If {@code [1.20.2,)} means the
 * wrong thing, a mixin is applied on a version where its target does not exist (a
 * crash) or withheld where it does (a silent missing feature) - and both look like
 * someone else's bug in the issue tracker.
 */
final class VersionRangeTest {
    @Test
    @DisplayName("a componentwise compare beats a lexicographic one where Minecraft needs it")
    void compareIsNumericPerComponent() {
        assertTrue(VersionRange.compare("1.21.10", "1.21.9") > 0, "1.21.10 must outrank 1.21.9");
        assertTrue(VersionRange.compare("1.20.1", "1.20.10") < 0);
        assertEquals(0, VersionRange.compare("1.21", "1.21.0"));
        assertTrue(VersionRange.compare("1.19.4", "1.20") < 0);
        assertTrue(VersionRange.compare("26.3", "1.21.1") > 0, "the date-based ids must sort after 1.x");
        assertTrue(VersionRange.compare("1.20.2-rc1", "1.20.2") < 0, "a prerelease must sort before its release");
    }

    @Test
    @DisplayName("inclusive/exclusive bounds behave as Gradle intervals do")
    void intervalEnds() {
        assertTrue(VersionRange.contains("1.20.2", "[1.20.2,)"));
        assertTrue(VersionRange.contains("99.0", "[1.20.2,)"));
        assertFalse(VersionRange.contains("1.20.1", "[1.20.2,)"));
        assertTrue(VersionRange.contains("1.20.4", "[1.20.2,1.20.4]"));
        assertFalse(VersionRange.contains("1.20.4", "[1.20.2,1.20.4)"), "a ')' end must exclude");
        assertTrue(VersionRange.contains("1.17", "[,1.18)"), "an empty lower bound means -infinity");
    }

    @Test
    @DisplayName("the wildcard range allows everything, because that is what '*' documents")
    void wildcardIsPermissive() {
        assertTrue(VersionRange.contains("1.16.5", VersionRange.ANY));
        assertTrue(VersionRange.contains("26.3", VersionRange.ANY));
        assertTrue(VersionRange.contains("1.21.1", ""), "an empty range means ungated");
        assertTrue(VersionRange.contains("1.21.1", null));
    }

    @Test
    @DisplayName("a malformed range refuses, it does not guess")
    void malformedRangesFailClosed() {
        assertFalse(VersionRange.contains("1.21.1", "1.20.2"), "a bare version is not an interval");
        assertFalse(VersionRange.contains("1.21.1", "[1.20.2"));
        assertFalse(VersionRange.contains("1.21.1", "nonsense"));
        assertFalse(VersionRange.isWellFormed("1.20.2"));
        assertFalse(VersionRange.isWellFormed("[1.20.2"));
        assertFalse(VersionRange.isWellFormed("[abc,def]"));
        assertTrue(VersionRange.isWellFormed("[1.20.2,)"));
        assertTrue(VersionRange.isWellFormed("[,1.17)"));
        assertTrue(VersionRange.isWellFormed(VersionRange.ANY));
    }

    @Test
    @DisplayName("every range string generated for a port is well formed")
    void generatedRangesAreUsable() {
        // Mirrors what tools/gen_deltas.py writes into AetheriumMixinPlugin. If a table
        // row ever produces a string this cannot read, the generator must fail, and the
        // cheapest place to notice is here.
        for (final String range : new String[]{"*", "[1.17.4,)", "[9999,)", "[1.20.2,1.21)", "[1.16.5,)"}) {
            assertTrue(VersionRange.isWellFormed(range), "unusable range emitted: " + range);
        }
        assertFalse(VersionRange.contains("1.21.1", "[9999,)"),
                "the 'disabled on this version' sentinel must exclude every real version");
    }
}
