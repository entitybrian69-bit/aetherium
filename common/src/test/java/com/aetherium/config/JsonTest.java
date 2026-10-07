package com.aetherium.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The hand-written JSON codec. It is hand-written on purpose - a config parser must not
 * break when Minecraft swaps GSON versions - which also means it now has to be tested
 * like the library it has become.
 */
final class JsonTest {
    @Test
    @DisplayName("write then parse returns an equal tree")
    void roundTrip() {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("fps", Boolean.TRUE);
        nested.put("corner", "top-left");
        nested.put("count", 12);
        nested.put("ratio", 0.25d);
        nested.put("nothing", null);
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", 3L);
        root.put("aetherium", nested);
        root.put("list", List.of(1, 2, 3));

        final Map<String, Object> back = Json.parseObject(Json.write(root));

        assertEquals(3L, ((Number) back.get("version")).longValue());
        @SuppressWarnings("unchecked")
        final Map<String, Object> inner = (Map<String, Object>) back.get("aetherium");
        assertEquals(Boolean.TRUE, inner.get("fps"));
        assertEquals("top-left", inner.get("corner"));
        assertEquals(12, ((Number) inner.get("count")).intValue());
        assertEquals(0.25d, ((Number) inner.get("ratio")).doubleValue(), 1.0E-9);
        // An explicit null is preserved rather than dropped: ConfigStore distinguishes
        // "key absent" (a new option, take the default) from "key null" (the user
        // cleared it, keep it clear).
        assertTrue(inner.containsKey("nothing"), "an explicit null did not survive the round trip");
        assertNull(inner.get("nothing"));
        @SuppressWarnings("unchecked")
        final List<Object> list = (List<Object>) back.get("list");
        assertEquals(3, list.size());
        assertEquals(2, ((Number) list.get(1)).intValue());
    }

    @Test
    @DisplayName("escapes, control characters and non-ASCII survive as data")
    void stringEscapes() {
        final Map<String, Object> root = new LinkedHashMap<>();
        final String tricky = "a\nb\"c" + '\t' + "d" + '\\' + "e ünïcødé <tag/>";
        root.put("tricky", tricky);
        root.put("backslash-n-as-text", "a\\nb");

        final Map<String, Object> back = Json.parseObject(Json.write(root));
        assertEquals(tricky, back.get("tricky"), "a string round trip must be byte-exact");
        assertEquals("a\\nb", back.get("backslash-n-as-text"), "the two-character sequence must not become a newline");
        // Control characters must be escaped on write, or the file is not valid JSON.
        final String written = Json.write(root);
        assertTrue(written.indexOf('\n') >= 0, "the writer pretty-prints, so newlines exist as formatting");
        assertTrue(!Json.write("x" + (char) 1).contains(String.valueOf((char) 1)),
                "a raw control byte must never be written into the file");
    }

    @Test
    @DisplayName("non-finite numbers become null, because JSON cannot hold them")
    void nonFiniteNumbers() {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("nan", Double.NaN);
        root.put("inf", Double.POSITIVE_INFINITY);
        final String written = Json.write(root);
        assertTrue(written.contains("null"), "expected nulls, got: " + written);
        assertTrue(!written.toLowerCase().contains("nan") && !written.toLowerCase().contains("infinity"),
                "the writer emitted a literal a JSON reader cannot parse");
        // And the reader takes them back as null, so ConfigValue falls to its default.
        final Map<String, Object> back = Json.parseObject(written);
        assertNull(back.get("nan"));
        assertNull(back.get("inf"));
    }

    @Test
    @DisplayName("malformed input reports a position, it does not return a partial tree")
    void malformedInputThrowsWithLocation() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("{unterminated:"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("[1, 2"))
                .getMessage();
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"unterminated string"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("nul"));
        assertThrows(IllegalArgumentException.class, () -> Json.parse("0x10"));
    }

    @Test
    @DisplayName("the error message carries a line and column a user can act on")
    void errorCarriesPosition() {
        final String bad = "{\n  \"a\": 1,\n  \"b\": tru\n}";
        final IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> Json.parse(bad));
        final String message = failure.getMessage();
        assertTrue(message != null && message.matches("(?s).*(line|row|Ln)[^0-9]*3.*"),
                "expected a line 3 hint, got: " + message);
        assertTrue(message.matches("(?s).*(col|column)[^0-9]*\\d+.*"),
                "expected a column hint, got: " + message);
    }

    @Test
    @DisplayName("comments and trailing whitespace are tolerated in a hand-edited file")
    void tolerantReading() {
        final String withComments = """
                  // header comment from a pack author
                  {
                    "version": 3, // trailing comment on a value
                    "aetherium": { "enabled": true }
                  }
                """;
        final Map<String, Object> back = Json.parseObject(withComments);
        assertEquals(3L, ((Number) back.get("version")).longValue());
        @SuppressWarnings("unchecked")
        final Map<String, Object> section = (Map<String, Object>) back.get("aetherium");
        assertEquals(Boolean.TRUE, section.get("enabled"));
    }

    @Test
    @DisplayName("path() reads nested keys and reports a type mismatch instead of lying")
    void dottedPath() {
        final Map<String, Object> back = Json.parseObject("""
                { "aetherium": { "gamma": { "amount": 0.65, "on": true }, "list": [1, 2] } }
                """);
        assertEquals(0.65d, ((Number) Json.path(back, "aetherium.gamma.amount")).doubleValue(), 1.0E-9);
        assertEquals(Boolean.TRUE, Json.path(back, "aetherium.gamma.on"));
        assertNull(Json.path(back, "aetherium.gamma.missing"), "a missing leaf is null, not an exception");
        assertNull(Json.path(back, "aetherium.nowhere.deep"), "a missing branch is null too");
        // Walking *through* a scalar is indistinguishable from a missing key, and the
        // store treats both as "this file does not describe that option" - so null, not
        // an exception. Pinned here because a future "helpful" throw would turn a stale
        // config key into a startup crash.
        assertNull(Json.path(back, "aetherium.gamma.amount.nested"));
        assertNull(Json.path(null, "aetherium"));
    }

    @Test
    @DisplayName("flatten() produces dotted keys for every leaf")
    void flatten() {
        final Map<String, Object> back = Json.parseObject("""
                { "a": { "b": { "c": 1 }, "d": 2 }, "e": [3, 4] }
                """);
        final Map<String, Object> flat = Json.flatten(back);
        assertEquals(1, ((Number) flat.get("a.b.c")).intValue());
        assertEquals(2, ((Number) flat.get("a.d")).intValue());
        // Lists are leaves: the config format is two levels deep and never indexes into
        // an array, so flattening one would invent keys that serialize back wrong.
        assertInstanceOf(List.class, flat.get("e"));
        assertEquals(2, ((List<?>) flat.get("e")).size());
        assertEquals(3, ((Number) ((List<?>) flat.get("e")).get(0)).intValue());
        assertEquals(2, flat.size(), "flatten produced " + flat.keySet());
    }

    @Test
    @DisplayName("an empty container round-trips, so a cleared preset file stays parseable")
    void emptyContainers() {
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("object", new LinkedHashMap<String, Object>());
        root.put("list", List.of());
        final Map<String, Object> back = Json.parseObject(Json.write(root));
        assertInstanceOf(Map.class, back.get("object"));
        assertTrue(((Map<?, ?>) back.get("object")).isEmpty());
        assertInstanceOf(List.class, back.get("list"));
        assertTrue(((List<?>) back.get("list")).isEmpty());
    }
}
