package com.aetherium.backport;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import com.aetherium.client.FrameHud;
import com.aetherium.client.FreelookHooks;
import com.aetherium.client.TerrainRenderer;
import com.aetherium.client.ZoomKeys;
import com.aetherium.qol.Scenery;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

/** Forge event hooks of Aetherium Backport. */
public final class ForgeEvents {

    private final float[] fog = new float[3];

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientHooks.onClientTick(Minecraft.getMinecraft());
        }
    }

    @SubscribeEvent
    public void onRenderTick(final TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ClientHooks.onFrameStart();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onGuiOpen(final GuiOpenEvent event) {
        final GuiScreen replacement = ClientHooks.replaceScreen(Minecraft.getMinecraft(), event.gui);
        if (replacement != null) {
            event.gui = replacement;
        }
    }

    // Hide vignette: 1.8.9 has no VIGNETTE overlay element, see VignetteMixin.

    @SubscribeEvent
    public void onOverlayPost(final RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            FrameHud.render(event.resolution);
        }
    }

    @SubscribeEvent
    public void onMouse(final MouseEvent event) {
        final int wheel = event.dwheel;
        if (wheel != 0 && ZoomKeys.isZooming() && ZoomKeys.onScroll(wheel > 0 ? 1.0 : -1.0)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onCamera(final EntityViewRenderEvent.CameraSetup event) {
        if (FreelookHooks.active()) {
            event.yaw = FreelookHooks.yaw() + 180.0f;
            event.pitch = FreelookHooks.pitch();
        }
    }

    /**
     * Fires at the start of every {@code setupFog}, before vanilla picks the fog for the medium at
     * the camera. Water and lava fog are exponential in 1.8.9 and handled here; air fog is linear
     * and handled in {@link #onRenderFog}.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFogDensity(final EntityViewRenderEvent.FogDensity event) {
        TerrainRenderer.noteFog(false, 0.0f);
        if (!Aetherium.isActive() || event.isCanceled() || blind(event.entity)) {
            return;
        }
        final Material material = event.block.getMaterial();
        if (material == Material.water && Scenery.waterFogPercent < 100) {
            Scenery.adjustFog(Scenery.WATER, false, 0.0f, 0.0f, 0.0f, vanillaWaterDensity(event.entity), this.fog);
            GlStateManager.setFog(GL11.GL_EXP);
            event.density = Math.max(0.0f, this.fog[Scenery.DENSITY]);
            event.setCanceled(true);
        } else if (material == Material.lava && Scenery.clearLava) {
            final float far = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0f;
            Scenery.adjustFog(Scenery.LAVA, false, far, 0.0f, 0.5f, 2.0f, this.fog);
            GlStateManager.setFog(GL11.GL_LINEAR);
            GlStateManager.setFogStart(this.fog[Scenery.START]);
            GlStateManager.setFogEnd(this.fog[Scenery.END]);
            event.density = 0.0f;
            event.setCanceled(true);
        }
    }

    /** Air fog: runs after vanilla set its linear fog for this pass (sky -1, terrain 0). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderFog(final EntityViewRenderEvent.RenderFogEvent event) {
        final float far = event.farPlaneDistance;
        final Minecraft mc = Minecraft.getMinecraft();
        final Entity entity = event.entity;
        final boolean thick = mc.theWorld != null && mc.theWorld.provider.doesXZShowFog((int) entity.posX, (int) entity.posZ);
        float start;
        float end;
        if (thick) {
            start = far * 0.05f;
            end = Math.min(far, 192.0f) * 0.5f;
        } else if (event.fogMode == -1) {
            start = 0.0f;
            end = far;
        } else {
            start = far * 0.75f;
            end = far;
        }
        if (Aetherium.isActive() && !Scenery.fogVanilla()
                && Scenery.adjustFog(Scenery.AIR, thick, far, start, end, 0.0f, this.fog)) {
            start = this.fog[Scenery.START];
            end = this.fog[Scenery.END];
            GlStateManager.setFogStart(start);
            GlStateManager.setFogEnd(end);
        }
        if (event.fogMode == 0) {
            TerrainRenderer.noteFog(true, end);
        }
    }

    private static boolean blind(final Entity entity) {
        return entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.blindness);
    }

    /** What 1.8.9's setupFog uses under water (respiration lowers it). */
    private static float vanillaWaterDensity(final Entity entity) {
        float density = 0.1f;
        if (entity instanceof EntityLivingBase) {
            final EntityLivingBase living = (EntityLivingBase) entity;
            if (living.isPotionActive(Potion.waterBreathing)) {
                density = 0.01f;
            } else {
                density = 0.1f - net.minecraft.enchantment.EnchantmentHelper.getRespiration(living) * 0.03f;
            }
        }
        return density;
    }
}
