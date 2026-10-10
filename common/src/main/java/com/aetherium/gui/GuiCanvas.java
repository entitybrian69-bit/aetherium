package com.aetherium.gui;

/**
 * The drawing surface the Aetherium menu and HUD paint on.
 *
 * <p>Deliberately tiny: filled rectangles, unshadowed text, rectangular clips and an explicit
 * layer break. That is everything the design needs, and every one of the 33 supported versions
 * can do it with stock GUI calls ({@code GuiComponent.fill}/{@code Font.draw} up to 1.19.4,
 * {@code GuiGraphics} from 1.20, {@code GuiGraphicsExtractor} on 26.x). No textures, no shaders,
 * no custom GL state: the menu runs on anything that runs Minecraft (OpenGL 3.0-class hardware,
 * GL4ES/ANGLE/Zink on Android).</p>
 *
 * <p>The base class owns translation, global alpha and the clip stack so the version-specific
 * subclass ({@code com.aetherium.client.McCanvas}) only forwards five primitive calls. Offsets
 * are applied here rather than through a pose stack because the pose type itself changed three
 * times across the range.</p>
 */
public abstract class GuiCanvas {
    private static final int MAX_CLIP_DEPTH = 8;

    private int offsetX;
    private int offsetY;
    private float alpha = 1f;
    private final int[] clips = new int[MAX_CLIP_DEPTH * 4];
    private int clipDepth;
    private int fills;

    // ------------------------------------------------------------------ state

    public final void setOffset(final int dx, final int dy) {
        this.offsetX = dx;
        this.offsetY = dy;
    }

    public final void setAlpha(final float value) {
        this.alpha = value < 0f ? 0f : (value > 1f ? 1f : value);
    }

    public final float getAlpha() {
        return this.alpha;
    }

    /** Back to no offset, full opacity. Clips are left alone (they must be popped explicitly). */
    public final void reset() {
        this.offsetX = 0;
        this.offsetY = 0;
        this.alpha = 1f;
    }

    /** Fill calls since {@link #beginFrame()}, for the performance budget check in tests. */
    public final int fillCount() {
        return this.fills;
    }

    protected final void beginFrame() {
        this.fills = 0;
        this.clipDepth = 0;
        reset();
    }

    // ------------------------------------------------------------------ drawing

    public final void fill(final int x1, final int y1, final int x2, final int y2, final int argb) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        final int color = applyAlpha(argb);
        if ((color >>> 24) == 0) {
            return;
        }
        this.fills++;
        rawFill(x1 + this.offsetX, y1 + this.offsetY, x2 + this.offsetX, y2 + this.offsetY, color);
    }

    public final void text(final String text, final int x, final int y, final int argb) {
        if (text == null || text.isEmpty()) {
            return;
        }
        final int color = applyAlpha(argb);
        // Vanilla's font renderer treats alpha < 4 as fully opaque; skip instead of flashing.
        if ((color >>> 24) < 8) {
            return;
        }
        rawText(text, x + this.offsetX, y + this.offsetY, color);
    }

    public final int textWidth(final String text) {
        return text == null || text.isEmpty() ? 0 : rawTextWidth(text);
    }

    /**
     * Starts a new draw layer: everything drawn afterwards is guaranteed to be on top of
     * everything drawn before, including text. Needed from 1.21.6, where GUI drawing is deferred
     * and text in one layer is composited after that layer's rectangles.
     */
    public final void layer() {
        rawLayer();
    }

    /** Clip to the given rectangle (in current-offset coordinates), intersected with any outer clip. */
    public final void pushClip(final int x1, final int y1, final int x2, final int y2) {
        int cx1 = x1 + this.offsetX;
        int cy1 = y1 + this.offsetY;
        int cx2 = x2 + this.offsetX;
        int cy2 = y2 + this.offsetY;
        if (this.clipDepth > 0) {
            final int base = (this.clipDepth - 1) * 4;
            cx1 = Math.max(cx1, this.clips[base]);
            cy1 = Math.max(cy1, this.clips[base + 1]);
            cx2 = Math.min(cx2, this.clips[base + 2]);
            cy2 = Math.min(cy2, this.clips[base + 3]);
        }
        if (cx2 < cx1) {
            cx2 = cx1;
        }
        if (cy2 < cy1) {
            cy2 = cy1;
        }
        if (this.clipDepth >= MAX_CLIP_DEPTH) {
            throw new IllegalStateException("clip stack overflow");
        }
        final int base = this.clipDepth * 4;
        this.clips[base] = cx1;
        this.clips[base + 1] = cy1;
        this.clips[base + 2] = cx2;
        this.clips[base + 3] = cy2;
        this.clipDepth++;
        rawPushClip(cx1, cy1, cx2, cy2);
    }

    public final void popClip() {
        if (this.clipDepth == 0) {
            return;
        }
        this.clipDepth--;
        if (this.clipDepth > 0) {
            final int base = (this.clipDepth - 1) * 4;
            rawPopClip(true, this.clips[base], this.clips[base + 1], this.clips[base + 2], this.clips[base + 3]);
        } else {
            rawPopClip(false, 0, 0, 0, 0);
        }
    }

    private int applyAlpha(final int argb) {
        if (this.alpha >= 0.999f) {
            return argb;
        }
        final int a = (int) (((argb >>> 24) & 0xFF) * this.alpha + 0.5f);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    // ------------------------------------------------------------------ primitives

    protected abstract void rawFill(int x1, int y1, int x2, int y2, int argb);

    protected abstract void rawText(String text, int x, int y, int argb);

    protected abstract int rawTextWidth(String text);

    /** Already intersected with the outer clip, in absolute GUI coordinates. */
    protected abstract void rawPushClip(int x1, int y1, int x2, int y2);

    /**
     * Undo the innermost clip. {@code restore} says whether an outer clip remains (its rectangle
     * follows) — implementations with their own scissor stack can ignore the rectangle.
     */
    protected abstract void rawPopClip(boolean restore, int x1, int y1, int x2, int y2);

    protected void rawLayer() {
    }
}
