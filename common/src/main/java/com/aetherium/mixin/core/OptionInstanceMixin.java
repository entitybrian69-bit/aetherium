package com.aetherium.mixin.core;

// @era:options-begin instances
import com.aetherium.client.GammaSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Fullbright support with zero cost per option read: exposes the stored value through
 * {@link GammaSlot} instead of injecting into {@code OptionInstance.get()}. The field is
 * {@code T value} on every 1.19+ version (package-private up to 1.21.10, private from 1.21.11);
 * its erasure is {@code Object}. No injector runs, so vanilla's {@code get()} bytecode is untouched.
 */
@Mixin(net.minecraft.client.OptionInstance.class)
public abstract class OptionInstanceMixin implements GammaSlot {

    @Shadow
    private Object value;

    @Override
    public Object aetheriumRawValue() {
        return this.value;
    }

    @Override
    public void aetheriumSetRawValue(final Object replacement) {
        this.value = replacement;
    }
}
// @era:options-else fields
//~ import org.spongepowered.asm.mixin.Mixin;

//~ /** Before 1.19 gamma is a plain field written directly by VanillaOptions; the plugin skips this mixin. */
//~ @Mixin(net.minecraft.client.Options.class)
//~ public abstract class OptionInstanceMixin {
//~ }
// @era:options-end
