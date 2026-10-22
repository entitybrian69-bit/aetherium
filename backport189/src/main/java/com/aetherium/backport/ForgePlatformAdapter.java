package com.aetherium.backport;

import com.aetherium.platform.PlatformAdapter;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

/** Aetherium's view of Forge 1.12.2. */
public final class ForgePlatformAdapter implements PlatformAdapter {

    @Override
    public String platformName() {
        return "forge";
    }

    @Override
    public Path gameDirectory() {
        return Loader.instance().getConfigDir().toPath().toAbsolutePath().getParent();
    }

    @Override
    public Path configDirectory() {
        return Loader.instance().getConfigDir().toPath();
    }

    @Override
    public boolean isModLoaded(final String modId) {
        // OptiFine is handled by the mixin gate (only the terrain renderer steps aside), so the
        // conflict scanner must not switch the whole mod off for it.
        if ("optifine".equals(modId) || "of".equals(modId)) {
            return false;
        }
        return Loader.isModLoaded(modId);
    }

    @Override
    public Optional<String> getModVersion(final String modId) {
        final ModContainer container = Loader.instance().getIndexedModList().get(modId);
        return container == null ? Optional.<String>empty() : Optional.ofNullable(container.getVersion());
    }

    @Override
    public String minecraftVersion() {
        return Loader.MC_VERSION;
    }

    @Override
    public boolean isClient() {
        return FMLCommonHandler.instance().getSide().isClient();
    }
}
