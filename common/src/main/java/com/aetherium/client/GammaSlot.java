package com.aetherium.client;

/**
 * Raw access to the value stored in vanilla's gamma {@code OptionInstance} (1.19+).
 *
 * <p>{@code OptionInstanceMixin} adds this interface to {@code OptionInstance} and implements it
 * with a {@code @Shadow} of the {@code value} field. Fullbright writes its override straight into
 * that field, which skips vanilla's 0..1 validation without hooking {@code OptionInstance.get()}:
 * {@code get()} runs for every option read, including once per block while chunks are meshed, so
 * an injected callback there costs frame time even when fullbright is off. This way every read
 * stays exactly as cheap as vanilla. It lives outside the mixin package so normal code can cast
 * to it.</p>
 */
public interface GammaSlot {
    Object aetheriumRawValue();

    void aetheriumSetRawValue(Object value);
}
