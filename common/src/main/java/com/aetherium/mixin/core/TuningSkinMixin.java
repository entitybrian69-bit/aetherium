package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-skin-begin v16
import com.aetherium.util.AetheriumLog;
import com.aetherium.perf.Tuning;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-skin-else none
// @era:t-skin-end

/**
 * Background skin loading (1.12.0, Minecraft 1.16.5): when a player skin or cape is already in the
 * local skin cache, {@code HttpTexture.load} reads and decodes the PNG on the main thread (and
 * converts old 64x32 skins there too), a small freeze for every player that joins or comes into
 * view. With the option on, that file is read and decoded on the background pool and handed to
 * the main thread afterwards, exactly where vanilla's own download path hands its image over;
 * the default skin is shown in the meantime, as during a download.
 *
 * <p>The hook sits right before vanilla's {@code file.isFile()} check, after the default texture
 * upload has been queued and after the "already loading" check. If the cached file turns out to
 * be unreadable, the texture falls back to vanilla's path on the main thread (which then
 * downloads it again).</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.HttpTexture")
public abstract class TuningSkinMixin {
    // @era:t-skin-begin v16
    @Unique
    private static final AetheriumLog LOGGER = AetheriumLog.of(TuningSkinMixin.class);

    @Shadow
    @Final
    private File file;

    @Shadow
    private CompletableFuture<?> future;

    /** Set when the background read failed: the next load takes vanilla's path. */
    @Unique
    private boolean aetherium$vanillaPath;

    @Inject(method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z"), cancellable = true, require = 0)
    private void aetherium$background(final ResourceManager resources, final CallbackInfo ci) {
        final File cached = file;
        if (!Tuning.asyncSkins || aetherium$vanillaPath || cached == null || !cached.isFile()) {
            return;
        }
        final HttpTexture self = (HttpTexture) (Object) this;
        final TuningHttpTextureAccessor access = (TuningHttpTextureAccessor) (Object) this;
        future = CompletableFuture.runAsync(() -> {
            NativeImage image = null;
            try (InputStream in = new FileInputStream(cached)) {
                image = access.aetherium$read(in);
            } catch (final IOException | RuntimeException e) {
                LOGGER.warn("Background skin read failed, loading it the vanilla way: " + e);
            }
            final NativeImage done = image;
            Minecraft.getInstance().execute(() -> {
                if (done != null) {
                    access.aetherium$loaded(done);
                    return;
                }
                aetherium$vanillaPath = true;
                future = null;
                try {
                    self.load(resources);
                } catch (final IOException e) {
                    LOGGER.warn("Skin load failed: " + e);
                }
            });
        }, Util.backgroundExecutor());
        ci.cancel();
    }
    // @era:t-skin-else none
    // @era:t-skin-end
}
