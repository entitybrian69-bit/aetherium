package com.aetherium.shader;

import java.io.File;
// @era:shader-begin none
// @era:shader-else v16
//~ import com.aetherium.Aetherium;
//~ import com.aetherium.client.ClientHooks;
//~ import com.aetherium.config.AetheriumConfig;
//~ import com.mojang.blaze3d.pipeline.RenderTarget;
//~ import java.awt.image.BufferedImage;
//~ import java.io.FileOutputStream;
//~ import java.io.OutputStreamWriter;
//~ import java.io.Writer;
//~ import java.nio.ByteBuffer;
//~ import java.nio.charset.StandardCharsets;
//~ import java.util.ArrayList;
//~ import java.util.List;
//~ import java.util.Map;
//~ import java.util.TreeMap;
//~ import javax.imageio.ImageIO;
//~ import net.minecraft.client.Minecraft;
//~ import net.minecraft.core.RegistryAccess;
//~ import net.minecraft.world.Difficulty;
//~ import net.minecraft.world.level.DataPackConfig;
//~ import net.minecraft.world.level.GameRules;
//~ import net.minecraft.world.level.GameType;
//~ import net.minecraft.world.level.LevelSettings;
//~ import net.minecraft.world.level.levelgen.WorldGenSettings;
//~ import org.lwjgl.BufferUtils;
//~ import org.lwjgl.opengl.GL11;
// @era:shader-end

/**
 * CI smoke test of the shader engine (only with {@code -Daetherium.smoke}, called by
 * {@link com.aetherium.mixin.SmokeTest}): at the title screen every pack (the bundled one and
 * whatever the workflow put in shaderpacks/) is compiled natively and in forced compatibility mode,
 * then a world is created and each pack draws it for a while; status, problems, frame rate and a
 * small screenshot per pack go to the smoke folder. Never active in a normal game.
 */
public final class ShaderSmoke {

    private ShaderSmoke() {
    }

    // @era:shader-begin none
    public static String compileReport(final File dir) {
        return "n/a";
    }

    public static boolean startWorld(final File dir) {
        return false;
    }

    public static void tick() {
    }
    // @era:shader-else v16
    //~ private static final long STAGE_MIN_MS = 12000L;
    //~ private static final long STAGE_MAX_MS = 75000L;
    //~ private static final long WORLD_WAIT_MS = 45000L;
    //~ private static final long TOTAL_MS = 1500000L;

    //~ private static File dir;
    //~ private static final List<String[]> STAGES = new ArrayList<>();
    //~ private static final StringBuilder OUT = new StringBuilder();
    //~ private static int stage = -2;
    //~ private static long stageStart;
    //~ private static int frameMark;
    //~ private static long worldSeen;
    //~ private static int drew;
    //~ private static int failed;
    //~ private static volatile boolean finished;

    //~ /** Title screen: builds every pack natively and with the compatibility path forced. */
    //~ public static String compileReport(final File out) {
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ ShaderBridge.setGameDir(mc.gameDirectory.toPath());
        //~ final StringBuilder detail = new StringBuilder();
        //~ int configs = 0;
        //~ int clean = 0;
        //~ int unusable = 0;
        //~ for (final String name : testPacks()) {
            //~ for (final boolean compat : new boolean[] {false, true}) {
                //~ configs++;
                //~ detail.append("=== ").append(name).append(compat ? " [compat forced]" : " [native]").append('\n');
                //~ try {
                    //~ final ShaderPipeline pl = ShaderRuntime.buildForTest(mc, name, compat);
                    //~ detail.append(pl.summary()).append('\n');
                    //~ if (!compat) {
                        //~ detail.append(pl.describePasses());
                    //~ }
                    //~ if (pl.errors.isEmpty()) {
                        //~ clean++;
                    //~ }
                    //~ if (pl.program("gbuffers_terrain") == null && pl.program("gbuffers_textured") == null) {
                        //~ unusable++;
                        //~ detail.append("UNUSABLE: no terrain program\n");
                    //~ }
                    //~ for (final String e : pl.errors) {
                        //~ detail.append("ERROR ").append(e).append('\n');
                    //~ }
                    //~ int shown = 0;
                    //~ for (final String w : pl.warnings) {
                        //~ if (shown++ < 25) {
                            //~ detail.append("WARN  ").append(w).append('\n');
                        //~ }
                    //~ }
                    //~ pl.delete();
                //~ } catch (final Throwable t) {
                    //~ unusable++;
                    //~ detail.append("CRASH ").append(t).append('\n');
                    //~ for (final StackTraceElement e : t.getStackTrace()) {
                        //~ if (e.getClassName().startsWith("com.aetherium")) {
                            //~ detail.append("    at ").append(e).append('\n');
                        //~ }
                    //~ }
                //~ }
            //~ }
        //~ }
        //~ write(new File(out, "shaders-compile.txt"), detail.toString());
        //~ return configs + " builds, " + clean + " without errors, " + unusable + " unusable"
                //~ + " (device: " + safeDevice() + ")";
    //~ }

