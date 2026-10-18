package com.aetherium.perf;

/**
 * Explosion particle budget (1.10.0): at most N big explosion clouds ({@code EXPLOSION_EMITTER})
 * per game tick on the client; the rest of a TNT chain shows the small puff instead. Each emitter
 * spawns a burst of large animated clouds for several ticks, which is what collapses the frame rate
 * when hundreds of TNT go off together.
 */
public final class ExplosionBudget {
    private static long tick = Long.MIN_VALUE;
    private static int used;

    private ExplosionBudget() {
    }

    /**
     * @param gameTime the level's game time (one budget per tick)
     * @param cap      emitters allowed per tick; {@link Tuning#EXPLOSIONS_UNLIMITED} or more = unlimited
     * @return true when this explosion may show the big cloud
     */
    public static boolean allowEmitter(final long gameTime, final int cap) {
        if (cap >= Tuning.EXPLOSIONS_UNLIMITED) {
            return true;
        }
        if (gameTime != tick) {
            tick = gameTime;
            used = 0;
        }
        if (used < cap) {
            used++;
            return true;
        }
        return false;
    }
}
