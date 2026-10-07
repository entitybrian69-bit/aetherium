package com.aetherium.neoforge;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Optional;

import com.aetherium.platform.PlatformAdapter;
import com.aetherium.util.AetheriumLog;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge implementation of {@link PlatformAdapter}.
 *
 * <p>{@code FMLPaths} and {@code ModList} are the two FML surfaces used; both have
 * kept their names since Forge 1.13 and are stable on NeoForge. The Minecraft version
 * string is read reflectively (see {@link #detectMinecraftVersion()}) because the
 * helper class that carries it has moved packages more than once across the porting
 * range, and a mod that needs 33 versions must not fail to compile because a
 * version-probe constant moved.</p>
 */
public final class NeoForgePlatformAdapter implements PlatformAdapter {
    private static final AetheriumLog LOGGER = AetheriumLog.of(NeoForgePlatformAdapter.class);

    private final ModContainer container;
    private final String minecraftVersion;

    public NeoForgePlatformAdapter(final ModContainer container) {
        this.container = container;
        this.minecraftVersion = detectMinecraftVersion();
    }

    @Override
    public String platformName() {
        return "neoforge";
    }

    @Override
    public Path gameDirectory() {
        return FMLPaths.GAME_DIRECTORY.get();
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIG_DIRECTORY.get();
    }

    @Override
    public boolean isModLoaded(final String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Optional<String> getModVersion(final String modId) {
        return ModList.get().getModContainerById(modId)
                .map(loaded -> loaded.getModInfo().getVersion().toString());
    }

    @Override
    public String minecraftVersion() {
        return this.minecraftVersion;
    }

    @Override
    public boolean isClient() {
        // Dist check via the presence of the client-only event bus registration we
        // own: on a dedicated server NeoForge never fires FMLClientSetupEvent, so the
        // adapter exists but reports false and Aetherium stays off. Reflection keeps
        // this free of the net.neoforged.api.distmarker import, whose package moved
        // from Forge's net.minecraftforge.api.distmarker.
        try {
            final Class<?> environment = Class.forName("net.neoforged.fml.loading.FMLEnvironment");
            final Object dist = environment.getField("dist").get(null);
            return "CLIENT".equals(String.valueOf(dist));
        } catch (final ReflectiveOperationException | RuntimeException | LinkageError error) {
            LOGGER.dev("FMLEnvironment.dist unavailable ({}); assuming client because this adapter "
                    + "is only constructed from the client entry path", error.getClass().getSimpleName());
            return true;
        }
    }

    /**
     * Reads the game version. Order: NeoForge's own version helper (reflective), then
     * the FML mod-list attribute for the {@code minecraft} pseudo-mod, then
     * {@code Minecraft#getLaunchedVersion}, then "unknown" — because a wrong version
     * string in a crash report is worse than an honest "unknown".
     */
    // [UNVERIFIED: the two version-helper class names below (net.neoforged.neoforge.
    // *internal.versions.neoforge.NeoForgeVersion#mcVersion and net.neoforged.neoforge.common.
    // NeoForgeVersion#mcVersion)] - one of them exists on 21.1.x, and both are read reflectively
    // precisely so the wrong guess costs a log line instead of a compile error.
    // [UNVERIFIED: net.neoforged.fml.loading.FMLEnvironment#dist is the public static field for
    // the logical side on NeoForge 1.21.1; read reflectively for the same reason.]
    // [UNVERIFIED: FMLPaths.GAME_DIRECTORY / CONFIG_DIRECTORY and ModList#getModContainerById are
    // named from NeoForge 20.x-21.x usage; they have not been renamed in any release in the
    // porting range as of the pinned 21.1.77, but this file is the only consumer.]
    private static String detectMinecraftVersion() {
        for (final String[] candidate : new String[][]{
                {"net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion", "mcVersion"},
                {"net.neoforged.neoforge.common.NeoForgeVersion", "mcVersion"},
        }) {
            try {
                final Class<?> type = Class.forName(candidate[0]);
                final Method method = type.getMethod(candidate[1]);
                final Object value = method.invoke(null);
                if (value instanceof String text && !text.isBlank()) {
                    return text;
                }
            } catch (final ReflectiveOperationException error) {
                LOGGER.dev("Version helper {}#{} unavailable: {}", candidate[0], candidate[1], error.getClass().getSimpleName());
            }
        }
        final Optional<String> fromList = ModList.get().getModContainerById("minecraft")
                .map(mod -> mod.getModInfo().getVersion().toString());
        return fromList.orElseGet(() -> {
            LOGGER.warn("Could not determine the Minecraft version on NeoForge; Aetherium will still "
                    + "load but the version-specific mixin gating uses its fallback range");
            return "unknown";
        });
    }

    /**
     * No FML-event subscription lives here on purpose. Aetherium's per-frame work is
     * driven from {@code GameRendererMixin} (both loaders) plus
     * {@code RenderLevelStageEvent} (NeoForge, see {@link AetheriumNeoForge}), and a
     * common-setup listener would only add a place for the two to disagree. Bus access
     * is exposed instead so the entry point owns registration.
     */
}
