package com.aetherium.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The hand editor's live preview in the settings view (Punchy-style): a preset dropdown that
 * stages other rows, the screen fading away to the one row being dragged, and a short peek after
 * picking a preset.
 */
final class LivePreviewTest {
    private static final int W = 427;
    private static final int H = 240;

    private static final class Canvas extends GuiCanvas {
        int fills;

        void frame() {
            beginFrame();
            this.fills = 0;
        }

        @Override
        protected void rawFill(final int x1, final int y1, final int x2, final int y2, final int argb) {
            this.fills++;
        }

        @Override
        protected void rawText(final String text, final int x, final int y, final int argb) {
        }

        @Override
        protected int rawTextWidth(final String text) {
            return text.length() * 6;
        }

        @Override
        protected void rawPushClip(final int x1, final int y1, final int x2, final int y2) {
        }

        @Override
        protected void rawPopClip(final boolean restore, final int x1, final int y1, final int x2, final int y2) {
        }
    }

    private static final class Host implements ScreenHost {
        boolean world = true;
        int frames;

        @Override
        public void applyChanges(final List<Setting> changed) {
            for (final Setting setting : changed) {
                setting.commit();
            }
        }

        @Override
        public void requestClose() {
        }

        @Override
        public int currentFps() {
            return 60;
        }

        @Override
        public String footerLeft() {
            return "";
        }

        @Override
        public String footerRight() {
            return "";
        }

        @Override
        public boolean touchMode() {
            return false;
        }

        @Override
        public boolean darkMode() {
            return false;
        }

        @Override
        public void setDarkMode(final boolean dark) {
        }

        @Override
        public void playSound(final UiSound sound) {
        }

        @Override
        public boolean livePreviewAvailable() {
            return this.world;
        }

        @Override
        public void beforeFrame() {
            this.frames++;
        }
    }

    private static final int[][] PRESETS = {{0, 0}, {-22, 100}, {6, 65}};

    private Host host;
    private Canvas canvas;
    private AetheriumView view;
    private long now;
    private final int[] live = new int[4];

    @BeforeEach
    void setUp() {
        this.host = new Host();
        this.canvas = new Canvas();
        final Page page = new Page("QoL", "QoL", "Hand", PixelArt.GEAR);
        page.add(Setting.choice("preset", Setting.Kind.DROPDOWN, "Hand preset", "Preset",
                new String[]{"Custom", "Default", "Lowered", "Small"}, () -> this.live[0], v -> this.live[0] = v)
                .livePreview()
                .onPick(index -> {
                    if (index >= 1) {
                        this.view.find("y").stage(PRESETS[index - 1][0]);
                        this.view.find("scale").stage(PRESETS[index - 1][1]);
                    }
                }));
        page.add(Setting.slider("y", "Hand height", "Height", -50, 50, 1, null, () -> this.live[1], v -> this.live[1] = v)
                .livePreview());
        page.add(Setting.slider("scale", "Hand size", "Size", 25, 200, 5, null, () -> this.live[2], v -> this.live[2] = v)
                .livePreview());
        page.add(Setting.toggle("plain", "Plain", "Not a preview row", () -> this.live[3], v -> this.live[3] = v));
        this.live[2] = 100;
        this.view = new AetheriumView(Collections.singletonList(page), this.host, 0);
        this.now = 1_000_000_000L;
        this.view.setTimeSource(() -> this.now);
        this.view.resize(W, H);
        frames(40);
    }

    @AfterEach
    void lightAgain() {
        AetheriumTheme.apply(0f);
    }

    private void frames(final int count) {
        for (int i = 0; i < count; i++) {
            this.now += 16_666_667L;
            this.canvas.frame();
            this.view.render(this.canvas, -1, -1);
        }
    }

    private int rowY(final int index) {
        return this.view.rowTopForTest(index) + this.view.rowHeightForTest() / 2;
    }

