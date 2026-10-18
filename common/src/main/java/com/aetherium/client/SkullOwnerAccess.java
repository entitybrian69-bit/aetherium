package com.aetherium.client;

import com.mojang.authlib.GameProfile;

/** Implemented by player heads (Minecraft 1.16.5): receives a skin looked up off the render thread. */
public interface SkullOwnerAccess {
    /**
     * @param requested the profile the lookup was started for
     * @param filled    the same profile with its skin properties
     */
    void aetherium$profileFilled(GameProfile requested, GameProfile filled);
}
