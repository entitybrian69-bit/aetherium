package com.aetherium.android;

/**
 * Reads the device temperature on a background daemon thread.
 *
 * <p>A poll lists {@code /sys/class/thermal} and reads two files per zone; phones expose 30 to 80
 * zones, so one poll is dozens of blocking file reads. Done on the render thread that was a
 * visible hitch every five seconds. Here the render thread only reads a {@code volatile} int. The
 * thread starts the first time the guard is wanted, sleeps while it is not, and never keeps the
 * JVM alive.</p>
 */
public final class ThermalMonitor {
    static final long PERIOD_MS = 5000L;

    private final AndroidEnvironment environment;
    private volatile boolean wanted;
    private volatile int lastMilliCelsius = -1;
    private Thread thread;

    public ThermalMonitor(final AndroidEnvironment environment) {
        this.environment = environment;
    }

    /** Turns polling on or off. Cheap; call it whenever the setting may have changed. */
    public void setWanted(final boolean on) {
        if (on == this.wanted) {
            return;
        }
        this.wanted = on;
        if (!on) {
            this.lastMilliCelsius = -1;
            return;
        }
        synchronized (this) {
            if (this.thread == null) {
                final Thread worker = new Thread(this::run, "Aetherium thermal monitor");
                worker.setDaemon(true);
                worker.setPriority(Thread.MIN_PRIORITY);
                this.thread = worker;
                worker.start();
            }
        }
    }

    /** @return milli-degrees C from the latest background poll, or -1 when unknown / not polling */
    public int lastMilliCelsius() {
        return this.wanted ? this.lastMilliCelsius : -1;
    }

    private void run() {
        while (true) {
            if (this.wanted) {
                try {
                    this.lastMilliCelsius = this.environment.readThermalMilliCelsius();
                } catch (final RuntimeException error) {
                    this.lastMilliCelsius = -1;
                }
            }
            try {
                Thread.sleep(PERIOD_MS);
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
