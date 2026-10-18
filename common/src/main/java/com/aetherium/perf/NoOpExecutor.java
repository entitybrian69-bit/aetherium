package com.aetherium.perf;

import java.util.concurrent.Executor;

/**
 * An executor that drops its tasks. Handed to {@code DataFixerBuilder.build} at startup (Minecraft
 * 1.16.5, lazy DataFixerUpper): vanilla uses the executor to optimize every data-fixer rule
 * up-front on background threads, which costs several seconds of CPU and hundreds of MB on a phone
 * while the game starts. Without it each rule is optimized the first time a world from an older
 * version actually needs it.
 */
public enum NoOpExecutor implements Executor {
    INSTANCE;

    @Override
    public void execute(final Runnable command) {
        // intentionally dropped
    }
}
