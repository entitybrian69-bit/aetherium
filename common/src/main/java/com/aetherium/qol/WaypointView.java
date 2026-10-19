package com.aetherium.qol;

import com.aetherium.gui.AetheriumTheme;
import com.aetherium.gui.GuiCanvas;
import com.aetherium.gui.PixelArt;
import com.aetherium.gui.UiSound;

import java.util.List;

import static com.aetherium.gui.AetheriumTheme.*;

/**
 * The waypoint list (Quality of life), drawn in Aetherium's theme. Version-independent: the
 * Minecraft screen that hosts it only forwards render and input calls, like {@code AetheriumView}.
 *
 * <p>Each row: colour swatch (click to change colour), name (click to rename, Enter to keep, Esc
 * to cancel), coordinates with dimension and distance, a Show / Hide switch and a delete button
 * that has to be clicked twice.</p>
 */
public final class WaypointView {

    /** What the view needs from the game. */
    public interface Host {
        double playerX();

        double playerY();

        double playerZ();

        /** Dimension id of the player, e.g. "minecraft:overworld". */
        String dimension();

        void close();

        void playSound(UiSound sound);

        boolean darkMode();
    }

    private static final int ROW_H = 24;
    private static final int KEY_ESCAPE = 256;
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;
    private static final int KEY_BACKSPACE = 259;
    private static final long DELETE_ARM_MS = 3000L;
    private static final int DELETE_RED = 0xFFD03030;

    private final Waypoints store;
    private final Host host;

    private int width;
    private int height;
    private int px1;
    private int px2;
    private int py1;
    private int py2;
    private int listY1;
    private int listY2;
    private int scroll;

    /** Waypoint being renamed, its text so far, or null. */
    private Waypoints.Waypoint editing;
    private final StringBuilder edit = new StringBuilder();
    private Waypoints.Waypoint armedDelete;
    private long armedAt;
    private String message = "";

    public WaypointView(final Waypoints store, final Host host) {
        this.store = store;
        this.host = host;
    }

    public void resize(final int width, final int height) {
        this.width = width;
        this.height = height;
        final int pw = Math.min(400, width - 16);
        this.px1 = (width - pw) / 2;
        this.px2 = this.px1 + pw;
        this.py1 = 10;
        this.py2 = height - 10;
        this.listY1 = this.py1 + 58;
        this.listY2 = this.py2 - 22;
        clampScroll();
    }

    // ------------------------------------------------------------------ render

    public void render(final GuiCanvas canvas, final int mouseX, final int mouseY) {
        AetheriumTheme.apply(this.host.darkMode() ? 1f : 0f);
        canvas.fill(0, 0, this.width, this.height, BACKGROUND);
        canvas.fill(this.px1 + 2, this.py1 + 2, this.px2 + 2, this.py2 + 2, SURFACE_SHADOW);
        canvas.fill(this.px1, this.py1, this.px2, this.py2, SURFACE_BORDER);
        canvas.fill(this.px1 + 1, this.py1 + 1, this.px2 - 1, this.py2 - 1, SURFACE);

        PixelArt.drawText(canvas, "WAYPOINTS", this.px1 + 10, this.py1 + 9, 2, INK);
        final String dim = this.host.dimension();
        final String here = "You: " + (int) Math.floor(this.host.playerX()) + ", " + (int) Math.floor(this.host.playerY())
                + ", " + (int) Math.floor(this.host.playerZ()) + "  \u00b7  " + Waypoints.dimensionName(dim);
        canvas.text(fit(canvas, here, this.px2 - this.px1 - 20), this.px1 + 10, this.py1 + 28, MUTED);

        final boolean full = this.store.world().isEmpty();
        drawButton(canvas, addX1(), this.py1 + 38, addX2(), "+ Add here", mouseX, mouseY, !full);
        drawButton(canvas, doneX1(), this.py1 + 38, this.px2 - 10, "Done", mouseX, mouseY, true);
        canvas.fill(this.px1 + 1, this.listY1 - 2, this.px2 - 1, this.listY1 - 1, DIVIDER);

        final List<Waypoints.Waypoint> all = this.store.all();
        canvas.pushClip(this.px1 + 1, this.listY1, this.px2 - 1, this.listY2);
        if (all.isEmpty()) {
            canvas.text("No waypoints in this world yet.", this.px1 + 12, this.listY1 + 10, MUTED);
            canvas.text("Press the Add waypoint key (B) in game, or Add here.", this.px1 + 12, this.listY1 + 22, FAINT);
        }
        final long now = System.currentTimeMillis();
        for (int i = 0; i < all.size(); i++) {
            final int y = this.listY1 + i * ROW_H - this.scroll;
            if (y + ROW_H < this.listY1 || y > this.listY2) {
                continue;
            }
            drawRow(canvas, all.get(i), y, mouseX, mouseY, now);
        }
        canvas.popClip();

        drawScrollbar(canvas, all.size());
        canvas.fill(this.px1 + 1, this.listY2, this.px2 - 1, this.listY2 + 1, DIVIDER);
        final String hint = this.message.isEmpty()
                ? "Click a colour to change it, a name to rename it."
                : this.message;
        canvas.text(fit(canvas, hint, this.px2 - this.px1 - 20), this.px1 + 10, this.listY2 + 7, FAINT);
    }

