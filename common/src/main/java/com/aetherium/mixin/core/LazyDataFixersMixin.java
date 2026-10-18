package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-lazy-data-fixers-begin none
// @era:t-lazy-data-fixers-else v16
//~ import com.aetherium.perf.NoOpExecutor;
//~ import com.mojang.datafixers.DataFixer;
//~ import com.mojang.datafixers.DataFixerBuilder;
//~ import java.util.concurrent.Executor;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-lazy-data-fixers-else d19
//~ import com.aetherium.perf.NoOpExecutor;
//~ import com.mojang.datafixers.DataFixer;
//~ import com.mojang.datafixers.DataFixerBuilder;
//~ import java.util.concurrent.Executor;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:t-lazy-data-fixers-end

/**
 * Lazy DataFixerUpper (1.10.0, Minecraft 1.16.5): faster game start and less memory, see
 * {@link com.aetherium.perf.NoOpExecutor}. Skipped when LazyDFU is installed.
 */
@Mixin(targets = "net.minecraft.util.datafix.DataFixers")
public abstract class LazyDataFixersMixin {
    // @era:t-lazy-data-fixers-begin none
    // @era:t-lazy-data-fixers-else v16
    //~ @Redirect(method = "createFixerUpper", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/datafixers/DataFixerBuilder;build(Ljava/util/concurrent/Executor;)Lcom/mojang/datafixers/DataFixer;"),
            //~ require = 0)
    //~ private static DataFixer aetherium$lazy(final DataFixerBuilder builder, final Executor executor) {
        //~ return builder.build(NoOpExecutor.INSTANCE);
    //~ }
    // @era:t-lazy-data-fixers-else d19
    //~ @Redirect(method = "createFixerUpper", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/datafixers/DataFixerBuilder;buildOptimized(Ljava/util/concurrent/Executor;)Lcom/mojang/datafixers/DataFixer;"),
            //~ require = 0)
    //~ private static DataFixer aetherium$lazy(final DataFixerBuilder builder, final Executor executor) {
        //~ return builder.buildUnoptimized();
    //~ }
    // @era:t-lazy-data-fixers-end
}
