package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:boost16-begin none
// @era:boost16-else v16
//~ import com.mojang.blaze3d.vertex.PoseStack;
//~ import com.mojang.blaze3d.vertex.VertexConsumer;
//~ import net.minecraft.client.model.geom.ModelPart;
//~ import org.spongepowered.asm.mixin.gen.Accessor;
//~ import org.spongepowered.asm.mixin.gen.Invoker;
// @era:boost16-end

/**
 * Batched chests (1.17.2, Minecraft 1.16.5): the chest model parts of a (per mesh worker) chest
 * renderer and its private part drawing method, so a closed chest can be meshed into its section.
 */
@Mixin(targets = "net.minecraft.client.renderer.blockentity.ChestRenderer")
public interface TuningChestPartsAccessor {
// @era:boost16-begin none
// @era:boost16-else v16
    //~ @Accessor("lid")
    //~ ModelPart aetherium$lid();

    //~ @Accessor("bottom")
    //~ ModelPart aetherium$bottom();

    //~ @Accessor("lock")
    //~ ModelPart aetherium$lock();

    //~ @Accessor("doubleLeftLid")
    //~ ModelPart aetherium$doubleLeftLid();

    //~ @Accessor("doubleLeftBottom")
    //~ ModelPart aetherium$doubleLeftBottom();

    //~ @Accessor("doubleLeftLock")
    //~ ModelPart aetherium$doubleLeftLock();

    //~ @Accessor("doubleRightLid")
    //~ ModelPart aetherium$doubleRightLid();

    //~ @Accessor("doubleRightBottom")
    //~ ModelPart aetherium$doubleRightBottom();

    //~ @Accessor("doubleRightLock")
    //~ ModelPart aetherium$doubleRightLock();

    //~ @Accessor("xmasTextures")
    //~ boolean aetherium$xmasTextures();

    //~ @Invoker("render")
    //~ void aetherium$renderParts(PoseStack poseStack, VertexConsumer consumer, ModelPart lid, ModelPart lock,
                               //~ ModelPart bottom, float openness, int light, int overlay);
// @era:boost16-end
}