    private void drawRow(final GuiCanvas canvas, final Waypoints.Waypoint w, final int y, final int mouseX,
                         final int mouseY, final long now) {
        final int x1 = this.px1 + 1;
        final int x2 = this.px2 - 7;
        if (mouseY >= y && mouseY < y + ROW_H && mouseY >= this.listY1 && mouseY < this.listY2
                && mouseX >= x1 && mouseX < x2) {
            canvas.fill(x1, y, x2, y + ROW_H, ROW_HOVER);
        }
        // swatch
        final int sx = this.px1 + 10;
        final int sy = y + 7;
        canvas.fill(sx - 1, sy - 1, sx + 11, sy + 11, INK);
        canvas.fill(sx, sy, sx + 10, sy + 10, w.argb());

        // name
        final int nameX = this.px1 + 28;
        final int nameMax = toggleX1() - 8 - nameX;
        if (w == this.editing) {
            final String text = this.edit.toString();
            final int fieldX2 = toggleX1() - 8;
            canvas.fill(nameX - 3, y + 2, fieldX2, y + 13, INK);
            canvas.fill(nameX - 2, y + 3, fieldX2 - 1, y + 12, SURFACE);
            final String shown = fitTail(canvas, text, fieldX2 - nameX - 6);
            canvas.text(shown, nameX, y + 4, INK);
            if ((now / 500L) % 2L == 0L) {
                final int cx = nameX + canvas.textWidth(shown);
                canvas.fill(cx, y + 4, cx + 1, y + 12, INK);
            }
        } else {
            canvas.text(fit(canvas, w.name, Math.max(20, nameMax)), nameX, y + 4, w.visible ? INK : FAINT);
        }

        // coordinates, dimension, distance
        final String dim = this.host.dimension();
        String detail = w.x + ", " + w.y + ", " + w.z + "  \u00b7  " + Waypoints.dimensionName(w.dimension);
        final double tx = Waypoints.convert(w.x + 0.5, w.dimension, dim);
        final double tz = Waypoints.convert(w.z + 0.5, w.dimension, dim);
        if (!Double.isNaN(tx)) {
            final double dx = tx - this.host.playerX();
            final double dz = tz - this.host.playerZ();
            final double dy = w.dimension.equals(dim) ? w.y + 0.5 - this.host.playerY() : 0.0;
            detail += "  \u00b7  " + (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)) + " m";
            if (!w.dimension.equals(dim)) {
                detail += " (" + (int) Math.floor(tx) + ", " + (int) Math.floor(tz) + " here)";
            }
        }
        canvas.text(fit(canvas, detail, toggleX1() - nameX - 8), nameX, y + 14, MUTED);

