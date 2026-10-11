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
import com.aetherium.perf.VisibilitySearch;
import com.aetherium.qol.ParticleFilter;
import com.aetherium.qol.Scenery;

import com.aetherium.mixin.core.BlockableEventLoopInvoker;

import net.minecraft.client.Minecraft;

// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.mojang.blaze3d.systems.RenderSystem;

//~ import java.util.IdentityHashMap;
//~ import java.util.Map;
//~ import java.util.concurrent.Executor;

//~ import net.minecraft.client.Camera;
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
 * rebuild coalescing, chunk-mesh threads), the stutter log and the fog/sky/particle controls
 * (freelook lives in {@link FreelookHooks}). Everything here is render-thread only unless noted,
 * and costs one field read while a feature is at its vanilla setting.
 */
public final class QolHooks {
    private static final float[] FOG = new float[3];

    private static volatile boolean prepareUploads;
    private static volatile boolean meshThreads;

    // @era:qol16-begin none
    // @era:qol16-else v16
    //~ private static final Map<ParticleType<?>, Integer> CATEGORY = new IdentityHashMap<ParticleType<?>, Integer>();
    // @era:qol16-end

    private QolHooks() {
    }

    /** After Apply and at startup. */
    public static void configure(final AetheriumConfig config) {
        final boolean active = Aetherium.isActive() && AetheriumMixinPlugin.isMixinActive();
        final boolean smooth = config.smoothStreaming.get().booleanValue();
        PacketBudget.setEnabled(active && Capabilities.PACKET_BUDGET && smooth);
        prepareUploads = active && Capabilities.STREAMING && smooth;
        meshThreads = Capabilities.MESH_THREADS && smooth;
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

        FreelookHooks.configure(config);
        final Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.gameDirectory != null) {
            StutterRecorder.configure(Capabilities.STUTTER_LOG && config.stutterLog.get().booleanValue(), mc.gameDirectory);
        }
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
        FreelookHooks.onLevelChange();
    }

    // ------------------------------------------------------------------ per frame

    /** GameRenderer.render HEAD (via ClientHooks), before the camera is set up. */
    public static void frame(final Minecraft mc) {
        StutterRecorder.frame(System.nanoTime(), null);
        UploadBudget.adapt(StutterRecorder.averageFrameNanos());
    }

    // ------------------------------------------------------------------ packet budget

    /**
     * Minecraft.runTick, instead of {@code runAllTasks()}: runs queued packet tasks within the
     * frame budget. False when the budget is off (or no world is loaded) and vanilla should run.
     */
    public static boolean drainTasks(final Minecraft mc) {
        if (!PacketBudget.enabled() || mc.level == null || !((Object) mc instanceof BlockableEventLoopInvoker)) {
            return false;
        }
        taskLoop = (BlockableEventLoopInvoker) (Object) mc;
        StutterRecorder.packetTasks += PacketBudget.drain(POLLER, PacketBudget.SYSTEM);
        StutterRecorder.packetBacklog = PacketBudget.pressure();
        return true;
    }

    private static BlockableEventLoopInvoker taskLoop;
    private static final PacketBudget.Poller POLLER = new PacketBudget.Poller() {
        @Override
        public boolean pollTask() {
            return taskLoop.aetheriumPollTask();
        }
    };

    // @era:qol16-begin none
    // @era:qol16-else v16
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

    //~ /**
     //~ * compileChunksUntil: whether this frame's uploads should trigger a new visible-section search.
     //~ * Sections already in the visible list draw their new mesh at once either way; the search only
     //~ * discovers sections behind them, so it is limited to 20 per second while uploads stream in
     //~ * (shared with the other streaming triggers, see {@code VisibilitySearch}).
     //~ */
    //~ public static boolean visibilityUpdateAfterUploads(final boolean uploaded) {
        //~ if (!PacketBudget.enabled()) {
            //~ return uploaded;
        //~ }
        //~ return VisibilitySearch.afterUploads(uploaded, System.nanoTime());
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
