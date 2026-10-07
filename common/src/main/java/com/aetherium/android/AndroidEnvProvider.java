package com.aetherium.android;

/**
 * Read-only view of the process environment.
 *
 * <p>Indirection exists for one reason: the detection logic in
 * {@link AndroidLauncher} and {@link AndroidRenderer} is the part of Aetherium
 * most likely to be wrong on an unseen device, and it must be testable without
 * spawning a JVM with a different environment. Production uses
 * {@link #SYSTEM}; tests hand in a map.</p>
 */
public interface AndroidEnvProvider {
    /** @return the value, or null when the variable is not set */
    String get(String name);

    AndroidEnvProvider SYSTEM = System::getenv;

    /** Fixed-map provider for tests; unknown keys return null like the real env. */
    static AndroidEnvProvider of(final java.util.Map<String, String> values) {
        final java.util.Map<String, String> copy = new java.util.LinkedHashMap<>(values);
        return name -> copy.get(name);
    }
}
