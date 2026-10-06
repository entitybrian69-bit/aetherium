package dev.aetherium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aetherium.Aetherium;
import dev.aetherium.config.AetheriumConfig;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.lwjgl.opengl.GL11;

/** Pushes Aetherium config values into vanilla Options and GL state immediately; no restart, no resource reload. */
public final class LiveSettingsApplier {
    private static final int GL_TEXTURE_MAX_ANISOTROPY = 0x84FE;
    private LiveSettingsApplier() {}

    public static void applyAll(AetheriumConfig cfg) {
        Minecraft mc = Minecraft.getInstance();
        Options o = mc.options;

        if (o.renderDistance().get() != cfg.general.renderDistance) o.renderDistance().set(cfg.general.renderDistance);
        if (o.simulationDistance().get() != cfg.general.simulationDistance) o.simulationDistance().set(cfg.general.simulationDistance);
        if (o.guiScale().get() != cfg.general.guiScale) { o.guiScale().set(cfg.general.guiScale); mc.resizeDisplay(); }
        if (o.enableVsync().get() != cfg.general.vsync) { o.enableVsync().set(cfg.general.vsync); mc.getWindow().updateVsync(cfg.general.vsync); }
        if (o.fullscreen().get() != cfg.general.fullscreen) { o.fullscreen().set(cfg.general.fullscreen); mc.getWindow().toggleFullScreen(); }

        if (o.mipmapLevels().get() != cfg.quality.mipmapLevels) {
            o.mipmapLevels().set(cfg.quality.mipmapLevels);
            mc.updateMaxMipLevel(cfg.quality.mipmapLevels);
            mc.delayTextureReload();
        }
        if (o.ambientOcclusion().get() != cfg.quality.smoothLighting) { o.ambientOcclusion().set(cfg.quality.smoothLighting); mc.levelRenderer.allChanged(); }
        if (o.biomeBlendRadius().get() != cfg.quality.biomeBlend) { o.biomeBlendRadius().set(cfg.quality.biomeBlend); mc.levelRenderer.allChanged(); }
        CloudStatus clouds = switch (cfg.quality.cloudQuality) { case OFF -> CloudStatus.OFF; case FAST -> CloudStatus.FAST; case FANCY -> CloudStatus.FANCY; };
        if (o.cloudStatus().get() != clouds) o.cloudStatus().set(clouds);
        ParticleStatus particles = switch (cfg.quality.particleDensity) { case ALL -> ParticleStatus.ALL; case DECREASED -> ParticleStatus.DECREASED; case MINIMAL -> ParticleStatus.MINIMAL; };
        if (o.particles().get() != particles) o.particles().set(particles);

        applyTextureFiltering(cfg);
        o.save();
    }

    /** Anisotropic / linear filtering on the block atlas via raw GL (vanilla exposes no option for it). */
    public static void applyTextureFiltering(AetheriumConfig cfg) {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(() -> applyTextureFiltering(cfg)); return; }
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (atlas == null) return;
        RenderSystem.bindTexture(atlas.getId());
        float aniso = switch (cfg.quality.textureFiltering) {
            case NEAREST, LINEAR -> 1f;
            case ANISOTROPIC -> Math.max(1, cfg.quality.anisotropicFiltering);
        };
        try {
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY, aniso);
        } catch (Throwable t) {
            Aetherium.LOGGER.debug("[Aetherium] Anisotropic filtering unsupported", t);
        }
        boolean linear = cfg.quality.textureFiltering != AetheriumConfig.TextureFiltering.NEAREST;
        int mag = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        int min = cfg.quality.mipmapLevels > 0 ? (linear ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_NEAREST_MIPMAP_LINEAR) : mag;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
    }
}
