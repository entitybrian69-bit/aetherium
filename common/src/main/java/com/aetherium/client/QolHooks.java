package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.Capabilities;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.perf.ChunkBuildExecutor;
import com.aetherium.perf.DirectPool;
import com.aetherium.perf.PacketBudget;
import com.aetherium.perf.RebuildCoalescer;
import com.aetherium.perf.StutterRecorder;
import com.aetherium.perf.UploadBudget;
import com.aetherium.qol.Freelook;
import com.aetherium.qol.ParticleFilter;
import com.aetherium.qol.Scenery;

// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.mixin.core.BlockableEventLoopInvoker;
//~ import com.mojang.blaze3d.systems.RenderSystem;

//~ import java.util.IdentityHashMap;
//~ import java.util.Map;
//~ import java.util.concurrent.Executor;

//~ import net.minecraft.client.Camera;
//~ import net.minecraft.client.CameraType;
//~ import net.minecraft.client.KeyMapping;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Registry;
//~ import net.minecraft.core.particles.ParticleOptions;
//~ import net.minecraft.core.particles.ParticleType;
//~ import net.minecraft.resources.ResourceLocation;
//~ import net.minecraft.tags.FluidTags;
//~ import net.minecraft.tags.Tag;
//~ import net.minecraft.world.effect.MobEffects;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.entity.LivingEntity;
//~ import net.minecraft.world.level.material.Fluid;
//~ import net.minecraft.world.level.material.FluidState;
//~ import net.minecraft.world.phys.Vec3;
// @era:qol16-end

/**
 * Minecraft side of the 1.5.0 features: terrain streaming (packet budget, prepared uploads,
 * rebuild coalescing, chunk-mesh threads), the stutter log, fog/sky/particle controls and
 * freelook. Everything here is render-thread only unless noted, and costs one field read while a
 * feature is at its vanilla setting.
 */
public final class QolHooks {
    private static final Freelook FREELOOK = new Freelook();
    private static final float[] FOG = new float[3];

    private static volatile boolean prepareUploads;
    private static volatile boolean meshThreads;
    private static boolean freelookToggle;
    private static boolean freelookThirdPerson = true;

    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ private static final int KEY_LEFT_ALT = 342;
    //~ private static final Map<ParticleType<?>, Integer> CATEGORY = new IdentityHashMap<ParticleType<?>, Integer>();
    //~ private static KeyMapping freelook;
    //~ /** Camera type to restore when freelook ends, or null when freelook did not change it. */
    //~ private static CameraType restoreCamera;
    //~ private static CameraType forcedCamera;
    // @era:qol16-end

    private QolHooks() {
    }

