package com.aetherium.backport;

import com.aetherium.Aetherium;
import com.aetherium.client.AetheriumScreen;
import com.aetherium.client.TerrainRenderer;
import com.aetherium.mixin.AetheriumMixinPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Automated in-game check used by CI only ({@code -Daetherium.smoke=<dir>}; inert otherwise):
 * main menu, a fresh creative world, the world after the chunks loaded, every settings tab, then
 * a report (applied hooks, renderer state, frame rate) and a clean shutdown.
 */
public final class SmokeTest {

    private static final int SETTLE_TICKS = 300;
    private static final int TAB_TICKS = 20;
    private static final int TABS = 6;

    private final File dir;
    private final List<String> log = new ArrayList<String>();
    private int stage;
    private int wait;
    private int tab;

    private SmokeTest(final File dir) {
        this.dir = dir;
    }

    /** Registers the test when the property is set. */
    public static void installIfRequested() {
        final String target = System.getProperty("aetherium.smoke");
        if (target == null || target.isEmpty()) {
            return;
        }
        final File dir = new File(target);
        dir.mkdirs();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SmokeTest(dir));
    }

    @SubscribeEvent
    public void onTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        final Minecraft mc = Minecraft.getMinecraft();
        try {
            step(mc);
        } catch (final RuntimeException | LinkageError error) {
            note("FAILED in stage " + this.stage + ": " + error);
            finish(mc);
        }
    }

    private void step(final Minecraft mc) {
        if (this.wait > 0) {
            this.wait--;
            return;
        }
        switch (this.stage) {
            case 0:
                if (mc.currentScreen instanceof GuiMainMenu) {
                    this.wait = 40;
                    this.stage = 1;
                }
                break;
            case 1:
                shot(mc, "01-menu");
                note("menu ok");
                mc.launchIntegratedServer("aetherium-smoke", "aetherium-smoke",
                        new WorldSettings(20261014L, GameType.CREATIVE, true, false, WorldType.DEFAULT));
                this.stage = 2;
                break;
            case 2:
                if (mc.world != null && mc.player != null && mc.currentScreen == null) {
                    note("world joined");
                    mc.player.rotationPitch = 10.0f;
                    this.wait = SETTLE_TICKS;
                    this.stage = 3;
                }
                break;
            case 3:
                shot(mc, "02-world");
                note(String.format(Locale.ROOT, "fps=%.1f frame=%.2fms p99=%.2fms",
                        Aetherium.frameStats().getFps(), Aetherium.frameStats().getFrameMs(),
                        Aetherium.frameStats().getP99Ms()));
                note("renderer: " + TerrainRenderer.statusText());
                mc.displayGuiScreen(new GuiVideoSettings(new GuiOptions(null, mc.gameSettings), mc.gameSettings));
                this.wait = TAB_TICKS;
                this.stage = 4;
                break;
            case 4:
                if (!(mc.currentScreen instanceof AetheriumScreen)) {
                    note("FAILED: video settings did not open the Aetherium screen: " + mc.currentScreen);
                    this.stage = 5;
                    break;
                }
                shot(mc, String.format(Locale.ROOT, "03-tab%d", this.tab));
                this.tab++;
                if (this.tab >= TABS) {
                    note("settings screen ok (" + TABS + " tabs)");
                    mc.displayGuiScreen(null);
                    this.wait = 40;
                    this.stage = 5;
                } else {
                    ((AetheriumScreen) mc.currentScreen).selectTabForTest(this.tab);
                    this.wait = TAB_TICKS;
                }
                break;
            case 5:
                note("hooks applied (" + AetheriumMixinPlugin.appliedCount() + "): "
                        + new TreeSet<String>(AetheriumMixinPlugin.applied()));
                note("renderer: " + TerrainRenderer.statusText());
                note("PASS");
                finish(mc);
                break;
            default:
                break;
        }
    }

    private void shot(final Minecraft mc, final String name) {
        ScreenShotHelper.saveScreenshot(this.dir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private void note(final String line) {
        this.log.add(line);
        writeReport();
    }

    private void writeReport() {
        try {
            Files.write(new File(this.dir, "report.txt").toPath(), this.log, StandardCharsets.UTF_8);
        } catch (final IOException ignored) {
            // CI notices the missing report.
        }
    }

    private void finish(final Minecraft mc) {
        this.stage = 99;
        writeReport();
        mc.shutdown();
    }
}
