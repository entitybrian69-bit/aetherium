package com.aetherium.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

import com.aetherium.util.MathUtil;

/**
 * One validated, observable configuration entry.
 *
 * <p>Thread-safety: {@code value}, {@code dirty} and the lifecycle flags are
 * {@code volatile}; writes are single-word and every reader tolerates a stale
 * read for one frame (this is a renderer setting, not a financial ledger). The
 * listener list is copy-on-write because the GUI adds listeners on the render
 * thread while worker threads may observe changes during mesh rebuilds —
 * iterating an ArrayList there would be a real race.</p>
 *
 * <p>Deferred application: options that cannot be changed mid-frame set
 * {@link #getRequiresRendererRestart()} or {@link #getRequiresWorldReload()};
 * {@code ConfigStore} then records the value as <em>pending</em> and the GUI
 * offers "Apply now". This is what makes "switch backend without restart"
 * honest — the swap happens at a safe point, not under a live draw call.</p>
 *
 * @param <T> the value type
 */
public final class ConfigValue<T> {
    private final String key;
    private final T defaultValue;
    private final Function<T, String> serializer;
    private final Function<String, Optional<T>> parser;
    private final T min;
    private final T max;
    private final String translationKey;
    private final String comment;
    private final Flags flags;

    /** Copy-on-write: mutated from the render thread, iterated from workers. */
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    /** All guards below are volatile writes; last writer wins (GUI is the only writer in practice). */
    private volatile T value;
    private volatile boolean dirty;
    private volatile boolean pendingApply;

    private ConfigValue(final String key, final T defaultValue, final Function<T, String> serializer,
                        final Function<String, Optional<T>> parser, final T min, final T max,
                        final String translationKey, final String comment, final Flags flags) {
        this.key = Objects.requireNonNull(key, "key");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.serializer = Objects.requireNonNull(serializer, "serializer");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.min = min;
        this.max = max;
        this.translationKey = translationKey == null ? "aetherium.option." + key : translationKey;
        this.comment = comment == null ? "" : comment;
        this.flags = flags;
        this.value = defaultValue;
    }

    // ------------------------------------------------------------------ factories

