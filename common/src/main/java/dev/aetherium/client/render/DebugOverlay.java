package dev.aetherium.client.render;

import dev.aetherium.chunk.AetheriumChunkBuilder;
import dev.aetherium.config.AetheriumConfig;
import dev.aetherium.engine.AetheriumRenderEngine;
import dev.aetherium.engine.gl.GlBackend;
import dev.aetherium.frame.FrameTimeTracker;
import dev.aetherium.gui.theme.PurpleTheme;
import dev.aetherium.light.DynamicLightEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Top-left engine stats and bottom-left frame-time graph (both toggled from Advanced). */
public final class DebugOverlay {
    private DebugOverlay() {}

    public static void render(GuiGraphics g) {
        AetheriumConfig cfg = AetheriumConfig.get();
        if (!cfg.advanced.debugOverlay && !cfg.advanced.frameTimeGraph) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getDebugOverlay().showDebugScreen()) return;
        FrameTimeTracker ft = FrameTimeTracker.get();
        AetheriumRenderEngine engine = AetheriumRenderEngine.get();

        if (cfg.advanced.debugOverlay) {
            String[] lines = {
                    "Aetherium " + engine.statusLine(),
                    String.format("%.0f fps  avg %.2f ms  p99 %.2f ms", ft.fps(), ft.average(), ft.percentile(0.99)),
                    "chunk builder: " + AetheriumChunkBuilder.get().threads() + " thr, " + AetheriumChunkBuilder.get().queued() + " queued, "
                            + AetheriumChunkBuilder.get().pendingUploads() + " uploads",
                    "dynamic lights: " + DynamicLightEngine.get().sourceCount() + " sources",
                    engine.backend() instanceof GlBackend gl ? "hzb: " + gl.lastVisibleSections() + "/" + gl.lastSubmittedSections() + " sections visible" : ""
            };
            int y = 4;
            for (String l : lines) {
                if (l.isEmpty()) continue;
                g.fill(2, y - 1, 6 + mc.font.width(l), y + 9, 0x90120A22);
                g.drawString(mc.font, l, 4, y, PurpleTheme.ACCENT_BRIGHT, false);
                y += 10;
            }
        }
        if (cfg.advanced.frameTimeGraph) {
            int w = Math.min(ft.capacity(), g.guiWidth() / 2), h = 40;
            int x0 = 4, y0 = g.guiHeight() - 4;
            g.fill(x0 - 1, y0 - h - 1, x0 + w + 1, y0 + 1, 0xA00B0614);
            int line16 = y0 - (int) (h * 16.7f / 50f);
            g.fill(x0, line16, x0 + w, line16 + 1, PurpleTheme.ACCENT_DIM);
            for (int i = 0; i < w; i++) {
                float ms = ft.sample(w - 1 - i);
                int bar = Math.min(h, (int) (h * ms / 50f));
                int c = ms > 33 ? PurpleTheme.DANGER : ms > 16.7f ? PurpleTheme.WARNING : PurpleTheme.ACCENT;
                g.fill(x0 + i, y0 - bar, x0 + i + 1, y0, c);
            }
        }
    }
}
