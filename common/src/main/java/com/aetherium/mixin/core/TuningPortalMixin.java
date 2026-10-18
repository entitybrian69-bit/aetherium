package com.aetherium.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
// @era:t-portal-begin none
// @era:t-portal-else v16
//~ import com.aetherium.perf.Tuning;
//~ import net.minecraft.core.BlockPos;
//~ import net.minecraft.resources.ResourceKey;
//~ import net.minecraft.server.MinecraftServer;
//~ import net.minecraft.server.level.ServerLevel;
//~ import net.minecraft.server.level.ServerPlayer;
//~ import net.minecraft.server.level.TicketType;
//~ import net.minecraft.world.entity.Entity;
//~ import net.minecraft.world.level.ChunkPos;
//~ import net.minecraft.world.level.Level;
//~ import org.spongepowered.asm.mixin.Shadow;
//~ import org.spongepowered.asm.mixin.Unique;
//~ import org.spongepowered.asm.mixin.injection.At;
//~ import org.spongepowered.asm.mixin.injection.Inject;
//~ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// @era:t-portal-end

/**
 * Portal preloading (1.12.0, Minecraft 1.16.5, singleplayer): while a player stands in a Nether
 * portal (80 ticks in survival before the trip), the area around the matching position in the
 * other dimension gets the same {@code PORTAL} ticket vanilla adds when it searches for the exit
 * portal (radius 3, 300-tick timeout). The chunks, and the portal points of interest stored with
 * them, then load or generate in the background during the wait instead of all at once in the
 * tick of the trip. Overworld and Nether only; renewed at most every 10 seconds.
 */
@Mixin(targets = "net.minecraft.world.entity.Entity")
public abstract class TuningPortalMixin {
    // @era:t-portal-begin none
    // @era:t-portal-else v16
    //~ @Shadow
    //~ public Level level;

    //~ @Shadow
    //~ protected boolean isInsidePortal;

    //~ /** Game time of the last preload, or -1 before the first one. */
    //~ @Unique
    //~ private long aetherium$preloadedAt = -1L;

    //~ @Inject(method = "handleNetherPortal", at = @At("HEAD"), require = 0)
    //~ private void aetherium$preload(final CallbackInfo ci) {
        //~ if (!Tuning.portalPreload || !isInsidePortal || !((Object) this instanceof ServerPlayer)
                //~ || !(level instanceof ServerLevel)) {
            //~ return;
        //~ }
        //~ final long now = level.getGameTime();
        //~ if (aetherium$preloadedAt >= 0L && now >= aetherium$preloadedAt && now - aetherium$preloadedAt < 200L) {
            //~ return;
        //~ }
        //~ aetherium$preloadedAt = now;
        //~ final ResourceKey<Level> here = level.dimension();
        //~ final double scale;
        //~ final ResourceKey<Level> there;
        //~ if (here == Level.OVERWORLD) {
            //~ there = Level.NETHER;
            //~ scale = 1.0 / 8.0;
        //~ } else if (here == Level.NETHER) {
            //~ there = Level.OVERWORLD;
            //~ scale = 8.0;
        //~ } else {
            //~ return;
        //~ }
        //~ final MinecraftServer server = ((ServerLevel) level).getServer();
        //~ if (!server.isNetherEnabled()) {
            //~ return;
        //~ }
        //~ final ServerLevel target = server.getLevel(there);
        //~ if (target == null) {
            //~ return;
        //~ }
        //~ final Entity self = (Entity) (Object) this;
        //~ final int x = aetherium$clamp(Math.floor(self.getX() * scale), 29_999_872);
        //~ final int z = aetherium$clamp(Math.floor(self.getZ() * scale), 29_999_872);
        //~ final int y = Math.max(0, Math.min(255, (int) Math.floor(self.getY())));
        //~ final BlockPos pos = new BlockPos(x, y, z);
        //~ target.getChunkSource().addRegionTicket(TicketType.PORTAL, new ChunkPos(pos), 3, pos);
    //~ }

    //~ @Unique
    //~ private static int aetherium$clamp(final double value, final int limit) {
        //~ return (int) Math.max(-limit, Math.min(limit, value));
    //~ }
    // @era:t-portal-end
}
