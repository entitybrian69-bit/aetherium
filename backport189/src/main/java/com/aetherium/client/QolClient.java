package com.aetherium.client;

import com.aetherium.Aetherium;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.config.ConfigStore;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.gui.UiSound;
import com.aetherium.qol.AccuratePlacement;
import com.aetherium.qol.DamageNumbers;
import com.aetherium.qol.Qol;
import com.aetherium.qol.QolSupport;
import com.aetherium.qol.Waypoints;
import com.aetherium.util.AetheriumLog;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ChatComponentText;
import org.lwjgl.input.Keyboard;

/**
 * Quality of life on 1.12.2: keys, waypoint store, death point, damage numbers and the
 * Nether / Overworld coordinates line. The same behaviour as the main mod's QolClient, written
 * against 1.12.2's MCP names; the logic itself lives in the shared qol package.
 */
public final class QolClient {
    private static final AetheriumLog LOGGER = AetheriumLog.of(QolClient.class);
    /** Damage numbers are tracked for living entities this close (blocks). */
    private static final double DAMAGE_RANGE_SQ = 48.0 * 48.0;

    public static final AccuratePlacement PLACEMENT = new AccuratePlacement();
    public static final DamageNumbers DAMAGE = new DamageNumbers();
    private static final Random JITTER = new Random();

    private static Waypoints waypoints;
    private static KeyBinding addWaypointKey;
    private static KeyBinding waypointsKey;
    private static KeyBinding sortKey;
    private static boolean wasDead;

    private QolClient() {
    }

