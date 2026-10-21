package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.aetherium.client.ChestBaking;
//~ import com.aetherium.client.ChestLid;
//~ import com.aetherium.client.ChestQuads;
//~ import com.aetherium.core.AetheriumLog;
//~ import com.aetherium.perf.Tuning;
//~ import com.mojang.blaze3d.vertex.BufferBuilder;
//~ import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.math.Vector3f;
//~ import java.util.Set;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import net.minecraft.client.renderer.ChunkBufferBuilderPack;
//~ import net.minecraft.client.renderer.LevelRenderer;
//~ import net.minecraft.client.renderer.LightTexture;
//~ import net.minecraft.client.renderer.RenderType;
//~ import net.minecraft.client.renderer.Sheets;
//~ import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
//~ import net.minecraft.client.renderer.blockentity.ChestRenderer;
//~ import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
//~ import net.minecraft.client.renderer.chunk.RenderChunkRegion;
//~ import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
//~ import net.minecraft.client.renderer.texture.TextureAtlas;
//~ import net.minecraft.client.renderer.texture.TextureAtlasSprite;
//~ import net.minecraft.client.resources.model.Material;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.core.Direction;
//~ import net.minecraft.world.level.block.AbstractChestBlock;
//~ import net.minecraft.world.level.block.ChestBlock;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import net.minecraft.world.level.block.entity.ChestBlockEntity;
//~ import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
//~ import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
//~ import net.minecraft.world.level.block.state.BlockState;
//~ import net.minecraft.world.level.block.state.properties.ChestType;
//~ import net.minecraft.world.level.chunk.LevelChunk;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): while a chunk section is meshed, a closed, untouched
 * chest, trapped chest or ender chest is written into the section's cutout terrain (its own
 * model, the chest texture from the block atlas, the block entity light and shading) instead of
 * being listed for drawing on its own every frame. Everything else, and every chest that is
 * open, opening, closing, being broken, uses another mod's renderer or whose texture is missing,
 * goes through vanilla's handling unchanged. See {@code ChestBaking}.
 *
 * <p>Runs on the mesh workers: each worker has its own chest renderer (the model's lid angle is
 * per-instance state) and reads the world only through the section's snapshot. Mob detail levels
 * ({@code ModelLod}) may skip small parts while the render thread draws a mob; a bake that did not
 * get every face is dropped, so a chest never misses its lock.</p>
 */