    public static ConfigValue<Boolean> bool(final String key, final boolean defaultValue, final String comment) {
        return new ConfigValue<>(key, defaultValue, String::valueOf, text -> {
            final String normalized = text.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("on")) {
                return Optional.of(Boolean.TRUE);
            }
            if (normalized.equals("false") || normalized.equals("0") || normalized.equals("no") || normalized.equals("off")) {
                return Optional.of(Boolean.FALSE);
            }
            return Optional.empty();
        }, null, null, null, comment, new Flags(false, false));
    }

    public static ConfigValue<Integer> intRange(final String key, final int defaultValue, final int min,
                                                final int max, final boolean worldReload, final String comment) {
        return new ConfigValue<>(key, defaultValue, String::valueOf, text -> {
            try {
                return Optional.of(Integer.parseInt(text.trim()));
            } catch (final NumberFormatException error) {
                return Optional.empty();
            }
        }, min, max, null, comment, new Flags(worldReload, false));
    }

    public static ConfigValue<Double> doubleRange(final String key, final double defaultValue, final double min,
                                                   final double max, final String comment) {
        return new ConfigValue<>(key, defaultValue, value -> String.format(Locale.ROOT, "%.5f", value), text -> {
            try {
                return Optional.of(Double.parseDouble(text.trim()));
            } catch (final NumberFormatException error) {
                return Optional.empty();
            }
        }, min, max, null, comment, new Flags(false, false));
    }

    public static <E extends Enum<E>> ConfigValue<E> enumerated(final String key, final E defaultValue,
                                                                 final Class<E> type, final boolean rendererRestart,
                                                                 final String comment) {
        final E[] constants = type.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("enum " + type.getName() + " has no constants");
        }
        return new ConfigValue<>(key, defaultValue, Enum::name, text -> {
            final String normalized = text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            for (final E constant : constants) {
                if (constant.name().equals(normalized)) {
                    return Optional.of(constant);
                }
            }
            return Optional.empty();
        }, null, null, null, comment, new Flags(false, rendererRestart));
    }

    public static ConfigValue<String> string(final String key, final String defaultValue, final String comment) {
        return new ConfigValue<>(key, defaultValue, Function.identity(), Optional::of, null, null, null, comment, new Flags(false, false));
    }

    // -------------------------------------------------------------------- access

    public String getKey() {
        return this.key;
    }

    public T get() {
        return this.value;
    }

    public T getDefault() {
        return this.defaultValue;
    }

    public boolean isDefault() {
        return Objects.equals(this.value, this.defaultValue);
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getComment() {
        return this.comment;
    }

    /** Null for unbounded options. */
    public Object getMin() {
        return this.min;
    }

    public Object getMax() {
        return this.max;
    }

    public boolean getRequiresWorldReload() {
        return this.flags.worldReload;
    }

    public boolean getRequiresRendererRestart() {
        return this.flags.rendererRestart;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    /**
     * Clears the dirty flag. Called by {@link AetheriumConfig#clearDirty()} after a
     * successful write; re-setting the current value would not clear it, because
     * {@link #set(Object)} only ever sets the flag on change.
     */
    void clearDirtyFlag() {
        this.dirty = false;
    }

    /** True when the value is stored but not yet applied at a safe point. */
    public boolean isPendingApply() {
        return this.pendingApply;
    }

    public void clearPendingApply() {
        this.pendingApply = false;
    }

    public void addListener(final Consumer<T> listener) {
        this.listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /**
     * Parses a value from a config file or a GUI widget.
     *
     * @return false when the input could not be read at all (the caller keeps the current
     *         value and warns); a numeric value that is readable but out of range is
     *         clamped and reported as accepted, because silently reverting a hand-edited
     *         {@code "fps": 300} to the default is worse than giving the user 260
     */
    public boolean accept(final Object raw) {
        if (raw == null) {
            return false;
        }
        final T parsed;
        if (typeMatches(raw)) {
            parsed = typeCast(raw);
        } else {
            final Optional<T> fromString = this.parser.apply(String.valueOf(raw));
            if (fromString.isEmpty()) {
                return false;
            }
            parsed = fromString.get();
        }
        if (!withinBounds(parsed)) {
            // clampToBounds is Object-in/Object-out on purpose (the slider widget calls it that
            // way, and ConfigValueTest asserts it); typeCast is this class's own tested
            // Integer/Long/Double normaliser, so use it rather than a bare (T) cast.
            this.set(this.typeCast(clampToBounds(parsed)));
            return true;
        }
        this.set(parsed);
        return true;
    }

    public void set(final T newValue) {
        Objects.requireNonNull(newValue, "newValue for " + this.key);
        final T previous = this.value;
        this.value = newValue;
        if (!Objects.equals(previous, newValue)) {
            this.dirty = true;
            if (this.flags.rendererRestart) {
                this.pendingApply = true;
            }
            // Snapshot already copied by CopyOnWriteArrayList: a listener that
            // mutates another option (linked options) cannot corrupt this loop.
            for (final Consumer<T> listener : this.listeners) {
                listener.accept(newValue);
            }
        }
    }

    public void reset() {
        this.set(this.defaultValue);
    }

    public String serialize() {
        return this.serializer.apply(this.value);
    }

    public Object serializeForJson() {
        final T current = this.value;
        if (current instanceof Boolean || current instanceof String) {
            return current;
        }
        if (current instanceof Integer) {
            return ((Integer) current).longValue();
        }
        if (current instanceof Double) {
            return ((Double) current).doubleValue();
        }
        if (current instanceof Enum) {
            return ((Enum<?>) current).name().toLowerCase(Locale.ROOT);
        }
        return String.valueOf(current);
    }

    /** Numeric clamping for the slider widget; non-numeric options pass through. */
    public Object clampToBounds(final Object candidate) {
        if (candidate instanceof Double && this.min instanceof Double) {
            return MathUtil.clamp(((Double) candidate).doubleValue(), ((Double) this.min).doubleValue(), ((Double) this.max).doubleValue());
        }
        if (candidate instanceof Integer && this.min instanceof Integer) {
            return MathUtil.clamp(((Integer) candidate).intValue(), ((Integer) this.min).intValue(), ((Integer) this.max).intValue());
        }
        return candidate;
    }

    public List<String> describeRange() {
        final List<String> out = new ArrayList<>(2);
        if (this.min != null && this.max != null) {
            out.add(String.valueOf(this.min));
            out.add(String.valueOf(this.max));
        }
        return out;
    }

    private boolean typeMatches(final Object raw) {
        return this.defaultValue.getClass().isInstance(raw);
    }

    @SuppressWarnings("unchecked")
    private T typeCast(final Object raw) {
        if (this.defaultValue instanceof Integer && raw instanceof Long) {
            return (T) (Object) (Integer) Math.toIntExact((Long) raw);
        }
        if (this.defaultValue instanceof Double && raw instanceof Long) {
            return (T) (Object) (Double) (double) (long) (Long) raw;
        }
        if (this.defaultValue instanceof Double && raw instanceof Integer) {
            return (T) (Object) (Double) (double) (int) (Integer) raw;
        }
        return (T) raw;
    }

    private boolean withinBounds(final T candidate) {
        if (this.min == null || this.max == null) {
            return true;
        }
        if (candidate instanceof Double) {
            final double value = ((Double) candidate).doubleValue();
            return value >= ((Double) this.min).doubleValue() && value <= ((Double) this.max).doubleValue();
        }
        if (candidate instanceof Integer) {
            final int value = ((Integer) candidate).intValue();
            return value >= ((Integer) this.min).intValue() && value <= ((Integer) this.max).intValue();
        }
        return true;
    }

    /** Immutable lifecycle metadata, kept out of the mutable state above. */
    private static final class Flags {
        private final boolean worldReload;
        private final boolean rendererRestart;

        Flags(final boolean worldReload, final boolean rendererRestart) {
            this.worldReload = worldReload;
            this.rendererRestart = rendererRestart;
        }
    }
}