    //~ /** Every listed pack plus the unlisted Diagnostic pack (it proves block ids reach the shaders). */
    //~ private static List<String> testPacks() {
        //~ final List<String> names = new java.util.ArrayList<>(ShaderBridge.available());
        //~ names.add(Math.min(1, names.size()), ShaderBridge.DIAGNOSTIC);
        //~ return names;
    //~ }

    //~ private static String safeDevice() {
        //~ try {
            //~ return GlCaps.read(false, false).describe();
        //~ } catch (final Throwable t) {
            //~ return "?";
        //~ }
    //~ }

    //~ /** After the title-screen report: creates a world and walks the packs through it. */
    //~ public static boolean startWorld(final File out) {
        //~ dir = out;
        //~ STAGES.add(new String[] {"", "auto"});
        //~ for (final String name : testPacks()) {
            //~ STAGES.add(new String[] {name, "off"});
            //~ STAGES.add(new String[] {name, "on"});
        //~ }
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ mc.options.renderDistance = 6;
        //~ mc.options.hideGui = true;
        //~ final Thread watchdog = new Thread(() -> {
            //~ try {
                //~ Thread.sleep(TOTAL_MS);
            //~ } catch (final InterruptedException ignored) {
                //~ return;
            //~ }
            //~ if (!finished) {
                //~ OUT.append("TIMEOUT during stage ").append(stage).append('\n');
                //~ finish();
            //~ }
        //~ }, "Aetherium shader smoke watchdog");
        //~ watchdog.setDaemon(true);
        //~ watchdog.start();
        //~ mc.tell(() -> {
            //~ try {
                //~ final RegistryAccess.RegistryHolder registries = RegistryAccess.builtin();
                //~ final WorldGenSettings gen = WorldGenSettings.demoSettings(registries);
                //~ final LevelSettings settings = new LevelSettings("aetherium-smoke", GameType.CREATIVE, false,
                        //~ Difficulty.PEACEFUL, true, new GameRules(), DataPackConfig.DEFAULT);
                //~ mc.createLevel("aetherium-smoke", settings, registries, gen);
            //~ } catch (final Throwable t) {
                //~ OUT.append("WORLD CREATION FAILED ").append(t).append('\n');
                //~ finish();
            //~ }
        //~ });
        //~ stage = -1;
        //~ return true;
    //~ }

    //~ /** Client tick after {@link #startWorld}. */
    //~ public static void tick() {
        //~ if (stage < -1 || finished) {
            //~ return;
        //~ }
        //~ final Minecraft mc = Minecraft.getInstance();
        //~ final long now = System.currentTimeMillis();
        //~ if (stage == -1) {
            //~ if (mc.level == null || mc.player == null || mc.screen != null) {
                //~ return;
            //~ }
            //~ if (worldSeen == 0L) {
                //~ worldSeen = now;
                //~ mc.player.chat("/time set 1000");
                //~ mc.player.chat("/gamerule doDaylightCycle false");
                //~ mc.player.chat("/weather clear 100000");
                //~ mc.player.xRot = 12.0F;
                //~ return;
            //~ }
            //~ if (now - worldSeen < WORLD_WAIT_MS) {
                //~ return;
            //~ }
            //~ next(mc, now);
            //~ return;
        //~ }
        //~ final int drawn = ShaderRuntime.frames - frameMark;
        //~ final long elapsed = now - stageStart;
        //~ if ((drawn >= 8 && elapsed >= STAGE_MIN_MS) || elapsed >= STAGE_MAX_MS) {
            //~ record(mc, drawn, elapsed);
            //~ next(mc, now);
        //~ }
    //~ }

    //~ private static void next(final Minecraft mc, final long now) {
        //~ stage++;
        //~ if (stage >= STAGES.size()) {
            //~ finish();
            //~ return;
        //~ }
        //~ final String[] s = STAGES.get(stage);
        //~ final AetheriumConfig config = Aetherium.config();
        //~ config.shaderPack.set(s[0]);
        //~ config.shaderCompat.set(s[1]);
        //~ ClientHooks.onSettingsApplied();
        //~ stageStart = now;
        //~ frameMark = ShaderRuntime.frames;
    //~ }

