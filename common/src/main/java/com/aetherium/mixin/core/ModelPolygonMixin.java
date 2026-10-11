package com.aetherium.mixin.core;

import com.aetherium.perf.ModelPolygonData;

import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.mojang.math.Vector3f;
//~ import org.spongepowered.asm.mixin.Final;
//~ import org.spongepowered.asm.mixin.Shadow;
// @era:qol16-end

/** A model face's normal for the fast model path (1.16.5). Applied only where {@code Capabilities.STREAMING} is true. */
@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Polygon")
public abstract class ModelPolygonMixin implements ModelPolygonData {
    // @era:qol16-begin none
    @Override
    public float aetheriumNormalX() {
        return 0.0f;
    }

    @Override
    public float aetheriumNormalY() {
        return 0.0f;
    }

    @Override
    public float aetheriumNormalZ() {
        return 0.0f;
    }
    // @era:qol16-else v16
    //~ @Shadow
    //~ @Final
    //~ public Vector3f normal;

    //~ @Override
    //~ public float aetheriumNormalX() {
        //~ return this.normal.x();
    //~ }

    //~ @Override
    //~ public float aetheriumNormalY() {
        //~ return this.normal.y();
    //~ }

    //~ @Override
    //~ public float aetheriumNormalZ() {
        //~ return this.normal.z();
    //~ }
    // @era:qol16-end
}
