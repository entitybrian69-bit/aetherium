package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:anim-begin on
import com.aetherium.mixin.AetheriumMixinPlugin;
import com.aetherium.perf.SectionSprites;
import com.aetherium.perf.SpriteActivity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:section-api-begin srd
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
// @era:section-api-else crd
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
// @era:section-api-else v16|srd-ro|mesh|none
// @era:section-api-end
// @era:anim-else v16|none
// @era:anim-end

/**
 * Visible-only animations (1.17..1.21.4; 1.16.5 does this in StreamingLevelMixin): once per atlas
 * tick, the animated sprites of every section in last frame's visible list count as seen. Applied
 * only where {@code Capabilities.VISIBLE_ANIMATION_PASS} is true.
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class VisibleSpritesMixin {
    // @era:anim-begin on
    // @era:fog-cull-begin section
    @Shadow
    @Final
    private ObjectArrayList<Object> visibleSections;
    // @era:fog-cull-else bfs
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<Object> renderChunks;
    // @era:fog-cull-else info
    //~ @Shadow
    //~ @Final
    //~ private ObjectArrayList<Object> renderChunksInFrustum;
    // @era:fog-cull-else none
    // @era:fog-cull-end

    @Unique
    private static boolean aetherium$spriteHooksChecked;

    @Inject(method = "renderLevel", at = @At("HEAD"), require = 0)
    private void aetherium$visibleSprites(final CallbackInfo ci) {
        SpriteActivity.frame();
        if (!SpriteActivity.enabled() || !SpriteActivity.seenPassDue()) {
            return;
        }
        // @era:fog-cull-begin section
        final List<Object> list = aetherium$list(this.visibleSections);
        // @era:fog-cull-else bfs
        //~ final List<Object> list = aetherium$list(this.renderChunks);
        // @era:fog-cull-else info
        //~ final List<Object> list = aetherium$list(this.renderChunksInFrustum);
        // @era:fog-cull-else none
        //~ final List<Object> list = java.util.Collections.emptyList();
        // @era:fog-cull-end
        final int count = list.size();
        if (count == 0) {
            return;
        }
        if (!aetherium$spriteHooksChecked) {
            // Sections exist, so their mesh hooks had their chance to apply: without them visible
            // textures would pause, so everything keeps animating instead.
            aetherium$spriteHooksChecked = true;
            if (!AetheriumMixinPlugin.isApplied("SectionSpritesMixin")
                    || !AetheriumMixinPlugin.isApplied("CompiledChunkSpritesMixin")
                    || !AetheriumMixinPlugin.isApplied("BlockSpriteUseMixin")) {
                SpriteActivity.markBroken();
                return;
            }
        }
        for (int i = 0; i < count; i++) {
            final Object item = list.get(i);
            // @era:section-api-begin srd
            final Object compiled = ((SectionRenderDispatcher.RenderSection) item).getCompiled();
            // @era:section-api-else crd
            //~ if (!(item instanceof RenderChunkInfoAccessor)) {
                //~ SpriteActivity.markBroken();
                //~ return;
            //~ }
            //~ final ChunkRenderDispatcher.RenderChunk chunk = ((RenderChunkInfoAccessor) item).aetheriumChunk();
            //~ final Object compiled = chunk.getCompiledChunk();
            // @era:section-api-else v16|srd-ro|mesh|none
            //~ final Object compiled = null;
            // @era:section-api-end
            if (!(compiled instanceof SectionSprites)) {
                SpriteActivity.markBroken();
                return;
            }
            SpriteActivity.seen(((SectionSprites) compiled).aetheriumSprites());
        }
    }

    @SuppressWarnings("unchecked")
    @Unique
    private static List<Object> aetherium$list(final Object vanilla) {
        return (List<Object>) vanilla;
    }
    // @era:anim-else v16|none
    // @era:anim-end
}