    /** After Apply and at startup. */
    public static void configure(final AetheriumConfig config) {
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        final boolean streaming = active && Capabilities.STREAMING && config.smoothStreaming.get().booleanValue();
        PacketBudget.setEnabled(streaming);
        prepareUploads = streaming;
        meshThreads = Capabilities.STREAMING && config.smoothStreaming.get().booleanValue();
        RebuildCoalescer.setEnabled(active && Capabilities.STREAMING && config.coalesceRebuilds.get().booleanValue());

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

        freelookToggle = config.freelookToggle.get().booleanValue();
        freelookThirdPerson = config.freelookThirdPerson.get().booleanValue();
        // @era:qol16-begin none
        // @era:qol16-else v16
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ if (mc != null && mc.gameDirectory != null) {
            //~ StutterRecorder.configure(Capabilities.STREAMING && config.stutterLog.get().booleanValue(), mc.gameDirectory);
        //~ }
        //~ if (!active && FREELOOK.active()) {
            //~ stopFreelook(mc);
        //~ }
        // @era:qol16-end
    }

    /** True while queued chunk uploads should be converted on the worker threads. */
    public static boolean prepareUploads() {
        return prepareUploads;
    }

    /**
     * World change / disconnect. Prepared meshes are not dropped here: their uploads may still be
     * queued (the first client tick of a world runs after its first chunk builds), and each one
     * needs its record. They are dropped with the dispatcher ({@code ChunkPrepareMixin} on
     * {@code dispose}), when no queued upload can run any more.
     */
    public static void onLevelChange() {
        DirectPool.trim();
        // @era:qol16-begin none
        // @era:qol16-else v16
        //~ if (FREELOOK.active()) {
            //~ stopFreelook(Minecraft.getInstance());
        //~ }
        // @era:qol16-end
    }

    // @era:qol16-begin none
    public static void frame(final net.minecraft.client.Minecraft mc) {
    }

    public static String describeKey() {
        return "Not available on this version";
    }
    // @era:qol16-else v16
    //~ // ------------------------------------------------------------------ per frame

    //~ /** GameRenderer.render HEAD (via ClientHooks), before the camera is set up. */
    //~ public static void frame(final Minecraft mc) {
        //~ StutterRecorder.frame(System.nanoTime(), null);
        //~ UploadBudget.adapt(StutterRecorder.averageFrameNanos());
        //~ if (freelook == null || mc == null) {
            //~ return;
        //~ }
        //~ if (mc.level == null || mc.player == null) {
            //~ if (FREELOOK.active()) {
                //~ stopFreelook(mc);
            //~ }
            //~ return;
        //~ }
        //~ final boolean down = mc.screen == null && freelook.isDown() && Aetherium.isActive();
        //~ final int change = FREELOOK.update(down, freelookToggle);
        //~ if (change == Freelook.STARTED) {
            //~ FREELOOK.begin(mc.player.yRot, mc.player.xRot);
            //~ restoreCamera = null;
            //~ forcedCamera = null;
            //~ if (freelookThirdPerson && mc.options.getCameraType().isFirstPerson()) {
                //~ restoreCamera = mc.options.getCameraType();
                //~ forcedCamera = CameraType.THIRD_PERSON_BACK;
                //~ setCamera(mc, forcedCamera);
            //~ }
        //~ } else if (change == Freelook.STOPPED) {
            //~ restore(mc);
        //~ }
    //~ }

    //~ private static void stopFreelook(final Minecraft mc) {
        //~ FREELOOK.stop();
        //~ restore(mc);
    //~ }

    //~ private static void restore(final Minecraft mc) {
        //~ // Only undo our own switch: if the player pressed F5 meanwhile, their choice stays.
        //~ if (mc != null && restoreCamera != null && mc.options.getCameraType() == forcedCamera) {
            //~ setCamera(mc, restoreCamera);
        //~ }
        //~ restoreCamera = null;
        //~ forcedCamera = null;
    //~ }

    //~ private static void setCamera(final Minecraft mc, final CameraType type) {
        //~ final boolean wasFirst = mc.options.getCameraType().isFirstPerson();
        //~ mc.options.setCameraType(type);
        //~ if (wasFirst != type.isFirstPerson()) {
            //~ mc.gameRenderer.checkEntityPostEffect(type.isFirstPerson() ? mc.getCameraEntity() : null);
        //~ }
    //~ }

    //~ // ------------------------------------------------------------------ freelook hooks

    //~ /** MouseHandler.turnPlayer: true when the mouse turned the free camera instead of the player. */
    //~ public static boolean freelookTurn(final double dx, final double dy) {
        //~ if (!FREELOOK.active()) {
            //~ return false;
        //~ }
        //~ FREELOOK.turn(dx, dy);
        //~ return true;
    //~ }

    //~ public static float cameraYaw(final float original) {
        //~ return FREELOOK.active() ? FREELOOK.yaw() : original;
    //~ }

    //~ public static float cameraPitch(final float original) {
        //~ return FREELOOK.active() ? FREELOOK.pitch() : original;
    //~ }

    //~ public static boolean freelookActive() {
        //~ return FREELOOK.active();
    //~ }

    //~ /** Options.load HEAD: the key list with the Freelook key appended (once). */
    //~ public static KeyMapping[] appendKeys(final KeyMapping[] existing) {
        //~ if (freelook == null) {
            //~ freelook = new KeyMapping("key.aetherium.freelook", KEY_LEFT_ALT, ZoomKeys.category());
        //~ }
        //~ if (existing == null) {
            //~ return new KeyMapping[] {freelook};
        //~ }
        //~ for (final KeyMapping key : existing) {
            //~ if (key == freelook) {
                //~ return existing;
            //~ }
        //~ }
        //~ final KeyMapping[] out = java.util.Arrays.copyOf(existing, existing.length + 1);
        //~ out[existing.length] = freelook;
        //~ return out;
    //~ }

    //~ /** "Left Alt" for the settings screen. */
    //~ public static String describeKey() {
        //~ return freelook == null ? "Not registered" : freelook.getTranslatedKeyMessage().getString();
    //~ }

    //~ // ------------------------------------------------------------------ streaming hooks

    //~ /** LevelRenderer.allChanged: the executor the new chunk dispatcher builds meshes on. */
    //~ public static Executor meshExecutor(final Executor vanilla) {
        //~ if (!meshThreads) {
            //~ return vanilla;
        //~ }
        //~ final Executor own = ChunkBuildExecutor.get(Runtime.getRuntime().availableProcessors(),
                //~ Aetherium.subsystemsOrNull() != null && Aetherium.subsystemsOrNull().android.isAndroid());
        //~ return own != null ? own : vanilla;
    //~ }

    //~ /**
     //~ * Minecraft.runTick, instead of {@code runAllTasks()}: runs queued packet tasks within the
     //~ * frame budget. False when the budget is off (or no world is loaded) and vanilla should run.
     //~ */
    //~ public static boolean drainTasks(final Minecraft mc) {
        //~ if (!PacketBudget.enabled() || mc.level == null || !((Object) mc instanceof BlockableEventLoopInvoker)) {
            //~ return false;
        //~ }
        //~ taskLoop = (BlockableEventLoopInvoker) (Object) mc;
        //~ StutterRecorder.packetTasks += PacketBudget.drain(POLLER, PacketBudget.SYSTEM);
        //~ StutterRecorder.packetBacklog = PacketBudget.pressure();
        //~ return true;
    //~ }

    //~ private static BlockableEventLoopInvoker taskLoop;
    //~ private static final PacketBudget.Poller POLLER = new PacketBudget.Poller() {
        //~ @Override
        //~ public boolean pollTask() {
            //~ return taskLoop.aetherium$pollTask();
        //~ }
    //~ };

    //~ private static double rebuildCamX;
    //~ private static double rebuildCamY;
    //~ private static double rebuildCamZ;

    //~ /** LevelRenderer.setupRender HEAD. */
    //~ public static void setRebuildCamera(final Camera camera) {
        //~ final Vec3 pos = camera.getPosition();
        //~ rebuildCamX = pos.x;
        //~ rebuildCamY = pos.y;
        //~ rebuildCamZ = pos.z;
    //~ }

    //~ /** setupRender's dirty check, for a section that is dirty: true to leave it for a later frame. */
    //~ public static boolean deferRebuild(final ChunkRenderDispatcher.RenderChunk chunk) {
        //~ if (!RebuildCoalescer.enabled() || !((Object) chunk instanceof RenderChunkDirtyAccess)) {
            //~ return false;
        //~ }
        //~ final RenderChunkDirtyAccess times = (RenderChunkDirtyAccess) (Object) chunk;
        //~ final BlockPos origin = chunk.getOrigin();
        //~ final double dx = origin.getX() + 8.0 - rebuildCamX;
        //~ final double dy = origin.getY() + 8.0 - rebuildCamY;
        //~ final double dz = origin.getZ() + 8.0 - rebuildCamZ;
        //~ final boolean compiled = chunk.getCompiledChunk() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
        //~ return RebuildCoalescer.defer(System.nanoTime(), times.aetheriumFirstDirty(), times.aetheriumLastDirty(),
                //~ chunk.isDirtyFromPlayer(), compiled, dx * dx + dy * dy + dz * dz);
    //~ }

    //~ /** Minimum time between visible-section searches caused only by finished uploads. */
    //~ static final long VISIBILITY_INTERVAL_NANOS = 50_000_000L;
    //~ private static long lastVisibilityUpdate;
    //~ private static boolean visibilityPending;

    //~ /**
     //~ * compileChunksUntil: whether this frame's uploads should trigger a new visible-section search.
     //~ * Sections already in the visible list draw their new mesh at once either way; the search only
     //~ * discovers sections behind them, so it is limited to 20 per second while uploads stream in.
     //~ */
    //~ public static boolean visibilityUpdateAfterUploads(final boolean uploaded) {
        //~ if (!PacketBudget.enabled()) {
            //~ return uploaded;
        //~ }
        //~ final long now = System.nanoTime();
        //~ if (uploaded) {
            //~ visibilityPending = true;
        //~ }
        //~ if (visibilityPending && now - lastVisibilityUpdate >= VISIBILITY_INTERVAL_NANOS) {
            //~ visibilityPending = false;
            //~ lastVisibilityUpdate = now;
            //~ return true;
        //~ }
        //~ return false;
    //~ }

    //~ // ------------------------------------------------------------------ fog and sky

    //~ /** {@code Tag.Named} extends {@code Tag}; widened here, as {@code FluidState.is} takes a {@code Tag}. */
    //~ @SuppressWarnings("unchecked")
    //~ private static Tag<Fluid> fluidTag(final Object named) {
        //~ return (Tag<Fluid>) named;
    //~ }

    //~ /** FogRenderer.setupFog TAIL, terrain fog only. */
    //~ public static void adjustFog(final Camera camera, final float farPlane, final boolean thick) {
        //~ if (Scenery.fogVanilla()) {
            //~ return;
        //~ }
        //~ final Entity entity = camera.getEntity();
        //~ if (entity instanceof LivingEntity && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS)) {
            //~ return;
        //~ }
        //~ final FluidState fluid = camera.getFluidInCamera();
        //~ final int medium = fluid.is(fluidTag(FluidTags.WATER)) ? Scenery.WATER
                //~ : fluid.is(fluidTag(FluidTags.LAVA)) ? Scenery.LAVA : Scenery.AIR;
        //~ if (!Scenery.adjustFog(medium, thick, farPlane, FogTracker.start(), FogTracker.end(), FogTracker.density(), FOG)) {
            //~ return;
        //~ }
        //~ if (medium == Scenery.WATER) {
            //~ RenderSystem.fogDensity(FOG[Scenery.DENSITY]);
        //~ } else {
            //~ RenderSystem.fogStart(FOG[Scenery.START]);
            //~ RenderSystem.fogEnd(FOG[Scenery.END]);
        //~ }
    //~ }

    //~ // ------------------------------------------------------------------ particles

    //~ /** ParticleEngine.createParticle HEAD: true to drop this particle. */
    //~ public static boolean dropParticle(final ParticleOptions options) {
        //~ if (!ParticleFilter.anyDisabled() || options == null) {
            //~ return false;
        //~ }
        //~ final ParticleType<?> type = options.getType();
        //~ Integer category = CATEGORY.get(type);
        //~ if (category == null) {
            //~ final ResourceLocation id = Registry.PARTICLE_TYPE.getKey(type);
            //~ category = Integer.valueOf(ParticleFilter.categoryOf(id == null ? null : id.toString()));
            //~ CATEGORY.put(type, category);
        //~ }
        //~ return !ParticleFilter.allowed(category.intValue());
    //~ }
    // @era:qol16-end
}