@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public abstract class TuningChestBakeMixin {
// @era:boost16-begin none
// @era:boost16-else v16
    //~ @Unique
    //~ private ChunkBufferBuilderPack aetherium$pack;

    //~ @Unique
    //~ private ChunkRenderDispatcher.CompiledChunk aetherium$compiled;

    //~ @Inject(method = "compile", at = @At("HEAD"), require = 0)
    //~ private void aetherium$compileStart(final float x, final float y, final float z,
                                        //~ final ChunkRenderDispatcher.CompiledChunk compiled,
                                        //~ final ChunkBufferBuilderPack pack,
                                        //~ final CallbackInfoReturnable<Set<BlockEntity>> cir) {
        //~ this.aetherium$pack = pack;
        //~ this.aetherium$compiled = compiled;
    //~ }

    //~ /**
     //~ * The block entity lookup of the meshing loop: a chest that was baked into the section is
     //~ * reported as absent, so vanilla does not also list it for drawing on its own.
     //~ */
    //~ @Redirect(method = "compile", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),
            //~ require = 0)
    //~ private BlockEntity aetherium$bakeChest(final RenderChunkRegion world, final BlockPos pos,
                                            //~ final LevelChunk.EntityCreationType creation) {
        //~ final BlockEntity entity = world.getBlockEntity(pos, creation);
        //~ if (entity != null && Tuning.batchedChests && ChestBaking.enabled() && this.aetherium$pack != null
                //~ && this.aetherium$compiled != null && aetherium$candidate(entity)) {
            //~ try {
                //~ if (this.aetherium$bake(world, this.aetherium$compiled, entity)) {
                    //~ return null;
                //~ }
            //~ } catch (final RuntimeException | LinkageError error) {
                //~ ChestBaking.fail();
                //~ AetheriumLog.of(ChestBaking.class).warn("Batched chests turned off: " + error);
            //~ }
        //~ }
        //~ return entity;
    //~ }

    //~ @Unique
    //~ private static boolean aetherium$candidate(final BlockEntity entity) {
        //~ final Class<?> type = entity.getClass();
        //~ return type == ChestBlockEntity.class || type == TrappedChestBlockEntity.class
                //~ || type == EnderChestBlockEntity.class;
    //~ }

    //~ @Unique
    //~ private static boolean aetherium$closed(final BlockEntity entity) {
        //~ if (!(entity instanceof ChestLid) || ChestBaking.destroying(entity.getBlockPos().asLong())) {
            //~ return false;
        //~ }
        //~ final ChestLid lid = (ChestLid) entity;
        //~ return ChestBaking.closed(lid.aetherium$openness(), lid.aetherium$previousOpenness(), lid.aetherium$openCount());
    //~ }

    //~ @Unique
    //~ private boolean aetherium$bake(final RenderChunkRegion world, final ChunkRenderDispatcher.CompiledChunk compiled,
                                   //~ final BlockEntity entity) {
        //~ if (BlockEntityRenderDispatcher.instance.getRenderer(entity) == null
                //~ || BlockEntityRenderDispatcher.instance.getRenderer(entity).getClass() != ChestRenderer.class
                //~ || !aetherium$closed(entity)) {
            //~ return false;
        //~ }
        //~ final BlockPos pos = entity.getBlockPos();
        //~ final BlockState state = world.getBlockState(pos);
        //~ if (!(state.getBlock() instanceof AbstractChestBlock) || !state.hasProperty(ChestBlock.FACING)) {
            //~ return false;
        //~ }
        //~ final Direction facing = state.getValue(ChestBlock.FACING);
        //~ final ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        //~ int light = LevelRenderer.getLightColor(world, pos);
        //~ if (type != ChestType.SINGLE) {
            //~ final BlockPos otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
            //~ final BlockState other = world.getBlockState(otherPos);
            //~ if (other.getBlock() != state.getBlock() || !other.hasProperty(ChestBlock.TYPE)
                    //~ || other.getValue(ChestBlock.TYPE) != type.getOpposite()
                    //~ || other.getValue(ChestBlock.FACING) != facing) {
                //~ return false;
            //~ }
            //~ final BlockEntity otherEntity = world.getBlockEntity(otherPos, LevelChunk.EntityCreationType.CHECK);
            //~ if (otherEntity == null || otherEntity.getClass() != entity.getClass() || !aetherium$closed(otherEntity)) {
                //~ return false;
            //~ }
            //~ final int otherLight = LevelRenderer.getLightColor(world, otherPos);
            //~ light = LightTexture.pack(Math.max(LightTexture.block(light), LightTexture.block(otherLight)),
                    //~ Math.max(LightTexture.sky(light), LightTexture.sky(otherLight)));
        //~ }
        //~ Object model = ChestBaking.model();
        //~ if (!(model instanceof TuningChestPartsAccessor)) {
            //~ model = new ChestRenderer<ChestBlockEntity>(BlockEntityRenderDispatcher.instance);
            //~ ChestBaking.setModel(model);
        //~ }
        //~ final TuningChestPartsAccessor parts = (TuningChestPartsAccessor) model;
        //~ final Material material = Sheets.chooseMaterial(entity, type, parts.aetherium$xmasTextures());
        //~ final TextureAtlasSprite sprite =
                //~ Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(material.texture());
        //~ if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.getName())) {
            //~ return false;
        //~ }
        //~ final ModelPart lid;
        //~ final ModelPart lock;
        //~ final ModelPart bottom;
        //~ if (type == ChestType.LEFT) {
            //~ lid = parts.aetherium$doubleRightLid();
            //~ lock = parts.aetherium$doubleRightLock();
            //~ bottom = parts.aetherium$doubleRightBottom();
        //~ } else if (type == ChestType.RIGHT) {
            //~ lid = parts.aetherium$doubleLeftLid();
            //~ lock = parts.aetherium$doubleLeftLock();
            //~ bottom = parts.aetherium$doubleLeftBottom();
        //~ } else {
            //~ lid = parts.aetherium$lid();
            //~ lock = parts.aetherium$lock();
            //~ bottom = parts.aetherium$bottom();
        //~ }
        //~ final PoseStack pose = new PoseStack();
        //~ pose.translate((pos.getX() & 15) + 0.5, (pos.getY() & 15) + 0.5, (pos.getZ() & 15) + 0.5);
        //~ pose.mulPose(Vector3f.YP.rotationDegrees(-facing.toYRot()));
        //~ pose.translate(-0.5, -0.5, -0.5);
        //~ final ChestQuads quads = new ChestQuads(sprite);
        //~ parts.aetherium$renderParts(pose, quads, lid, lock, bottom, 0f, light, 655360);
        //~ if (!quads.complete(72)) { // three boxes of six quads
            //~ return false;
        //~ }
        //~ final RenderType layer = RenderType.cutout();
        //~ final BufferBuilder builder = this.aetherium$pack.builder(layer);
        //~ final TuningCompiledAccessor sections = (TuningCompiledAccessor) (Object) compiled;
        //~ if (sections.aetherium$hasLayer().add(layer)) {
            //~ builder.begin(7, DefaultVertexFormat.BLOCK); // GL_QUADS, as RenderChunk.beginLayer
        //~ }
        //~ quads.flush(builder);
        //~ sections.aetherium$hasBlocks().add(layer);
        //~ sections.aetherium$setCompletelyEmpty(false);
        //~ return true;
    //~ }
// @era:boost16-end
}
