package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.Capabilities;
import com.aetherium.android.AndroidEnvironment;
import com.aetherium.android.ThermalMonitor;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.hud.BenchmarkRecorder;
import com.aetherium.hud.FrameStats;
import com.aetherium.lighting.DynamicLightTracker;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.perf.AdaptiveDistance;
import com.aetherium.perf.BlockEntityCull;
import com.aetherium.perf.FrameLimiter;
import com.aetherium.perf.RenderToggles;
import com.aetherium.perf.StutterRecorder;
import com.aetherium.perf.UploadBudget;
import com.aetherium.qol.ParticleFilter;
import com.aetherium.qol.Scenery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;

/**
 * The 1.12.2 glue between Minecraft's callbacks (Forge events and mixins) and Aetherium's
 * version-independent engine. Render / client thread only unless stated otherwise.
 */
public final class ClientHooks {

    private static final FrameLimiter LIMITER = new FrameLimiter();
    private static final AdaptiveDistance ADAPTIVE = new AdaptiveDistance();
    private static final int ADAPTIVE_PERIOD_TICKS = 40; // 2 s
    private static final int THERMAL_PERIOD_TICKS = 100; // 5 s
    private static final int THERMAL_HYSTERESIS_C = 3;
    private static final int THERMAL_CAP_FPS = 30;
    private static final float HUGE_ENTITY = 3.0f;

    /** Particle category per {@link EnumParticleTypes} id (1.12.2 names, sorted like the main mod's). */
    private static final int[] PARTICLE_CATEGORY = buildParticleTable();

    private static DynamicLightTracker tracker;
    private static boolean initialised;
    private static Object lastLevel;
    private static int ticks;
    private static long lastFrameNanos;
    private static boolean thermalCapped;
    private static ThermalMonitor thermal;
    private static boolean adaptiveWasOn;
    private static int userRenderDistance = -1;
    private static boolean bypassVideoScreen;

    private ClientHooks() {
    }

    // ------------------------------------------------------------------ per frame

