package com.aetherium.qol;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Floating damage numbers (Quality of life), Minecraft-free.
 *
 * <p>The server already tells the client the health of every living entity it can see (it is part
 * of the entity's synced data), so no extra information is needed: once per tick the hook reports
 * the health of the entities near the player, and a drop since the last report becomes a number
 * that rises and fades above the entity for {@link #LIFETIME_MS}. The first report of an entity
 * only records it, so entities coming into view never show a number.</p>
 *
 * <p>Bounded: at most {@link #MAX_POPUPS} numbers exist at once (the oldest is replaced), and
 * entities not reported for a tick are forgotten, so the map never outgrows the entities nearby.</p>
 */
public final class DamageNumbers {
    public static final long LIFETIME_MS = 1100L;
    public static final int MAX_POPUPS = 48;
    /** Changes smaller than this are rounding noise (regeneration ticks of 0.0x). */
    private static final float MIN_CHANGE = 0.05f;

    /** One floating number. Position in world coordinates. */
    public static final class Popup {
        public double x;
        public double y;
        public double z;
        public float amount;
        public boolean heal;
        public long born;
        boolean live;

        /** True while the popup is shown. */
        public boolean isLive() {
            return this.live;
        }

        /** 0 at birth, 1 when it disappears. */
        public float age(final long now) {
            return Math.max(0f, Math.min(1f, (now - this.born) / (float) LIFETIME_MS));
        }

        /** "4", "3.5", "+2" for heals. */
        public String text() {
            final float rounded = Math.round(this.amount * 2f) / 2f;
            final String number = rounded == (int) rounded ? Integer.toString((int) rounded) : Float.toString(rounded);
            return this.heal ? "+" + number : number;
        }
    }

    private static final class Entry {
        float health;
        int tick;
    }

    private final Map<Integer, Entry> health = new HashMap<Integer, Entry>();
    private final Popup[] popups = new Popup[MAX_POPUPS];
    private int tick;
    private int next;
    private int live;

    public DamageNumbers() {
        for (int i = 0; i < MAX_POPUPS; i++) {
            this.popups[i] = new Popup();
        }
    }

    /** Starts a reporting round (one per client tick). */
    public void beginTick() {
        this.tick++;
    }

    /**
     * Reports one entity's health this tick; may start a popup at (x, y, z).
     *
     * @param jitter a value in [0, 1) used to spread numbers of the same entity sideways
     */
    public void report(final int entityId, final float currentHealth, final double x, final double y, final double z,
                       final boolean showHeals, final long now, final double jitter) {
        final Integer key = Integer.valueOf(entityId);
        Entry entry = this.health.get(key);
        if (entry == null) {
            entry = new Entry();
            entry.health = currentHealth;
            entry.tick = this.tick;
            this.health.put(key, entry);
            return;
        }
        final float change = currentHealth - entry.health;
        entry.health = currentHealth;
        entry.tick = this.tick;
        if (change <= -MIN_CHANGE) {
            spawn(-change, false, x, y, z, now, jitter);
        } else if (showHeals && change >= MIN_CHANGE) {
            spawn(change, true, x, y, z, now, jitter);
        }
    }

    /** Ends the round: forgets entities that were not reported and expires old popups. */
    public void endTick(final long now) {
        final Iterator<Entry> it = this.health.values().iterator();
        while (it.hasNext()) {
            if (it.next().tick != this.tick) {
                it.remove();
            }
        }
        expire(now);
    }

    private void spawn(final float amount, final boolean heal, final double x, final double y, final double z,
                       final long now, final double jitter) {
        final Popup p = this.popups[this.next];
        this.next = (this.next + 1) % MAX_POPUPS;
        if (!p.live) {
            this.live++;
        }
        p.live = true;
        p.amount = amount;
        p.heal = heal;
        p.born = now;
        p.x = x + (jitter - 0.5) * 0.6;
        p.y = y;
        p.z = z + ((jitter * 7.0) % 1.0 - 0.5) * 0.6;
    }

    private void expire(final long now) {
        for (final Popup p : this.popups) {
            if (p.live && now - p.born >= LIFETIME_MS) {
                p.live = false;
                this.live--;
            }
        }
    }

    /** True when there is nothing to draw (the render hook returns at once). */
    public boolean isIdle() {
        return this.live == 0;
    }

    /** The live popup at {@code index} of the ring, or null. */
    public Popup popup(final int index) {
        final Popup p = this.popups[index];
        return p.live ? p : null;
    }

    public int capacity() {
        return MAX_POPUPS;
    }

    /** World change: every entity id is new again. */
    public void clear() {
        this.health.clear();
        for (final Popup p : this.popups) {
            p.live = false;
        }
        this.live = 0;
    }

    int tracked() {
        return this.health.size();
    }
}
