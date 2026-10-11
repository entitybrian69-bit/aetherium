package com.aetherium.perf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Packs a model cube's faces once for the fast model path ({@link ModelCubeData}).
 *
 * <p>The face and vertex classes are not public in 1.16.5, so the two arrays are located by type
 * (the one object-array instance field of each class), never by name, which keeps this independent
 * of mappings; the values come from the {@link ModelPolygonData} / {@link ModelVertexData} ducks.
 * Anything unexpected (no or several array fields, a face without four vertices, a missing duck)
 * yields null and the cube keeps vanilla's path.</p>
 */
public final class ModelCubes {

    private ModelCubes() {
    }

    /** @param declaring the class declaring the face array (the cube class itself) */
    public static float[] pack(final Object cube, final Class<?> declaring) {
        try {
            final Object[] faces = objectArray(cube, declaring);
            if (faces == null) {
                return null;
            }
            final float[] out = new float[faces.length * ModelCubeData.FACE_FLOATS];
            int at = 0;
            for (int f = 0; f < faces.length; f++) {
                final Object face = faces[f];
                if (!(face instanceof ModelPolygonData)) {
                    return null;
                }
                final Object[] vertices = objectArray(face, face.getClass());
                if (vertices == null || vertices.length != 4) {
                    return null;
                }
                for (int v = 0; v < 4; v++) {
                    if (!(vertices[v] instanceof ModelVertexData)) {
                        return null;
                    }
                    final ModelVertexData vertex = (ModelVertexData) vertices[v];
                    out[at++] = vertex.aetheriumX();
                    out[at++] = vertex.aetheriumY();
                    out[at++] = vertex.aetheriumZ();
                    out[at++] = vertex.aetheriumU();
                    out[at++] = vertex.aetheriumV();
                }
                final ModelPolygonData polygon = (ModelPolygonData) face;
                out[at++] = polygon.aetheriumNormalX();
                out[at++] = polygon.aetheriumNormalY();
                out[at++] = polygon.aetheriumNormalZ();
            }
            return out;
        } catch (final RuntimeException | IllegalAccessException | LinkageError e) {
            return null;
        }
    }

    /** The single object-array instance field declared by {@code type} (or its nearest superclass declaring one). */
    private static Object[] objectArray(final Object owner, final Class<?> type) throws IllegalAccessException {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            Field found = null;
            for (final Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                final Class<?> t = field.getType();
                if (t.isArray() && !t.getComponentType().isPrimitive()) {
                    if (found != null) {
                        return null; // ambiguous
                    }
                    found = field;
                }
            }
            if (found != null) {
                found.setAccessible(true);
                return (Object[]) found.get(owner);
            }
        }
        return null;
    }
}
