package com.aetherium.mixin.core;

import com.aetherium.client.SkullOwnerAccess;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
// @era:qol16-begin none
// @era:qol16-else v16
//~ import com.aetherium.client.SkullLookup;
//~ import com.aetherium.perf.Tuning;
//~ import com.google.common.collect.Multimap;
//~ import com.mojang.authlib.minecraft.MinecraftSessionService;
//~ import net.minecraft.server.players.GameProfileCache;
//~ import net.minecraft.world.level.block.entity.BlockEntity;
//~ import net.minecraft.world.level.block.entity.SkullBlockEntity;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Redirect;
// @era:qol16-end

/**
 * Async player-head skins (1.10.0, Minecraft 1.16.5): see {@link com.aetherium.client.SkullLookup}.
 * Only a complete profile (name + UUID) without a skin goes async, and only on the client thread;
 * everything else (name-only heads, the server) stays exactly vanilla.
 */
@Mixin(targets = "net.minecraft.world.level.block.entity.SkullBlockEntity")
public abstract class TuningSkullMixin implements SkullOwnerAccess {
    // @era:qol16-begin none
    @Override
    public void aetherium$profileFilled(final GameProfile requested, final GameProfile filled) {
    }
    // @era:qol16-else v16
    //~ @Shadow
    //~ private static GameProfileCache profileCache;

    //~ @Shadow
    //~ private static MinecraftSessionService sessionService;

    //~ @Shadow
    //~ private GameProfile owner;

    //~ @Redirect(method = "updateOwnerProfile", at = @At(value = "INVOKE",
            //~ target = "Lnet/minecraft/world/level/block/entity/SkullBlockEntity;updateGameprofile(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;"),
            //~ require = 0)
    //~ private GameProfile aetherium$profile(final GameProfile profile) {
        //~ final MinecraftSessionService service = sessionService;
        //~ if (Tuning.asyncSkulls && profile != null && profileCache != null && service != null
                //~ && profile.isComplete() && !((Multimap<?, ?>) (Object) profile.getProperties()).containsKey("textures")
                //~ && SkullLookup.onClientThread()) {
            //~ SkullLookup.fill(this, profile, service);
            //~ return profile;
        //~ }
        //~ return SkullBlockEntity.updateGameprofile(profile);
    //~ }

    //~ @Override
    //~ public void aetherium$profileFilled(final GameProfile requested, final GameProfile filled) {
        //~ if (this.owner == requested) {
            //~ this.owner = filled;
            //~ ((BlockEntity) (Object) this).setChanged();
        //~ }
    //~ }
    // @era:qol16-end
}
