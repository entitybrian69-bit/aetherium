package com.aetherium;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The single safe point where Aetherium may be torn down and rebuilt.
 *
 * <p>Backends own GPU objects (buffers, pipelines, semaphores) that cannot be
 * destroyed while a command buffer referencing them is still in flight. Rather
 * than reason about implicit ordering, both sides take an explicit lock:</p>
 * <ul>
 *   <li>the render thread holds it for the duration of a frame via
 *       {@link #beginFrame()}/{@link #endFrame()};</li>
 *   <li>a swap requests it with {@link #beginSwap()}/{@link #endSwap()}, which
 *       blocks until the current frame has finished.</li>
 * </ul>
 *
 * <p>Because the swap path only runs from a GUI button press, contention is
 * rare; a fair {@link ReentrantLock} keeps the requester from starving behind a
 * 400-fps render loop. The lock is reentrant because the shadow render pass
 * takes a nested frame while the F3 overlay is drawn from inside a frame.</p>
 */
public final class RenderSafety {
    private final ReentrantLock lock = new ReentrantLock(true);

    /** Debug aid: which thread owns the safe point right now. */
    private volatile long ownerThreadId = -1L;
    private volatile String ownerLabel;

    public void beginFrame() {
        this.lock.lock();
        this.ownerThreadId = Thread.currentThread().getId();
        this.ownerLabel = "render-frame";
    }

    public void endFrame() {
        if (this.lock.isHeldByCurrentThread()) {
            this.ownerThreadId = -1L;
            this.ownerLabel = null;
        }
        if (this.lock.isHeldByCurrentThread()) {
            this.lock.unlock();
        }
    }

    public void beginSwap() {
        this.lock.lock();
        this.ownerThreadId = Thread.currentThread().getId();
        this.ownerLabel = "backend-swap";
    }

    public void endSwap() {
        if (this.lock.isHeldByCurrentThread()) {
            this.ownerThreadId = -1L;
            this.ownerLabel = null;
            this.lock.unlock();
        }
    }

    /**
     * Non-blocking acquisition for optional work (HUD overlay, shadow pass
     * measurement) that must never delay a swap or deadlock a debug screen.
     */
    public boolean tryBeginFrame() {
        if (this.lock.tryLock()) {
            this.ownerThreadId = Thread.currentThread().getId();
            this.ownerLabel = "optional-frame";
            return true;
        }
        return false;
    }

    public void endOptionalFrame() {
        if (this.lock.isHeldByCurrentThread()) {
            this.ownerThreadId = -1L;
            this.ownerLabel = null;
            this.lock.unlock();
        }
    }

    public boolean isHeldByCurrentThread() {
        return this.lock.isHeldByCurrentThread();
    }

    /** @throws IllegalStateException with the current owner, aiding deadlock reports */
    public void assertRenderThread(final String what) {
        if (!this.lock.isHeldByCurrentThread()) {
            throw new IllegalStateException(what + " must run at a render safe point (owner="
                    + this.ownerLabel + " thread=" + this.ownerThreadId + " current=" + Thread.currentThread().getName() + ')');
        }
    }

    /** Only used by tests and the debug screen. */
    public boolean isSwapping() {
        return this.ownerLabel != null && this.ownerLabel.startsWith("backend");
    }
}
