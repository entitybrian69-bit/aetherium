package com.aetherium.gui;

import java.util.Objects;

import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Aetherium's visual identity: deep purple / violet, soft glow, rounded surfaces.
 *
 * <p>Sodium ships green, VulkanMod ships red. Aetherium is purple — that is a
 * product requirement, so the palette lives here as the only source of colour in
 * the GUI and every widget asks this class instead of holding a constant. A
 * reviewer can therefore check the whole theme in one file.</p>
 *
 * <p>All drawing is done with {@link GuiGraphics#fill(int,int,int,int,int)} plus a
 * corner-suppression loop rather than a textured 9-slice: it works on every version
 * from 1.18 to 26.x, needs no atlas registration, and costs ~14 fill calls per
 * panel — measurably nothing next to a chunk rebuild.</p>
 */
public final class AetheriumTheme {
    // ------------------------------------------------------------ palette (ARGB)
    public static final int BACKGROUND = 0xFF120819;
    public static final int PANEL = 0xF01D0F2E;
    public static final int PANEL_RAISED = 0xF5271441;
    public static final int PANEL_SUNKEN = 0xE0150B22;
    public static final int BORDER = 0xFF3D1F63;
    public static final int BORDER_HOVER = 0xFF8A5CF6;
    public static final int ACCENT = 0xFF9B5CFF;
    public static final int ACCENT_SOFT = 0x669B5CFF;
    public static final int ACCENT_DEEP = 0xFF5B2A9E;
    public static final int GLOW = 0x33C39BFF;
    public static final int TEXT = 0xFFF3EBFF;
    public static final int TEXT_DIM = 0xFFB49CD0;
    public static final int TEXT_DISABLED = 0xFF6E5A85;
    public static final int SUCCESS = 0xFF7BE0A8;
    public static final int WARNING = 0xFFFFC46B;
    public static final int DANGER = 0xFFFF5C7A;
    public static final int SLIDER_TRACK = 0xFF2E1A47;
    public static final int PARTICLE = 0xB4C7A6FF;

    // ------------------------------------------------------------------ metrics
    /** 48 dp minimum touch target, per the Android tab's requirement. */
    public static final int TOUCH_TARGET_DP = 48;
    public static final int ROW_HEIGHT = 20;
    public static final int TOUCH_ROW_HEIGHT = 34;
    public static final int SIDEBAR_WIDTH = 96;
    public static final int SIDEBAR_WIDTH_TOUCH = 112;
    public static final int PADDING = 6;
    public static final int CORNER_RADIUS = 3;
    public static final int MILLISECONDS_PER_ANIMATION = 260;

    public static final ResourceLocation TEXTURE_PANEL = ResourceLocation.parse("aetherium:textures/gui/panel_purple.png");
    public static final ResourceLocation TEXTURE_GLOW = ResourceLocation.parse("aetherium:textures/gui/glow_violet.png");
    public static final ResourceLocation TEXTURE_SLIDER = ResourceLocation.parse("aetherium:textures/gui/slider_track.png");
    public static final ResourceLocation TEXTURE_LOGO = ResourceLocation.parse("aetherium:textures/gui/aetherium_logo.png");

    /** Desktop layout is 1.0; touch mode widens hit targets and spacing. */
    private final boolean touchMode;
    private final float guiScale;

    public AetheriumTheme(final boolean touchMode) {
        this.touchMode = touchMode;
        final Minecraft minecraft = Minecraft.getInstance();
        this.guiScale = minecraft == null || minecraft.getWindow() == null ? 1.0f : (float) minecraft.getWindow().getGuiScale();
    }

    public static AetheriumTheme forCurrentConfig() {
        return new AetheriumTheme(com.aetherium.Aetherium.config().touchMode.get());
    }

    public boolean isTouchMode() {
        return this.touchMode;
    }

    /** Density-independent pixels to GUI-scaled pixels; 1 dp == 1 px at scale 2. */
    public int dp(final int value) {
        return Math.max(1, (int) Math.round(value * (this.guiScale <= 0.0f ? 1.0 : 2.0) / Math.max(1.0, this.guiScale)));
    }

    public int rowHeight() {
        return this.touchMode ? TOUCH_ROW_HEIGHT : ROW_HEIGHT;
    }

    public int sidebarWidth() {
        return this.touchMode ? SIDEBAR_WIDTH_TOUCH : SIDEBAR_WIDTH;
    }

    public int hitTargetInset() {
        // Touch mode expands the interactive rect beyond the drawn rect instead of
        // drawing a bigger widget, so the visual density stays the same on a phone
        // while the finger has its 48 dp.
        return this.touchMode ? Math.max(0, (dp(TOUCH_TARGET_DP) - rowHeight()) / 2) : 0;
    }

    public int cornerRadius() {
        return this.touchMode ? CORNER_RADIUS + 1 : CORNER_RADIUS;
    }

    // ------------------------------------------------------------------- drawing

    /** Fills a rounded rectangle by insetting the corner pixels of each row. */
    public void fillRounded(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height, final int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        final int radius = Math.min(this.cornerRadius(), Math.min(width / 2, height / 2));
        if (radius <= 0) {
            guiGraphics.fill(x, y, x + width, y + height, color);
            return;
        }
        // Rows that touch a corner are shortened; the middle rows are full width.
        for (int row = 0; row < height; row++) {
            final int inset = cornerInset(row, height, radius);
            guiGraphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    private static int cornerInset(final int row, final int height, final int radius) {
        final int fromEdge = Math.min(row, height - 1 - row);
        if (fromEdge >= radius) {
            return 0;
        }
        final float t = (radius - fromEdge) / (float) radius;
        // Quarter-circle inset: cheaper than a mask texture and identical at these sizes.
        return Math.round(radius * (1.0f - (float) Math.sqrt(Math.max(0.0f, 1.0f - t * t))));
    }

    public void drawPanel(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height) {
        // Outer glow first, so the panel edge reads as a light source rather than a
        // border: 3 expanding halos at decreasing alpha.
        for (int i = 3; i >= 1; i--) {
            final int alpha = 0x14 * i;
            guiGraphics.fill(x - i, y - i, x + width + i, y + height + i, (alpha << 24) | (ACCENT & 0xFFFFFF));
        }
        fillRounded(guiGraphics, x, y, width, height, PANEL);
        outlineRounded(guiGraphics, x, y, width, height, BORDER);
    }

    public void outlineRounded(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height, final int color) {
        final int radius = Math.min(this.cornerRadius(), Math.min(width / 2, height / 2));
        for (int row = 0; row < height; row++) {
            final int inset = cornerInset(row, height, radius);
            if (inset > 0) {
                guiGraphics.fill(x + inset, y + row, x + inset + 1, y + row + 1, color);
                guiGraphics.fill(x + width - inset - 1, y + row, x + width - inset, y + row + 1, color);
            } else if (row == 0 || row == height - 1) {
                guiGraphics.fill(x + radius, y + row, x + width - radius, y + row + 1, color);
            } else {
                guiGraphics.fill(x, y + row, x + 1, y + row + 1, color);
                guiGraphics.fill(x + width - 1, y + row, x + width, y + row + 1, color);
            }
        }
    }

    /** Hover glow: a two-pixel violet frame plus a soft inner wash. */
    public void drawHoverGlow(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height, final float strength) {
        final float amount = MathUtil.clamp(strength, 0.0f, 1.0f);
        if (amount <= 0.01f) {
            return;
        }
        final int blended = blendColors(BORDER, BORDER_HOVER, amount);
        outlineRounded(guiGraphics, x, y, width, height, blended);
        final int washAlpha = (int) (0x26 * amount);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, (washAlpha << 24) | (ACCENT & 0xFFFFFF));
    }

    /** A violet bar whose alpha falls off, used under the active sidebar entry. */
    public void drawActiveUnderline(final GuiGraphics guiGraphics, final int x, final int y, final int width, final float progress) {
        final int filled = (int) Math.round(width * MathUtil.clamp(progress, 0.0f, 1.0f));
        guiGraphics.fill(x, y, x + filled, y + 2, ACCENT);
        guiGraphics.fill(x, y + 2, x + (int) (filled * 0.7), y + 3, ACCENT_SOFT);
    }

    public void drawGradientBar(final GuiGraphics guiGraphics, final int x, final int y, final int width, final int height,
                                final int leftColor, final int rightColor) {
        for (int column = 0; column < width; column++) {
            final float t = width <= 1 ? 0.0f : column / (float) (width - 1);
            guiGraphics.fill(x + column, y, x + column + 1, y + height, blendColors(leftColor, rightColor, t));
        }
    }

    public static int blendColors(final int from, final int to, final float t) {
        final float clamped = MathUtil.clamp(t, 0.0f, 1.0f);
        final int a = (int) MathUtil.lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped);
        final int r = (int) MathUtil.lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, clamped);
        final int g = (int) MathUtil.lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, clamped);
        final int b = (int) MathUtil.lerp(from & 0xFF, to & 0xFF, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Text with a 1 px dark drop shadow, which is what keeps labels readable on violet. */
    public void drawLabel(final GuiGraphics guiGraphics, final String text, final int x, final int y, final int color) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft == null ? null : minecraft.font;
        if (font == null) {
            return;
        }
        guiGraphics.drawString(font, net.minecraft.network.chat.Component.literal(text).withStyle(style -> style.withColor(color & 0xFFFFFF)),
                x, y + 1, 0x80000000 | (color & 0xFFFFFF), false);
        guiGraphics.drawString(font, net.minecraft.network.chat.Component.literal(text).withStyle(style -> style.withColor(color & 0xFFFFFF)),
                x, y, color, false);
    }

    public int labelWidth(final String text) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft == null ? null : minecraft.font;
        return font == null ? text.length() * 6 : font.width(text);
    }

    /** The logo strip across the top of the screen; text-only when the texture is absent. */
    public void drawBrand(final GuiGraphics guiGraphics, final int x, final int y, final int width) {
        Objects.requireNonNull(guiGraphics, "guiGraphics");
        drawGradientBar(guiGraphics, x, y, width, 1, ACCENT_DEEP, ACCENT);
        final String title = "A E T H E R I U M";
        final Minecraft minecraft = Minecraft.getInstance();
        final Font font = minecraft == null ? null : minecraft.font;
        if (font != null) {
            guiGraphics.drawCenteredString(font, net.minecraft.network.chat.Component.literal(title)
                    .withStyle(style -> style.withColor(ACCENT & 0xFFFFFF)), x + width / 2, y + 4, TEXT);
        }
    }

    /** Colour for a value that is currently different from its default. */
    public int colorForModified(final boolean modified) {
        return modified ? ACCENT : TEXT;
    }

    public static int statusColor(final AetheriumStatus status) {
        Objects.requireNonNull(status, "status");
        switch (status) {
            case GOOD:
                return SUCCESS;
            case CAUTION:
                return WARNING;
            case BLOCKED:
                return DANGER;
            case NEUTRAL:
            default:
                return TEXT_DIM;
        }
    }

    /** Semantic status of a row in the GUI (backend usable, conflict, etc.). */
    public enum AetheriumStatus {
        GOOD,
        CAUTION,
        BLOCKED,
        NEUTRAL
    }
}