    /** Forge pre-init (with the zoom keys): the Quality of life keys, in the Aetherium category. */
    public static void registerKeys() {
        if (addWaypointKey == null) {
            addWaypointKey = ZoomKeys.newKey("key.aetherium.add_waypoint", Keyboard.KEY_B);
            waypointsKey = ZoomKeys.newKey("key.aetherium.waypoints", Keyboard.KEY_N);
            sortKey = ZoomKeys.newKey("key.aetherium.sort", Keyboard.KEY_NONE);
        }
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

    public static KeyBinding sortKey() {
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
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        if (addWaypointKey != null) {
            while (addWaypointKey.isPressed()) {
                if (Qol.waypoints) {
                    addWaypointHere(mc);
                }
            }
            while (waypointsKey.isPressed()) {
                if (Qol.waypoints || Qol.deathPoint) {
                    openWaypoints(mc, null);
                }
            }
        }
        if (Qol.waypoints || Qol.deathPoint) {
            waypoints().selectWorld(worldKey(mc));
        }
        if (Qol.deathPoint) {
            final boolean dead = mc.thePlayer.getHealth() <= 0f;
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
        final Waypoints.Waypoint w = store.add(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ),
                dimension(mc));
        if (w == null) {
            message(mc, "Could not add a waypoint: this world already has 256.");
        } else {
            message(mc, "Added " + w.name + " at " + w.x + ", " + w.y + ", " + w.z
                    + ". Press " + Keyboard.getKeyName(waypointsKey.getKeyCode()) + " to rename or manage waypoints.");
        }
    }

    private static void recordDeath(final Minecraft mc) {
        final int x = floor(mc.thePlayer.posX);
        final int y = floor(mc.thePlayer.posY);
        final int z = floor(mc.thePlayer.posZ);
        final String dim = dimension(mc);
        final Waypoints store = waypoints();
        store.selectWorld(worldKey(mc));
        store.setDeath(x, y, z, dim);
        LOGGER.info("Death point saved at {}, {}, {} in {}", x, y, z, dim);
    }

    /** "You died at x, y, z (Overworld)" for the death screen, or null. */
    public static String deathLine(final Minecraft mc) {
        if (!Qol.deathPoint || mc.thePlayer == null || mc.theWorld == null) {
            return null;
        }
        return "You died at " + floor(mc.thePlayer.posX) + ", " + floor(mc.thePlayer.posY) + ", "
                + floor(mc.thePlayer.posZ) + " (" + Waypoints.dimensionName(dimension(mc)) + ")";
    }

    private static void tickDamage(final Minecraft mc) {
        final long now = System.currentTimeMillis();
        DAMAGE.beginTick();
        final List<Entity> entities = mc.theWorld.loadedEntityList;
        for (int i = 0; i < entities.size(); i++) {
            final Entity entity = entities.get(i);
            if (!(entity instanceof EntityLivingBase) || entity == mc.thePlayer
                    || entity.getDistanceSqToEntity(mc.thePlayer) > DAMAGE_RANGE_SQ) {
                continue;
            }
            final EntityLivingBase living = (EntityLivingBase) entity;
            DAMAGE.report(entity.getEntityId(), living.getHealth(), entity.posX, entity.posY + entity.height,
                    entity.posZ, Qol.damageHeals, now, JITTER.nextDouble());
        }
        DAMAGE.endTick(now);
    }

    // ------------------------------------------------------------------ HUD

    /** After the HUD (RenderGameOverlayEvent.Post ALL): the other dimension's coordinates. */
    public static void renderHud(final ScaledResolution res) {
        if (!Qol.dimensionCoords || res == null) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        final String dim = dimension(mc);
        final String line;
        if (Waypoints.NETHER.equals(dim)) {
            line = "Overworld: " + floor(mc.thePlayer.posX * 8.0) + ", " + floor(mc.thePlayer.posZ * 8.0);
        } else if (Waypoints.OVERWORLD.equals(dim)) {
            line = "Nether: " + floor(mc.thePlayer.posX / 8.0) + ", " + floor(mc.thePlayer.posZ / 8.0);
        } else {
            return;
        }
        final GuiCanvas canvas = McCanvas.INSTANCE.begin(res.getScaleFactor());
        final String corner = Qol.dimensionCoordsCorner;
        final int sw = res.getScaledWidth();
        final int sh = res.getScaledHeight();
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
        UiSounds.play(mc, UiSound.CLICK);
    }

    /** A chat line only this player sees. */
    private static void message(final Minecraft mc, final String text) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(text));
        }
    }

    static int floor(final double value) {
        return (int) Math.floor(value);
    }

    /** Which waypoint list applies: the server address, or the save folder for single player. */
    static String worldKey(final Minecraft mc) {
        final ServerData server = mc.getCurrentServerData();
        if (server != null && server.serverIP != null) {
            return "server:" + server.serverIP.trim().toLowerCase(java.util.Locale.ROOT);
        }
        final IntegratedServer local = mc.getIntegratedServer();
        if (local != null) {
            return "local:" + local.getFolderName();
        }
        return "realms";
    }

    /** Dimension id of the player's world, e.g. "minecraft:the_nether" (1.8.9 only numbers them). */
    static String dimension(final Minecraft mc) {
        if (mc.theWorld == null) {
            return Waypoints.OVERWORLD;
        }
        final int id = mc.theWorld.provider.getDimensionId();
        switch (id) {
            case 0:
                return Waypoints.OVERWORLD;
            case -1:
                return Waypoints.NETHER;
            case 1:
                return "minecraft:the_end";
            default:
                return "minecraft:dimension_" + id;
        }
    }

    /** "B / N / Not bound" (Add waypoint, Waypoints, Sort) for the settings screen. */
    public static String describeKeys() {
        if (addWaypointKey == null) {
            return "Not registered";
        }
        return ZoomKeys.keyName(addWaypointKey) + " / " + ZoomKeys.keyName(waypointsKey) + " / "
                + (sortKey.getKeyCode() == Keyboard.KEY_NONE ? "Not bound" : ZoomKeys.keyName(sortKey));
    }

    /** Opens the waypoint list over {@code parent} (null = the game). */
    public static void openWaypoints(final Minecraft mc, final GuiScreen parent) {
        if (mc.thePlayer == null) {
            return;
        }
        waypoints().selectWorld(worldKey(mc));
        mc.displayGuiScreen(new WaypointScreen(parent));
    }
}
