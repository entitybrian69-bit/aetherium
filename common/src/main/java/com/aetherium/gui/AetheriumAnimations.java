package com.aetherium.gui;

import java.util.ArrayList;
import java.util.List;

import com.aetherium.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Motion for the purple GUI: per-widget animated values, the morphing screen
 * transition, and the glow particles that carry it.
 *
 * <p>The morph transition is not a crossfade between two screens. Opening the
 * settings screen and switching tabs both run the same three-phase sequence —
 * collapse (old content slides + fades + scales 0.98), a 1-frame burst of violet
 * particles along the seam, expand (new content slides in) — because a single
 * mechanism for both is one code path to maintain and it reads as the same
 * "material" gesture to the user.</p>
 *
 * <p>Everything is time-based (nanoseconds), never frame-count based, so the
 * animation looks the same at 60 and at 400 fps — the failure mode of
 * Sodium-style {@code tick++} animations. Frame pacing is taken from
 * {@code Minecraft#getFrameTimeNs()} when available and falls back to wall clock.</p>
 *
 * <p>Thread-safety: render-thread only. {@code particles} is an ArrayList reused via
 * an index cursor and never grows past {@link #MAX_PARTICLES}, so a tab switch
 * allocates nothing.</p>
 */
public final class AetheriumAnimations {
    private static final int MAX_PARTICLES = 224;
    private static final float PARTICLE_LIFE_SECONDS = 0.85f;

    /** Phases of the morph; {@link Phase#IDLE} means "no transition running". */
    public enum Phase {
        IDLE,
        COLLAPSE,
        BURST,
        EXPAND
    }

    /** One animating scalar. Widgets hold two or three of these. */
    public static final class AnimationValue {
        /** Render-thread only. */
        private float current;
        private float target;
        private float velocity;
        private final float halfLifeSeconds;
        private final float overshoot;

        public AnimationValue(final float initial, final float halfLifeSeconds) {
            this(initial, halfLifeSeconds, 0.0f);
        }

        public AnimationValue(final float initial, final float halfLifeSeconds, final float overshoot) {
            this.current = initial;
            this.target = initial;
            this.halfLifeSeconds = Math.max(0.01f, halfLifeSeconds);
            this.overshoot = overshoot;
        }

        public void set(final float value) {
            this.target = value;
        }

        public void snap(final float value) {
            this.current = value;
            this.target = value;
            this.velocity = 0.0f;
        }

        public float get() {
            return this.current;
        }

        public float getTarget() {
            return this.target;
        }

        public boolean isSettled() {
            return Math.abs(this.target - this.current) < 1.0E-4f;
        }

        public void tick(final float deltaSeconds) {
            if (isSettled()) {
                this.current = this.target;
                return;
            }
            final float next = MathUtil.smoothDamp(this.current, this.target, this.halfLifeSeconds, deltaSeconds);
            // Overshoot injects a small springiness on top of the exponential: used by
            // toggles so a tap has a physical "pop" without a separate spring type.
            this.velocity = this.overshoot > 0.0f ? (next - this.current) * this.overshoot : 0.0f;
            this.current = next + this.velocity;
        }
    }

    /** A particle in the seam burst; plain float fields to keep the array primitive-ish. */
    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float size;
        int color;
        boolean alive;
    }

    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);
    private final AnimationValue openProgress = new AnimationValue(0.0f, 0.16f);
    private final AnimationValue contentOffset = new AnimationValue(0.0f, 0.14f);
    private final AnimationValue contentScale = new AnimationValue(1.0f, 0.14f);
    private final AnimationValue contentAlpha = new AnimationValue(1.0f, 0.14f);

    private Phase phase = Phase.IDLE;
    private long phaseStartedNanos;
    private long lastTickNanos;
    private float phaseProgress;
    private int burstOriginX;
    private int burstOriginY;
    private int burstWidth;
    private int activeParticles;
    private boolean reducedMotion;

    public AetheriumAnimations() {
        // Options#get(Options.Keybindings?) has no reduced-motion flag in vanilla, and
        // the launcher setting is the only signal that exists on Android.
        this.reducedMotion = Boolean.getBoolean("aetherium.reduced_motion")
                || "1".equals(System.getenv("AETHERIUM_REDUCED_MOTION"));
    }

    /** Called every render pass of the screen, before any widget draws. */
    public void beginFrame() {
        final long now = System.nanoTime();
        final float delta = this.lastTickNanos == 0L ? 1.0f / 60.0f
                : Math.min(0.1f, (now - this.lastTickNanos) / 1_000_000_000.0f);
        this.lastTickNanos = now;

        this.openProgress.tick(delta);
        this.contentOffset.tick(delta);
        this.contentScale.tick(delta);
        this.contentAlpha.tick(delta);

        if (this.phase != Phase.IDLE) {
            final float elapsed = (now - this.phaseStartedNanos) / 1_000_000_000.0f;
            final float duration = this.reducedMotion ? 0.02f : phaseDuration(this.phase);
            this.phaseProgress = MathUtil.clamp(elapsed / duration, 0.0f, 1.0f);
            advancePhaseIfNeeded(now, duration, delta);
        }

        // Particle integration: fixed step count, no allocation.
        this.activeParticles = 0;
        for (final Particle particle : this.particles) {
            if (!particle.alive) {
                continue;
            }
            particle.life -= delta / PARTICLE_LIFE_SECONDS;
            if (particle.life <= 0.0f) {
                particle.alive = false;
                continue;
            }
            particle.x += particle.vx * delta;
            particle.y += particle.vy * delta;
            // Upward drift with horizontal damping: embers, not confetti.
            particle.vy -= 14.0f * delta;
            particle.vx *= 0.985f;
            this.activeParticles++;
        }
    }

    private void advancePhaseIfNeeded(final long now, final float duration, final float delta) {
        if (this.phaseProgress < 1.0f) {
            // Continuous part of the transition: content slides/fades during COLLAPSE
            // and EXPAND so the seam is never an empty frame.
            if (this.phase == Phase.COLLAPSE) {
                this.contentOffset.snap(0.0f);
                this.contentAlpha.set(1.0f - this.phaseProgress);
                this.contentScale.set(1.0f - 0.02f * this.phaseProgress);
            } else if (this.phase == Phase.EXPAND) {
                this.contentAlpha.set(this.phaseProgress);
                this.contentScale.set(0.98f + 0.02f * this.phaseProgress);
            }
            this.contentOffset.tick(delta);
            this.contentAlpha.tick(delta);
            this.contentScale.tick(delta);
            return;
        }
        switch (this.phase) {
            case COLLAPSE:
                this.phase = Phase.BURST;
                this.phaseStartedNanos = now;
                spawnBurst();
                break;
            case BURST:
                this.phase = Phase.EXPAND;
                this.phaseStartedNanos = now;
                this.contentOffset.snap(-this.burstWidth * 0.06f);
                this.contentAlpha.snap(0.0f);
                break;
            case EXPAND:
            case IDLE:
            default:
                this.phase = Phase.IDLE;
                this.phaseProgress = 1.0f;
                this.contentAlpha.snap(1.0f);
                this.contentScale.snap(1.0f);
                this.contentOffset.snap(0.0f);
                break;
        }
        if (this.phase != Phase.IDLE && duration <= 0.0f) {
            this.phase = Phase.IDLE;
        }
    }

    private static float phaseDuration(final Phase phase) {
        switch (phase) {
            case COLLAPSE:
                return 0.11f;
            case BURST:
                return 0.16f;
            case EXPAND:
                return 0.15f;
            default:
                return 0.0f;
        }
    }

    /** Starts a transition; called on open and on every tab change. */
    public void startMorph(final int x, final int y, final int width) {
        this.burstOriginX = x;
        this.burstOriginY = y;
        this.burstWidth = Math.max(16, width);
        this.phase = Phase.COLLAPSE;
        this.phaseProgress = 0.0f;
        this.phaseStartedNanos = System.nanoTime();
        this.contentOffset.set(6.0f);
        this.contentAlpha.set(1.0f);
    }

    public boolean isTransitioning() {
        return this.phase != Phase.IDLE;
    }

    public Phase getPhase() {
        return this.phase;
    }

    /** 0..1 open progress, used for the initial slide-up of the whole screen. */
    public float getOpenProgress() {
        return MathUtil.easeOutCubic(this.openProgress.get());
    }

    public void beginOpen() {
        this.openProgress.snap(0.0f);
        this.openProgress.set(1.0f);
    }

    public float getContentOffsetX() {
        return this.contentOffset.get();
    }

    public float getContentScale() {
        return this.contentScale.get();
    }

    public float getContentAlpha() {
        return MathUtil.clamp(this.contentAlpha.get(), 0.0f, 1.0f);
    }

    private void spawnBurst() {
        if (this.reducedMotion) {
            return;
        }
        final int count = Math.min(48, Math.max(12, this.burstWidth / 12));
        for (int i = 0; i < count; i++) {
            final Particle particle = borrowParticle();
            final float t = count <= 1 ? 0.5f : i / (float) (count - 1);
            particle.x = this.burstOriginX + this.burstWidth * t;
            particle.y = this.burstOriginY + (float) Math.sin(t * Math.PI) * 3.0f;
            final float angle = (float) ((t - 0.5) * Math.PI * 0.8) + (float) (Math.random() - 0.5) * 0.7f;
            final float speed = 24.0f + 60.0f * (float) Math.random();
            particle.vx = (float) Math.sin(angle) * speed;
            particle.vy = -Math.abs((float) Math.cos(angle)) * speed * 0.65f;
            particle.life = 1.0f;
            particle.size = 1.0f + (float) Math.random() * 1.6f;
            particle.color = MathUtil.packRgb(0.62f + 0.2f * particle.size / 2.6f, 0.36f, 1.0f);
            particle.alive = true;
        }
    }

    private Particle borrowParticle() {
        for (final Particle particle : this.particles) {
            if (!particle.alive) {
                return particle;
            }
        }
        if (this.particles.size() < MAX_PARTICLES) {
            final Particle created = new Particle();
            this.particles.add(created);
            return created;
        }
        // Pool saturated: recycle the oldest rather than growing, which bounds the
        // worst-case cost of a user hammering the tab bar on a phone.
        return this.particles.get((int) (Math.random() * this.particles.size()));
    }

    /** Draws the particles. Called after content so sparks sit on top of the panel. */
    public void drawParticles(final GuiGraphics guiGraphics) {
        for (final Particle particle : this.particles) {
            if (!particle.alive) {
                continue;
            }
            final float life = MathUtil.clamp(particle.life, 0.0f, 1.0f);
            final int alpha = (int) (0xE0 * life);
            final int x = Math.round(particle.x);
            final int y = Math.round(particle.y);
            final int size = Math.max(1, Math.round(particle.size * (0.5f + life)));
            final int color = (alpha << 24) | (particle.color & 0xFFFFFF);
            guiGraphics.fill(x, y, x + size, y + size, color);
            // Halo: one pixel of halo on each side at a quarter alpha, which is what
            // makes it read as a glow rather than as a stray dot.
            guiGraphics.fill(x - 1, y, x, y + size, (alpha / 4 << 24) | (particle.color & 0xFFFFFF));
            guiGraphics.fill(x + size, y, x + size + 1, y + size, (alpha / 4 << 24) | (particle.color & 0xFFFFFF));
        }
    }

    public int getActiveParticleCount() {
        return this.activeParticles;
    }

    public boolean isReducedMotion() {
        return this.reducedMotion;
    }

    public void setReducedMotion(final boolean value) {
        this.reducedMotion = value;
        if (value) {
            for (final Particle particle : this.particles) {
                particle.alive = false;
            }
            this.phase = Phase.IDLE;
            this.contentAlpha.snap(1.0f);
            this.contentScale.snap(1.0f);
            this.contentOffset.snap(0.0f);
        }
    }

    /** Seconds per frame as the widgets should see it, for tests and headless runs. */
    public static float suggestedDeltaSeconds() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 1.0f / 60.0f;
        }
        try {
            final double lastFrameMs = minecraft.getLastFrameTime();
            return lastFrameMs <= 0.0 ? 1.0f / 60.0f : Math.min(0.1f, (float) (lastFrameMs / 1000.0));
        } catch (final RuntimeException error) {
            // getLastFrameTime is absent on some ports; wall clock is a fine answer.
            return 1.0f / 60.0f;
        }
    }
}
