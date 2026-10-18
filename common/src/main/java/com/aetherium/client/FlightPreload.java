package com.aetherium.client;

// @era:t-flight-preload-begin v16|f20
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
// @era:t-flight-preload-else none
// @era:t-flight-preload-else f215|f216
//~ import java.util.List;
//~ import net.minecraft.server.MinecraftServer;
//~ import net.minecraft.server.level.ServerPlayer;
//~ import net.minecraft.server.level.TicketType;
//~ import net.minecraft.world.level.ChunkPos;
//~ import net.minecraft.world.phys.Vec3;
// @era:t-flight-preload-end

/**
 * Elytra look-ahead (1.10.0, Minecraft 1.16.5, single player): while a player glides fast, the
 * integrated server is asked to load / generate the chunks about two seconds ahead of the flight
 * path, so they are ready (and sent) before the player gets there instead of popping in late. The
 * temporary tickets expire after 5 seconds by themselves.
 */
public final class FlightPreload {
    private FlightPreload() {
    }

    // @era:t-flight-preload-begin f20
    /** Ticks of flight to look ahead (2 s). */
    private static final int LOOK_AHEAD_TICKS = 40;
    /** Only while gliding faster than 0.5 blocks per tick (10 m/s). */
    private static final double MIN_SPEED_SQ = 0.25;
    private static final TicketType<ChunkPos> FLIGHT =
            TicketType.create("aetherium_flight", Comparator.comparingLong(ChunkPos::toLong), 100);

    /** MinecraftServer.tickServer tail. */
    public static void tick(final MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) {
            return;
        }
        final List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (int i = 0, n = players.size(); i < n; i++) {
            final ServerPlayer player = players.get(i);
            if (!player.isFallFlying()) {
                continue;
            }
            final Vec3 motion = player.getDeltaMovement();
            if (motion.x * motion.x + motion.z * motion.z < MIN_SPEED_SQ) {
                continue;
            }
            final double ax = player.getX() + motion.x * LOOK_AHEAD_TICKS;
            final double az = player.getZ() + motion.z * LOOK_AHEAD_TICKS;
            final ChunkPos target = new ChunkPos((int) Math.floor(ax) >> 4, (int) Math.floor(az) >> 4);
            player.serverLevel().getChunkSource().addRegionTicket(FLIGHT, target, 2, target);
        }
    }
    // @era:t-flight-preload-else none
    //~ /** MinecraftServer.tickServer tail (not ported to this version). */
    //~ public static void tick(final net.minecraft.server.MinecraftServer server) {
    //~ }
    // @era:t-flight-preload-else v16
    //~ /** Ticks of flight to look ahead (2 s). */
    //~ private static final int LOOK_AHEAD_TICKS = 40;
    //~ /** Only while gliding faster than 0.5 blocks per tick (10 m/s). */
    //~ private static final double MIN_SPEED_SQ = 0.25;
    //~ private static final TicketType<ChunkPos> FLIGHT =
            //~ TicketType.create("aetherium_flight", Comparator.comparingLong(ChunkPos::toLong), 100);

    //~ /** MinecraftServer.tickServer tail. */
    //~ public static void tick(final MinecraftServer server) {
        //~ if (server.getTickCount() % 10 != 0) {
            //~ return;
        //~ }
        //~ final List<ServerPlayer> players = server.getPlayerList().getPlayers();
        //~ for (int i = 0, n = players.size(); i < n; i++) {
            //~ final ServerPlayer player = players.get(i);
            //~ if (!player.isFallFlying()) {
                //~ continue;
            //~ }
            //~ final Vec3 motion = player.getDeltaMovement();
            //~ if (motion.x * motion.x + motion.z * motion.z < MIN_SPEED_SQ) {
                //~ continue;
            //~ }
            //~ final double ax = player.getX() + motion.x * LOOK_AHEAD_TICKS;
            //~ final double az = player.getZ() + motion.z * LOOK_AHEAD_TICKS;
            //~ final ChunkPos target = new ChunkPos((int) Math.floor(ax) >> 4, (int) Math.floor(az) >> 4);
            //~ player.getLevel().getChunkSource().addRegionTicket(FLIGHT, target, 2, target);
        //~ }
    //~ }
    // @era:t-flight-preload-else f215
    //~ /** Ticks of flight to look ahead (2 s). */
    //~ private static final int LOOK_AHEAD_TICKS = 40;
    //~ /** Only while gliding faster than 0.5 blocks per tick (10 m/s). */
    //~ private static final double MIN_SPEED_SQ = 0.25;
    //~ /** Vanilla's ender pearl ticket: loads and simulates for 40 ticks, never saved. */
    //~ private static final TicketType FLIGHT = TicketType.ENDER_PEARL;

    //~ /** MinecraftServer.tickServer tail. */
    //~ public static void tick(final MinecraftServer server) {
        //~ if (server.getTickCount() % 10 != 0) {
            //~ return;
        //~ }
        //~ final List<ServerPlayer> players = server.getPlayerList().getPlayers();
        //~ for (int i = 0, n = players.size(); i < n; i++) {
            //~ final ServerPlayer player = players.get(i);
            //~ if (!player.isFallFlying()) {
                //~ continue;
            //~ }
            //~ final Vec3 motion = player.getDeltaMovement();
            //~ if (motion.x * motion.x + motion.z * motion.z < MIN_SPEED_SQ) {
                //~ continue;
            //~ }
            //~ final double ax = player.getX() + motion.x * LOOK_AHEAD_TICKS;
            //~ final double az = player.getZ() + motion.z * LOOK_AHEAD_TICKS;
            //~ final ChunkPos target = new ChunkPos((int) Math.floor(ax) >> 4, (int) Math.floor(az) >> 4);
            //~ player.serverLevel().getChunkSource().addTicketWithRadius(FLIGHT, target, 2);
        //~ }
    //~ }
    // @era:t-flight-preload-else f216
    //~ /** Ticks of flight to look ahead (2 s). */
    //~ private static final int LOOK_AHEAD_TICKS = 40;
    //~ /** Only while gliding faster than 0.5 blocks per tick (10 m/s). */
    //~ private static final double MIN_SPEED_SQ = 0.25;
    //~ /** Vanilla's ender pearl ticket: loads and simulates for 40 ticks, never saved. */
    //~ private static final TicketType FLIGHT = TicketType.ENDER_PEARL;

    //~ /** MinecraftServer.tickServer tail. */
    //~ public static void tick(final MinecraftServer server) {
        //~ if (server.getTickCount() % 10 != 0) {
            //~ return;
        //~ }
        //~ final List<ServerPlayer> players = server.getPlayerList().getPlayers();
        //~ for (int i = 0, n = players.size(); i < n; i++) {
            //~ final ServerPlayer player = players.get(i);
            //~ if (!player.isFallFlying()) {
                //~ continue;
            //~ }
            //~ final Vec3 motion = player.getDeltaMovement();
            //~ if (motion.x * motion.x + motion.z * motion.z < MIN_SPEED_SQ) {
                //~ continue;
            //~ }
            //~ final double ax = player.getX() + motion.x * LOOK_AHEAD_TICKS;
            //~ final double az = player.getZ() + motion.z * LOOK_AHEAD_TICKS;
            //~ final ChunkPos target = new ChunkPos((int) Math.floor(ax) >> 4, (int) Math.floor(az) >> 4);
            //~ player.level().getChunkSource().addTicketWithRadius(FLIGHT, target, 2);
        //~ }
    //~ }
    // @era:t-flight-preload-end
}
