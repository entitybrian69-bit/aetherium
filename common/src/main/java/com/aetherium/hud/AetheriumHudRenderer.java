package com.aetherium.hud;

import com.aetherium.Aetherium;
import com.aetherium.client.ClientHooks;
import com.aetherium.config.AetheriumConfig;
import com.aetherium.render.gl.GlDevice;
import com.aetherium.render.mesh.ChunkMeshScheduler;
import com.aetherium.render.mesh.MeshCounters;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The purple performance overlay: corner FPS, 99th-percentile frame time, a spike
 * graph, and the backend tag.
 *
 * <p>Drawn with {@link GuiGraphics} only — no RenderLayer, no custom vertex format —
 * so it works identically on every version from 1.18 (when GuiGraphics appeared) to
 * 26.x, and on 1.16.5/1.17 the delta swaps the two {@code fill} calls for
 * {@code PoseStack}-based quads (see {@code deltas/1.16.5/changes.patch}).</p>
 *
 * <p>Cost discipline: this class allocates nothing per frame. The graph buffer is a
 * field, strings are built only when the display window elapses (4 Hz), and the
 * cache is invalidated by Minecraft's own resize, not by a listener.</p>
 */
public final class AetheriumHudRenderer {
    /** Purple identity: 0xFF8A3DF5-ish glow on 0x1D0F2E panel. */
    private static final int PANEL_COLOR = 0xE61D0F2E;
    private static final int PANEL_EDGE = 0xCC5B2A9E;
    private static final int ACCENT = 0xFF9B5CFF;
    private static final int TEXT = 0xFFF3EBFF;
    private static final int TEXT_DIM = 0xFFB49CD0;
    private static final int GRAPH_FILL = 0x669B5CFF;
    private static final int SPIKE_COLOR = 0xFFFF5C7A;

    private static final int GRAPH_WIDTH = 96;
    private static final int GRAPH_HEIGHT = 26;
    private static final double REFRESH_SECONDS = 0.25;

    /** Reused across frames: the graph readout is the only allocation source avoided. */
    private final double[] graphSamples = new double[480];

    private static final AetheriumHudRenderer INSTANCE = new AetheriumHudRenderer();

    private String cachedLines = "";
    private double cacheAge;
    private double lastFrameMs;
    private double cachedPeak;
    private int sampleCount;
    private int corner = -1;
    private int cachedWidth;
    private int cachedHeight;

    private AetheriumHudRenderer() {
    }

    public static void render(final GuiGraphics guiGraphics) {
        INSTANCE.renderInternal(guiGraphics);
    }

