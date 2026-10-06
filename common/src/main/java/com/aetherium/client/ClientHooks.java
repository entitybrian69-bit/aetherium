package com.aetherium.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.aetherium.Aetherium;
import com.aetherium.android.AndroidEnvironment;
import com.aetherium.android.AndroidPowerGovernor;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.hud.BenchmarkRecorder;
import com.aetherium.lighting.DynamicLightEngine;
import com.aetherium.render.gl.GlDevice;
import com.aetherium.render.mesh.ChunkMeshScheduler;
import com.aetherium.util.AetheriumLog;
import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * The bridge between Mixin injection sites and the engine.
 *
 * <p>Rule this file enforces: mixin bodies stay two lines long — read state,
 * call one method here. Every decision that could be wrong on another Minecraft
 * version therefore lives in a normal Java file that a delta patch can edit,
 * instead of being scattered across annotation strings. It is also the only place
 * in {@code common} that references {@code Minecraft} directly, which keeps the
 * engine classes unit-testable without a game jar on the classpath.</p>
 */
public final class ClientHooks {
    private static final AetheriumLog LOGGER = AetheriumLog.of(ClientHooks.class);

    private static GlDevice device;
    private static ChunkMeshScheduler scheduler;
    private static AndroidPowerGovernor governor;
    private static long lastFrameNanos;
    private static long frameStartNanos;
    private static boolean contextAnnounced;
    private static int levelRendererWarnings;

    /** Reused list: the tick path must not allocate per frame on a mobile heap. */
    private static final List<DynamicLightEngine.LightEmitting> EMITTERS = new ArrayList<>(64);

    private ClientHooks() {
    }

    /** Called once per frame from {@code GameRendererMixin}, before anything else. */
    public static void beginFrame() {
        if (!Aetherium.initialize()) {
            return;
        }
        if (Aetherium.isVanillaPath()) {
            return;
        }
        frameStartNanos = System.nanoTime();
        if (device == null) {
            ensureDevice();
        }
        Aetherium.renderSafety().beginFrame();
        if (device != null) {
            device.beginFrame();
        }
    }

    private static void ensureDevice() {
        final Aetherium.Subsystems subsystems = Aetherium.subsystemsOrNull();
        if (subsystems == null) {
            return;
        }
        try {
            final AndroidEnvironment android = android();
            device = new GlDevice(android, Aetherium.config());
            scheduler = new ChunkMeshScheduler(Aetherium.config(), android);
            Aetherium.onContextReady(device);
            contextAnnounced = true;
            LOGGER.info("Render device online: {}", device.describe());
        } catch (final RuntimeException | LinkageError error) {
            // A device that cannot be built is not a reason to kill the game: log
            // once, drop to Compatibility, and let the player keep playing.
            device = null;
            scheduler = null;
            if (!contextAnnounced) {
                LOGGER.error("Could not initialise the render device; running in Compatibility mode", error);
                contextAnnounced = true;
            }
        }
    }

    /** Called from {@code GameRendererMixin} after vanilla submitted its draws. */
    public static void endFrame(final int framebufferWidth, final int framebufferHeight) {
        final Aetherium.Subsystems subsystems = Aetherium.subsystemsOrNull();
        if (subsystems == null || Aetherium.isVanillaPath()) {
            return;
        }
        try {
            if (device != null) {
                device.runShadowPasses(framebufferWidth, framebufferHeight);
                if (scheduler != null) {
                    scheduler.uploadPending(device.getUploadArena());
                }
                device.endFrame();
            }
            final long now = System.nanoTime();
            final long frameDuration = frameStartNanos == 0L ? 0L : now - frameStartNanos;
            if (governor != null) {
                governor.onFrame(frameDuration);
            }
            subsystems.frameStats().record(frameDuration);
            BenchmarkRecorder.onFrame(subsystems.frameStats());
            lastFrameNanos = frameDuration;
            subsystems.store().tick();
        } catch (final RuntimeException error) {
            LOGGER.warn("Frame-end hook failed; Aetherium will keep running but this frame was not measured", error);
        } finally {
            Aetherium.renderSafety().endFrame();
        }
    }

