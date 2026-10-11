package com.aetherium.perf;

/**
 * A model cube's faces packed for {@link ModelVertexWriter} (mixed into {@code ModelPart.Cube}):
 * per face 4 x (x / 16, y / 16, z / 16, u, v) followed by the face normal, {@link #FACE_FLOATS}
 * floats per face, in vanilla's face order. Null when the cube cannot be packed.
 */
public interface ModelCubeData {

    int FACE_FLOATS = 4 * 5 + 3;

    float[] aetheriumFaces();
}