    /** Small always-on tag: "[Aetherium/GL46_DSA]" in the top-right, under the ping. */
    // [UNVERIFIED: the 6-arg GuiGraphics#drawString(Font, Component, int, int, int, boolean)
    // overload on 1.21.1 (the 5-arg form exists on 1.20.x); the delta for pre-1.20.2 rows swaps
    // it. GuiGraphics#guiWidth/guiHeight are read the same way.]
    public static void drawBackendTag(final GuiGraphics guiGraphics) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.font == null) {
            return;
        }
        final GlDevice device = ClientHooks.device();
        final int lineHeight = minecraft.font.lineHeight;
        int nextLineY = 2;

        if (Aetherium.config().showBackendTag.get()) {
            final String tag = "[Aetherium/"
                    + (device == null ? Aetherium.getState().name() : device.getBackend().getId()) + ']';
            final int width = minecraft.font.width(tag);
            final int x = guiGraphics.guiWidth() - width - 3;
            guiGraphics.fill(x - 2, 1, x + width + 2, lineHeight + 3, 0x991D0F2E);
            guiGraphics.drawString(minecraft.font, Component.literal(tag).withStyle(s -> s.withColor(ACCENT & 0xFFFFFF)), x, nextLineY, TEXT, false);
            nextLineY += lineHeight + 2;
        }

        // The conflict notice lives here rather than in a screen of its own: it has to be
        // visible on the title screen (where a stand-down is first noticed), it must not
        // require the FPS HUD to be enabled, and every piece of it - font, fill, drawString -
        // is already proven on this path. `general.notify_conflicts` switches it off for
        // people who run two renderers on purpose.
        if (Aetherium.config().notifyConflicts.get()) {
            final String notice = conflictNotice();
            if (!notice.isEmpty()) {
                final int width = Math.min(220, minecraft.font.width(notice));
                final int x = guiGraphics.guiWidth() - width - 5;
                guiGraphics.fill(x - 3, nextLineY - 2, guiGraphics.guiWidth() - 1,
                        nextLineY + lineHeight + 2, 0xCC2A0E1E);
                // Wrapped by hand: GuiGraphics has no word-wrap primitive on any version we
                // target, and a clipped sentence about a crash-adjacent problem is useless.
                drawWrappedNotice(guiGraphics, minecraft.font, notice, x, nextLineY);
            }
        }
    }

    /** @return the one-line notice, or empty when there is nothing to report. */
    private static String conflictNotice() {
        if (Aetherium.subsystemsOrNull() == null) {
            return "";
        }
        final com.aetherium.compat.ModConflictScanner scanner = Aetherium.conflicts();
        if (!scanner.hasAnything()) {
            return "";
        }
        if (scanner.requiresIncompatible()) {
            return "Aetherium is standing down: " + scanner.summarize();
        }
        return "Delegated to: " + scanner.summarize();
    }

    private static void drawWrappedNotice(final GuiGraphics guiGraphics, final Font font, final String notice,
                                          final int x, final int y) {
        final int maxWidth = 220;
        final StringBuilder line = new StringBuilder(notice.length());
        int cursorY = y;
        for (final String word : notice.split(" ")) {
            if (font.width(line + " " + word) > maxWidth && line.length() > 0) {
                guiGraphics.drawString(font, Component.literal(line.toString()), x, cursorY, TEXT, false);
                cursorY += font.lineHeight;
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        if (line.length() > 0) {
            guiGraphics.drawString(font, Component.literal(line.toString()), x, cursorY, TEXT, false);
        }
    }

    private void renderInternal(final GuiGraphics guiGraphics) {
        final AetheriumConfig config = Aetherium.config();
        final boolean fps = config.showFrameHud.get();
        final boolean graph = config.showFrameGraph.get();
        if (!fps && !graph) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft == null ? null : minecraft.font;
        if (font == null) {
            return;
        }
        final int scaledWidth = guiGraphics.guiWidth();
        final int scaledHeight = guiGraphics.guiHeight();
        if (scaledWidth != this.cachedWidth || scaledHeight != this.cachedHeight) {
            this.cachedWidth = scaledWidth;
            this.cachedHeight = scaledHeight;
            this.corner = -1;
        }
        final int cornerIndex = resolveCorner(config, scaledWidth);

        final FrameStats stats = Aetherium.frameStats();
        this.cacheAge += 1.0 / Math.max(1.0, stats.getFps());
        if (this.cacheAge >= REFRESH_SECONDS || this.cachedLines.isEmpty()) {
            this.cacheAge = 0.0;
            this.cachedLines = buildLines(stats);
            this.lastFrameMs = stats.getFrameMs();
        }

        final int lineHeight = font.lineHeight + 2;
        final int padding = 3;
        int textWidth = 0;
        for (final String line : this.cachedLines.split("\n")) {
            textWidth = Math.max(textWidth, font.width(line));
        }
        final int graphArea = graph ? GRAPH_WIDTH + padding : 0;
        final int panelWidth = Math.max(textWidth, graphArea) + padding * 2;
        final int lineCount = this.cachedLines.split("\n").length;
        final int panelHeight = lineCount * lineHeight + (graph ? GRAPH_HEIGHT + padding : 0) + padding * 2;

        final int x = cornerX(cornerIndex, scaledWidth, panelWidth);
        final int y = cornerY(cornerIndex, scaledHeight, panelHeight);

        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, PANEL_COLOR);
        drawEdges(guiGraphics, x, y, panelWidth, panelHeight);

        int cursorY = y + padding;
        for (final String line : this.cachedLines.split("\n")) {
            guiGraphics.drawString(font, Component.literal(line), x + padding, cursorY, TEXT, false);
            cursorY += lineHeight;
        }
        if (graph) {
            drawGraph(guiGraphics, stats, x + padding, cursorY + padding);
        }
    }

    private String buildLines(final FrameStats stats) {
        final StringBuilder builder = new StringBuilder(128);
        builder.append(String.format(java.util.Locale.ROOT, "%d fps  %.2f ms", (int) Math.round(stats.getFps()), stats.getFrameMs()));
        builder.append('\n');
        builder.append(String.format(java.util.Locale.ROOT, "p50 %.2f  p99 %.2f  p99.9 %.2f", stats.getP50Ms(), stats.getP99Ms(), stats.getP999Ms()));
        final GlDevice device = ClientHooks.device();
        if (device != null) {
            builder.append('\n').append(device.describe());
            builder.append('\n').append(device.getCapabilities().describe());
        }
        final ChunkMeshScheduler scheduler = ClientHooks.scheduler();
        final boolean debug = Aetherium.config().debugLogging.get();
        if (scheduler != null && debug) {
            builder.append('\n').append(scheduler.describe());
        }
        if (debug) {
            // Vanilla-side counters: "built" drifting above "dirty" is the overlap
            // signature, and it can only be seen if both halves are printed.
            final String meshLine = MeshCounters.formatLine();
            if (!meshLine.isEmpty()) {
                builder.append('\n').append(meshLine);
            }
        }
        builder.append('\n').append(Aetherium.gamma().describeCurve());
        if (stats.getSpikesOver100ms() > 0) {
            builder.append('\n').append(String.format(java.util.Locale.ROOT, "%d spikes > 100 ms", stats.getSpikesOver100ms()));
        }
        return builder.toString();
    }

    private void drawGraph(final GuiGraphics guiGraphics, final FrameStats stats, final int x, final int y) {
        final int count = stats.copyRecentGraph(this.graphSamples, this.graphSamples.length);
        this.sampleCount = count;
        double peak = Math.max(1.0, this.cachedPeak * 0.9);
        for (int i = 0; i < count; i++) {
            peak = Math.max(peak, this.graphSamples[i]);
        }
        this.cachedPeak = peak;
        guiGraphics.fill(x, y, x + GRAPH_WIDTH, y + GRAPH_HEIGHT, 0x66000000);
        // 16.67 ms reference line: a graph without a budget line cannot be read.
        final int budgetLine = y + GRAPH_HEIGHT - (int) (GRAPH_HEIGHT * Math.min(1.0, 16.67 / peak));
        guiGraphics.fill(x, budgetLine, x + GRAPH_WIDTH, budgetLine + 1, 0x33FFFFFF);
        final int bars = Math.max(1, Math.min(count, GRAPH_WIDTH));
        final int offset = Math.max(0, count - bars);
        for (int i = 0; i < bars; i++) {
            final double ms = this.graphSamples[offset + i];
            final int barHeight = (int) Math.max(1, GRAPH_HEIGHT * Math.min(1.0, ms / peak));
            final int color = ms > 50.0 ? SPIKE_COLOR : ms > 33.3 ? GRAPH_FILL : ACCENT;
            guiGraphics.fill(x + i, y + GRAPH_HEIGHT - barHeight, x + i + 1, y + GRAPH_HEIGHT, color);
        }
    }

    private static void drawEdges(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height) {
        guiGraphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, PANEL_EDGE);
        guiGraphics.fill(x, y, x + 1, y + height, PANEL_EDGE);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, PANEL_EDGE);
    }

    private static int resolveCorner(final AetheriumConfig config, final int scaledWidth) {
        // Corner comes from the GUI's dropdown; the string is parsed rather than
        // stored as an enum so a delta can add corners without a config migration.
        final String value = config.hudCorner.get();
        if (value == null) {
            return 0;
        }
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "top-right", "topright" -> 1;
            case "bottom-left", "bottomleft" -> 2;
            case "bottom-right", "bottomright" -> 3;
            default -> 0;
        };
    }

    private static int cornerX(final int corner, final int screenWidth, final int panelWidth) {
        return corner == 1 || corner == 3 ? screenWidth - panelWidth - 4 : 4;
    }

    private static int cornerY(final int corner, final int screenHeight, final int panelHeight) {
        return corner >= 2 ? screenHeight - panelHeight - 4 : 4;
    }

    /** Used by the GUI preview widget so the panel and the preview match exactly. */
    public static String previewText() {
        return INSTANCE.buildLines(Aetherium.frameStats());
    }

    public static int getGraphSampleCount() {
        return INSTANCE.sampleCount;
    }

    /** {@code ChatFormatting} keeps the colour codes out of the lang files. */
    public static Component colored(final String text, final ChatFormatting formatting) {
        return Component.literal(text).withStyle(formatting);
    }
}
