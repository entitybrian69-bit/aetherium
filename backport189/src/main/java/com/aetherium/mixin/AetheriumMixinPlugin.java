package com.aetherium.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Mixin gate of Aetherium Backport (1.12.2) and the record of which hooks were applied.
 *
 * <p>The terrain renderer hooks are left out when another mod replaces the 1.12.2 chunk renderer
 * (OptiFine, Vintagium, Celeritas, Nothirium): two renderers editing the same draw loop is how games
 * crash. Everything else (screen, zoom, fog, lights, ...) still works next to them.</p>
 */
public final class AetheriumMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOG = LogManager.getLogger("Aetherium");
    private static final String PACKAGE = "com.aetherium.mixin.core.";
    private static final String[] RENDERER_MIXINS = {"ChunkUploadMixin", "VertexBufferMixin", "ChunkDrawMixin",
            "ChunkContainerMixin"};
    private static final String[][] RENDERER_REPLACEMENTS = {
            {"optifine.OptiFineClassTransformer", "OptiFine"},
            {"net.optifine.Config", "OptiFine"},
            {"me.jellysquid.mods.sodium.client.SodiumClientMod", "Vintagium"},
            {"org.embeddedt.vintagium.Vintagium", "Vintagium"},
            {"org.taumc.celeritas.CeleritasVintage", "Celeritas"},
            {"org.taumc.celeritas.CeleritasMod", "Celeritas"},
            {"meldexun.nothirium.Nothirium", "Nothirium"},
            {"meldexun.nothirium.mc.Nothirium", "Nothirium"},
    };

    private static final Set<String> APPLIED = ConcurrentHashMap.newKeySet();
    private static volatile boolean mixinActive = true;
    private static volatile String minecraftVersion = "1.8.9";
    private static volatile String rendererOwner;

    // ------------------------------------------------------------------ public API

    public static void setMixinActive(final boolean active) {
        mixinActive = active;
    }

    public static boolean isMixinActive() {
        return mixinActive;
    }

    public static void announceMinecraftVersion(final String version) {
        if (version != null && !version.isEmpty()) {
            minecraftVersion = version;
        }
    }

    public static String minecraftVersion() {
        return minecraftVersion;
    }

    public static boolean isApplied(final String simpleName) {
        return APPLIED.contains(simpleName);
    }

    public static boolean lightHooksInstalled() {
        return APPLIED.contains("LightLevelMixin");
    }

    public static int appliedCount() {
        return APPLIED.size();
    }

    public static Set<String> applied() {
        return Collections.unmodifiableSet(APPLIED);
    }

    /** The other mod that owns the terrain renderer, or null. */
    public static String rendererOwner() {
        return rendererOwner;
    }

    static boolean enabledOnThisVersion(final String simpleName) {
        for (final String name : RENDERER_MIXINS) {
            if (name.equals(simpleName)) {
                return rendererOwner == null;
            }
        }
        return true;
    }

    private static boolean present(final String className) {
        // Resource lookup only: loading the class this early would break the other mod's transformer.
        final ClassLoader loader = AetheriumMixinPlugin.class.getClassLoader();
        return loader.getResource(className.replace('.', '/') + ".class") != null;
    }

    // ------------------------------------------------------------------ IMixinConfigPlugin

    @Override
    public void onLoad(final String mixinPackage) {
        for (final String[] candidate : RENDERER_REPLACEMENTS) {
            if (present(candidate[0])) {
                rendererOwner = candidate[1];
                LOG.info("Aetherium: {} replaces the chunk renderer; the Aetherium terrain renderer stays off",
                        candidate[1]);
                break;
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
        final String simple = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length())
                : mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        return enabledOnThisVersion(simple);
    }

    @Override
    public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName,
                         final IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(final String targetClassName, final ClassNode targetClass, final String mixinClassName,
                          final IMixinInfo mixinInfo) {
        APPLIED.add(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
    }
}