    //~ private static void record(final Minecraft mc, final int drawn, final long elapsed) {
        //~ final String[] s = STAGES.get(stage);
        //~ final String label = (s[0].isEmpty() ? "vanilla" : s[0]) + " [compat " + s[1] + "]";
        //~ final boolean running = ShaderRuntime.running();
        //~ final boolean ok = s[0].isEmpty() ? drawn > 0 : running && drawn > 0;
        //~ if (ok) {
            //~ drew++;
        //~ } else {
            //~ failed++;
        //~ }
        //~ OUT.append("=== ").append(label).append('\n');
        //~ OUT.append(ok ? "OK" : "FAILED").append(" frames ").append(drawn).append(" in ").append(elapsed / 1000)
                //~ .append(" s (").append(String.format(java.util.Locale.ROOT, "%.1f", drawn * 1000.0 / Math.max(1L, elapsed)))
                //~ .append(" fps), running ").append(running).append('\n');
        //~ OUT.append("status ").append(ShaderBridge.status()).append('\n');
        //~ final List<String> problems = ShaderBridge.problems();
        //~ OUT.append("problems ").append(problems.size()).append('\n');
        //~ for (int i = 0; i < Math.min(12, problems.size()); i++) {
            //~ OUT.append("  ").append(problems.get(i)).append('\n');
        //~ }
        //~ final ShaderPipeline pl = ShaderBridge.pipeline();
        //~ if (running && pl != null) {
            //~ try {
                //~ OUT.append("targets").append(pl.probe()).append('\n');
            //~ } catch (final Throwable t) {
                //~ OUT.append("targets FAILED ").append(t).append('\n');
            //~ }
        //~ }
        //~ final String shot = "shot-" + String.format("%02d", stage) + ".png";
        //~ OUT.append("screenshot ").append(shot).append(' ').append(screenshot(mc, new File(dir, shot))).append('\n');
        //~ write(new File(dir, "shaders-world.txt"), OUT.toString());
    //~ }

    //~ /** Half-size PNG of the last frame; returns the mean colour and spread (all-black = broken). */
    //~ private static String screenshot(final Minecraft mc, final File file) {
        //~ try {
            //~ final RenderTarget target = mc.getMainRenderTarget();
            //~ final int w = target.width;
            //~ final int h = target.height;
            //~ target.bindWrite(true);
            //~ final ByteBuffer px = BufferUtils.createByteBuffer(w * h * 4);
            //~ GL11.glReadPixels(0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, px);
            //~ final int sw = Math.max(1, w / 2);
            //~ final int sh = Math.max(1, h / 2);
            //~ final BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
            //~ long sum = 0;
            //~ long sumSq = 0;
            //~ long red = 0;
            //~ long yellow = 0;
            //~ long blue = 0;
            //~ for (int y = 0; y < sh; y++) {
                //~ for (int x = 0; x < sw; x++) {
                    //~ final int i = ((h - 1 - y * 2) * w + x * 2) * 4;
                    //~ final int r = px.get(i) & 255;
                    //~ final int g = px.get(i + 1) & 255;
                    //~ final int b = px.get(i + 2) & 255;
                    //~ img.setRGB(x, y, (r << 16) | (g << 8) | b);
                    //~ if (r > 200 && b < 60) {
                        //~ if (g < 60) {
                            //~ red++;
                        //~ } else if (g > 200) {
                            //~ yellow++;
                        //~ }
                    //~ } else if (b > 120 && r < 50 && g < 50) {
                        //~ blue++;
                    //~ }
                    //~ final int l = (r * 3 + g * 6 + b) / 10;
                    //~ sum += l;
                    //~ sumSq += (long) l * l;
                //~ }
            //~ }
            //~ ImageIO.write(img, "png", file);
            //~ final double n = (double) sw * sh;
            //~ final double mean = sum / n;
            //~ final double spread = Math.sqrt(Math.max(0.0, sumSq / n - mean * mean));
            //~ return String.format(java.util.Locale.ROOT, "luma mean %.0f spread %.0f red %.2f%% yellow %.2f%% blue %.2f%%",
                    //~ mean, spread, red * 100.0 / n, yellow * 100.0 / n, blue * 100.0 / n);
        //~ } catch (final Throwable t) {
            //~ return "FAILED " + t;
        //~ }
    //~ }

    //~ private static synchronized void finish() {
        //~ if (finished) {
            //~ return;
        //~ }
        //~ finished = true;
        //~ final String line = "shaders world " + drew + " drew, " + failed + " failed of " + STAGES.size() + " stages";
        //~ OUT.append(line).append('\n');
        //~ write(new File(dir, "shaders-world.txt"), OUT.toString());
        //~ try (Writer w = new OutputStreamWriter(new FileOutputStream(new File(dir, "report.txt"), true),
                //~ StandardCharsets.UTF_8)) {
            //~ w.write(line + "\n");
        //~ } catch (final Exception ignored) {
            //~ // shaders-world.txt has it
        //~ }
        //~ System.out.println("[Aetherium smoke] " + line);
        //~ Runtime.getRuntime().halt(0);
    //~ }

    //~ private static void write(final File file, final String text) {
        //~ try {
            //~ file.getParentFile().mkdirs();
            //~ try (Writer w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                //~ w.write(text);
            //~ }
        //~ } catch (final Exception e) {
            //~ System.out.println("[Aetherium smoke] could not write " + file + ": " + e);
        //~ }
    //~ }
    // @era:shader-end
}
