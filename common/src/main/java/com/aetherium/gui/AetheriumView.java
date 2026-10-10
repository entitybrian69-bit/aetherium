package com.aetherium.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.aetherium.gui.AetheriumTheme.*;

/**
 * The Aetherium settings screen, minus every Minecraft type.
 *
 * <p>Layout, animation, drawing and input all live here and are identical in
 * every version delta; the per-version screen class only forwards render and
 * mouse calls, and {@link GuiCanvas} is the single file that touches the
 * game's drawing API. Everything is drawn with rectangle fills and the
 * vanilla font (no textures, no blur, no widgets), and the screen paints its
 * own opaque background, so vanilla never renders the world or a blurred menu
 * background behind it: on weak GPUs the settings screen is one of the
 * cheapest screens in the game.</p>
 */
public final class AetheriumView {
    private static final int DOTS = 28;
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_DOWN = 264;
    private static final int KEY_UP = 265;
    private static final int KEY_PAGE_DOWN = 267;
    private static final int KEY_PAGE_UP = 266;

    private final List<Page> pages;
    private final ScreenHost host;
    private final Setting preset;

    private int width = 320;
    private int height = 240;
    private int selected;

    private final Anim openAnim = new Anim(0f, 9f);
    private final Anim tabAnim = new Anim(0f, 16f);
    private final Anim contentAnim = new Anim(0f, 11f);
    private final Anim scrollAnim = new Anim(0f, 18f);
    private final Anim toastAnim = new Anim(0f, 2.2f);
    private final Anim applyHover = new Anim(0f, 18f);
    private final Anim doneHover = new Anim(0f, 18f);
    private final Anim[] tabHover;

    private float scrollTarget;
    private int maxScroll;
    private Setting dragging;
    private boolean scrollDragging;
    private double dragStartY;
    private float dragStartScroll;
    private Setting dropdown;
    private Setting closingDropdown;
    private Setting hovered;
    private String toastText = "";
    private long lastNanos;
    private float clock;
    private int fpsShown = -1;
    private float fpsTimer;

    private final float[] dotX = new float[DOTS];
    private final float[] dotY = new float[DOTS];
    private final float[] dotSpeed = new float[DOTS];
    private final int[] dotSize = new int[DOTS];

    // Layout, recomputed every frame (cheap) so resizes need no bookkeeping.
    private int margin;
    private int headerY;
    private int headerH;
    private int bodyTop;
    private int bodyBottom;
    private int footerY;
    private int sideX;
    private int sideW;
    private int tabH;
    private int tabGap;
    private boolean compact;
    private int panelX1;
    private int panelY1;
    private int panelX2;
    private int panelY2;
    private int rowsTop;
    private int rowsBottom;
    private int rowH;
    private int ctrlW;
    private int btnY;
    private int btnH;
    private int applyX1;
    private int applyX2;
    private int doneX1;
    private int doneX2;

