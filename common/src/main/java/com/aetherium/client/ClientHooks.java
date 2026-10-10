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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;

/**
 * Everything the mixins call, in one place. Every entry point is render-thread
 * only, allocation-free on the per-frame and per-entity paths, and a no-op
 * while Aetherium is not ACTIVE (master switch off, conflict, or init failure),
 * so turning the mod off really is vanilla.
 */
public final class ClientHooks {

    private static final FrameLimiter LIMITER = new FrameLimiter();
    private static final AdaptiveDistance ADAPTIVE = new AdaptiveDistance();

    private static final int ADAPTIVE_PERIOD_TICKS = 40; // 2 s
    private static final int THERMAL_PERIOD_TICKS = 100; // 5 s
    private static final int THERMAL_HYSTERESIS_C = 3;
    private static final int THERMAL_CAP_FPS = 30;

    /** Entities this large stay visible at any distance (dragons, withers, ghasts). */
    private static final float HUGE_ENTITY = 3.0f;

    private static DynamicLightTracker tracker;
    private static boolean initialised;
    private static Object lastLevel;
    private static int ticks;
    private static long lastFrameNanos;

    private static boolean thermalCapped;
    private static ThermalMonitor thermal;
    private static boolean adaptiveWasOn;
    /** Render distance the user picked; adaptive distance never exceeds it and restores it when turned off. */
    private static int userRenderDistance = -1;

    /** Set by the "Vanilla video settings" button so the next video screen is not swapped. */
    private static boolean bypassVideoScreen;

    private ClientHooks() {
    }

    // ------------------------------------------------------------------ per frame

    /** GameRenderer.render HEAD: frame pacing (battery/thermal caps) and frame-time stats. */
    public static void onFrameStart() {
        if (Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive()) {
            LIMITER.beforeFrame();
        }
        if (BlockEntityCull.active()) {
            final Entity view = Minecraft.getInstance().getCameraEntity();
            if (view != null) {
                BlockEntityCull.setView(view.getX(), view.getEyeY(), view.getZ());
            }
        }
        final long now = System.nanoTime();
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

    /**
     * EntityRenderDispatcher.shouldRender HEAD.
     *
     * @return true to skip rendering this entity this frame
     */
    public static boolean shouldCullEntity(final Entity entity, final double camX, final double camY, final double camZ) {
        if (!RenderToggles.entityCulling) {
            return false;
        }
        final double distanceSq = entity.distanceToSqr(camX, camY, camZ);
        if (!RenderToggles.beyondCullDistance(distanceSq)) {
            return false;
        }
        if (entity.getBbWidth() >= HUGE_ENTITY || entity.getBbHeight() >= HUGE_ENTITY) {
            return false;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (entity == mc.getCameraEntity() || entity == mc.player) {
            return false;
        }
        if (mc.player != null && (entity == mc.player.getVehicle() || entity.hasPassenger(mc.player))) {
            return false;
        }
        return true;
    }

    /** ParticleEngine.add HEAD: true to drop the particle. */
    public static boolean shouldDropParticle() {
        return RenderToggles.active && RenderToggles.dropParticle();
    }

    public static boolean hideWeather() {
        return RenderToggles.hideWeather;
    }

    /** BlockEntityRenderDispatcher.render HEAD: true to skip a block entity past the distance limit. */
    public static boolean shouldCullBlockEntity(final BlockEntity blockEntity) {
        if (!BlockEntityCull.active() || blockEntity instanceof BeaconBlockEntity || blockEntity instanceof TheEndGatewayBlockEntity) {
            return false;
        }
        final BlockPos pos = blockEntity.getBlockPos();
        return BlockEntityCull.beyond(pos.getX(), pos.getY(), pos.getZ());
    }

    /** TextureAtlas.cycleAnimationFrames HEAD: true to keep animated textures on their current frame. */
    public static boolean freezeTextureAnimations() {
        return RenderToggles.freezeTextureAnimations;
    }

    public static boolean hideVignette() {
        return RenderToggles.hideVignette;
    }

    // ------------------------------------------------------------------ per tick

    /** Minecraft.tick TAIL. */
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

        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            tracker.reset();
            ADAPTIVE.reset();
        }

        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        if (mc.level == null || mc.player == null) {
            return;
        }
        ticks++;
        final AetheriumConfig config = sub.config;

        // Without the light hook (dynamic lights were Off at launch) the tracker would only cause
        // pointless chunk rebuilds, so it stays idle until the next restart.
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
        // Phones only: desktop CPUs sit above any sensible phone ceiling under normal load.
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
        // Read on a background thread; the render thread only picks up the latest value.
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

    /** Current live frame cap (0 = none), for the GUI. */
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

    /** The user picked a render distance in our GUI; adaptive distance treats it as the ceiling. */
    public static void noteUserRenderDistance(final int chunks) {
        userRenderDistance = chunks;
        ADAPTIVE.reset();
    }

    /** Republishes every live flag from the config. Called after Apply and once at startup. */
    public static void onSettingsApplied() {
        final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
        if (sub == null) {
            return;
        }
        Aetherium.refreshState();
        final AetheriumConfig config = sub.config;
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();

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
            tracker.clear(Minecraft.getInstance());
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

    // ------------------------------------------------------------------ screens

    /**
     * Minecraft.setScreen HEAD. Returns the screen to open instead of {@code next},
     * or null to let vanilla proceed unchanged.
     */
    public static Screen replaceScreen(final Minecraft mc, final Screen next) {
        if (!isVideoSettings(next)) {
            return null;
        }
        if (bypassVideoScreen) {
            bypassVideoScreen = false;
            return null;
        }
        if (Aetherium.subsystemsOrNull() == null) {
            return null;
        }
        return new AetheriumScreen(currentScreen(mc));
    }

    /** Opens vanilla's own video screen on top of ours (escape hatch). */
    public static void openVanillaVideoSettings(final Minecraft mc, final Screen parent) {
        bypassVideoScreen = true;
        // @era:screen-pkg-begin options
        setScreen(mc, new net.minecraft.client.gui.screens.options.VideoSettingsScreen(parent, mc, mc.options));
        // @era:screen-pkg-else flat
        //~ setScreen(mc, new net.minecraft.client.gui.screens.VideoSettingsScreen(parent, mc.options));
        // @era:screen-pkg-end
        bypassVideoScreen = false;
    }

    /** The open screen; it moved from {@code Minecraft} to {@code Gui} in 26.2. */
    public static Screen currentScreen(final Minecraft mc) {
        // @era:screen-owner-begin minecraft
        return mc.screen;
        // @era:screen-owner-else gui
        //~ return mc.gui.screen();
        // @era:screen-owner-end
    }

    public static void setScreen(final Minecraft mc, final Screen screen) {
        // @era:screen-owner-begin minecraft
        mc.setScreen(screen);
        // @era:screen-owner-else gui
        //~ mc.gui.setScreen(screen);
        // @era:screen-owner-end
    }

    private static boolean isVideoSettings(final Screen screen) {
        // @era:screen-pkg-begin options
        return screen instanceof net.minecraft.client.gui.screens.options.VideoSettingsScreen;
        // @era:screen-pkg-else flat
        //~ return screen instanceof net.minecraft.client.gui.screens.VideoSettingsScreen;
        // @era:screen-pkg-end
    }
}