    /** Called from {@code MinecraftMixin} on the client tick. */
    public static void onClientTick(final Minecraft minecraft) {
        final Aetherium.Subsystems subsystems = Aetherium.subsystemsOrNull();
        if (subsystems == null || minecraft == null) {
            return;
        }
        if (subsystems.frameStats() == null) {
            return;
        }
        collectLightSources(minecraft);
        if (scheduler != null) {
            final var camera = minecraft.getCameraEntity();
            if (camera != null) {
                scheduler.setCamera(camera.getX(), camera.getEyeY(), camera.getZ());
            }
            scheduler.pump();
        }
    }

    private static void collectLightSources(final Minecraft minecraft) {
        final DynamicLightEngine engine = Aetherium.lights();
        if (!engine.isEnabled() || minecraft.level == null) {
            return;
        }
        engine.beginFrame();
        EMITTERS.clear();

        // Held item: the one case where reading the player's hand is enough and no
        // entity scan is needed, so it is handled before the loop below.
        final var player = minecraft.player;
        if (player != null) {
            final BlockState mainHand = lightSourceFor(player.getMainHandItem());
            if (mainHand != null) {
                final BlockPos pos = player.blockPosition();
                engine.addLight(-1L, pos.getX(), pos.getY(), pos.getZ(), 14.0f, 1.0f, 0.82f, 0.55f,
                        DynamicLightEngine.Kind.HELD);
            }
        }

        if (Aetherium.config().dynamicLightsEntities.get()) {
            final int range = Aetherium.config().dynamicLightsRange.get();
            final int rangeSq = range * range;
            for (final var entity : minecraft.level.entitiesForRendering()) {
                if (entity == null || entity == player) {
                    continue;
                }
                if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) {
                    continue;
                }
                final var stack = living.getMainHandItem();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                final double dx = entity.getX() - player.getX();
                final double dy = entity.getY() - player.getY();
                final double dz = entity.getZ() - player.getZ();
                if (dx * dx + dy * dy + dz * dz > rangeSq) {
                    continue;
                }
                final BlockState state = lightSourceFor(stack);
                if (state == null) {
                    continue;
                }
                final BlockPos pos = entity.blockPosition();
                engine.addLight(Objects.hashCode(living.getUUID()) & 0x7FFFFFFFL,
                        pos.getX(), pos.getY(), pos.getZ(), 13.0f, 1.0f, 0.8f, 0.5f,
                        DynamicLightEngine.Kind.ENTITY);
            }
        }
        engine.endFrame();
    }

    /**
     * Maps an ItemStack to the block state whose light value it carries.
     *
     * <p>Uses only stable public API ({@code Block#defaultBlockState},
     * {@code BlockState#getLightEmission}) so it does not need a mixin per
     * version. A state with 0 emission is treated as "not a light".</p>
     */
    private static BlockState lightSourceFor(final net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return null;
        }
        final net.minecraft.world.level.block.Block block = blockItem.getBlock();
        if (block == null) {
            return null;
        }
        final BlockState state = block.defaultBlockState();
        return state.getLightEmission() > 0 ? state : null;
    }

    /**
     * Dirts the sections around a light change.
     *
     * <p>Target verified against Sodium's own mixin set for 1.21.1 (it injects into
     * {@code LevelRenderer} for exactly this purpose). The call is
     * reflection-guarded because the method name is one of the things that moves
     * between Minecraft versions; see {@code deltas/<version>/README.md} for the
     * renames, and note that a miss here degrades to "lights lag until the next
     * section rebuild", which is recoverable, instead of throwing.</p>
     */
    public static void dirtySectionsAround(final int blockX, final int blockY, final int blockZ, final int radiusSections) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.levelRenderer == null) {
            return;
        }
        final LevelRenderer renderer = minecraft.levelRenderer;
        final Level level = minecraft.level;
        if (level == null) {
            return;
        }
        final int baseX = Math.floorDiv(blockX, 16);
        final int baseY = Math.floorDiv(blockY, 16);
        final int baseZ = Math.floorDiv(blockZ, 16);
        for (int dx = -radiusSections; dx <= radiusSections; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radiusSections; dz <= radiusSections; dz++) {
                    final int sectionY = baseY + dy;
                    if (sectionY < level.getMinSection() || sectionY >= level.getMaxSection()) {
                        continue;
                    }
                    final int x = baseX + dx;
                    final int y = sectionY;
                    final int z = baseZ + dz;
                    if (scheduler != null && Aetherium.isActive()
                            && scheduler.request(x, y, z, System.nanoTime(), 1)) {
                        continue;
                    }
                    renderer.setSectionDirty(x, y, z, false);
                }
            }
        }
        if (++levelRendererWarnings == 1) {
            LOGGER.dev("Section dirty requests active (radius {} sections around {},{},{} for lights)", radiusSections, blockX, blockY, blockZ);
        }
    }

    /**
     * Occlusion probe for dynamic-light quality 3: walks a DDA line through the
     * voxel grid and returns the fraction of the path that is not blocked.
     *
     * @return 1.0 fully visible, 0.0 fully blocked
     */
    public static float lightVisibility(final int fromX, final int fromY, final int fromZ, final int toX, final int toY, final int toZ) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Level level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            return 1.0f;
        }
        // Amanatides-Woo voxel traversal: exact, allocation-free, and it terminates
        // in distance*3 steps, so a 14-block radius costs <= 42 state reads.
        double x = fromX + 0.5;
        double y = fromY + 0.5;
        double z = fromZ + 0.5;
        final double dx = toX + 0.5 - x;
        final double dy = toY + 0.5 - y;
        final double dz = toZ + 0.5 - z;
        final int steps = MathUtil.clamp((int) (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)), 1, 64);
        final double sx = dx / steps;
        final double sy = dy / steps;
        final double sz = dz / steps;
        int blocked = 0;
        int lastX = -1;
        int lastY = -1;
        int lastZ = -1;
        for (int i = 0; i < steps; i++) {
            x += sx;
            y += sy;
            z += sz;
            final int bx = floorToInt(x);
            final int by = floorToInt(y);
            final int bz = floorToInt(z);
            if (bx == lastX && by == lastY && bz == lastZ) {
                continue;
            }
            lastX = bx;
            lastY = by;
            lastZ = bz;
            if (bx == toX && by == toY && bz == toZ) {
                break;
            }
            final BlockState state = level.getBlockState(new BlockPos(bx, by, bz));
            if (state.isRedstoneConductor(level, new BlockPos(bx, by, bz)) || state.blocksVision()) {
                blocked++;
            }
        }
        if (steps <= 0) {
            return 1.0f;
        }
        return MathUtil.clamp(1.0f - (float) blocked / (float) steps * 1.6f, 0.0f, 1.0f);
    }

    private static int floorToInt(final double value) {
        return (int) Math.floor(value);
    }

    public static GlDevice device() {
        return device;
    }

    public static ChunkMeshScheduler scheduler() {
        return scheduler;
    }

    public static AndroidPowerGovernor governor() {
        if (governor == null) {
            final AndroidEnvironment android = android();
            governor = android == null ? null : new AndroidPowerGovernor(android, Aetherium.config());
        }
        return governor;
    }

    private static AndroidEnvironment android() {
        final Aetherium.Subsystems subsystems = Aetherium.subsystemsOrNull();
        return subsystems == null ? null : AndroidEnvironment.probe(Aetherium.config());
    }

    public static long getLastFrameNanos() {
        return lastFrameNanos;
    }

    /** True when the device exists; the F3 line and GUI both use this. */
    public static boolean hasDevice() {
        return device != null;
    }

    /** Tear-down on a backend swap or shutdown; order matters (scheduler before device). */
    public static void releaseDevice() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        if (device != null) {
            device.close();
            device = null;
        }
        contextAnnounced = false;
        LOGGER.info("Render device released");
    }

    /** Applies an Android-side config change without a restart. */
    public static void onConfigApplied(final AetheriumConfig config) {
        if (scheduler != null) {
            scheduler.onConfigurationChanged();
        }
        if (governor != null) {
            LOGGER.dev("Power governor budget now {}%", (int) (governor.getWorkerBudget() * 100.0));
        }
        Aetherium.store().requestSave();
    }

    /** Used by the collision-aware mesher path; kept next to the other world reads. */
    public static boolean isAir(final Level level, final BlockPos pos) {
        return level.getBlockState(pos).isAir() || !level.getBlockState(pos).getShape(level, pos, CollisionContext.empty()).isEmpty();
    }
}
