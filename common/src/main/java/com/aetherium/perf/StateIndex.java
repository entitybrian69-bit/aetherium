package com.aetherium.perf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Compact block states (1.17.2): one shared lookup per block instead of a property table per
 * block state.
 *
 * <p>Vanilla gives every block state its own table of "this state with property P set to value
 * V" (built once per state when the block registers). A block with three properties and a dozen
 * values in total keeps a dozen-cell table, two key lists and two index maps in every one of its
 * states; across all registered blocks that is tens of megabytes, and far more with large mod
 * packs. All states of one block are the full combination of its property values, so the same
 * answer can be computed from one flat array per block: a state's position in that array is a
 * mixed-radix number with one digit per property, and changing property P to value V only
 * changes P's digit.</p>
 *
 * <p>Minecraft-free on purpose (properties, values and states are plain objects), so it is
 * unit-tested directly; the mixin on the block-state class only stores each state's
 * {@link StateIndex} and number and calls {@link #neighbour}.</p>
 *
 * <p>Thread safety: built once while blocks register and never changed afterwards; lookups only
 * read final arrays.</p>
 */
public final class StateIndex {

    private static final int NOT_CONTIGUOUS = Integer.MIN_VALUE;

    private final Object[] properties;
    private final Object[][] values;
    private final int[] sizes;
    private final int[] strides;
    private final Object[] states;
    /** Per property: first value when the values are the ascending integers v, v + 1, ...; else NOT_CONTIGUOUS. */
    private final int[] intBase;
    /** Per property: enum ordinal to value position (-1 = not a value), or null when the values are not enums. */
    private final int[][] ordinals;

    private StateIndex(final Object[] properties, final Object[][] values, final int[] sizes, final int[] strides,
                       final Object[] states) {
        this.properties = properties;
        this.values = values;
        this.sizes = sizes;
        this.strides = strides;
        this.states = states;
        this.intBase = new int[properties.length];
        this.ordinals = new int[properties.length][];
        for (int p = 0; p < properties.length; p++) {
            this.intBase[p] = contiguousBase(values[p]);
            this.ordinals[p] = ordinalTable(values[p]);
        }
    }

    /**
     * Builds the index of one block (or fluid) from vanilla's own map of every state by its values.
     *
     * @param properties the block's properties, in any fixed order
     * @param valuesOf   possible values of each property, in the property's own order
     * @param byValues   every state keyed by its property-to-value map (vanilla's map)
     * @return the index, or null when the map is not the complete combination of the values (then
     *         the caller keeps vanilla's tables)
     */
    public static StateIndex build(final List<?> properties, final List<? extends Collection<?>> valuesOf,
                                   final Map<? extends Map<?, ?>, ?> byValues) {
        final int n = properties.size();
        if (n == 0 || valuesOf.size() != n) {
            return null;
        }
        final Object[] props = properties.toArray();
        final Object[][] values = new Object[n][];
        final int[] sizes = new int[n];
        final int[] strides = new int[n];
        long total = 1L;
        for (int p = n - 1; p >= 0; p--) {
            values[p] = valuesOf.get(p).toArray();
            sizes[p] = values[p].length;
            if (sizes[p] == 0) {
                return null;
            }
            strides[p] = (int) total;
            total *= sizes[p];
            if (total > (1 << 24)) {
                return null;
            }
        }
        if (total != byValues.size()) {
            return null;
        }
        final Object[] states = new Object[(int) total];
        final StateIndex index = new StateIndex(props, values, sizes, strides, states);
        for (final Map.Entry<? extends Map<?, ?>, ?> entry : byValues.entrySet()) {
            final int at = index.indexOf(entry.getKey());
            if (at < 0 || states[at] != null || entry.getValue() == null) {
                return null;
            }
            states[at] = entry.getValue();
        }
        return index;
    }

    /** Position of the state with these property values, or -1 when they do not belong to this block. */
    public int indexOf(final Map<?, ?> stateValues) {
        if (stateValues == null || stateValues.size() != this.properties.length) {
            return -1;
        }
        int at = 0;
        for (int p = 0; p < this.properties.length; p++) {
            final int v = this.valueIndex(p, stateValues.get(this.properties[p]));
            if (v < 0) {
                return -1;
            }
            at += v * this.strides[p];
        }
        return at;
    }

    /**
     * The state at {@code index} with {@code property} set to {@code value}; null when the property
     * is not one of this block's or the value is not one of the property's (vanilla then throws
     * its usual "not an allowed value" error).
     */
    public Object neighbour(final int index, final Object property, final Object value) {
        if (index < 0 || index >= this.states.length) {
            return null;
        }
        final int p = this.propertyIndex(property);
        if (p < 0) {
            return null;
        }
        final int v = this.valueIndex(p, value);
        if (v < 0) {
            return null;
        }
        final int stride = this.strides[p];
        final int current = index / stride % this.sizes[p];
        return this.states[index + (v - current) * stride];
    }

    /** Receives (property, value, state) for {@link #forEachNeighbour}. */
    public interface NeighbourVisitor {
        void accept(Object property, Object value, Object state);
    }

    /**
     * Every (property, value) of this block with the state at {@code index} changed to it, the
     * state's own values included (as the state itself); properties in index order.
     */
    public void forEachNeighbour(final int index, final NeighbourVisitor visitor) {
        if (index < 0 || index >= this.states.length) {
            return;
        }
        for (int p = 0; p < this.properties.length; p++) {
            final int stride = this.strides[p];
            final int current = index / stride % this.sizes[p];
            for (int v = 0; v < this.sizes[p]; v++) {
                visitor.accept(this.properties[p], this.values[p][v], this.states[index + (v - current) * stride]);
            }
        }
    }

    /** Entries of vanilla's per-state table: every other value of every property. */
    public int neighbourCount() {
        int count = 0;
        for (final int size : this.sizes) {
            count += size - 1;
        }
        return count;
    }

    /** The state stored at {@code index} (tests and the self-test). */
    public Object state(final int index) {
        return index >= 0 && index < this.states.length ? this.states[index] : null;
    }

    public int size() {
        return this.states.length;
    }

    private int propertyIndex(final Object property) {
        final Object[] props = this.properties;
        for (int p = 0; p < props.length; p++) {
            if (props[p] == property) {
                return p;
            }
        }
        if (property == null) {
            return -1;
        }
        for (int p = 0; p < props.length; p++) {
            if (property.equals(props[p])) {
                return p;
            }
        }
        return -1;
    }

    private int valueIndex(final int p, final Object value) {
        if (value == null) {
            return -1;
        }
        final Object[] vals = this.values[p];
        final int base = this.intBase[p];
        if (base != NOT_CONTIGUOUS && value instanceof Integer) {
            final long v = (long) ((Integer) value).intValue() - base;
            return v >= 0 && v < vals.length ? (int) v : -1;
        }
        final int[] table = this.ordinals[p];
        if (table != null && value instanceof Enum) {
            final Enum<?> e = (Enum<?>) value;
            final int ordinal = e.ordinal();
            if (ordinal < table.length) {
                final int at = table[ordinal];
                if (at >= 0 && vals[at] == value) {
                    return at;
                }
            }
            return -1;
        }
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] == value) {
                return i;
            }
        }
        for (int i = 0; i < vals.length; i++) {
            if (value.equals(vals[i])) {
                return i;
            }
        }
        return -1;
    }

    private static int contiguousBase(final Object[] vals) {
        if (vals.length == 0 || !(vals[0] instanceof Integer)) {
            return NOT_CONTIGUOUS;
        }
        final int base = ((Integer) vals[0]).intValue();
        if (base == NOT_CONTIGUOUS) {
            return NOT_CONTIGUOUS;
        }
        for (int i = 1; i < vals.length; i++) {
            if (!(vals[i] instanceof Integer) || ((Integer) vals[i]).intValue() != base + i) {
                return NOT_CONTIGUOUS;
            }
        }
        return base;
    }

    private static int[] ordinalTable(final Object[] vals) {
        int max = -1;
        for (final Object v : vals) {
            if (!(v instanceof Enum)) {
                return null;
            }
            max = Math.max(max, ((Enum<?>) v).ordinal());
        }
        if (max < 0 || max > 4096) {
            return null;
        }
        final int[] table = new int[max + 1];
        java.util.Arrays.fill(table, -1);
        for (int i = 0; i < vals.length; i++) {
            final int ordinal = ((Enum<?>) vals[i]).ordinal();
            if (table[ordinal] != -1) {
                return null; // two values of different enum types with the same ordinal
            }
            table[ordinal] = i;
        }
        return table;
    }

    // ------------------------------------------------------------------ building per block

    /**
     * Vanilla populates every state of a block one after another with the same map; the index is
     * built for the first and reused for the rest. One per thread, so blocks registered in
     * parallel never share it.
     */
    private static final ThreadLocal<Object[]> LAST = new ThreadLocal<Object[]>();

    /**
     * The index for the block whose states are {@code byValues}, built on first use and cached for
     * the following states of the same block. {@code firstValues} supplies the property order.
     */
    public static StateIndex forBlock(final Map<? extends Map<?, ?>, ?> byValues, final Map<?, ?> firstValues,
                                      final java.util.function.Function<Object, Collection<?>> possibleValues) {
        final Object[] last = LAST.get();
        if (last != null && last[0] == byValues) {
            return (StateIndex) last[1];
        }
        final java.util.ArrayList<Object> props = new java.util.ArrayList<Object>(firstValues.keySet());
        final java.util.ArrayList<Collection<?>> values = new java.util.ArrayList<Collection<?>>(props.size());
        for (final Object property : props) {
            values.add(possibleValues.apply(property));
        }
        StateIndex built;
        try {
            built = build(props, values, byValues);
        } catch (final RuntimeException error) {
            built = null;
        }
        LAST.set(new Object[] {byValues, built});
        return built;
    }

    /** Forgets the per-thread cache (registration finished; tests). */
    public static void forgetLast() {
        LAST.remove();
    }
}
