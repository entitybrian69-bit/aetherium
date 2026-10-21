package com.aetherium.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.EarlyConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 1.17.2 performance features: shared state tables, HUD refresh rate, early flags, crash guard. */
final class Tuning117LogicTest {

    /** A fake block: properties "facing" and "lit"; states are strings. */
    private static Map<Map<?, ?>, Object> states(final List<Object> facing, final List<Object> lit) {
        final Map<Map<?, ?>, Object> out = new HashMap<Map<?, ?>, Object>();
        for (final Object f : facing) {
            for (final Object l : lit) {
                out.put(values(f, l), f + "/" + l);
            }
        }
        return out;
    }

    private static Map<Object, Object> values(final Object facing, final Object lit) {
        final Map<Object, Object> values = new LinkedHashMap<Object, Object>();
        values.put("facing", facing);
        values.put("lit", lit);
        return values;
    }

    private static List<Collection<?>> valuesOf(final List<Object> facing, final List<Object> lit) {
        final List<Collection<?>> out = new ArrayList<Collection<?>>();
        out.add(facing);
        out.add(lit);
        return out;
    }

    @Test
    @DisplayName("compact block states: neighbours match vanilla's per-state table")
    void stateIndex() {
        final List<Object> facing = Arrays.<Object>asList("north", "east", "south", "west");
        final List<Object> lit = Arrays.<Object>asList(Boolean.FALSE, Boolean.TRUE);
        final Map<Map<?, ?>, Object> byValues = states(facing, lit);
        final StateIndex index = StateIndex.build(Arrays.<Object>asList("facing", "lit"), valuesOf(facing, lit), byValues);
        assertEquals(8, index.size());
        assertEquals(4, index.neighbourCount(), "3 other facings + 1 other lit");
        for (final Map.Entry<Map<?, ?>, Object> e : byValues.entrySet()) {
            final int at = index.indexOf(e.getKey());
            assertEquals(e.getValue(), index.state(at));
            final Object f = e.getKey().get("facing");
            final boolean l = ((Boolean) e.getKey().get("lit")).booleanValue();
            for (final Object other : facing) {
                assertEquals(other + "/" + l, index.neighbour(at, "facing", other));
            }
            assertEquals(f + "/" + !l, index.neighbour(at, "lit", Boolean.valueOf(!l)));
            assertNull(index.neighbour(at, "facing", "up"), "not a value of the property");
            assertNull(index.neighbour(at, "power", Integer.valueOf(1)), "not a property of the block");
        }
        assertEquals(-1, index.indexOf(values("up", Boolean.TRUE)));
        final int[] visited = {0};
        index.forEachNeighbour(index.indexOf(values("east", Boolean.TRUE)), (p, v, s) -> visited[0]++);
        assertEquals(6, visited[0], "every value of every property, own values included");
    }

    @Test
    @DisplayName("compact block states: an incomplete combination keeps vanilla's tables")
    void stateIndexIncomplete() {
        final List<Object> facing = Arrays.<Object>asList("north", "east");
        final List<Object> lit = Arrays.<Object>asList(Boolean.FALSE, Boolean.TRUE);
        final Map<Map<?, ?>, Object> byValues = states(facing, lit);
        byValues.remove(values("east", Boolean.TRUE));
        assertNull(StateIndex.build(Arrays.<Object>asList("facing", "lit"), valuesOf(facing, lit), byValues));
        assertNull(StateIndex.build(new ArrayList<Object>(), new ArrayList<Collection<?>>(), byValues), "no properties");
    }

