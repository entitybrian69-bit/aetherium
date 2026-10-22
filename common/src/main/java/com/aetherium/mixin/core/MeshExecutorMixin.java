package com.aetherium.mixin.core;

import com.aetherium.client.QolHooks;
import java.util.concurrent.Executor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
// @era:mesh-exec-begin crd16|crd|srd
// @era:mesh-exec-else tracing
//~ import java.util.concurrent.ExecutorService;
//~ import net.minecraft.TracingExecutor;
// @era:mesh-exec-end

/**
 * Mesh threads: the chunk mesh dispatcher LevelRenderer creates (in {@code allChanged}, or in
 * {@code invalidateCompiledGeometry} from 26.2) builds on Aetherium's own worker pool instead of
 * the background pool it shares with world generation, resource loading and saving, so a busy
 * integrated server no longer starves chunk meshing. The dispatcher is matched by owner and name
 * and its executor argument by type (1.16.5 spells out the constructor, which its release jar needs):
 * <ul>
 *   <li>up to 1.20.1 {@code ChunkRenderDispatcher}, 1.20.2+ {@code SectionRenderDispatcher};</li>
 *   <li>from 1.21.2 the executor is a {@code TracingExecutor} wrapping an executor service.</li>
 * </ul>
 * Applied only where {@code Capabilities.MESH_THREADS} is true; the setting only takes effect
 * when the world reloads (as in 1.16.5).
 */
@Mixin(targets = "net.minecraft.client.renderer.LevelRenderer")
public abstract class MeshExecutorMixin {
    // @era:mesh-exec-begin srd
    @ModifyArg(method = "allChanged", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;<init>"), require = 0)
    private Executor aetherium$meshExecutor(final Executor vanilla) {
        return QolHooks.meshExecutor(vanilla);
    }
    // @era:mesh-exec-else crd16
    //~ // Release jars carry no refmap: the call is only renamed for them with its full descriptor.
    //~ @ModifyArg(method = "allChanged", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/renderer/LevelRenderer;Ljava/util/concurrent/Executor;ZLnet/minecraft/client/renderer/ChunkBufferBuilderPack;)V"), require = 0)
    //~ private Executor aetherium$meshExecutor(final Executor vanilla) {
        //~ return QolHooks.meshExecutor(vanilla);
    //~ }
    // @era:mesh-exec-else crd
    //~ @ModifyArg(method = "allChanged", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;<init>"), require = 0)
    //~ private Executor aetherium$meshExecutor(final Executor vanilla) {
        //~ return QolHooks.meshExecutor(vanilla);
    //~ }
    // @era:mesh-exec-else tracing
    //~ @ModifyArg(method = {"allChanged", "invalidateCompiledGeometry"}, at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;<init>"), require = 0)
    //~ private TracingExecutor aetherium$meshExecutor(final TracingExecutor vanilla) {
        //~ final Executor own = QolHooks.meshExecutor(null);
        //~ return own instanceof ExecutorService ? new TracingExecutor((ExecutorService) own) : vanilla;
    //~ }
    // @era:mesh-exec-end
}
