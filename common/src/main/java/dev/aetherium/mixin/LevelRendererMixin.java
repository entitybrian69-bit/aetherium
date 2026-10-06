package dev.aetherium.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aetherium.chunk.AetheriumChunkBuilder;
import dev.aetherium.client.render.DynamicLightClient;
import dev.aetherium.client.render.EntityCuller;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.AetheriumRenderEngine;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.Executor;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private ClientLevel level;

    /** Hand our distance-prioritised executor to SectionRenderDispatcher instead of Util.backgroundExecutor(). */
    @ModifyArg(method = "allChanged",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/client/renderer/LevelRenderer;Ljava/util/concurrent/Executor;Lnet/minecraft/client/renderer/RenderBuffers;Lnet/minecraft/client/renderer/block/BlockRenderDispatcher;Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;)V"),
            index = 2)
    private Executor aetherium$chunkExecutor(Executor vanilla) {
        AetheriumChunkBuilder builder = AetheriumChunkBuilder.get();
        builder.applyConfig(AetheriumConfig.get());
        return AetheriumRenderEngine.get().isAetheriumActive() || AetheriumConfig.get().performance.asyncChunkMeshing ? builder : vanilla;
    }

    /** Per-frame: camera for chunk priority, dynamic-light source update, view-projection for the HZB pass. */
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void aetherium$onRenderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer,
                                         LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        AetheriumChunkBuilder.get().setCameraPosition(pos.x, pos.y, pos.z);
        if (level != null) DynamicLightClient.tick(level, pos.x, pos.y, pos.z);
        Matrix4f vp = new Matrix4f(projectionMatrix).mul(frustumMatrix);
        float[] m = new float[16];
        vp.get(m);
        AetheriumRenderEngine.get().setViewProjection(m);
    }

    /** After the solid terrain layers have written depth, build the HZB and cull everything that follows. */
    @Inject(method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void aetherium$afterOpaqueTerrain(CallbackInfo ci) {
        AetheriumRenderEngine.get().occlusionPass(minecraft.getMainRenderTarget().getDepthTextureId());
    }

    /** Entity culling (distance + raycast occlusion tiers). */
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void aetherium$cullEntity(Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack,
                                      MultiBufferSource buffer, CallbackInfo ci) {
        if (AetheriumRenderEngine.get().isAetheriumActive() && EntityCuller.shouldSkip(entity, camX, camY, camZ)) ci.cancel();
    }

    /** Dynamic lights: inject into the packed light used by block/fluid/entity rendering. */
    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"), cancellable = true)
    private static void aetherium$dynamicLight(BlockAndTintGetter level, BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int packed = cir.getReturnValueI();
        int combined = DynamicLightClient.combinePackedLight(packed, pos);
        if (combined != packed) cir.setReturnValue(combined);
    }

    /** Fog occlusion: skip sections entirely beyond the fog end when enabled. */
    @Inject(method = "setupRender", at = @At("HEAD"))
    private void aetherium$setupRender(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator, CallbackInfo ci) {
        AetheriumChunkBuilder.get().drainCompleted();
    }
}
