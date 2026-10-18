package com.aetherium.client;

import com.aetherium.perf.Tuning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
// @era:t-client-begin v16
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;
// @era:t-client-else none
// @era:t-client-else v19
//~ import it.unimi.dsi.fastutil.longs.LongIterator;
//~ import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//~ import net.minecraft.client.gui.screens.ChatScreen;
//~ import net.minecraft.client.gui.screens.ConnectScreen;
//~ import net.minecraft.client.gui.screens.LevelLoadingScreen;
//~ import net.minecraft.client.gui.screens.ProgressScreen;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.core.SectionPos;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.level.BlockGetter;
//~ import net.minecraft.world.level.block.Blocks;
//~ import net.minecraft.world.level.block.LeavesBlock;
//~ import net.minecraft.world.level.block.PipeBlock;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import org.lwjgl.glfw.GLFW;
// @era:t-client-else v262
//~ import it.unimi.dsi.fastutil.longs.LongIterator;
//~ import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
//~ import net.minecraft.client.gui.screens.ChatScreen;
//~ import net.minecraft.client.gui.screens.ConnectScreen;
//~ import net.minecraft.client.gui.screens.LevelLoadingScreen;
//~ import net.minecraft.client.gui.screens.ProgressScreen;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.core.SectionPos;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.level.BlockGetter;
//~ import net.minecraft.world.level.block.Blocks;
//~ import net.minecraft.world.level.block.LeavesBlock;
//~ import net.minecraft.world.level.block.PipeBlock;
//~ import net.minecraft.world.level.block.state.BlockState;
// @era:t-client-end

/**
 * Client side of the 1.10.0 tuning options (Minecraft 1.16.5; {@code Capabilities.TUNING}). Every
 * other version gets the no-op variant, so the calls from {@code ClientHooks} compile everywhere.
 *
 * <ul>
 *   <li>frame-rate caps: background (15 FPS), minimized (1 FPS) and menus open in a world;</li>
 *   <li>a garbage collection while a loading screen is shown;</li>
 *   <li>light merging: section rebuilds requested by light updates, deduplicated per 2 ticks;</li>
 *   <li>reloading the chunks when the leaves mode changes.</li>
 * </ul>
 */
public final class TuningClient {
    /** Frame rate while the window is in the background. */
    public static final int BACKGROUND_FPS = 15;
    /** Frame rate while the window is minimized. */
    public static final int MINIMIZED_FPS = 1;

    private TuningClient() {
    }

    // @era:t-client-begin v16
    private static final long GC_SPACING_NANOS = 10_000_000_000L;
    private static final LongOpenHashSet DIRTY_LIGHT = new LongOpenHashSet();
    private static long lastGc;
    private static boolean gcPending;
    private static int lightTicks;
    private static int lastLeaves = -1;
    /** Mesh-affecting switches as bits (leaves excluded): fast grass, End chorus culling. */
    private static int lastMesh = -1;

