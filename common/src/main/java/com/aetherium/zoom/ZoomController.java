package com.aetherium.zoom;

/**
 * Zume-style zoom, as pure state and arithmetic: no Minecraft types, so it is unit-tested and
 * identical on every version. The version-specific glue ({@code ZoomKeys} plus four small
 * mixins) feeds it the keys once per frame and asks it for the FOV, the mouse sensitivity, the
 * cinematic-camera flag and the third-person camera distance.
 *
 * <p>The model follows Zume's documented behaviour; the code is Aetherium's own:</p>
 * <ul>
 *   <li>The zoom amount {@code z} runs from 1 (no zoom) to 0 (fully zoomed). The FOV is
 *       {@code minFov + (fov - minFov) * z^2}; the square makes each step feel the same size.
 *       The default zoom of 50 % (z = 0.5) gives about 18 degrees from 70, OptiFine's strength.</li>
 *   <li>Every change of {@code z} is animated over {@code smoothnessMs} with a quartic ease-out,
 *       starting from wherever the previous animation was, so nothing ever jumps.</li>
 *   <li>While zooming, the scroll wheel moves the target by {@code speed * 0.004} per notch and the
 *       Zoom In / Zoom Out keys by {@code speed * 0.0001} per millisecond held.</li>
 *   <li>Mouse sensitivity is scaled by {@code floor + (1 - floor) * z}, and the cinematic camera
 *       can be forced on while zoomed.</li>
 *   <li>In third person the same keys move the camera between 0.5 and {@code maxThirdPersonDistance}
 *       blocks instead of changing the FOV (vanilla's 4 blocks is the resting point), and the
 *       Zoom key toggles by default.</li>
 * </ul>
 */
public final class ZoomController {

    /** Camera perspective, as far as zoom cares. */
    public enum View {
        FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT
    }

    /** Exponent of the ease-out used for every animation. */
    public static final double ANIMATION_EXPONENT = 4.0;
    /** Exponent that maps the zoom amount to FOV and distance (perceptually even steps). */
    public static final double ZOOM_EXPONENT = 2.0;
    /** Closest third-person camera distance, in blocks. */
    public static final double MIN_THIRD_PERSON_DISTANCE = 0.5;
    /** Vanilla's third-person camera distance, the resting point of third-person zoom. */
    public static final double VANILLA_THIRD_PERSON_DISTANCE = 4.0;
    static final double SCROLL_STEP_PER_SPEED = 0.004;
    static final double KEY_RATE_PER_SPEED_MS = 0.0001;
    /** A frame longer than this (a hitch, a breakpoint) does not turn into a giant key step. */
    static final long MAX_FRAME_MS = 250L;

    /** One immutable-by-convention snapshot of the zoom options. */
    public static final class Settings {
        public boolean cinematic = true;
        /** 0..1: sensitivity is never reduced below this fraction. */
        public double sensitivityFloor = 0.4;
        public int speed = 20;
        public boolean scrolling = true;
        public int smoothnessMs = 150;
        /** 0..1: how far a fresh zoom goes in. */
        public double defaultZoom = 0.5;
        public boolean toggle = false;
        public boolean thirdPersonToggle = true;
        public double minFov = 1.0;
        /** 0 disables third-person zoom (the Zoom key then zooms the FOV in every view). */
        public double maxThirdPersonDistance = 15.0;
    }

    private Settings settings = new Settings();

    private double from = 1.0;
    private double target = 1.0;
    private long startMs;
    private long endMs;

    private boolean zooming;
    private boolean wasZooming;
    private boolean wasHeld;
    private int scroll;
    private long lastFrameMs;
    private boolean haveLastFrame;
    private View view = View.FIRST_PERSON;

    public void configure(final Settings next) {
        if (next != null) {
            this.settings = next;
        }
    }

    public Settings settings() {
        return this.settings;
    }

    /** Forget all state (world change, disconnect). */
    public void reset() {
        this.from = 1.0;
        this.target = 1.0;
        this.startMs = 0L;
        this.endMs = 0L;
        this.zooming = false;
        this.wasZooming = false;
        this.wasHeld = false;
        this.scroll = 0;
        this.haveLastFrame = false;
    }

    // ------------------------------------------------------------------ per frame

    /**
     * Advances the state machine once per rendered frame.
     *
     * @param held    the Zoom key is down (and no screen is open)
     * @param zoomIn  the Zoom In key is down
     * @param zoomOut the Zoom Out key is down
     */
    public void frame(final long nowMs, final boolean held, final boolean zoomIn, final boolean zoomOut, final View current) {
        this.view = current == null ? View.FIRST_PERSON : current;
        final boolean toggleMode = firstPersonZoom() ? this.settings.toggle : this.settings.thirdPersonToggle;
        if (toggleMode) {
            if (held && !this.wasHeld) {
                this.zooming = !this.zooming;
            }
        } else {
            this.zooming = held;
        }

        if (this.zooming) {
            if (!this.wasZooming) {
                activate(nowMs);
            }
            final long elapsed = this.haveLastFrame ? Math.max(0L, Math.min(MAX_FRAME_MS, nowMs - this.lastFrameMs)) : 0L;
            if (this.settings.scrolling && this.scroll != 0) {
                animateTo(nowMs, this.target - this.scroll * this.settings.speed * SCROLL_STEP_PER_SPEED);
            } else if (zoomIn != zoomOut && elapsed > 0L) {
                final double step = this.settings.speed * KEY_RATE_PER_SPEED_MS * elapsed;
                animateTo(nowMs, this.target + (zoomIn ? -step : step));
            }
        } else if (this.wasZooming) {
            deactivate(nowMs);
        }

        this.scroll = 0;
        this.lastFrameMs = nowMs;
        this.haveLastFrame = true;
        this.wasHeld = held;
        this.wasZooming = this.zooming;
    }

