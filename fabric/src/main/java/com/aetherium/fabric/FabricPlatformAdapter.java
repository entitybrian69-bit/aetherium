package com.aetherium.fabric;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import com.aetherium.platform.PlatformAdapter;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

/**
 * Fabric implementation of {@link PlatformAdapter}.
 *
 * <p>Only three Fabric APIs are touched — {@code FabricLoader#getGameDir},
 * {@code isModLoaded}, {@code getModContainer} — all of which have been stable since
 * loader 0.8 and are therefore safe for the whole 1.16.5 -&gt; 26.x porting range.
 * Nothing here references a Minecraft class, which is what keeps this module
 * compile-clean on every mapping flavor (yarn or mojmap).</p>
 */
public final class FabricPlatformAdapter implements PlatformAdapter {
    private final FabricLoader loader;
    private final String minecraftVersion;

    /** ServiceLoader-visible no-arg constructor (the service file needs it). */
    public FabricPlatformAdapter() {
        this(FabricLoader.getInstance());
    }

    public FabricPlatformAdapter(final FabricLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
        this.minecraftVersion = loader.getGameDir() == null ? "unknown" : readMinecraftVersion(loader);
    }

    private static String readMinecraftVersion(final FabricLoader loader) {
        return loader.getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public String platformName() {
        return "fabric";
    }

    @Override
    public Path gameDirectory() {
        return this.loader.getGameDir();
    }

    @Override
    public Path configDirectory() {
        // Fabric's convention is `<game>/config`; Aetherium's own file lives at the
        // game root (see ConfigStore) so that a user can find it next to the shader
        // packs it interacts with. This method still returns the conventional
        // directory because other code (crash reports) quotes it.
        return this.loader.getConfigDir();
    }

    @Override
    public boolean isModLoaded(final String modId) {
        return this.loader.isModLoaded(modId);
    }

    @Override
    public Optional<String> getModVersion(final String modId) {
        final Optional<ModContainer> container = this.loader.getModContainer(modId);
        return container.map(mod -> mod.getMetadata().getVersion().getFriendlyString());
    }

    @Override
    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    @Override
    public boolean isClient() {
        // The entry point that constructed this adapter is client-only, and
        // FabricLoader#isDevelopmentEnvironment is unrelated to side; a dedicated
        // server never constructs this class.
        return true;
    }
}