    /**
     * The frame-rate cap these options want right now (0 = none). Read on the client tick, so it
     * follows focus changes and opened menus within 50 ms.
     */
    public static int frameCap(final Minecraft mc) {
        if (!Tuning.on || AutoBench.ACTIVE) {
            return 0;
        }
        if (Tuning.dynamicFps) {
            final long window = mc.getWindow().getWindow();
            if (window != 0L && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) != 0) {
                return MINIMIZED_FPS;
            }
            if (!mc.isWindowActive()) {
                return BACKGROUND_FPS;
            }
        }
        final Screen screen = mc.screen;
        if (Tuning.menuFps > 0 && mc.level != null && screen != null && !(screen instanceof ChatScreen)
                && !screen.getClass().getName().startsWith("com.aetherium.")) {
            // Aetherium's own screen keeps full speed for its animations.
            return Tuning.menuFps;
        }
        return 0;
    }

    /** Minecraft.setScreen: one garbage collection when a loading screen opens. */
    public static void onScreen(final Minecraft mc, final Screen next) {
        if (!Tuning.loadGc || next == null) {
            return;
        }
        if (!loadingScreen(next)) {
            return;
        }
        if (loadingScreen(mc.screen)) {
            return; // already behind a loading screen
        }
        gcPending = true; // collected on the next tick, once the loading screen is on screen
    }

    private static boolean loadingScreen(final Screen screen) {
        return screen instanceof ReceivingLevelScreen || screen instanceof LevelLoadingScreen
                || screen instanceof ConnectScreen || screen instanceof ProgressScreen;
    }

    /**
     * ClientChunkCache.onLightUpdate: remembers the section instead of rebuilding it now.
     *
     * @return true when the rebuild was deferred (vanilla must not run)
     */
    public static boolean deferLight(final SectionPos pos) {
        if (!Tuning.lightUpdates) {
            return false;
        }
        DIRTY_LIGHT.add(pos.asLong());
        return true;
    }

    /** Minecraft.tick (also while no world is loaded): pending GC, merged light rebuilds every 2 ticks. */
    public static void tick(final Minecraft mc) {
        if (gcPending) {
            gcPending = false;
            final long now = System.nanoTime();
            if (Tuning.loadGc && loadingScreen(mc.screen) && (lastGc == 0L || now - lastGc >= GC_SPACING_NANOS)) {
                lastGc = now;
                System.gc();
            }
        }
        if (DIRTY_LIGHT.isEmpty()) {
            lightTicks = 0;
            return;
        }
        if (++lightTicks < 2 && Tuning.lightUpdates) {
            return;
        }
        lightTicks = 0;
        flushLight(mc);
    }

    private static void flushLight(final Minecraft mc) {
        if (mc.levelRenderer != null && mc.level != null) {
            final LongIterator it = DIRTY_LIGHT.iterator();
            while (it.hasNext()) {
                final long key = it.nextLong();
                mc.levelRenderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
            }
        }
        DIRTY_LIGHT.clear();
    }

    /** The world changed (join, leave, dimension change). */
    public static void levelChanged() {
        DIRTY_LIGHT.clear();
        lightTicks = 0;
        FarTerrain.levelChanged();
        WorldCache.levelChanged();
        OceanFloor.levelChanged();
    }

    /** After Apply: a leaves mode change needs the chunks rebuilt with the new render type. */
    public static void configured(final Minecraft mc) {
        final int leaves = Tuning.leaves;
        final int mesh = (Tuning.fastGrass ? 1 : 0) | (Tuning.endOptimizations ? 2 : 0);
        final boolean changed = (lastLeaves != -1 && leaves != lastLeaves) || (lastMesh != -1 && mesh != lastMesh);
        if (changed && mc.levelRenderer != null && mc.level != null) {
            mc.levelRenderer.allChanged();
        }
        lastLeaves = leaves;
        lastMesh = mesh;
    }

    /**
     * Leaf culling (chunk build threads): the face of a leaf block toward {@code side} is hidden when
     * the neighbour there is leaves too. With fancy leaves the block beyond must also be leaves (or
     * opaque), so the outer two layers stay visible and trees keep their depth; with fast (solid)
     * leaves every leaf-to-leaf face is invisible anyway.
     */
    public static boolean leafFaceHidden(final BlockGetter level, final BlockPos pos, final Direction side) {
        return leafNeighbourHidden(level, pos.relative(side), side);
    }

    /** {@link #leafFaceHidden} given the neighbour position itself (1.21.5+ hands it over). */
    public static boolean leafNeighbourHidden(final BlockGetter level, final BlockPos next, final Direction side) {
        if (!(level.getBlockState(next).getBlock() instanceof LeavesBlock)) {
            return false;
        }
        if (!Tuning.leavesFancy) {
            return true;
        }
        final BlockState beyond = level.getBlockState(next.relative(side));
        return beyond.getBlock() instanceof LeavesBlock || beyond.canOcclude();
    }

    /**
     * Chorus culling (End optimizations, 1.12.0): whether the {@code side} face of a chorus plant
     * stem is the end cap of an arm joined to the next chorus plant's arm, which is buried inside
     * the joint. Chunk build threads.
     */
    public static boolean chorusFaceHidden(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final Direction side) {
        return chorusNeighbourHidden(state, level, pos.relative(side), side);
    }

    /** {@link #chorusFaceHidden} given the neighbour position itself. */
    public static boolean chorusNeighbourHidden(final BlockState state, final BlockGetter level, final BlockPos at,
                                                final Direction side) {
        if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
            return false;
        }
        final BlockState next = level.getBlockState(at);
        return next.getBlock() == Blocks.CHORUS_PLANT
                && next.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()));
    }

    /** Squared distance from the camera to an entity (render thread). */
    public static double distanceToCamera(final Entity entity) {
        return Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);
    }
    // @era:t-client-else none
    //~ public static int frameCap(final Minecraft mc) {
        //~ return 0;
    //~ }

    //~ public static void onScreen(final Minecraft mc, final Screen next) {
    //~ }

    //~ public static void tick(final Minecraft mc) {
    //~ }

    //~ public static void levelChanged() {
    //~ }

    //~ public static void configured(final Minecraft mc) {
    //~ }
    // @era:t-client-else v19
    //~ private static final long GC_SPACING_NANOS = 10_000_000_000L;
    //~ private static final LongOpenHashSet DIRTY_LIGHT = new LongOpenHashSet();
    //~ private static long lastGc;
    //~ private static boolean gcPending;
    //~ private static int lightTicks;
    //~ private static int lastLeaves = -1;
    //~ /** Mesh-affecting switches as bits (leaves excluded): fast grass, End chorus culling. */
    //~ private static int lastMesh = -1;

    //~ /**
     //~ * The frame-rate cap these options want right now (0 = none). Read on the client tick, so it
     //~ * follows focus changes and opened menus within 50 ms.
     //~ */
    //~ public static int frameCap(final Minecraft mc) {
        //~ if (!Tuning.on || AutoBench.ACTIVE) {
            //~ return 0;
        //~ }
        //~ if (Tuning.dynamicFps) {
            //~ final long window = mc.getWindow().handle();
            //~ if (window != 0L && GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_ICONIFIED) != 0) {
                //~ return MINIMIZED_FPS;
            //~ }
            //~ if (!mc.isWindowActive()) {
                //~ return BACKGROUND_FPS;
            //~ }
        //~ }
        //~ final Screen screen = mc.screen;
        //~ if (Tuning.menuFps > 0 && mc.level != null && screen != null && !(screen instanceof ChatScreen)
                //~ && !screen.getClass().getName().startsWith("com.aetherium.")) {
            //~ // Aetherium's own screen keeps full speed for its animations.
            //~ return Tuning.menuFps;
        //~ }
        //~ return 0;
    //~ }

    //~ /** Minecraft.setScreen: one garbage collection when a loading screen opens. */
    //~ public static void onScreen(final Minecraft mc, final Screen next) {
        //~ if (!Tuning.loadGc || next == null) {
            //~ return;
        //~ }
        //~ if (!loadingScreen(next)) {
            //~ return;
        //~ }
        //~ if (loadingScreen(mc.screen)) {
            //~ return; // already behind a loading screen
        //~ }
        //~ gcPending = true; // collected on the next tick, once the loading screen is on screen
    //~ }

    //~ private static boolean loadingScreen(final Screen screen) {
        //~ return screen instanceof LevelLoadingScreen
                //~ || screen instanceof ConnectScreen || screen instanceof ProgressScreen;
    //~ }

    //~ /**
     //~ * ClientChunkCache.onLightUpdate: remembers the section instead of rebuilding it now.
     //~ *
     //~ * @return true when the rebuild was deferred (vanilla must not run)
     //~ */
    //~ public static boolean deferLight(final SectionPos pos) {
        //~ if (!Tuning.lightUpdates) {
            //~ return false;
        //~ }
        //~ DIRTY_LIGHT.add(pos.asLong());
        //~ return true;
    //~ }

    //~ /** Minecraft.tick (also while no world is loaded): pending GC, merged light rebuilds every 2 ticks. */
    //~ public static void tick(final Minecraft mc) {
        //~ if (gcPending) {
            //~ gcPending = false;
            //~ final long now = System.nanoTime();
            //~ if (Tuning.loadGc && loadingScreen(mc.screen) && (lastGc == 0L || now - lastGc >= GC_SPACING_NANOS)) {
                //~ lastGc = now;
                //~ System.gc();
            //~ }
        //~ }
        //~ if (DIRTY_LIGHT.isEmpty()) {
            //~ lightTicks = 0;
            //~ return;
        //~ }
        //~ if (++lightTicks < 2 && Tuning.lightUpdates) {
            //~ return;
        //~ }
        //~ lightTicks = 0;
        //~ flushLight(mc);
    //~ }

    //~ private static void flushLight(final Minecraft mc) {
        //~ if (mc.levelRenderer != null && mc.level != null) {
            //~ final LongIterator it = DIRTY_LIGHT.iterator();
            //~ while (it.hasNext()) {
                //~ final long key = it.nextLong();
                //~ mc.levelRenderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
            //~ }
        //~ }
        //~ DIRTY_LIGHT.clear();
    //~ }

    //~ /** The world changed (join, leave, dimension change). */
    //~ public static void levelChanged() {
        //~ DIRTY_LIGHT.clear();
        //~ lightTicks = 0;
        //~ FarTerrain.levelChanged();
        //~ WorldCache.levelChanged();
        //~ OceanFloor.levelChanged();
    //~ }

    //~ /** After Apply: a leaves mode change needs the chunks rebuilt with the new render type. */
    //~ public static void configured(final Minecraft mc) {
        //~ final int leaves = Tuning.leaves;
        //~ final int mesh = (Tuning.fastGrass ? 1 : 0) | (Tuning.endOptimizations ? 2 : 0);
        //~ final boolean changed = (lastLeaves != -1 && leaves != lastLeaves) || (lastMesh != -1 && mesh != lastMesh);
        //~ if (changed && mc.levelRenderer != null && mc.level != null) {
            //~ mc.levelRenderer.allChanged();
        //~ }
        //~ lastLeaves = leaves;
        //~ lastMesh = mesh;
    //~ }

    //~ /**
     //~ * Leaf culling (chunk build threads): the face of a leaf block toward {@code side} is hidden when
     //~ * the neighbour there is leaves too. With fancy leaves the block beyond must also be leaves (or
     //~ * opaque), so the outer two layers stay visible and trees keep their depth; with fast (solid)
     //~ * leaves every leaf-to-leaf face is invisible anyway.
     //~ */
    //~ public static boolean leafFaceHidden(final BlockGetter level, final BlockPos pos, final Direction side) {
        //~ return leafNeighbourHidden(level, pos.relative(side), side);
    //~ }

    //~ /** {@link #leafFaceHidden} given the neighbour position itself (1.21.5+ hands it over). */
    //~ public static boolean leafNeighbourHidden(final BlockGetter level, final BlockPos next, final Direction side) {
        //~ if (!(level.getBlockState(next).getBlock() instanceof LeavesBlock)) {
            //~ return false;
        //~ }
        //~ if (!Tuning.leavesFancy) {
            //~ return true;
        //~ }
        //~ final BlockState beyond = level.getBlockState(next.relative(side));
        //~ return beyond.getBlock() instanceof LeavesBlock || beyond.canOcclude();
    //~ }

    //~ /**
     //~ * Chorus culling (End optimizations, 1.12.0): whether the {@code side} face of a chorus plant
     //~ * stem is the end cap of an arm joined to the next chorus plant's arm, which is buried inside
     //~ * the joint. Chunk build threads.
     //~ */
    //~ public static boolean chorusFaceHidden(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           //~ final Direction side) {
        //~ return chorusNeighbourHidden(state, level, pos.relative(side), side);
    //~ }

    //~ /** {@link #chorusFaceHidden} given the neighbour position itself. */
    //~ public static boolean chorusNeighbourHidden(final BlockState state, final BlockGetter level, final BlockPos at,
                                                //~ final Direction side) {
        //~ if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
            //~ return false;
        //~ }
        //~ final BlockState next = level.getBlockState(at);
        //~ return next.getBlock() == Blocks.CHORUS_PLANT
                //~ && next.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()));
    //~ }

    //~ /** Squared distance from the camera to an entity (render thread). */
    //~ public static double distanceToCamera(final Entity entity) {
        //~ return Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);
    //~ }
    // @era:t-client-else v262
    //~ private static final long GC_SPACING_NANOS = 10_000_000_000L;
    //~ private static final LongOpenHashSet DIRTY_LIGHT = new LongOpenHashSet();
    //~ private static long lastGc;
    //~ private static boolean gcPending;
    //~ private static int lightTicks;
    //~ private static int lastLeaves = -1;
    //~ /** Mesh-affecting switches as bits (leaves excluded): fast grass, End chorus culling. */
    //~ private static int lastMesh = -1;

    //~ /**
     //~ * The frame-rate cap these options want right now (0 = none). Read on the client tick, so it
     //~ * follows focus changes and opened menus within 50 ms.
     //~ */
    //~ public static int frameCap(final Minecraft mc) {
        //~ if (!Tuning.on || AutoBench.ACTIVE) {
            //~ return 0;
        //~ }
        //~ if (Tuning.dynamicFps) {
            //~ // 26.3 replaced GLFW with SDL; the window tracks its own minimized state.
            //~ if (mc.getWindow().isIconified()) {
                //~ return MINIMIZED_FPS;
            //~ }
            //~ if (!mc.isWindowActive()) {
                //~ return BACKGROUND_FPS;
            //~ }
        //~ }
        //~ final Screen screen = ClientHooks.currentScreen(mc);
        //~ if (Tuning.menuFps > 0 && mc.level != null && screen != null && !(screen instanceof ChatScreen)
                //~ && !screen.getClass().getName().startsWith("com.aetherium.")) {
            //~ // Aetherium's own screen keeps full speed for its animations.
            //~ return Tuning.menuFps;
        //~ }
        //~ return 0;
    //~ }

    //~ /** Minecraft.setScreen: one garbage collection when a loading screen opens. */
    //~ public static void onScreen(final Minecraft mc, final Screen next) {
        //~ if (!Tuning.loadGc || next == null) {
            //~ return;
        //~ }
        //~ if (!loadingScreen(next)) {
            //~ return;
        //~ }
        //~ if (loadingScreen(ClientHooks.currentScreen(mc))) {
            //~ return; // already behind a loading screen
        //~ }
        //~ gcPending = true; // collected on the next tick, once the loading screen is on screen
    //~ }

    //~ private static boolean loadingScreen(final Screen screen) {
        //~ return screen instanceof LevelLoadingScreen
                //~ || screen instanceof ConnectScreen || screen instanceof ProgressScreen;
    //~ }

    //~ /**
     //~ * ClientChunkCache.onLightUpdate: remembers the section instead of rebuilding it now.
     //~ *
     //~ * @return true when the rebuild was deferred (vanilla must not run)
     //~ */
    //~ public static boolean deferLight(final SectionPos pos) {
        //~ if (!Tuning.lightUpdates) {
            //~ return false;
        //~ }
        //~ DIRTY_LIGHT.add(pos.asLong());
        //~ return true;
    //~ }

    //~ /** Minecraft.tick (also while no world is loaded): pending GC, merged light rebuilds every 2 ticks. */
    //~ public static void tick(final Minecraft mc) {
        //~ if (gcPending) {
            //~ gcPending = false;
            //~ final long now = System.nanoTime();
            //~ if (Tuning.loadGc && loadingScreen(ClientHooks.currentScreen(mc)) && (lastGc == 0L || now - lastGc >= GC_SPACING_NANOS)) {
                //~ lastGc = now;
                //~ System.gc();
            //~ }
        //~ }
        //~ if (DIRTY_LIGHT.isEmpty()) {
            //~ lightTicks = 0;
            //~ return;
        //~ }
        //~ if (++lightTicks < 2 && Tuning.lightUpdates) {
            //~ return;
        //~ }
        //~ lightTicks = 0;
        //~ flushLight(mc);
    //~ }

    //~ private static void flushLight(final Minecraft mc) {
        //~ if (mc.levelRenderer != null && mc.level != null) {
            //~ final LongIterator it = DIRTY_LIGHT.iterator();
            //~ while (it.hasNext()) {
                //~ final long key = it.nextLong();
                //~ mc.level.setSectionRangeDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key),
                        //~ SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
            //~ }
        //~ }
        //~ DIRTY_LIGHT.clear();
    //~ }

    //~ /** The world changed (join, leave, dimension change). */
    //~ public static void levelChanged() {
        //~ DIRTY_LIGHT.clear();
        //~ lightTicks = 0;
        //~ FarTerrain.levelChanged();
        //~ WorldCache.levelChanged();
        //~ OceanFloor.levelChanged();
    //~ }

    //~ /** After Apply: a leaves mode change needs the chunks rebuilt with the new render type. */
    //~ public static void configured(final Minecraft mc) {
        //~ final int leaves = Tuning.leaves;
        //~ final int mesh = (Tuning.fastGrass ? 1 : 0) | (Tuning.endOptimizations ? 2 : 0);
        //~ final boolean changed = (lastLeaves != -1 && leaves != lastLeaves) || (lastMesh != -1 && mesh != lastMesh);
        //~ if (changed && mc.levelRenderer != null && mc.level != null) {
            //~ VanillaOptions.reloadChunks();
        //~ }
        //~ lastLeaves = leaves;
        //~ lastMesh = mesh;
    //~ }

    //~ /**
     //~ * Leaf culling (chunk build threads): the face of a leaf block toward {@code side} is hidden when
     //~ * the neighbour there is leaves too. With fancy leaves the block beyond must also be leaves (or
     //~ * opaque), so the outer two layers stay visible and trees keep their depth; with fast (solid)
     //~ * leaves every leaf-to-leaf face is invisible anyway.
     //~ */
    //~ public static boolean leafFaceHidden(final BlockGetter level, final BlockPos pos, final Direction side) {
        //~ return leafNeighbourHidden(level, pos.relative(side), side);
    //~ }

    //~ /** {@link #leafFaceHidden} given the neighbour position itself (1.21.5+ hands it over). */
    //~ public static boolean leafNeighbourHidden(final BlockGetter level, final BlockPos next, final Direction side) {
        //~ if (!(level.getBlockState(next).getBlock() instanceof LeavesBlock)) {
            //~ return false;
        //~ }
        //~ if (!Tuning.leavesFancy) {
            //~ return true;
        //~ }
        //~ final BlockState beyond = level.getBlockState(next.relative(side));
        //~ return beyond.getBlock() instanceof LeavesBlock || beyond.canOcclude();
    //~ }

    //~ /**
     //~ * Chorus culling (End optimizations, 1.12.0): whether the {@code side} face of a chorus plant
     //~ * stem is the end cap of an arm joined to the next chorus plant's arm, which is buried inside
     //~ * the joint. Chunk build threads.
     //~ */
    //~ public static boolean chorusFaceHidden(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           //~ final Direction side) {
        //~ return chorusNeighbourHidden(state, level, pos.relative(side), side);
    //~ }

    //~ /** {@link #chorusFaceHidden} given the neighbour position itself. */
    //~ public static boolean chorusNeighbourHidden(final BlockState state, final BlockGetter level, final BlockPos at,
                                                //~ final Direction side) {
        //~ if (!state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
            //~ return false;
        //~ }
        //~ final BlockState next = level.getBlockState(at);
        //~ return next.getBlock() == Blocks.CHORUS_PLANT
                //~ && next.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite()));
    //~ }

    //~ /** Squared distance from the camera to an entity (render thread). */
    //~ public static double distanceToCamera(final Entity entity) {
        //~ return Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);
    //~ }
    // @era:t-client-end
}
