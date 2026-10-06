package dev.aetherium.platform;

import dev.aetherium.Aetherium;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    private Services() {}

    private static <T> T load(Class<T> clazz) {
        T impl = ServiceLoader.load(clazz).findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Aetherium.LOGGER.debug("Loaded {} for service {}", impl, clazz);
        return impl;
    }
}
