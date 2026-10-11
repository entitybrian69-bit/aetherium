package com.aetherium.perf;

/** A model vertex as {@code ModelPart.compile} uses it (mixed into {@code ModelPart.Vertex}). */
public interface ModelVertexData {

    /** Position already divided by 16, as vanilla divides it. */
    float aetheriumX();

    float aetheriumY();

    float aetheriumZ();

    float aetheriumU();

    float aetheriumV();
}
