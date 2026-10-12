package com.aetherium.client;

/** Implemented by {@code LevelRenderer} (FogCullMixin): ask vanilla to rebuild its visible list. */
public interface FogCullAccess {

    /** The fog limit changed: refilter the visible list on the next frame. */
    void aetheriumRefreshFogCull();
}
