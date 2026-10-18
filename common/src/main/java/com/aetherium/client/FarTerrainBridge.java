package com.aetherium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
// @era:t-far-color-begin map
import net.minecraft.world.level.material.MapColor;
// @era:t-far-color-else material
//~ import net.minecraft.world.level.material.MaterialColor;
// @era:t-far-color-end
// @era:t-far-sections-begin section|chunk|visible|visible-t|none
// @era:t-far-sections-else acc17
//~ import com.aetherium.mixin.core.TuningFarAreaAccessor;
//~ import com.aetherium.mixin.core.TuningFarViewAccessor;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
// @era:t-far-sections-end
// @era:t-far-gl-begin id17
import com.mojang.blaze3d.pipeline.RenderTarget;
// @era:t-far-gl-else gl215|gl262
//~ import com.mojang.blaze3d.opengl.GlTexture;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import com.mojang.blaze3d.textures.GpuTexture;
// @era:t-far-gl-else none|pearl
// @era:t-far-gl-end

/**
 * The version-specific pieces of far terrain on 1.17+ ({@link FarTerrain}): the map colour of a
 * block (MaterialColor until 1.19.4, MapColor from 1.20), whether vanilla has built the section at
 * a position yet, and the OpenGL names of the main colour and depth textures.
 */
final class FarTerrainBridge {
    private FarTerrainBridge() {
    }

    /** Map colour of the block (0xRRGGBB), 0 for none. */
    static int mapColor(final BlockState state, final LevelChunk chunk, final BlockPos pos) {
        final BlockGetter getter = (BlockGetter) (Object) chunk;
        // @era:t-far-color-begin map
        final MapColor color = state.getMapColor(getter, pos);
        // @era:t-far-color-else material
        //~ final MaterialColor color = state.getMapColor(getter, pos);
        // @era:t-far-color-end
        return color == null ? 0 : color.col;
    }

    /**
     * Whether vanilla draws the section at {@code pos} (in chunk {@code chunkX, chunkZ}) yet; true
     * where that cannot be told, so nothing is drawn over real terrain.
     */
    static boolean compiled(final Minecraft mc, final BlockPos pos, final int chunkX, final int chunkZ) {
        if (mc.levelRenderer == null) {
            return true;
        }
        // @era:t-far-sections-begin section
        return mc.levelRenderer.isSectionCompiled(pos);
        // @era:t-far-sections-else chunk
        //~ return mc.levelRenderer.isChunkCompiled(pos);
        // @era:t-far-sections-else visible
        //~ return mc.levelRenderer.isSectionCompiledAndVisible(pos);
        // @era:t-far-sections-else visible-t
        //~ return mc.levelRenderer.isSectionCompiledAndVisible(pos, net.minecraft.util.Util.getMillis());
        // @era:t-far-sections-else acc17
        //~ final Object renderer = mc.levelRenderer;
        //~ if (!(renderer instanceof TuningFarAreaAccessor)) {
            //~ return true;
        //~ }
        //~ final Object area = ((TuningFarAreaAccessor) renderer).aetherium$farViewArea();
        //~ if (!(area instanceof TuningFarViewAccessor)) {
            //~ return true;
        //~ }
        //~ final ChunkRenderDispatcher.RenderChunk section = ((TuningFarViewAccessor) area).aetherium$farRenderChunkAt(pos);
        //~ // The view area wraps positions around (modulo): only a section of this very chunk counts.
        //~ final BlockPos origin = section == null ? null : section.getOrigin();
        //~ return origin != null && origin.getX() >> 4 == chunkX && origin.getZ() >> 4 == chunkZ
                //~ && section.getCompiledChunk() != ChunkRenderDispatcher.CompiledChunk.UNCOMPILED;
        // @era:t-far-sections-else none
        //~ return true;
        // @era:t-far-sections-end
    }

    // @era:t-far-gl-begin id17
    private static RenderTarget main(final Minecraft mc) {
        return mc.getMainRenderTarget();
    }
    // @era:t-far-gl-else gl215
    //~ private static RenderTarget main(final Minecraft mc) {
        //~ return mc.getMainRenderTarget();
    //~ }
    // @era:t-far-gl-else gl262
    //~ /** 26.2+: the main target belongs to the GameRenderer. */
    //~ private static RenderTarget main(final Minecraft mc) {
        //~ return mc.gameRenderer.mainRenderTarget();
    //~ }
    // @era:t-far-gl-else none|pearl
    // @era:t-far-gl-end

    // @era:t-far-gl-begin id17
    /** OpenGL name of the main colour texture, -1 where it is not an OpenGL texture. */
    static int colorTexture(final Minecraft mc) {
        return main(mc).getColorTextureId();
    }

    static int depthTexture(final Minecraft mc) {
        return main(mc).getDepthTextureId();
    }
    // @era:t-far-gl-else gl215|gl262
    //~ private static int glId(final GpuTexture texture) {
        //~ return texture instanceof GlTexture ? ((GlTexture) texture).glId() : -1;
    //~ }

    //~ /** OpenGL name of the main colour texture, -1 where it is not an OpenGL texture (Vulkan). */
    //~ static int colorTexture(final Minecraft mc) {
        //~ return glId(main(mc).getColorTexture());
    //~ }

    //~ static int depthTexture(final Minecraft mc) {
        //~ return glId(main(mc).getDepthTexture());
    //~ }
    // @era:t-far-gl-else none|pearl
    //~ static int colorTexture(final Minecraft mc) {
        //~ return -1;
    //~ }

    //~ static int depthTexture(final Minecraft mc) {
        //~ return -1;
    //~ }
    // @era:t-far-gl-end

    // @era:t-far-gl-begin id17|gl215|gl262
    static int width(final Minecraft mc) {
        return main(mc).width;
    }

    static int height(final Minecraft mc) {
        return main(mc).height;
    }
    // @era:t-far-gl-else none|pearl
    //~ static int width(final Minecraft mc) {
        //~ return 0;
    //~ }

    //~ static int height(final Minecraft mc) {
        //~ return 0;
    //~ }
    // @era:t-far-gl-end
}
