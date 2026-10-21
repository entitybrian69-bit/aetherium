package com.aetherium.perf;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Compact block states (1.17.2): the per-state "neighbour" table that vanilla's
 * {@code StateHolder.setValue} reads, answered from the block's shared {@link StateIndex} instead
 * of a hash table per state.
 *
 * <p>Vanilla's table maps (property, value) to the state with that one property changed, for every
 * value except the state's current one (looked up by identity). {@link #get} gives exactly the
 * same answers, including null for the current value and for anything that is not a value of the
 * block, so vanilla's {@code setValue} (and any other code that reads the table) behaves the same.
 * The rarely used whole-table views are computed on demand; the table is read-only, like
 * vanilla's after start-up.</p>
 *
 * <p>About 24 bytes per state instead of a hash table of hash maps.</p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class CompactNeighbours implements Table {
    private final StateIndex index;
    private final int at;
    private final Object self;

    public CompactNeighbours(final StateIndex index, final int at, final Object self) {
        this.index = index;
        this.at = at;
        this.self = self;
    }

    @Override
    public Object get(final Object rowKey, final Object columnKey) {
        final Object found = this.index.neighbour(this.at, rowKey, columnKey);
        // Vanilla never stores a state's own value: identical value -> no entry.
        return found == this.self ? null : found;
    }

    @Override
    public boolean contains(final Object rowKey, final Object columnKey) {
        return this.get(rowKey, columnKey) != null;
    }

    /** The equivalent vanilla table, built on demand for the rarely used views. */
    public ImmutableTable<Object, Object, Object> materialize() {
        final ImmutableTable.Builder<Object, Object, Object> out = ImmutableTable.builder();
        this.index.forEachNeighbour(this.at, (property, value, state) -> {
            if (state != this.self) {
                out.put(property, value, state);
            }
        });
        return out.build();
    }

    @Override
    public boolean containsRow(final Object rowKey) {
        return this.materialize().containsRow(rowKey);
    }

    @Override
    public boolean containsColumn(final Object columnKey) {
        return this.materialize().containsColumn(columnKey);
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.materialize().containsValue(value);
    }

    @Override
    public int size() {
        return this.index.neighbourCount();
    }

    @Override
    public boolean isEmpty() {
        return this.size() == 0;
    }

    @Override
    public Map row(final Object rowKey) {
        return this.materialize().row(rowKey);
    }

    @Override
    public Map column(final Object columnKey) {
        return this.materialize().column(columnKey);
    }

    @Override
    public Set cellSet() {
        return this.materialize().cellSet();
    }

    @Override
    public Set rowKeySet() {
        return this.materialize().rowKeySet();
    }

    @Override
    public Set columnKeySet() {
        return this.materialize().columnKeySet();
    }

    @Override
    public Collection values() {
        return this.materialize().values();
    }

    @Override
    public Map rowMap() {
        return this.materialize().rowMap();
    }

    @Override
    public Map columnMap() {
        return this.materialize().columnMap();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(final Object rowKey, final Object columnKey, final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Table table) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(final Object rowKey, final Object columnKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof Table && this.materialize().equals(other);
    }

    @Override
    public int hashCode() {
        return this.materialize().hashCode();
    }

    @Override
    public String toString() {
        return this.materialize().toString();
    }
}
