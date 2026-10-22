package com.aetherium.client;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.ActiveRenderInfo;

/**
 * The camera matrix vanilla reads back once per frame ({@code ActiveRenderInfo.updateRenderInfo}).
 * 1.8.9 keeps it in a private static field; it is looked up once by reflection (development name,
 * then the runtime name, then the first 16-float buffer of the class, which is the modelview).
 */
public final class CameraMatrix {

    private static final String[] NAMES = {"MODELVIEW", "field_178812_b"};
    private static boolean looked;
    private static FloatBuffer modelview;

    private CameraMatrix() {
    }

    /** The live modelview buffer vanilla fills each frame, or null when it could not be found. */
    public static FloatBuffer modelview() {
        if (!looked) {
            looked = true;
            modelview = find();
        }
        return modelview;
    }

    private static FloatBuffer find() {
        try {
            for (final String name : NAMES) {
                try {
                    final Field field = ActiveRenderInfo.class.getDeclaredField(name);
                    final FloatBuffer value = read(field);
                    if (value != null) {
                        return value;
                    }
                } catch (final NoSuchFieldException ignored) {
                    // try the next name
                }
            }
            for (final Field field : ActiveRenderInfo.class.getDeclaredFields()) {
                final FloatBuffer value = read(field);
                if (value != null) {
                    return value;
                }
            }
        } catch (final Throwable ignored) {
            // no culling by camera position; everything is drawn as before
        }
        return null;
    }

    private static FloatBuffer read(final Field field) throws IllegalAccessException {
        if (!Modifier.isStatic(field.getModifiers()) || field.getType() != FloatBuffer.class) {
            return null;
        }
        field.setAccessible(true);
        final FloatBuffer value = (FloatBuffer) field.get(null);
        return value != null && value.capacity() == 16 ? value : null;
    }
}
