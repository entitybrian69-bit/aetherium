package com.aetherium.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A single option's contract: parse, clamp, notify, serialize. The GUI, the file format
 * and every port delta lean on all four, and each has a distinct failure mode a user
 * would notice - so each gets its own test.
 */
final class ConfigValueTest {
    @Test
    @DisplayName("booleans accept the words people actually type into a config file")
    void booleanParsing() {
        final ConfigValue<Boolean> flag = ConfigValue.bool("general.flag", true, "test");
        for (final String truthy : new String[]{"true", "TRUE", " yes ", "1", "on"}) {
            final ConfigValue<Boolean> fresh = ConfigValue.bool("general.flag", true, "test");
            assertTrue(fresh.accept(truthy), "should parse: " + truthy);
            assertEquals(Boolean.TRUE, fresh.get(), "wrong value for: " + truthy);
        }
        for (final String falsy : new String[]{"false", "0", "no", "OFF"}) {
            final ConfigValue<Boolean> fresh = ConfigValue.bool("general.flag", true, "test");
            assertTrue(fresh.accept(falsy), "should parse: " + falsy);
            assertEquals(Boolean.FALSE, fresh.get(), "wrong value for: " + falsy);
        }
        // A value we cannot read must not silently become false: that is how "the mod
        // turned itself off" bug reports get invented.
        final ConfigValue<Boolean> untouched = ConfigValue.bool("general.flag", true, "test");
        assertFalse(untouched.accept("maybe"));
        assertTrue(untouched.get(), "a rejected value must leave the current value alone");
        assertFalse(untouched.accept(null), "null is a missing key, not a value");
    }

    @Test
    @DisplayName("an out-of-range number is clamped, not dropped")
    void numericClamping() {
        final ConfigValue<Integer> fps = ConfigValue.intRange("performance.max_fps", 60, 10, 260, false, "test");
        assertTrue(fps.accept(300), "a clamp is still an accepted value");
        assertEquals(260, fps.get().intValue());
        assertTrue(fps.accept(1));
        assertEquals(10, fps.get().intValue());
        assertTrue(fps.accept(120));
        assertEquals(120, fps.get().intValue());
        assertFalse(fps.accept("twelve"), "a word is not a number");
        assertEquals(120, fps.get().intValue());
        assertEquals(260, ((Integer) fps.clampToBounds(9000)).intValue());
        assertEquals(List.of("10", "260"), fps.describeRange());
    }

    @Test
    @DisplayName("a JSON integer lands in an int option without a cast error")
    void numberWideningFromJson() {
        // The reader in Json produces Long for every integer, so accept() must narrow.
        final ConfigValue<Integer> intOption = ConfigValue.intRange("performance.slider", 64, 1, 512, false, "test");
        assertTrue(intOption.accept(128L));
        assertEquals(128, intOption.get().intValue());

        final ConfigValue<Double> ratio = ConfigValue.doubleRange("quality.scale", 1.0d, 0.25d, 2.0d, "test");
        assertTrue(ratio.accept(1L), "an integral 1 must be readable as a double");
        assertEquals(1.0d, ratio.get(), 0.0d);
        assertTrue(ratio.accept(1536));
        assertEquals(2.0d, ratio.get(), 1.0E-9, "out of range clamps upward");
    }

    @Test
    @DisplayName("an enum option is case- and space-tolerant but never guesses a constant")
    void enumParsing() {
        final ConfigValue<AetheriumConfig.BackendChoice> backend = ConfigValue.enumerated(
                "performance.backend", AetheriumConfig.BackendChoice.AUTO, AetheriumConfig.BackendChoice.class,
                true, "test");
        assertTrue(backend.accept("gl46_dsa"));
        assertEquals(AetheriumConfig.BackendChoice.GL46_DSA, backend.get());
        assertTrue(backend.accept(" Vulkan 13 "));
        assertEquals(AetheriumConfig.BackendChoice.VULKAN_13, backend.get());
        assertFalse(backend.accept("directx12"));
        assertEquals(AetheriumConfig.BackendChoice.VULKAN_13, backend.get(), "a bad enum must not reset the option");
        assertEquals("vulkan_13", backend.serializeForJson(), "enums are written as lower-case names, which is what the lang files key on");
    }

    @Test
    @DisplayName("listeners fire once per real change, and never on a no-op write")
    void listenerSemantics() {
        final ConfigValue<Boolean> flag = ConfigValue.bool("general.flag", true, "test");
        final List<Boolean> seen = new ArrayList<>();
        flag.addListener(seen::add);
        flag.set(Boolean.TRUE);
        assertTrue(seen.isEmpty(), "setting the same value is not a change and must not trigger a rebuild");
        flag.set(Boolean.FALSE);
        assertEquals(List.of(Boolean.FALSE), seen);
        flag.accept(Boolean.FALSE);
        assertEquals(1, seen.size(), "accept() of an identical value must also stay silent");
        flag.reset();
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), seen);
        assertTrue(flag.isDefault());
    }

    @Test
    @DisplayName("a listener that writes another option cannot corrupt the notification")
    void reentrantListener() {
        final ConfigValue<Boolean> master = ConfigValue.bool("general.enabled", true, "test");
        final ConfigValue<Boolean> dependent = ConfigValue.bool("general.dependent", true, "test");
        final List<String> order = new ArrayList<>();
        master.addListener(value -> {
            order.add("master");
            dependent.set(Boolean.FALSE);
        });
        dependent.addListener(value -> order.add("dependent"));
        master.set(Boolean.FALSE);
        assertEquals(List.of("master", "dependent"), order, "the linked write must still be delivered");
        assertTrue(dependent.isDirty());
    }

    @Test
    @DisplayName("dirty tracking and the pending-apply flag drive the GUI's Apply button")
    void dirtyAndPendingApply() {
        final ConfigValue<AetheriumConfig.BackendChoice> restartOption = ConfigValue.enumerated(
                "performance.backend", AetheriumConfig.BackendChoice.AUTO, AetheriumConfig.BackendChoice.class,
                true, "test");
        assertFalse(restartOption.isPendingApply(), "a fresh option has nothing pending");
        restartOption.set(AetheriumConfig.BackendChoice.GL_CORE);
        assertTrue(restartOption.getRequiresRendererRestart());
        assertTrue(restartOption.isPendingApply(), "a renderer restart option must ask the GUI to offer Apply");
        restartOption.clearPendingApply();
        assertFalse(restartOption.isPendingApply());
        assertTrue(restartOption.isDirty(), "dirty is a separate concern from pending-apply");

        final ConfigValue<Boolean> liveOption = ConfigValue.bool("general.enabled", true, "test");
        liveOption.set(Boolean.FALSE);
        assertFalse(liveOption.getRequiresRendererRestart(), "a live option must not demand a restart");
        assertFalse(liveOption.isPendingApply());
    }

    @Test
    @DisplayName("the translation key is derived from the option key, which the lang files mirror")
    void keysAndMetadata() {
        final ConfigValue<String> text = ConfigValue.string("general.hud.corner", "top-left", "anchor");
        assertEquals("general.hud.corner", text.getKey());
        assertEquals("aetherium.option.general.hud.corner", text.getTranslationKey());
        assertEquals("anchor", text.getComment());
        assertEquals("top-left", text.getDefault());
        assertTrue(text.describeRange().isEmpty(), "a string option has no numeric range");
        assertThrows(NullPointerException.class, () -> ConfigValue.string("bad", null, "a null default is a bug"));
    }
}