    /**
     * A scroll-wheel event. Returns true when zoom consumed it, in which case the caller must not
     * pass it on (the hotbar would otherwise scroll while zooming).
     */
    public boolean scroll(final double delta) {
        if (!this.settings.scrolling || !this.zooming || delta == 0.0) {
            return false;
        }
        this.scroll += delta > 0.0 ? 1 : -1;
        return true;
    }

    // ------------------------------------------------------------------ queries

    public boolean isZooming() {
        return this.zooming;
    }

    /** Zooming, or still animating back out: the hooks must keep modifying until it settles. */
    public boolean isVisible(final long nowMs) {
        return this.zooming || nowMs < this.endMs;
    }

    /** The world FOV to use instead of {@code original}. */
    public double fov(final double original, final long nowMs) {
        if (!firstPersonZoom() || !isVisible(nowMs)) {
            return original;
        }
        final double min = this.settings.minFov;
        return min + (original - min) * Math.pow(amount(nowMs), ZOOM_EXPONENT);
    }

    /** The third-person camera distance to request instead of {@code original} (vanilla asks for 4). */
    public double cameraDistance(final double original, final long nowMs) {
        if (firstPersonZoom() || !isVisible(nowMs)) {
            return original;
        }
        final double min = MIN_THIRD_PERSON_DISTANCE;
        final double max = this.settings.maxThirdPersonDistance;
        final double blocks = min + (max - min) * Math.pow(amount(nowMs), ZOOM_EXPONENT);
        return original * (blocks / VANILLA_THIRD_PERSON_DISTANCE);
    }

    public double sensitivity(final double original, final long nowMs) {
        if (!this.zooming || !firstPersonZoom()) {
            return original;
        }
        final double floor = clamp01(this.settings.sensitivityFloor);
        return original * (floor + (1.0 - floor) * amount(nowMs));
    }

    public boolean smoothCamera(final boolean original) {
        if (this.settings.cinematic && this.zooming && firstPersonZoom()) {
            return true;
        }
        return original;
    }

    /** The current, animated zoom amount: 1 = none, 0 = fully zoomed. */
    public double amount(final long nowMs) {
        if (nowMs < this.endMs) {
            final double span = this.endMs - this.startMs;
            final double t = span <= 0.0 ? 1.0 : Math.max(0.0, Math.min(1.0, (nowMs - this.startMs) / span));
            final double eased = 1.0 - Math.pow(1.0 - t, ANIMATION_EXPONENT);
            return this.from + (this.target - this.from) * eased;
        }
        return this.target;
    }

    public double targetAmount() {
        return this.target;
    }

    // ------------------------------------------------------------------ internals

    /** Third-person zoom is off (max distance 0) or the camera is in first person. */
    private boolean firstPersonZoom() {
        return this.settings.maxThirdPersonDistance <= MIN_THIRD_PERSON_DISTANCE || this.view == View.FIRST_PERSON;
    }

    /** The zoom amount that puts the third-person camera at vanilla's 4 blocks. */
    double thirdPersonRest() {
        final double min = MIN_THIRD_PERSON_DISTANCE;
        final double max = this.settings.maxThirdPersonDistance;
        if (max <= min) {
            return 1.0;
        }
        final double linear = (VANILLA_THIRD_PERSON_DISTANCE - min) / (max - min);
        return clamp01(Math.pow(clamp01(linear), 1.0 / ZOOM_EXPONENT));
    }

    private void activate(final long nowMs) {
        final double zoomBy = clamp01(this.settings.defaultZoom);
        if (firstPersonZoom()) {
            animate(nowMs, 1.0, 1.0 - zoomBy);
            return;
        }
        final double rest = thirdPersonRest();
        // Behind the player a fresh zoom pulls the camera back; facing the player it moves closer.
        final double to = this.view == View.THIRD_PERSON_BACK
                ? 1.0 + (rest - 1.0) * zoomBy
                : rest + (0.0 - rest) * zoomBy;
        animate(nowMs, rest, to);
    }

    private void deactivate(final long nowMs) {
        animateTo(nowMs, firstPersonZoom() ? 1.0 : thirdPersonRest());
    }

    private void animateTo(final long nowMs, final double to) {
        animate(nowMs, amount(nowMs), to);
    }

    private void animate(final long nowMs, final double start, final double to) {
        final double clampedTo = clamp01(to);
        if (this.settings.smoothnessMs <= 0) {
            this.from = clampedTo;
            this.target = clampedTo;
            this.startMs = 0L;
            this.endMs = 0L;
            return;
        }
        this.from = clamp01(start);
        this.target = clampedTo;
        this.startMs = nowMs;
        this.endMs = nowMs + this.settings.smoothnessMs;
    }

    private static double clamp01(final double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