        // show / hide switch
        final int tx1 = toggleX1();
        final int ty = y + 7;
        canvas.fill(tx1, ty, tx1 + 20, ty + 10, w.visible ? TOGGLE_ON : TOGGLE_OFF);
        final int knob = w.visible ? tx1 + 11 : tx1 + 1;
        canvas.fill(knob, ty + 1, knob + 8, ty + 9, SURFACE);

        // delete
        final boolean armed = w == this.armedDelete && now - this.armedAt < DELETE_ARM_MS;
        final int dx1 = deleteX1();
        final int dx2 = this.px2 - 12;
        final boolean hover = mouseX >= dx1 && mouseX < dx2 && mouseY >= y + 5 && mouseY < y + 19;
        canvas.fill(dx1, y + 5, dx2, y + 19, armed ? DELETE_RED : (hover ? CONTROL_DARK_HOVER : CONTROL_TRACK));
        final String label = armed ? "Sure?" : "x";
        canvas.text(label, dx1 + (dx2 - dx1 - canvas.textWidth(label)) / 2, y + 8, armed ? 0xFFFFFFFF : INK);
    }

    private void drawButton(final GuiCanvas canvas, final int x1, final int y1, final int x2, final String text,
                            final int mouseX, final int mouseY, final boolean enabled) {
        final boolean hover = enabled && mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y1 + 14;
        canvas.fill(x1, y1, x2, y1 + 14, enabled ? INK : SURFACE_BORDER);
        canvas.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 13, hover ? ROW_HOVER : SURFACE);
        canvas.text(text, x1 + (x2 - x1 - canvas.textWidth(text)) / 2, y1 + 3, enabled ? INK : FAINT);
    }

    private void drawScrollbar(final GuiCanvas canvas, final int rows) {
        final int content = rows * ROW_H;
        final int view = this.listY2 - this.listY1;
        if (content <= view) {
            return;
        }
        final int x1 = this.px2 - 6;
        canvas.fill(x1, this.listY1, x1 + 3, this.listY2, SCROLL_TRACK);
        final int thumb = Math.max(12, view * view / content);
        final int top = this.listY1 + (view - thumb) * this.scroll / Math.max(1, content - view);
        canvas.fill(x1, top, x1 + 3, top + thumb, SCROLL_THUMB);
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        final int x = (int) mx;
        final int y = (int) my;
        if (y >= this.py1 + 38 && y < this.py1 + 52) {
            if (x >= addX1() && x < addX2()) {
                addHere();
                return true;
            }
            if (x >= doneX1() && x < this.px2 - 10) {
                this.host.playSound(UiSound.CLICK);
                this.host.close();
                return true;
            }
        }
        if (y < this.listY1 || y >= this.listY2 || x < this.px1 || x >= this.px2) {
            commitEdit();
            return false;
        }
        final List<Waypoints.Waypoint> all = this.store.all();
        final int index = (y - this.listY1 + this.scroll) / ROW_H;
        if (index < 0 || index >= all.size()) {
            commitEdit();
            return true;
        }
        final Waypoints.Waypoint w = all.get(index);
        if (x >= this.px1 + 8 && x < this.px1 + 22) {
            commitEdit();
            w.color = (w.color + 1) % Waypoints.COLORS.length;
            this.store.changed();
            this.host.playSound(UiSound.TICK);
        } else if (x >= toggleX1() && x < toggleX1() + 20) {
            commitEdit();
            w.visible = !w.visible;
            this.store.changed();
            this.host.playSound(w.visible ? UiSound.TOGGLE_ON : UiSound.TOGGLE_OFF);
        } else if (x >= deleteX1() && x < this.px2 - 12) {
            commitEdit();
            final long now = System.currentTimeMillis();
            if (w == this.armedDelete && now - this.armedAt < DELETE_ARM_MS) {
                this.store.remove(w);
                this.armedDelete = null;
                this.message = "Deleted " + w.name + ".";
                clampScroll();
            } else {
                this.armedDelete = w;
                this.armedAt = now;
            }
            this.host.playSound(UiSound.CLICK);
        } else if (x >= this.px1 + 26 && x < toggleX1() - 6) {
            if (w != this.editing) {
                commitEdit();
                startEdit(w);
                this.host.playSound(UiSound.SELECT);
            }
        } else {
            commitEdit();
        }
        return true;
    }

    public boolean mouseScrolled(final double mx, final double my, final double amount) {
        this.scroll -= (int) Math.round(amount * ROW_H);
        clampScroll();
        return true;
    }

    /** GLFW key codes. Esc while renaming cancels the rename; otherwise it is left to the screen (close). */
    public boolean keyPressed(final int key) {
        if (this.editing == null) {
            return false;
        }
        if (key == KEY_ESCAPE) {
            this.editing = null;
            return true;
        }
        if (key == KEY_ENTER || key == KEY_KP_ENTER) {
            commitEdit();
            return true;
        }
        if (key == KEY_BACKSPACE) {
            if (this.edit.length() > 0) {
                this.edit.setLength(this.edit.length() - 1);
            }
            return true;
        }
        // Swallow everything else (the inventory key must not close the screen while typing).
        return true;
    }

    public boolean charTyped(final char c) {
        if (this.editing == null) {
            return false;
        }
        if (c >= ' ' && c != 127 && c != '\u00a7' && this.edit.length() < Waypoints.MAX_NAME) {
            this.edit.append(c);
        }
        return true;
    }

    /** The screen is closing: a rename in progress is kept. */
    public void onClose() {
        commitEdit();
    }

    // ------------------------------------------------------------------ actions

    private void addHere() {
        commitEdit();
        final Waypoints.Waypoint w = this.store.add((int) Math.floor(this.host.playerX()),
                (int) Math.floor(this.host.playerY()), (int) Math.floor(this.host.playerZ()), this.host.dimension());
        if (w == null) {
            this.message = this.store.world().isEmpty() ? "Join a world first." : "This world is full (256 waypoints).";
            return;
        }
        this.host.playSound(UiSound.APPLY);
        this.scroll = Integer.MAX_VALUE;
        clampScroll();
        startEdit(w);
    }

    private void startEdit(final Waypoints.Waypoint w) {
        this.editing = w;
        this.edit.setLength(0);
        this.edit.append(w.name);
        this.message = "Type a name, Enter to keep it, Esc to cancel.";
    }

    private void commitEdit() {
        if (this.editing == null) {
            return;
        }
        final String name = Waypoints.clampName(this.edit.toString().trim());
        if (!name.isEmpty() && !name.equals(this.editing.name)) {
            this.editing.name = name;
            this.store.changed();
        }
        this.editing = null;
        this.message = "";
    }

    // ------------------------------------------------------------------ layout helpers

    private int addX1() {
        return this.px2 - 150;
    }

    private int addX2() {
        return this.px2 - 74;
    }

    private int doneX1() {
        return this.px2 - 68;
    }

    private int deleteX1() {
        return this.px2 - 46;
    }

    private int toggleX1() {
        return this.px2 - 74;
    }

    private void clampScroll() {
        final int content = this.store.all().size() * ROW_H;
        final int max = Math.max(0, content - (this.listY2 - this.listY1));
        this.scroll = Math.max(0, Math.min(max, this.scroll));
    }

    private static String fit(final GuiCanvas canvas, final String text, final int max) {
        if (canvas.textWidth(text) <= max) {
            return text;
        }
        String cut = text;
        while (!cut.isEmpty() && canvas.textWidth(cut + "...") > max) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /** The end of the text that fits (the cursor side of a text field). */
    private static String fitTail(final GuiCanvas canvas, final String text, final int max) {
        String cut = text;
        while (!cut.isEmpty() && canvas.textWidth(cut) > max) {
            cut = cut.substring(1);
        }
        return cut;
    }
}