    /** Start of every frame (Forge {@code RenderTickEvent} START). */
    public static void onFrameStart() {
        final Minecraft mc = Minecraft.getMinecraft();
        ZoomKeys.frame(mc);
        FreelookHooks.frame(mc);
        final long now = System.nanoTime();
        StutterRecorder.frame(now, null);
        UploadBudget.adapt(StutterRecorder.averageFrameNanos());
        TerrainRenderer.onFrame();
        if (Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive()) {
            LIMITER.beforeFrame();
        }
        if (BlockEntityCull.active()) {
            final Entity view = mc.getRenderViewEntity();
            if (view != null) {
                BlockEntityCull.setView(view.posX, view.posY + view.getEyeHeight(), view.posZ);
            }
        }
        final long last = lastFrameNanos;
        lastFrameNanos = now;
        if (last == 0L) {
            return;
        }
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub != null) {
            final FrameStats stats = sub.frameStats;
            stats.record(now - last);
            BenchmarkRecorder.onFrame(stats);
        }
    }

    /** {@code Render.shouldRender}: true when the entity is beyond the cull distance. */
    public static boolean shouldCullEntity(final Entity entity, final double camX, final double camY, final double camZ) {
        if (!RenderToggles.entityCulling) {
            return false;
        }
        final double distanceSq = entity.getDistanceSq(camX, camY, camZ);
        if (!RenderToggles.beyondCullDistance(distanceSq)) {
            return false;
        }
        if (entity.width >= HUGE_ENTITY || entity.height >= HUGE_ENTITY) {
            return false;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.getRenderViewEntity() || entity == mc.player) {
            return false;
        }
        return !(mc.player != null && (entity == mc.player.getRidingEntity() || entity.isPassenger(mc.player)));
    }

    public static boolean shouldCullBlockEntity(final TileEntity blockEntity) {
        if (!BlockEntityCull.active() || blockEntity instanceof TileEntityBeacon
                || blockEntity instanceof TileEntityEndGateway) {
            return false;
        }
        final BlockPos pos = blockEntity.getPos();
        return BlockEntityCull.beyond(pos.getX(), pos.getY(), pos.getZ());
    }

    /** {@code ParticleManager.addEffect}: density thinning. */
    public static boolean shouldDropParticle() {
        return RenderToggles.active && RenderToggles.dropParticle();
    }

    /** {@code ParticleManager.spawnEffectParticle}: the per-type toggles. */
    public static boolean particleTypeHidden(final int particleId) {
        if (!ParticleFilter.anyDisabled() || particleId < 0 || particleId >= PARTICLE_CATEGORY.length) {
            return false;
        }
        final int category = PARTICLE_CATEGORY[particleId];
        return category != ParticleFilter.NONE && !ParticleFilter.allowed(category);
    }

    public static boolean hideBlockBreakParticles() {
        return RenderToggles.active && !ParticleFilter.blockBreaking();
    }

    public static boolean hideRainParticles() {
        return RenderToggles.hideWeather || (RenderToggles.active && !ParticleFilter.allowed(ParticleFilter.WEATHER));
    }

    public static boolean hideWeather() {
        return RenderToggles.hideWeather;
    }

    public static boolean freezeTextureAnimations() {
        return RenderToggles.freezeTextureAnimations;
    }

    public static boolean hideVignette() {
        return RenderToggles.hideVignette;
    }

    private static int[] buildParticleTable() {
        final EnumParticleTypes[] types = EnumParticleTypes.values();
        int max = 0;
        for (final EnumParticleTypes type : types) {
            max = Math.max(max, type.getParticleID());
        }
        final int[] table = new int[max + 1];
        java.util.Arrays.fill(table, ParticleFilter.NONE);
        for (final EnumParticleTypes type : types) {
            table[type.getParticleID()] = categoryOf1122(type.getParticleName());
        }
        return table;
    }

    /** 1.12.2 particle names (pre-flattening) to the main mod's categories. */
    static int categoryOf1122(final String name) {
        if (name == null) {
            return ParticleFilter.NONE;
        }
        switch (name) {
            case "explode":
            case "largeexplode":
            case "hugeexplosion":
            case "fireworksSpark":
                return ParticleFilter.EXPLOSIONS;
            case "droplet":
            case "splash":
                return ParticleFilter.WEATHER;
            case "smoke":
            case "largesmoke":
            case "cloud":
                return ParticleFilter.SMOKE;
            case "spell":
            case "instantSpell":
            case "mobSpell":
            case "mobSpellAmbient":
            case "witchMagic":
            case "dragonbreath":
                return ParticleFilter.POTIONS;
            case "crit":
            case "magicCrit":
            case "sweepAttack":
            case "damageIndicator":
                return ParticleFilter.COMBAT;
            case "flame":
            case "lava":
                return ParticleFilter.FIRE;
            case "dripWater":
            case "dripLava":
            case "bubble":
                return ParticleFilter.DRIPS;
            case "portal":
                return ParticleFilter.PORTALS;
            case "townaura":
            case "suspended":
            case "depthsuspend":
                return ParticleFilter.AMBIENT;
            case "totem":
                return ParticleFilter.TOTEM;
            default:
                return ParticleFilter.NONE;
        }
    }

    // ------------------------------------------------------------------ per tick

    public static void onClientTick(final Minecraft mc) {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub == null) {
            return;
        }
        if (!initialised) {
            initialised = true;
            tracker = new DynamicLightTracker(sub.config);
            userRenderDistance = VanillaOptions.getRenderDistance();
            adaptiveWasOn = sub.config.adaptiveDistance.get().booleanValue();
            onSettingsApplied();
        }
        sub.store.tick();
        if (mc.world != lastLevel) {
            lastLevel = mc.world;
            tracker.reset();
            ZoomKeys.reset();
            FreelookHooks.onLevelChange();
            ADAPTIVE.reset();
        }
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        if (mc.world == null || mc.player == null) {
            return;
        }
        ticks++;
        final AetheriumConfig config = sub.config;
        final boolean lightsAllowed = active && Capabilities.DYNAMIC_LIGHTS && AetheriumMixinPlugin.lightHooksInstalled();
        tracker.tick(mc, lightsAllowed);
        if (!active) {
            LIMITER.setCap(0);
            return;
        }
        if (ticks % ADAPTIVE_PERIOD_TICKS == 0 && config.adaptiveDistance.get().booleanValue()) {
            final int current = VanillaOptions.getRenderDistance();
            final int next = ADAPTIVE.evaluate(sub.frameStats.getFps(), config.adaptiveTargetFps.get().intValue(),
                    current, userRenderDistance > 0 ? userRenderDistance : current);
            if (next != current) {
                VanillaOptions.setRenderDistance(next);
            }
        }
        if (ticks % THERMAL_PERIOD_TICKS == 0) {
            updateThermal(sub.android, config);
        }
        updateCap(config);
    }

    private static void updateThermal(final AndroidEnvironment android, final AetheriumConfig config) {
        final boolean wanted = config.thermalGuard.get().booleanValue() && android.isAndroid()
                && android.isThermalSensorUsable();
        if (thermal == null) {
            if (!wanted) {
                thermalCapped = false;
                return;
            }
            thermal = new ThermalMonitor(android);
        }
        thermal.setWanted(wanted);
        if (!wanted) {
            thermalCapped = false;
            return;
        }
        final int milli = thermal.lastMilliCelsius();
        if (milli <= 0) {
            return;
        }
        final int ceiling = config.thermalCeilingC.get().intValue();
        final int celsius = milli / 1000;
        if (celsius >= ceiling) {
            thermalCapped = true;
        } else if (celsius < ceiling - THERMAL_HYSTERESIS_C) {
            thermalCapped = false;
        }
    }

    private static void updateCap(final AetheriumConfig config) {
        int cap = 0;
        if (config.batterySaver.get().booleanValue()) {
            cap = config.batteryFpsCap.get().intValue();
        }
        if (thermalCapped) {
            cap = cap == 0 ? THERMAL_CAP_FPS : Math.min(cap, THERMAL_CAP_FPS);
        }
        LIMITER.setCap(cap);
    }

    public static int currentCap() {
        return LIMITER.getCap();
    }

    public static boolean isThermalCapped() {
        return thermalCapped;
    }

    public static int activeLightSources() {
        return tracker == null ? 0 : tracker.activeSources();
    }

    // ------------------------------------------------------------------ settings

    public static void noteUserRenderDistance(final int chunks) {
        userRenderDistance = chunks;
        ADAPTIVE.reset();
    }

    public static void onSettingsApplied() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub == null) {
            return;
        }
        Aetherium.refreshState();
        final AetheriumConfig config = sub.config;
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        ZoomKeys.configure(config);
        configureScenery(config, active);
        FreelookHooks.configure(config);
        QolClient.configure(config, active);
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc != null && mc.gameDir != null) {
            StutterRecorder.configure(Capabilities.STUTTER_LOG && config.stutterLog.get().booleanValue(), mc.gameDir);
        }
        if (active && config.fullbright.get().booleanValue()) {
            final double strength = config.fullbrightStrength.get().intValue() / 100.0;
            VanillaOptions.setGammaOverride(1.0 + strength * 14.0);
        } else {
            VanillaOptions.setGammaOverride(Double.NaN);
        }
        final boolean adaptiveOn = active && config.adaptiveDistance.get().booleanValue();
        if (adaptiveWasOn && !adaptiveOn && userRenderDistance > 0
                && VanillaOptions.getRenderDistance() != userRenderDistance) {
            VanillaOptions.setRenderDistance(userRenderDistance);
        }
        adaptiveWasOn = adaptiveOn;
        ADAPTIVE.reset();
        if (tracker != null && (!active || config.dynamicLights.get() == AetheriumConfig.LightMode.OFF)) {
            tracker.clear(mc);
        }
        if (!config.thermalGuard.get().booleanValue()) {
            thermalCapped = false;
            if (thermal != null) {
                thermal.setWanted(false);
            }
        }
        if (!active) {
            LIMITER.setCap(0);
        } else {
            updateCap(config);
        }
    }

    private static void configureScenery(final AetheriumConfig config, final boolean active) {
        final boolean scenery = active && Capabilities.SCENERY_CONTROLS;
        Scenery.distanceFog = !scenery || config.distanceFog.get().booleanValue();
        Scenery.fogStartPercent = scenery ? config.fogStart.get().intValue() : Scenery.VANILLA_FOG_START;
        Scenery.waterFogPercent = scenery ? config.waterFog.get().intValue() : 100;
        Scenery.clearLava = scenery && config.clearLava.get().booleanValue();
        Scenery.sky = !scenery || config.sky.get().booleanValue();
        Scenery.sunMoon = !scenery || config.sunMoon.get().booleanValue();
        Scenery.stars = !scenery || config.stars.get().booleanValue();
        Scenery.cloudHeight = scenery ? config.cloudHeight.get().intValue() : 0;
        ParticleFilter.set(ParticleFilter.EXPLOSIONS, !scenery || config.particlesExplosions.get().booleanValue());
        ParticleFilter.set(ParticleFilter.WEATHER, !scenery || config.particlesWeather.get().booleanValue());
        ParticleFilter.set(ParticleFilter.SMOKE, !scenery || config.particlesSmoke.get().booleanValue());
        ParticleFilter.set(ParticleFilter.POTIONS, !scenery || config.particlesPotions.get().booleanValue());
        ParticleFilter.set(ParticleFilter.COMBAT, !scenery || config.particlesCombat.get().booleanValue());
        ParticleFilter.set(ParticleFilter.FIRE, !scenery || config.particlesFire.get().booleanValue());
        ParticleFilter.set(ParticleFilter.DRIPS, !scenery || config.particlesDrips.get().booleanValue());
        ParticleFilter.set(ParticleFilter.PORTALS, !scenery || config.particlesPortals.get().booleanValue());
        ParticleFilter.set(ParticleFilter.AMBIENT, !scenery || config.particlesAmbient.get().booleanValue());
        ParticleFilter.set(ParticleFilter.TOTEM, !scenery || config.particlesTotem.get().booleanValue());
        ParticleFilter.setBlockBreaking(!scenery || config.particlesBlockBreak.get().booleanValue());
    }

    // ------------------------------------------------------------------ screens

    /** Forge {@code GuiOpenEvent}: the Aetherium screen in place of Video Settings, or null. */
    public static GuiScreen replaceScreen(final Minecraft mc, final GuiScreen next) {
        if (!(next instanceof GuiVideoSettings) || next instanceof AetheriumScreen) {
            return null;
        }
        if (bypassVideoScreen) {
            bypassVideoScreen = false;
            return null;
        }
        if (Aetherium.subsystemsOrNull() == null) {
            return null;
        }
        return new AetheriumScreen(mc.currentScreen);
    }

    public static void openVanillaVideoSettings(final Minecraft mc, final GuiScreen parent) {
        bypassVideoScreen = true;
        mc.displayGuiScreen(new GuiVideoSettings(parent, mc.gameSettings));
        bypassVideoScreen = false;
    }
}
