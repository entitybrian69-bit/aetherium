package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigStore;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.qol.AccuratePlacement;
import com.aetherium.qol.DamageNumbers;
import com.aetherium.qol.Qol;
import com.aetherium.qol.QolSupport;
import com.aetherium.qol.Waypoints;
import com.aetherium.util.AetheriumLog;

import java.nio.file.Path;
import java.util.Random;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Minecraft side of the Quality of life tab: its keys, the per-tick work (death point, damage
 * numbers, sorting) and the coordinates HUD line. Everything is client-only and uses packets a
 * vanilla client sends anyway, so it works on any server. Each piece is a field read while its
 * option is off.
 */
public final class QolClient {
    private static final AetheriumLog LOGGER = AetheriumLog.of(QolClient.class);
    private static final int KEY_B = 66;
    private static final int KEY_N = 78;
    private static final int UNBOUND = -1;
    /** Damage numbers are tracked for living entities this close (blocks). */
    private static final double DAMAGE_RANGE_SQ = 48.0 * 48.0;

    public static final AccuratePlacement PLACEMENT = new AccuratePlacement();
    public static final DamageNumbers DAMAGE = new DamageNumbers();
    private static final Random JITTER = new Random();

    private static Waypoints waypoints;
    private static KeyMapping addWaypointKey;
    private static KeyMapping waypointsKey;
    private static KeyMapping sortKey;
    private static boolean wasDead;

    private QolClient() {
    }

    /** After Apply and at startup (from ClientHooks.onSettingsApplied). */
    public static void configure(final AetheriumConfig config, final boolean active) {
        Qol.configure(config, active);
        if (!Qol.damageNumbers) {
            DAMAGE.clear();
        }
        if (!Qol.accuratePlacement) {
            PLACEMENT.reset();
        }
    }

    /** Options.load HEAD: the key list with the Quality of life keys appended (once). */
    public static KeyMapping[] appendKeys(final KeyMapping[] existing) {
        if (!QolSupport.ANY) {
            return existing;
        }
        if (addWaypointKey == null) {
            addWaypointKey = ZoomKeys.newKey("key.aetherium.add_waypoint", KEY_B);
            waypointsKey = ZoomKeys.newKey("key.aetherium.waypoints", KEY_N);
            sortKey = ZoomKeys.newKey("key.aetherium.sort", UNBOUND);
        }
        if (existing == null) {
            return new KeyMapping[] {addWaypointKey, waypointsKey, sortKey};
        }
        for (final KeyMapping key : existing) {
            if (key == addWaypointKey) {
                return existing;
            }
        }
        final KeyMapping[] out = java.util.Arrays.copyOf(existing, existing.length + 3);
        out[existing.length] = addWaypointKey;
        out[existing.length + 1] = waypointsKey;
        out[existing.length + 2] = sortKey;
        return out;
    }

    /** The Sort key, or null before the options were loaded. */
    public static KeyMapping sortKey() {
        return sortKey;
    }

    /** The waypoint store, loaded on first use from aetherium-waypoints.json next to the config. */
    public static Waypoints waypoints() {
        if (waypoints == null) {
            Path file = null;
            final Aetherium.Subsystems sub = Aetherium.subsystemsOrNull();
            if (sub != null) {
                final ConfigStore store = sub.store;
                file = store.getFile().resolveSibling("aetherium-waypoints.json");
            }
            waypoints = new Waypoints(file);
        }
        return waypoints;
    }

    /** Joining, leaving or changing worlds. */
    public static void onLevelChange() {
        DAMAGE.clear();
        PLACEMENT.reset();
        wasDead = false;
        QolInventory.cancel();
        QolFoodIcons.invalidate();
    }

    /** End of every client tick. */
    public static void onClientTick(final Minecraft mc) {
        if (!QolSupport.ANY) {
            return;
        }
        if (Qol.accuratePlacement) {
            PLACEMENT.tick();
        }
        QolInventory.tick(mc);
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (addWaypointKey != null) {
            while (addWaypointKey.consumeClick()) {
                if (Qol.waypoints) {
                    addWaypointHere(mc);
                }
            }
            while (waypointsKey.consumeClick()) {
                if (Qol.waypoints || Qol.deathPoint) {
                    openWaypoints(mc, null);
                }
            }
        }
        if (Qol.waypoints || Qol.deathPoint) {
            waypoints().selectWorld(worldKey(mc));
        }
        if (Qol.deathPoint) {
            final boolean dead = mc.player.isDeadOrDying();
            if (dead && !wasDead) {
                recordDeath(mc);
            }
            wasDead = dead;
        }
        if (Qol.damageNumbers) {
            tickDamage(mc);
        } else if (!DAMAGE.isIdle()) {
            DAMAGE.clear();
        }
    }

    private static void addWaypointHere(final Minecraft mc) {
        final Waypoints store = waypoints();
        store.selectWorld(worldKey(mc));
        final Waypoints.Waypoint w = store.add(floor(mc.player.getX()), floor(mc.player.getY()), floor(mc.player.getZ()),
                dimension(mc));
        if (w == null) {
            message(mc, "Could not add a waypoint: this world already has 256.");
        } else {
            message(mc, "Added " + w.name + " at " + w.x + ", " + w.y + ", " + w.z
                    + ". Press N to rename or manage waypoints.");
        }
    }

    private static void recordDeath(final Minecraft mc) {
        final int x = floor(mc.player.getX());
        final int y = floor(mc.player.getY());
        final int z = floor(mc.player.getZ());
        final String dim = dimension(mc);
        final Waypoints store = waypoints();
        store.selectWorld(worldKey(mc));
        store.setDeath(x, y, z, dim);
        LOGGER.info("Death point saved at {}, {}, {} in {}", x, y, z, dim);
    }

