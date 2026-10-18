package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-http-textureacc-begin none
// @era:t-http-textureacc-else v16
//~ import com.mojang.blaze3d.platform.NativeImage;
//~ import java.io.InputStream;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:t-http-textureacc-end

/** Background skin loading (1.12.0, Minecraft 1.16.5): the private decode and hand-over steps of {@code HttpTexture}. */
@Mixin(targets = "net.minecraft.client.renderer.texture.HttpTexture")
public interface TuningHttpTextureAccessor {
    // @era:t-http-textureacc-begin none
    // @era:t-http-textureacc-else v16
    //~ /** Reads the PNG and converts a legacy 64x32 skin; null (logged) when the file is broken. */
    //~ @Invoker("load")
    //~ NativeImage aetherium$read(InputStream in);

    //~ /** Runs the download callback and queues the upload on the render thread. */
    //~ @Invoker("loadCallback")
    //~ void aetherium$loaded(NativeImage image);
    // @era:t-http-textureacc-end
}
