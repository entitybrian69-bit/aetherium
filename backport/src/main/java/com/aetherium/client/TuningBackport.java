package com.aetherium.client;

import com.aetherium.perf.Tuning;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.Display;

/**
 * The 1.12.2 side of the shared tuning options ({@link Tuning}): frame caps for a background or
 * minimized window and open menus, the leaves mode and leaf culling, and the per-tick explosion
 * cloud budget. The mixins only call in here.
 */
public final class TuningBackport {
    public static final int BACKGROUND_FPS = 15;
    public static final int MINIMIZED_FPS = 1;

    private static int explosionsThisTick;
    private static int lastLeaves = -1;
    private static boolean lastLeafCulling;

    private TuningBackport() {
    }

    /** Dynamic FPS and the menu frame rate (0 = no cap). */
    public static int frameCap(final Minecraft mc) {
        if (!Tuning.on) {
            return 0;
        }
        if (Tuning.dynamicFps) {
            if (!Display.isVisible()) {
                return MINIMIZED_FPS;
            }
            if (!Display.isActive()) {
                return BACKGROUND_FPS;
            }
        }
        final GuiScreen screen = mc.currentScreen;
        if (Tuning.menuFps > 0 && mc.world != null && screen != null && !(screen instanceof GuiChat)
                && !screen.getClass().getName().startsWith("com.aetherium.")) {
            // Aetherium's own screen keeps full speed for its animations.
            return Tuning.menuFps;
        }
        return 0;
    }

    /** Start of every client tick. */
    public static void tick() {
        explosionsThisTick = 0;
    }

    /** A huge explosion emitter wants to spawn its clouds this tick: false = over the budget. */
    public static boolean allowExplosion() {
        if (Tuning.explosionEmitters >= Tuning.EXPLOSIONS_UNLIMITED) {
            return true;
        }
        if (explosionsThisTick >= Tuning.explosionEmitters) {
            return false;
        }
        explosionsThisTick++;
        return true;
    }

    /** After Apply: rebuild the chunks when the leaves mode or leaf culling changed. */
    public static void configured(final Minecraft mc) {
        final int leaves = Tuning.leaves;
        final boolean culling = Tuning.leafCulling;
        final boolean changed = lastLeaves != -1 && (leaves != lastLeaves || culling != lastLeafCulling);
        lastLeaves = leaves;
        lastLeafCulling = culling;
        if (changed && mc.renderGlobal != null && mc.world != null) {
            mc.renderGlobal.loadRenderers(); // applies the leaves mode (see TuningLeavesMixin)
        } else {
            applyLeaves(mc);
        }
    }

    /**
     * RenderGlobal.loadRenderers sets the leaves' graphics level from the graphics option; the
     * leaves option overrides it (Fast or Fancy whatever the graphics). Also keeps
     * {@link Tuning#leavesFancy} in step for leaf culling.
     */
    public static void applyLeaves(final Minecraft mc) {
        final int leaves = Tuning.leaves;
        final boolean fancy = leaves == 0 ? mc.gameSettings.fancyGraphics : leaves == 2;
        Tuning.leavesFancy = fancy;
        if (leaves != 0) {
            Blocks.LEAVES.setGraphicsLevel(fancy);
            Blocks.LEAVES2.setGraphicsLevel(fancy);
        }
    }

    /**
     * Leaf culling: a leaf face against another leaf block is hidden; with fancy leaves only when
     * the block behind that one is leaves or solid too, so tree edges keep their fancy look.
     * Chunk build threads.
     */
    public static boolean leafFaceHidden(final IBlockAccess world, final BlockPos pos, final EnumFacing side) {
        final BlockPos next = pos.offset(side);
        if (!(world.getBlockState(next).getBlock() instanceof BlockLeaves)) {
            return false;
        }
        if (!Tuning.leavesFancy) {
            return true;
        }
        final IBlockState beyond = world.getBlockState(next.offset(side));
        return beyond.getBlock() instanceof BlockLeaves || beyond.isOpaqueCube();
    }
}