    /** "You died at x, y, z (Overworld)" for the death screen (the body stays where it fell), or null. */
    public static String deathLine(final Minecraft mc) {
        if (!Qol.deathPoint || mc.player == null || mc.level == null) {
            return null;
        }
        return "You died at " + floor(mc.player.getX()) + ", " + floor(mc.player.getY()) + ", "
                + floor(mc.player.getZ()) + " (" + Waypoints.dimensionName(dimension(mc)) + ")";
    }

    private static void tickDamage(final Minecraft mc) {
        final long now = System.currentTimeMillis();
        DAMAGE.beginTick();
        for (final Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity) || entity == mc.player
                    || entity.distanceToSqr(mc.player) > DAMAGE_RANGE_SQ) {
                continue;
            }
            final LivingEntity living = (LivingEntity) entity;
            DAMAGE.report(entity.getId(), living.getHealth(), entity.getX(), entity.getY() + entity.getBbHeight(),
                    entity.getZ(), Qol.damageHeals, now, JITTER.nextDouble());
        }
        DAMAGE.endTick(now);
    }

    // ------------------------------------------------------------------ HUD

    /** Gui.render, after the frame box: the other dimension's coordinates. */
    public static void renderHud(final GuiCanvas canvas) {
        if (!Qol.dimensionCoords) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || debugScreenShown(mc)) {
            return;
        }
        final String dim = dimension(mc);
        final String line;
        if (Waypoints.NETHER.equals(dim)) {
            line = "Overworld: " + floor(mc.player.getX() * 8.0) + ", " + floor(mc.player.getZ() * 8.0);
        } else if (Waypoints.OVERWORLD.equals(dim)) {
            line = "Nether: " + floor(mc.player.getX() / 8.0) + ", " + floor(mc.player.getZ() / 8.0);
        } else {
            return;
        }
        final String corner = Qol.dimensionCoordsCorner;
        final int sw = mc.getWindow().getGuiScaledWidth();
        final int sh = mc.getWindow().getGuiScaledHeight();
        final int w = canvas.textWidth(line) + 6;
        final int h = 13;
        final int below = FrameHud.heightIn(corner);
        final int x = corner.endsWith("right") ? sw - w - 4 : 4;
        final int y = corner.startsWith("bottom") ? sh - h - 4 - below : 4 + below;
        canvas.fill(x, y, x + w, y + h, 0x90000000);
        canvas.text(line, x + 3, y + 3, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------------ helpers

    /** The button click (Sort button). */
    public static void clickSound(final Minecraft mc) {
        UiSounds.play(mc, com.aetherium.gui.UiSound.CLICK);
    }

    /** A chat line only this player sees. */
    private static void message(final Minecraft mc, final String text) {
        // @era:q-world-begin none
        // @era:q-world-else v16
        //~ if (mc.player != null) {
            //~ mc.player.displayClientMessage(net.minecraft.network.chat.Component.nullToEmpty(text), false);
        //~ }
        // @era:q-world-end
    }

    static int floor(final double value) {
        return (int) Math.floor(value);
    }

    // @era:q-world-begin none
    static String worldKey(final Minecraft mc) {
        return "";
    }

    static String dimension(final Minecraft mc) {
        return Waypoints.OVERWORLD;
    }

    private static boolean debugScreenShown(final Minecraft mc) {
        return true;
    }

    public static String describeKeys() {
        return "Not available on this version";
    }

    public static void openWaypoints(final Minecraft mc, final net.minecraft.client.gui.screens.Screen parent) {
    }
    // @era:q-world-else v16
    //~ /** Which waypoint list applies: the server address, or the save folder for single player. */
    //~ static String worldKey(final Minecraft mc) {
        //~ final net.minecraft.client.multiplayer.ServerData server = mc.getCurrentServer();
        //~ if (server != null && server.ip != null) {
            //~ return "server:" + server.ip.trim().toLowerCase(java.util.Locale.ROOT);
        //~ }
        //~ final net.minecraft.client.server.IntegratedServer local = mc.getSingleplayerServer();
        //~ if (local != null) {
            //~ final Path root = local.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    //~ .toAbsolutePath().normalize();
            //~ final Path name = root.getFileName();
            //~ return "local:" + (name == null ? root.toString() : name.toString());
        //~ }
        //~ return "realms";
    //~ }

    //~ /** Dimension id of the player's level, e.g. "minecraft:the_nether". */
    //~ static String dimension(final Minecraft mc) {
        //~ return mc.level == null ? Waypoints.OVERWORLD : mc.level.dimension().location().toString();
    //~ }

    //~ private static boolean debugScreenShown(final Minecraft mc) {
        //~ return mc.options.renderDebug;
    //~ }

    //~ /** "B / N / Not bound" (Add waypoint, Waypoints, Sort) for the settings screen. */
    //~ public static String describeKeys() {
        //~ if (addWaypointKey == null) {
            //~ return "Not registered";
        //~ }
        //~ return addWaypointKey.getTranslatedKeyMessage().getString() + " / "
                //~ + waypointsKey.getTranslatedKeyMessage().getString() + " / "
                //~ + sortKey.getTranslatedKeyMessage().getString();
    //~ }

    //~ /** Opens the waypoint list over {@code parent} (null = the game). */
    //~ public static void openWaypoints(final Minecraft mc, final net.minecraft.client.gui.screens.Screen parent) {
        //~ if (mc.player == null) {
            //~ return;
        //~ }
        //~ waypoints().selectWorld(worldKey(mc));
        //~ ClientHooks.setScreen(mc, new WaypointScreen(parent));
    //~ }
    // @era:q-world-end
}
