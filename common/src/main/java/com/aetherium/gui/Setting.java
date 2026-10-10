package com.aetherium.gui;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * One row of the settings screen. Values are staged: the row reads the live
 * value once when the screen opens ({@link #load}), edits only change
 * {@link #pending}, and {@link #commit} writes it back when the user presses
 * Apply/Done. Every value is an int (booleans are 0/1, choices are indices),
 * which keeps staging, presets and reset trivially uniform.
 */
public final class Setting {
    public enum Kind { TOGGLE, SEGMENTED, DROPDOWN, SLIDER, INFO, BUTTON }

    /** Marker for "no default" (vanilla options keep their own defaults). */
    public static final int NO_DEFAULT = Integer.MIN_VALUE;

    public final String id;
    public final Kind kind;
    public final String label;
    public final String description;

    IntSupplier reader;
    IntConsumer writer;
    String[] choices = new String[0];
    int min;
    int max = 1;
    int step = 1;
    IntFunction<String> format;
    Supplier<String> info;
    Runnable action;
    String buttonText = "";
    BooleanSupplier available;
    String unavailableReason = "";
    /** Changing this row means chunk meshes must be rebuilt after Apply. */
    boolean reloadsChunks;
    /** Editing this row by hand turns the performance preset into Custom. */
    boolean presetMember;
    int defaultValue = NO_DEFAULT;
    /** Invoked by the view right after the user picks a new value. */
    IntConsumer onPick;

    int pending;
    int committed;
    private boolean loadedOnce;
    /** Slider track bounds from the last frame, used for hit testing. */
    int sliderX1;
    int sliderX2 = 1;

    // Animation state owned by the view.
    final Anim hover = new Anim(0f, 18f);
    final Anim knob = new Anim(0f, 16f);
    final Anim open = new Anim(0f, 16f);

    private Setting(final String id, final Kind kind, final String label, final String description) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.description = description == null ? "" : description;
    }

    // ------------------------------------------------------------- builders

    public static Setting toggle(final String id, final String label, final String description,
                                 final IntSupplier reader, final IntConsumer writer) {
        final Setting s = new Setting(id, Kind.TOGGLE, label, description);
        s.reader = reader;
        s.writer = writer;
        s.max = 1;
        return s;
    }

    public static Setting choice(final String id, final Kind kind, final String label, final String description,
                                 final String[] choices, final IntSupplier reader, final IntConsumer writer) {
        final Setting s = new Setting(id, kind, label, description);
        s.reader = reader;
        s.writer = writer;
        s.choices = choices.clone();
        s.min = 0;
        s.max = choices.length - 1;
        return s;
    }

    public static Setting slider(final String id, final String label, final String description, final int min,
                                 final int max, final int step, final IntFunction<String> format,
                                 final IntSupplier reader, final IntConsumer writer) {
        final Setting s = new Setting(id, Kind.SLIDER, label, description);
        s.reader = reader;
        s.writer = writer;
        s.min = min;
        s.max = Math.max(min + 1, max);
        s.step = Math.max(1, step);
        s.format = format;
        return s;
    }

    public static Setting info(final String id, final String label, final String description, final Supplier<String> info) {
        final Setting s = new Setting(id, Kind.INFO, label, description);
        s.info = info;
        return s;
    }

    public static Setting button(final String id, final String label, final String description, final String buttonText,
                                 final Runnable action) {
        final Setting s = new Setting(id, Kind.BUTTON, label, description);
        s.buttonText = buttonText;
        s.action = action;
        return s;
    }

    // ------------------------------------------------------------- modifiers

    public Setting availableWhen(final BooleanSupplier condition, final String reason) {
        this.available = condition;
        this.unavailableReason = reason == null ? "" : reason;
        return this;
    }

    public Setting reloadsChunks() {
        this.reloadsChunks = true;
        return this;
    }

    public Setting presetMember() {
        this.presetMember = true;
        return this;
    }

    public Setting withDefault(final int value) {
        this.defaultValue = value;
        return this;
    }

    public Setting onPick(final IntConsumer listener) {
        this.onPick = listener;
        return this;
    }

    // ---------------------------------------------------------------- state

    public boolean isAvailable() {
        if (this.available == null) {
            return true;
        }
        try {
            return this.available.getAsBoolean();
        } catch (final RuntimeException | LinkageError error) {
            return false;
        }
    }

    public String unavailableReason() {
        return this.unavailableReason;
    }

    /** True when committing this row must be followed by a chunk rebuild. */
    public boolean needsChunkReload() {
        return this.reloadsChunks;
    }

    public boolean isEditable() {
        return this.writer != null && isAvailable();
    }

    /** Reads the live value; called when the screen opens and after Apply. */
    public void load() {
        if (this.reader == null) {
            return;
        }
        int value;
        try {
            value = this.reader.getAsInt();
        } catch (final RuntimeException | LinkageError error) {
            value = this.min;
        }
        this.committed = clamp(value);
        this.pending = this.committed;
        if (!this.loadedOnce) {
            this.knob.snap(Math.max(0, this.pending));
            this.loadedOnce = true;
        }
    }

    public boolean isDirty() {
        return this.writer != null && this.pending != this.committed;
    }

    public void commit() {
        if (!isDirty()) {
            return;
        }
        this.writer.accept(this.pending);
        this.committed = this.pending;
    }

    public int pending() {
        return this.pending;
    }

    public int committed() {
        return this.committed;
    }

    /** Stages a value (clamped and snapped to the step). Returns true when it changed. */
    public boolean stage(final int value) {
        final int next = clamp(value);
        if (next == this.pending) {
            return false;
        }
        this.pending = next;
        return true;
    }

    int clamp(final int value) {
        if (this.kind == Kind.SEGMENTED && value == -1) {
            return -1; // "no selection" (Custom preset)
        }
        int v = Math.max(this.min, Math.min(this.max, value));
        if (this.kind == Kind.SLIDER && this.step > 1) {
            v = this.min + Math.round((v - this.min) / (float) this.step) * this.step;
            v = Math.max(this.min, Math.min(this.max, v));
        }
        return v;
    }

    public int min() {
        return this.min;
    }

    public int max() {
        return this.max;
    }

    public String[] choices() {
        return this.choices.clone();
    }

    public String formatted() {
        switch (this.kind) {
            case SLIDER:
                return this.format == null ? Integer.toString(this.pending) : this.format.apply(this.pending);
            case SEGMENTED:
            case DROPDOWN:
                return this.pending >= 0 && this.pending < this.choices.length ? this.choices[this.pending] : "Custom";
            case TOGGLE:
                return this.pending != 0 ? "ON" : "OFF";
            case INFO:
                try {
                    final String text = this.info == null ? "" : this.info.get();
                    return text == null ? "" : text;
                } catch (final RuntimeException | LinkageError error) {
                    return "Unavailable";
                }
            default:
                return this.buttonText;
        }
    }

    public boolean resetToDefault() {
        return this.defaultValue != NO_DEFAULT && stage(this.defaultValue);
    }

    /** Runs the BUTTON action. */
    public void press() {
        if (this.action != null) {
            this.action.run();
        }
    }
}