    public AetheriumView(final List<Page> pages, final ScreenHost host, final int initialPage) {
        this.pages = new ArrayList<Page>(pages);
        this.host = host;
        this.tabHover = new Anim[this.pages.size()];
        for (int i = 0; i < this.tabHover.length; i++) {
            this.tabHover[i] = new Anim(0f, 18f);
        }
        this.selected = Math.max(0, Math.min(this.pages.size() - 1, initialPage));
        this.tabAnim.snap(this.selected);
        this.contentAnim.setTarget(1f);
        this.openAnim.setTarget(1f);
        this.preset = find("general.preset");
        final Random random = new Random(0xAE7E21L);
        for (int i = 0; i < DOTS; i++) {
            this.dotX[i] = random.nextFloat();
            this.dotY[i] = random.nextFloat();
            this.dotSpeed[i] = 0.006f + random.nextFloat() * 0.014f;
            this.dotSize[i] = 1 + random.nextInt(2);
        }
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                setting.load();
            }
        }
    }

    public Setting find(final String id) {
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                if (setting.id.equals(id)) {
                    return setting;
                }
            }
        }
        return null;
    }

    public int selectedPage() {
        return this.selected;
    }

    public void resize(final int newWidth, final int newHeight) {
        this.width = Math.max(1, newWidth);
        this.height = Math.max(1, newHeight);
        layout();
        clampScroll();
    }

    // ------------------------------------------------------------------ layout

    private void layout() {
        final boolean touch = this.host.touchMode();
        this.margin = this.width >= 480 ? 10 : 5;
        this.headerY = this.margin;
        this.headerH = 20;
        this.footerY = this.height - this.margin - 9;
        this.bodyTop = this.headerY + this.headerH + 4;
        this.bodyBottom = this.footerY - 5;
        this.compact = this.width < 400;
        this.sideW = this.compact ? 26 : clamp(this.width / 6, 84, 104);
        this.sideX = this.margin;
        final int count = Math.max(1, this.pages.size());
        this.tabGap = 3;
        this.tabH = clamp((this.bodyBottom - this.bodyTop - (count - 1) * this.tabGap) / count, 14, touch ? 28 : 24);
        this.panelX1 = this.sideX + this.sideW + 6;
        this.panelX2 = this.width - this.margin;
        this.panelY1 = this.bodyTop;
        this.panelY2 = this.bodyBottom;
        this.rowH = touch ? 24 : 20;
        this.btnH = touch ? 20 : 16;
        this.btnY = this.panelY2 - 6 - this.btnH;
        this.rowsTop = this.panelY1 + 34;
        this.rowsBottom = this.btnY - 5;
        this.ctrlW = clamp((this.panelX2 - this.panelX1) * 45 / 100, 70, 150);
        this.doneX2 = this.panelX2 - 8;
        this.doneX1 = this.doneX2 - 46;
        this.applyX2 = this.doneX1 - 5;
        this.applyX1 = this.applyX2 - 46;
        final int content = currentRows().size() * this.rowH;
        this.maxScroll = Math.max(0, content - (this.rowsBottom - this.rowsTop));
    }

    private List<Setting> currentRows() {
        return this.pages.isEmpty() ? new ArrayList<Setting>() : this.pages.get(this.selected).rows();
    }

    private int ctrlX2() {
        return this.panelX2 - 10;
    }

    private int ctrlX1() {
        return ctrlX2() - this.ctrlW;
    }

    private int rowTop(final int index) {
        return this.rowsTop + index * this.rowH - Math.round(this.scrollAnim.get());
    }

    private int tabTop(final int index) {
        return this.bodyTop + index * (this.tabH + this.tabGap);
    }

    // ------------------------------------------------------------------ render

    public void render(final GuiCanvas canvas, final int mouseX, final int mouseY) {
        final long now = System.nanoTime();
        final float dt = this.lastNanos == 0L ? 0f : Math.min(0.25f, (now - this.lastNanos) / 1.0e9f);
        this.lastNanos = now;
        this.clock += dt;
        layout();
        tick(dt, mouseX, mouseY);

        canvas.reset();
        canvas.fill(0, 0, this.width, this.height, BACKGROUND);
        final float open = Anim.easeOut(this.openAnim.get());
        drawDots(canvas, dt, open);

        canvas.setAlpha(open);
        final int lift = Math.round((1f - open) * 10f);
        canvas.setOffset(0, lift);
        drawHeader(canvas);
        drawSidebar(canvas);
        drawPanel(canvas, mouseX, mouseY, lift);
        drawFooter(canvas);
        drawDropdown(canvas, mouseX, mouseY);
        canvas.reset();
    }

    private void tick(final float dt, final int mouseX, final int mouseY) {
        this.openAnim.tick(dt);
        this.tabAnim.setTarget(this.selected);
        this.tabAnim.tick(dt);
        this.contentAnim.tick(dt);
        this.scrollAnim.setTarget(this.scrollTarget);
        this.scrollAnim.tick(dt);
        this.toastAnim.setTarget(0f);
        this.toastAnim.tick(dt);
        this.applyHover.setTarget(inside(mouseX, mouseY, this.applyX1, this.btnY, this.applyX2, this.btnY + this.btnH) ? 1f : 0f);
        this.applyHover.tick(dt);
        this.doneHover.setTarget(inside(mouseX, mouseY, this.doneX1, this.btnY, this.doneX2, this.btnY + this.btnH) ? 1f : 0f);
        this.doneHover.tick(dt);
        for (int i = 0; i < this.tabHover.length; i++) {
            final int top = tabTop(i);
            this.tabHover[i].setTarget(i != this.selected && inside(mouseX, mouseY, this.sideX, top, this.sideX + this.sideW, top + this.tabH) ? 1f : 0f);
            this.tabHover[i].tick(dt);
        }

        this.fpsTimer -= dt;
        if (this.fpsTimer <= 0f || this.fpsShown < 0) {
            this.fpsTimer = 0.25f;
            this.fpsShown = Math.max(0, this.host.currentFps());
        }

        this.hovered = null;
        final List<Setting> rows = currentRows();
        if (this.dropdown == null && mouseX >= this.panelX1 && mouseX < this.panelX2 && mouseY >= this.rowsTop && mouseY < this.rowsBottom) {
            final int index = (mouseY - this.rowsTop + Math.round(this.scrollAnim.get())) / this.rowH;
            if (index >= 0 && index < rows.size()) {
                this.hovered = rows.get(index);
            }
        }
        if (this.dragging != null) {
            this.hovered = this.dragging;
        }
        for (final Setting setting : rows) {
            setting.hover.setTarget(setting == this.hovered && setting.isAvailable() && setting.kind != Setting.Kind.INFO ? 1f : 0f);
            setting.hover.tick(dt);
            if (setting.pending >= 0) {
                setting.knob.setTarget(setting.pending);
            }
            setting.knob.tick(dt);
            setting.open.setTarget(setting == this.dropdown ? 1f : 0f);
            setting.open.tick(dt);
        }
        if (this.closingDropdown != null && this.closingDropdown.open.get() <= 0.01f) {
            this.closingDropdown = null;
        }
    }

    private void drawDots(final GuiCanvas canvas, final float dt, final float open) {
        final int color = fade(BACKGROUND_DOT, open);
        for (int i = 0; i < DOTS; i++) {
            this.dotY[i] -= this.dotSpeed[i] * dt;
            if (this.dotY[i] < 0f) {
                this.dotY[i] += 1f;
            }
            final int x = (int) (this.dotX[i] * this.width + Math.sin(this.clock * 0.6 + i) * 4.0);
            final int y = (int) (this.dotY[i] * this.height);
            canvas.fill(x, y, x + this.dotSize[i], y + this.dotSize[i], color);
        }
    }

    private void drawHeader(final GuiCanvas canvas) {
        final int scale = this.height >= 260 && !this.compact ? 2 : 1;
        final int titleY = this.headerY + (this.headerH - 7 * scale) / 2;
        final int titleX = this.margin + 2;
        PixelArt.drawText(canvas, "AETHERIUM", titleX + scale, titleY + scale, scale, 0x22000000);
        PixelArt.drawText(canvas, "AETHERIUM", titleX, titleY, scale, INK);

        final String fpsText = "FPS: " + this.fpsShown;
        final int badgeW = canvas.textWidth(fpsText) + 14;
        final int badgeH = 14;
        final int x2 = this.width - this.margin;
        final int x1 = x2 - badgeW;
        final int y1 = this.headerY + (this.headerH - badgeH) / 2;
        final int color = this.fpsShown >= 50 ? BADGE_GOOD : (this.fpsShown >= 30 ? BADGE_OK : BADGE_BAD);
        roundRect(canvas, x1, y1, x2, y1 + badgeH, color);
        final float pulse = 0.55f + 0.45f * (float) Math.abs(Math.sin(this.clock * 2.0));
        canvas.fill(x1 + 4, y1 + 6, x1 + 6, y1 + 8, fade(0xFFFFFFFF, pulse));
        canvas.text(fpsText, x1 + 9, y1 + 3, 0xFFFFFFFF);
    }

    private void drawSidebar(final GuiCanvas canvas) {
        for (int i = 0; i < this.pages.size(); i++) {
            final int top = tabTop(i);
            final int x1 = this.sideX;
            final int x2 = this.sideX + this.sideW;
            final int bottom = top + this.tabH;
            canvas.fill(x1, top, x2, bottom, mix(TAB_IDLE, TAB_HOVER, this.tabHover[i].get()));
            canvas.fill(x1, top, x2, top + 1, TAB_LIGHT_EDGE);
            canvas.fill(x1, top, x1 + 1, bottom, TAB_LIGHT_EDGE);
            canvas.fill(x1, bottom - 1, x2, bottom, TAB_DARK_EDGE);
            canvas.fill(x2 - 1, top, x2, bottom, TAB_DARK_EDGE);
        }
        // Sliding selection: drawn once at the animated position, over the idle tabs.
        final int selTop = Math.round(this.bodyTop + this.tabAnim.get() * (this.tabH + this.tabGap));
        canvas.fill(this.sideX, selTop, this.sideX + this.sideW, selTop + this.tabH, INK);
        canvas.fill(this.sideX + 1, selTop + 1, this.sideX + this.sideW - 1, selTop + this.tabH - 1, SURFACE);
        canvas.fill(this.sideX + 1, selTop + this.tabH - 1, this.sideX + this.sideW - 1, selTop + this.tabH, INK);

        for (int i = 0; i < this.pages.size(); i++) {
            final Page page = this.pages.get(i);
            final int top = tabTop(i);
            final boolean active = i == this.selected;
            final int iconX = this.compact ? this.sideX + (this.sideW - 12) / 2 : this.sideX + 6;
            final int iconY = top + (this.tabH - 12) / 2;
            page.icon.draw(canvas, iconX, iconY, 1, active ? INK : INK_SOFT);
            if (!this.compact) {
                canvas.text(fit(canvas, page.name, this.sideW - 26), this.sideX + 22, top + (this.tabH - 8) / 2, active ? INK : INK_SOFT);
            }
        }
    }

    private void drawPanel(final GuiCanvas canvas, final int mouseX, final int mouseY, final int lift) {
        canvas.fill(this.panelX1 + 2, this.panelY2, this.panelX2 + 1, this.panelY2 + 2, SURFACE_SHADOW);
        canvas.fill(this.panelX1, this.panelY1, this.panelX2, this.panelY2, SURFACE_BORDER);
        canvas.fill(this.panelX1 + 1, this.panelY1 + 1, this.panelX2 - 1, this.panelY2 - 1, SURFACE);
        if (this.pages.isEmpty()) {
            return;
        }
        final Page page = this.pages.get(this.selected);
        final float base = canvas.getAlpha();
        final float content = Anim.easeOut(this.contentAnim.get());
        final int slide = Math.round((1f - content) * 14f);

        canvas.setAlpha(base * content);
        canvas.setOffset(slide, lift);
        final int textX = this.panelX1 + 10;
        canvas.text(page.title, textX, this.panelY1 + 8, INK);
        canvas.text(page.title, textX + 1, this.panelY1 + 8, INK);
        canvas.text(fit(canvas, page.subtitle, this.panelX2 - textX - 10), textX, this.panelY1 + 20, MUTED);
        canvas.setOffset(0, lift);
        canvas.setAlpha(base);
        canvas.fill(this.panelX1 + 8, this.panelY1 + 31, this.panelX2 - 8, this.panelY1 + 32, SURFACE_BORDER);

        canvas.pushClip(this.panelX1 + 1, this.rowsTop, this.panelX2 - 1, this.rowsBottom);
        canvas.setAlpha(base * content);
        canvas.setOffset(slide, lift);
        final List<Setting> rows = page.rows();
        for (int i = 0; i < rows.size(); i++) {
            final int top = rowTop(i);
            if (top + this.rowH < this.rowsTop || top > this.rowsBottom) {
                continue;
            }
            drawRow(canvas, rows.get(i), top, mouseX, mouseY);
        }
        canvas.setOffset(0, lift);
        canvas.setAlpha(base);
        canvas.popClip();

        if (this.maxScroll > 0) {
            final int trackH = this.rowsBottom - this.rowsTop;
            final int contentH = trackH + this.maxScroll;
            final int thumbH = Math.max(12, trackH * trackH / contentH);
            final int thumbY = this.rowsTop + Math.round((trackH - thumbH) * (this.scrollAnim.get() / this.maxScroll));
            canvas.fill(this.panelX2 - 4, this.rowsTop, this.panelX2 - 2, this.rowsBottom, DIVIDER);
            canvas.fill(this.panelX2 - 4, thumbY, this.panelX2 - 2, thumbY + thumbH, FAINT);
        }

        drawButtonBar(canvas);
    }

    private void drawRow(final GuiCanvas canvas, final Setting setting, final int top, final int mouseX, final int mouseY) {
        final float hover = setting.hover.get();
        if (hover > 0.01f) {
            canvas.fill(this.panelX1 + 1, top, this.panelX2 - 1, top + this.rowH, fade(ROW_HOVER, hover));
        }
        canvas.fill(this.panelX1 + 10, top + this.rowH - 1, this.panelX2 - 10, top + this.rowH, DIVIDER);
        final boolean available = setting.isAvailable();
        final float base = canvas.getAlpha();
        if (!available) {
            canvas.setAlpha(base * 0.45f);
        }
        final int cx1 = ctrlX1();
        final int cx2 = ctrlX2();
        final int textY = top + (this.rowH - 8) / 2;
        final int labelLimit = (setting.kind == Setting.Kind.INFO || setting.kind == Setting.Kind.BUTTON
                || setting.kind == Setting.Kind.TOGGLE ? cx2 - 60 : cx1 - 6) - (this.panelX1 + 10);
        canvas.text(fit(canvas, setting.label, labelLimit), this.panelX1 + 10, textY, INK);

        switch (setting.kind) {
            case TOGGLE:
                drawToggle(canvas, setting, top, cx2);
                break;
            case SEGMENTED:
                drawSegmented(canvas, setting, top, cx1, cx2, mouseX, mouseY);
                break;
            case DROPDOWN:
                drawDropdownHeader(canvas, setting, top, cx1, cx2, mouseX, mouseY);
                break;
            case SLIDER:
                drawSlider(canvas, setting, top, cx1, cx2);
                break;
            case INFO: {
                final String text = fit(canvas, setting.formatted(), cx2 - (this.panelX1 + 10) - canvas.textWidth(setting.label) - 12);
                canvas.text(text, cx2 - canvas.textWidth(text), textY, MUTED);
                break;
            }
            case BUTTON:
                drawActionButton(canvas, setting, top, cx2, mouseX, mouseY);
                break;
            default:
                break;
        }
        canvas.setAlpha(base);
    }

    private void drawToggle(final GuiCanvas canvas, final Setting setting, final int top, final int cx2) {
        final int pillW = 40;
        final int pillH = 14;
        final int x1 = cx2 - pillW;
        final int y1 = top + (this.rowH - pillH) / 2;
        final float k = clamp01(setting.knob.get());
        roundRect(canvas, x1, y1, cx2, y1 + pillH, mix(TOGGLE_OFF, TOGGLE_ON, k));
        final int knobX = x1 + 2 + Math.round(k * (pillW - 14));
        roundRect(canvas, knobX, y1 + 2, knobX + 10, y1 + 12, 0xFFFFFFFF);
        if (k >= 0.5f) {
            canvas.text("ON", x1 + 5, y1 + 3, fade(0xFFFFFFFF, (k - 0.5f) * 2f));
        } else {
            canvas.text("OFF", cx2 - 5 - canvas.textWidth("OFF"), y1 + 3, fade(INK_SOFT, (0.5f - k) * 2f));
        }
    }

    private void drawSegmented(final GuiCanvas canvas, final Setting setting, final int top, final int cx1, final int cx2,
                               final int mouseX, final int mouseY) {
        final int h = 16;
        final int y1 = top + (this.rowH - h) / 2;
        final int count = Math.max(1, setting.choices.length);
        final int seg = (cx2 - cx1) / count;
        canvas.fill(cx1, y1, cx2, y1 + h, CONTROL_TRACK_EDGE);
        canvas.fill(cx1 + 1, y1 + 1, cx2 - 1, y1 + h - 1, CONTROL_TRACK);
        if (setting.isAvailable() && inside(mouseX, mouseY, cx1, y1, cx2, y1 + h) && this.dropdown == null) {
            final int hoverIndex = Math.min(count - 1, (mouseX - cx1) / Math.max(1, seg));
            if (hoverIndex != setting.pending) {
                final int hx = cx1 + hoverIndex * seg;
                canvas.fill(hx + 1, y1 + 1, hoverIndex == count - 1 ? cx2 - 1 : hx + seg - 1, y1 + h - 1, TAB_HOVER);
            }
        }
        if (setting.pending >= 0) {
            final float position = Math.max(0f, Math.min(count - 1, setting.knob.get()));
            final int sx = cx1 + Math.round(position * seg);
            final int ex = setting.pending == count - 1 && position > count - 1.01f ? cx2 - 1 : sx + seg - 1;
            canvas.fill(sx + 1, y1 + 1, ex, y1 + h - 1, CONTROL_DARK);
        }
        for (int i = 0; i < count; i++) {
            final int x = cx1 + i * seg;
            final String text = fit(canvas, setting.choices[i], seg - 4);
            final int tx = x + (seg - canvas.textWidth(text)) / 2;
            canvas.text(text, tx, y1 + 4, i == setting.pending ? 0xFFFFFFFF : INK_SOFT);
        }
    }

    private void drawDropdownHeader(final GuiCanvas canvas, final Setting setting, final int top, final int cx1, final int cx2,
                                    final int mouseX, final int mouseY) {
        final int h = 16;
        final int y1 = top + (this.rowH - h) / 2;
        final boolean over = setting.isAvailable() && inside(mouseX, mouseY, cx1, y1, cx2, y1 + h);
        roundRect(canvas, cx1, y1, cx2, y1 + h, over || setting == this.dropdown ? CONTROL_DARK_HOVER : CONTROL_DARK);
        canvas.text(fit(canvas, setting.formatted(), cx2 - cx1 - 22), cx1 + 6, y1 + 4, 0xFFFFFFFF);
        PixelArt.CHEVRON.draw(canvas, cx2 - 11, y1 + 7, 1, 0xFFFFFFFF);
    }

    private void drawSlider(final GuiCanvas canvas, final Setting setting, final int top, final int cx1, final int cx2) {
        final String value = setting.formatted();
        int valueW = Math.max(30, canvas.textWidth(value));
        if (setting.format != null) {
            valueW = Math.max(valueW, Math.max(canvas.textWidth(setting.format.apply(setting.min)), canvas.textWidth(setting.format.apply(setting.max))));
        }
        final int trackX1 = cx1;
        final int trackX2 = Math.max(trackX1 + 20, cx2 - valueW - 6);
        setting.sliderX1 = trackX1;
        setting.sliderX2 = trackX2;
        canvas.text(value, cx2 - canvas.textWidth(value), top + (this.rowH - 8) / 2, INK_SOFT);
        final int mid = top + this.rowH / 2;
        canvas.fill(trackX1, mid - 2, trackX2, mid + 2, CONTROL_TRACK);
        final float t = clamp01((setting.knob.get() - setting.min) / (float) (setting.max - setting.min));
        final int knobX = trackX1 + Math.round(t * (trackX2 - trackX1));
        canvas.fill(trackX1, mid - 2, knobX, mid + 2, CONTROL_DARK);
        final boolean active = setting == this.dragging || setting.hover.get() > 0.5f;
        final int half = active ? 4 : 3;
        canvas.fill(knobX - half, mid - 6, knobX + half, mid + 6, CONTROL_DARK);
        canvas.fill(knobX - half + 1, mid - 5, knobX + half - 1, mid + 5, 0xFFFFFFFF);
    }

    private void drawActionButton(final GuiCanvas canvas, final Setting setting, final int top, final int cx2,
                                  final int mouseX, final int mouseY) {
        final int w = canvas.textWidth(setting.buttonText) + 16;
        final int h = 16;
        final int x1 = cx2 - w;
        final int y1 = top + (this.rowH - h) / 2;
        final boolean over = setting.isAvailable() && inside(mouseX, mouseY, x1, y1, cx2, y1 + h);
        canvas.fill(x1, y1, cx2, y1 + h, INK);
        canvas.fill(x1 + 1, y1 + 1, cx2 - 1, y1 + h - 1, over ? TAB_IDLE : SURFACE);
        canvas.text(setting.buttonText, x1 + 8, y1 + 4, INK);
    }

    private void drawButtonBar(final GuiCanvas canvas) {
        boolean dirty = false;
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                if (setting.isDirty()) {
                    dirty = true;
                    break;
                }
            }
        }
        final int textY = this.btnY + (this.btnH - 8) / 2;
        // Apply: outlined, live only when something is staged.
        final float ah = this.applyHover.get();
        canvas.fill(this.applyX1, this.btnY, this.applyX2, this.btnY + this.btnH, dirty ? INK : SURFACE_BORDER);
        canvas.fill(this.applyX1 + 1, this.btnY + 1, this.applyX2 - 1, this.btnY + this.btnH - 1, mix(SURFACE, TAB_IDLE, dirty ? ah : 0f));
        final String apply = "Apply";
        canvas.text(apply, this.applyX1 + (this.applyX2 - this.applyX1 - canvas.textWidth(apply)) / 2, textY, dirty ? INK : FAINT);
        // Done: filled.
        roundRect(canvas, this.doneX1, this.btnY, this.doneX2, this.btnY + this.btnH, mix(CONTROL_DARK, CONTROL_DARK_HOVER, this.doneHover.get()));
        final String done = "Done";
        canvas.text(done, this.doneX1 + (this.doneX2 - this.doneX1 - canvas.textWidth(done)) / 2, textY, 0xFFFFFFFF);

        final int descX = this.panelX1 + 10;
        final int descLimit = this.applyX1 - descX - 8;
        final float toast = this.toastAnim.get();
        if (toast > 0.03f) {
            final int color = fade(ACCENT, Math.min(1f, toast * 2f));
            PixelArt.CHECK.draw(canvas, descX, textY + 1, 1, color);
            canvas.text(fit(canvas, this.toastText, descLimit - 10), descX + 10, textY, color);
            return;
        }
        String description;
        if (this.hovered != null) {
            description = this.hovered.isAvailable() ? this.hovered.description : this.hovered.unavailableReason();
        } else {
            description = dirty ? "Unsaved changes - press Apply or Done." : "";
        }
        if (description != null && !description.isEmpty()) {
            canvas.text(fit(canvas, description, descLimit), descX, textY, MUTED);
        }
    }

    private void drawFooter(final GuiCanvas canvas) {
        final String right = this.host.footerRight();
        final String left = this.host.footerLeft();
        final int rightW = Math.min(canvas.textWidth(right), (this.width - 2 * this.margin) / 2);
        final String rightFit = fit(canvas, right, rightW);
        canvas.text(rightFit, this.width - this.margin - canvas.textWidth(rightFit), this.footerY, MUTED);
        canvas.text(fit(canvas, left, this.width - 2 * this.margin - rightW - 12), this.margin, this.footerY, MUTED);
    }

    private void drawDropdown(final GuiCanvas canvas, final int mouseX, final int mouseY) {
        final Setting setting = this.dropdown != null ? this.dropdown : this.closingDropdown;
        if (setting == null) {
            return;
        }
        final float open = Anim.easeOut(setting.open.get());
        if (open <= 0.01f) {
            return;
        }
        final int[] box = dropdownBox(setting);
        if (box == null) {
            return;
        }
        // The list covers row text; on 1.21.6+ text composites after rectangles within a layer.
        canvas.layer();
        final int itemH = 14;
        final int visible = Math.round((box[3] - box[1]) * open);
        final boolean upward = box[4] == 1;
        final int clipY1 = upward ? box[3] - visible : box[1];
        final int clipY2 = upward ? box[3] : box[1] + visible;
        canvas.pushClip(box[0], clipY1, box[2], clipY2);
        canvas.fill(box[0] + 1, box[3], box[2] + 1, box[3] + 2, SURFACE_SHADOW);
        canvas.fill(box[0], box[1], box[2], box[3], INK);
        canvas.fill(box[0] + 1, box[1] + 1, box[2] - 1, box[3] - 1, SURFACE);
        for (int i = 0; i < setting.choices.length; i++) {
            final int y = box[1] + 1 + i * itemH;
            final boolean over = this.dropdown != null && inside(mouseX, mouseY, box[0], y, box[2], y + itemH);
            if (over) {
                canvas.fill(box[0] + 1, y, box[2] - 1, y + itemH, TAB_IDLE);
            }
            final boolean chosen = i == setting.pending;
            if (chosen) {
                PixelArt.CHECK.draw(canvas, box[0] + 5, y + 5, 1, ACCENT);
            }
            canvas.text(fit(canvas, setting.choices[i], box[2] - box[0] - 22), box[0] + 15, y + 3, chosen ? INK : INK_SOFT);
        }
        canvas.popClip();
    }

    /** x1, y1, x2, y2, upward(0/1) of the open dropdown list, or null when the row is off screen. */
    private int[] dropdownBox(final Setting setting) {
        final List<Setting> rows = currentRows();
        final int index = rows.indexOf(setting);
        if (index < 0) {
            return null;
        }
        final int top = rowTop(index);
        final int headerH = 16;
        final int listH = setting.choices.length * 14 + 2;
        final int below = top + (this.rowH + headerH) / 2 + 1;
        int upward = 0;
        int y1 = below;
        if (below + listH > this.height - 2) {
            upward = 1;
            y1 = top + (this.rowH - headerH) / 2 - 1 - listH;
        }
        return new int[]{ctrlX1(), y1, ctrlX2(), y1 + listH, upward};
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button != 0) {
            return false;
        }
        layout();
        final int x = (int) Math.floor(mouseX);
        final int y = (int) Math.floor(mouseY);
        if (this.dropdown != null) {
            final int[] box = dropdownBox(this.dropdown);
            if (box != null && inside(x, y, box[0], box[1], box[2], box[3])) {
                final int index = (y - box[1] - 1) / 14;
                if (index >= 0 && index < this.dropdown.choices.length && this.dropdown.stage(index)) {
                    edited(this.dropdown);
                }
            }
            closeDropdown();
            return true;
        }
        for (int i = 0; i < this.pages.size(); i++) {
            final int top = tabTop(i);
            if (inside(x, y, this.sideX, top, this.sideX + this.sideW, top + this.tabH)) {
                selectTab(i);
                return true;
            }
        }
        if (inside(x, y, this.applyX1, this.btnY, this.applyX2, this.btnY + this.btnH)) {
            apply();
            return true;
        }
        if (inside(x, y, this.doneX1, this.btnY, this.doneX2, this.btnY + this.btnH)) {
            apply();
            this.host.requestClose();
            return true;
        }
        if (x >= this.panelX1 && x < this.panelX2 && y >= this.rowsTop && y < this.rowsBottom) {
            final List<Setting> rows = currentRows();
            final int index = (y - this.rowsTop + Math.round(this.scrollAnim.get())) / this.rowH;
            if (index >= 0 && index < rows.size()) {
                final Setting setting = rows.get(index);
                if (setting.isAvailable() && clickControl(setting, x, y, rowTop(index))) {
                    return true;
                }
            }
            this.scrollDragging = this.maxScroll > 0;
            this.dragStartY = mouseY;
            this.dragStartScroll = this.scrollTarget;
            return true;
        }
        return false;
    }

    private boolean clickControl(final Setting setting, final int x, final int y, final int top) {
        final int cx1 = ctrlX1();
        final int cx2 = ctrlX2();
        switch (setting.kind) {
            case TOGGLE:
                if (setting.isEditable() && setting.stage(setting.pending == 0 ? 1 : 0)) {
                    edited(setting);
                }
                return true;
            case SEGMENTED:
                if (x >= cx1 && x < cx2 && setting.isEditable()) {
                    final int count = Math.max(1, setting.choices.length);
                    final int index = Math.min(count - 1, (x - cx1) * count / Math.max(1, cx2 - cx1));
                    if (setting.stage(index) || setting.onPick != null) {
                        edited(setting);
                    }
                    return true;
                }
                return false;
            case DROPDOWN:
                if (x >= cx1 && x < cx2 && setting.isEditable()) {
                    this.dropdown = setting;
                    this.closingDropdown = null;
                    return true;
                }
                return false;
            case SLIDER:
                if (x >= setting.sliderX1 - 6 && x <= setting.sliderX2 + 6 && setting.isEditable()) {
                    this.dragging = setting;
                    updateSlider(setting, x);
                    return true;
                }
                return false;
            case BUTTON: {
                if (x >= cx2 - 80 && x < cx2) {
                    setting.press();
                    return true;
                }
                return false;
            }
            default:
                return false;
        }
    }

    private void updateSlider(final Setting setting, final double x) {
        final int span = Math.max(1, setting.sliderX2 - setting.sliderX1);
        final double t = Math.max(0.0, Math.min(1.0, (x - setting.sliderX1) / span));
        final int value = setting.min + (int) Math.round(t * (setting.max - setting.min));
        if (setting.stage(value)) {
            edited(setting);
        }
    }

    public boolean mouseDragged(final double mouseX, final double mouseY, final int button) {
        if (this.dragging != null) {
            updateSlider(this.dragging, mouseX);
            return true;
        }
        if (this.scrollDragging) {
            this.scrollTarget = (float) (this.dragStartScroll - (mouseY - this.dragStartY));
            clampScroll();
            this.scrollAnim.snap(this.scrollTarget);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        final boolean had = this.dragging != null || this.scrollDragging;
        this.dragging = null;
        this.scrollDragging = false;
        return had;
    }

    public boolean mouseScrolled(final double mouseX, final double mouseY, final double amount) {
        if (this.dropdown != null) {
            return true;
        }
        this.scrollTarget -= (float) (amount * this.rowH * 1.5);
        clampScroll();
        return true;
    }

    public boolean keyPressed(final int key) {
        switch (key) {
            case KEY_ESCAPE:
                if (this.dropdown != null) {
                    closeDropdown();
                    return true;
                }
                return false;
            case KEY_ENTER:
                apply();
                return true;
            case KEY_DOWN:
            case KEY_PAGE_DOWN:
                selectTab(Math.min(this.pages.size() - 1, this.selected + 1));
                return true;
            case KEY_UP:
            case KEY_PAGE_UP:
                selectTab(Math.max(0, this.selected - 1));
                return true;
            default:
                return false;
        }
    }

    /**
     * Re-reads every row from its source, dropping staged edits. Used when the
     * screen is re-initialised after another screen (vanilla video settings, Iris)
     * may have changed the live values.
     */
    public void reload() {
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                setting.load();
            }
        }
    }

    /** Applies staged edits; called by ESC/close so nothing the user set is lost. */
    public void onClose() {
        apply();
    }

    // ---------------------------------------------------------------- actions

    public void selectTab(final int index) {
        if (index == this.selected || index < 0 || index >= this.pages.size()) {
            return;
        }
        this.selected = index;
        this.contentAnim.snap(0f);
        this.contentAnim.setTarget(1f);
        this.scrollTarget = 0f;
        this.scrollAnim.snap(0f);
        this.dragging = null;
        this.dropdown = null;
        this.closingDropdown = null;
    }

    private void closeDropdown() {
        this.closingDropdown = this.dropdown;
        this.dropdown = null;
    }

    private void edited(final Setting setting) {
        if (setting.onPick != null) {
            setting.onPick.accept(setting.pending);
        }
        if (setting.presetMember && this.preset != null && setting != this.preset) {
            this.preset.stage(-1);
        }
    }

    /** Resets every row that has a default (Aetherium options) to it. */
    public void resetDefaults() {
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                setting.resetToDefault();
            }
        }
        this.toastText = "Defaults staged - press Apply";
        this.toastAnim.snap(1f);
    }

    public void apply() {
        final List<Setting> changed = new ArrayList<Setting>();
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                if (setting.isDirty()) {
                    changed.add(setting);
                }
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        this.host.applyChanges(changed);
        for (final Page page : this.pages) {
            for (final Setting setting : page.rows()) {
                setting.load();
            }
        }
        this.toastText = changed.size() == 1 ? "Applied 1 change" : "Applied " + changed.size() + " changes";
        this.toastAnim.snap(1f);
    }

    // ---------------------------------------------------------------- helpers

    private void clampScroll() {
        if (this.scrollTarget < 0f) {
            this.scrollTarget = 0f;
        }
        if (this.scrollTarget > this.maxScroll) {
            this.scrollTarget = this.maxScroll;
        }
    }

    private static void roundRect(final GuiCanvas canvas, final int x1, final int y1, final int x2, final int y2, final int color) {
        canvas.fill(x1 + 1, y1, x2 - 1, y2, color);
        canvas.fill(x1, y1 + 1, x1 + 1, y2 - 1, color);
        canvas.fill(x2 - 1, y1 + 1, x2, y2 - 1, color);
    }

    static String fit(final GuiCanvas canvas, final String text, final int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (canvas.textWidth(text) <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        final int budget = maxWidth - canvas.textWidth(ellipsis);
        int end = text.length();
        while (end > 0 && canvas.textWidth(text.substring(0, end)) > budget) {
            end--;
        }
        return end <= 0 ? "" : text.substring(0, end) + ellipsis;
    }

    private static boolean inside(final int x, final int y, final int x1, final int y1, final int x2, final int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    private static int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp01(final float value) {
        return value < 0f ? 0f : (value > 1f ? 1f : value);
    }
}