    @Test
    @DisplayName("HUD refresh rate: direct when off or slow, then refresh / reuse at the chosen rate")
    void hudRate() {
        HudRate.reset();
        long now = 1_000_000_000L;
        final long frame = 1_000_000_000L / 200; // 200 fps
        assertEquals(HudRate.DIRECT, HudRate.decide(now, 100, 100, 1L, false), "off");
        HudRate.setFps(30);
        assertEquals(30, HudRate.fps());
        int refresh = 0;
        int reuse = 0;
        for (int i = 0; i < 400; i++) {
            now += frame;
            final int d = HudRate.decide(now, 100, 100, 1L, false);
            if (d == HudRate.REFRESH) {
                refresh++;
            } else if (d == HudRate.REUSE) {
                reuse++;
            }
        }
        assertTrue(refresh >= 55 && refresh <= 62, "about 30 refreshes a second over 2 s: " + refresh);
        assertEquals(400 - refresh, reuse);
        now += frame;
        assertEquals(HudRate.REFRESH, HudRate.decide(now, 100, 100, 2L, false), "state change refreshes at once");
        now += frame;
        assertEquals(HudRate.REFRESH, HudRate.decide(now, 120, 100, 2L, false), "resize refreshes at once");
        now += frame;
        assertEquals(HudRate.DIRECT, HudRate.decide(now, 120, 100, 2L, true), "live HUD is drawn directly");
        HudRate.setFps(HudRate.MAX + 50);
        assertEquals(HudRate.MAX, HudRate.fps());
        HudRate.setCapturing(true);
        HudRate.reset();
        assertFalse(HudRate.capturing());
    }

    @Test
    @DisplayName("HUD refresh rate: picture alpha stays right for normal and additive blending")
    void hudAlpha() {
        assertEquals(HudRate.GL_ONE << 16 | HudRate.GL_ONE_MINUS_SRC_ALPHA,
                HudRate.alphaFactorsFor(HudRate.GL_ONE_MINUS_SRC_ALPHA));
        assertEquals(HudRate.GL_ZERO << 16 | HudRate.GL_ONE, HudRate.alphaFactorsFor(HudRate.GL_ONE));
    }

    @Test
    @DisplayName("early flags: the master switch and the option are read before the config loads")
    void earlyFlags() {
        assertTrue(EarlyFlags.decide(null, "compact_block_states", true), "no file: default");
        assertTrue(EarlyFlags.decide(EarlyConfig.parse("{\"aetherium\":{\"performance\":{}}}"), "compact_block_states", true));
        assertFalse(EarlyFlags.decide(EarlyConfig.parse(
                "{\"aetherium\":{\"performance\":{\"compact_block_states\":false}}}"), "compact_block_states", true));
        assertFalse(EarlyFlags.decide(EarlyConfig.parse(
                "{\"aetherium\":{\"general\":{\"enabled\":false},\"performance\":{\"compact_block_states\":true}}}"),
                "compact_block_states", true), "Aetherium turned off");
    }

    @Test
    @DisplayName("pipeline crash guard: a crash while drawing packed terrain turns only packing off")
    void packedCrash() throws Exception {
        final Path dir = Files.createTempDirectory("aetherium-guard");
        final AetheriumConfig config = AetheriumConfig.createDefaults();
        config.aetheriumRenderer.set(Boolean.TRUE);
        config.packedTerrain.set(Boolean.TRUE);
        assertFalse(PipelineCrashGuard.checkAtStartup(dir, config));
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_PACKED);
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.packedTerrain.get());
        assertEquals(Boolean.TRUE, config.regionBatching.get(), "batching stays on");
        assertEquals(Boolean.TRUE, config.aetheriumRenderer.get(), "the pipeline stays on");
        assertTrue(PipelineCrashGuard.wasPackedTurnedOff());
        assertFalse(PipelineCrashGuard.wasBatchingTurnedOff());

        // Packed already off: the next packed-mode crash turns batching off.
        PipelineCrashGuard.arm(PipelineCrashGuard.MODE_PACKED);
        assertTrue(PipelineCrashGuard.checkAtStartup(dir, config));
        assertEquals(Boolean.FALSE, config.regionBatching.get());
        assertTrue(PipelineCrashGuard.wasBatchingTurnedOff());
        assertFalse(PipelineCrashGuard.wasPackedTurnedOff());
        PipelineCrashGuard.disarm();
    }
}
