package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.perf.NoOpExecutor;
//~ import com.mojang.datafixers.DataFixer;
//~ import com.mojang.datafixers.DataFixerBuilder;
//~ import java.util.concurrent.Executor;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Lazy DataFixerUpper (1.10.0, Minecraft 1.16.5): faster game start and less memory, see
 * {@link com.aetherium.perf.NoOpExecutor}. Skipped when LazyDFU is installed.
 */
@Mixin(targets = "net.minecraft.util.datafix.DataFixers")
public abstract class LazyDataFixersMixin {
    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ @Redirect(method = "createFixerUpper", at = @At(value = "INVOKE",
            //~ target = "Lcom/mojang/datafixers/DataFixerBuilder;build(Ljava/util/concurrent/Executor;)Lcom/mojang/datafixers/DataFixer;"),
            //~ require = 0)
    //~ private static DataFixer aetherium$lazy(final DataFixerBuilder builder, final Executor executor) {
        //~ return builder.build(NoOpExecutor.INSTANCE);
    //~ }
    // @era:qol16-end
}