    /** Opens the preset dropdown and picks {@code item}. */
    private void pickPreset(final int item) {
        final int x = this.view.ctrlRightForTest() - 6;
        this.view.mouseClicked(x, rowY(0), 0);
        this.view.mouseReleased(x, rowY(0), 0);
        frames(20);
        final int top = this.view.rowTopForTest(0);
        final int listTop = top + (this.view.rowHeightForTest() + 16) / 2 + 1;
        final int y = listTop + 1 + item * 14 + 7;
        assertTrue(this.view.mouseClicked(x, y, 0));
        this.view.mouseReleased(x, y, 0);
    }

    @Test
    @DisplayName("picking a preset stages its values into the hand rows (applied with Apply)")
    void presetStagesRows() {
        pickPreset(2);
        assertEquals(2, this.view.find("preset").pending());
        assertEquals(-22, this.view.find("y").pending());
        assertEquals(100, this.view.find("scale").pending());
        assertTrue(this.view.find("y").isDirty());
        pickPresetAfterPeek(3);
        assertEquals(6, this.view.find("y").pending());
        assertEquals(65, this.view.find("scale").pending());
        this.view.apply();
        assertEquals(3, this.live[0]);
        assertEquals(6, this.live[1]);
        assertEquals(65, this.live[2]);
    }

    private void pickPresetAfterPeek(final int item) {
        frames(200); // the peek after the first pick is over
        pickPreset(item);
    }

    @Test
    @DisplayName("a picked preset fades the screen away for a moment; a click brings it back at once")
    void presetPeek() {
        assertFalse(this.view.isIsolating());
        pickPreset(2);
        frames(20);
        assertTrue(this.view.isIsolating(), "the menu fades so the hand can be seen");
        final int isolatedFills = this.canvas.fills;
        assertTrue(isolatedFills < 20, "only the preset row is drawn: " + isolatedFills + " fills");
        assertTrue(this.view.mouseClicked(10, 10, 0), "a click during the peek is used up");
        assertEquals(2, this.view.find("preset").pending(), "and changes nothing");
        frames(30);
        assertFalse(this.view.isIsolating());
        // On its own the peek also ends.
        pickPresetAfterPeek(3);
        frames(20);
        assertTrue(this.view.isIsolating());
        frames(200);
        assertFalse(this.view.isIsolating());
    }

    @Test
    @DisplayName("dragging a hand slider leaves only that row on screen; releasing brings the menu back")
    void sliderIsolates() {
        final Setting y = this.view.find("y");
        final int sy = rowY(1);
        final int x = (y.sliderX1 + y.sliderX2) / 2;
        assertTrue(this.view.mouseClicked(x, sy, 0));
        frames(30);
        assertTrue(this.view.isIsolating());
        final int isolatedFills = this.canvas.fills;
        this.view.mouseDragged(y.sliderX2, sy, 0);
        frames(1);
        assertEquals(50, y.pending(), "the slider keeps working while alone");
        this.view.mouseReleased(y.sliderX2, sy, 0);
        frames(40);
        assertFalse(this.view.isIsolating());
        assertTrue(this.canvas.fills > isolatedFills * 3,
                "the whole screen is drawn again: " + this.canvas.fills + " vs " + isolatedFills);
        assertTrue(this.host.frames > 0, "the host hears about every frame (the live hand update)");
    }

    @Test
    @DisplayName("ordinary rows never isolate, and nothing isolates without a world behind the screen")
    void onlyPreviewRowsInAWorld() {
        final int x = this.view.ctrlRightForTest() - 6;
        this.view.mouseClicked(x, rowY(3), 0);
        this.view.mouseReleased(x, rowY(3), 0);
        frames(20);
        assertEquals(1, this.view.find("plain").pending());
        assertFalse(this.view.isIsolating());

        this.host.world = false; // title screen: nothing to look at behind the menu
        final Setting y = this.view.find("y");
        this.view.mouseClicked((y.sliderX1 + y.sliderX2) / 2, rowY(1), 0);
        frames(30);
        assertFalse(this.view.isIsolating());
        this.view.mouseReleased((y.sliderX1 + y.sliderX2) / 2, rowY(1), 0);
    }
}
