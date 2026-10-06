package dev.aetherium.gui;

public enum OptionCategory {
    GENERAL("aetherium.category.general", "General"),
    PERFORMANCE("aetherium.category.performance", "Performance"),
    QUALITY("aetherium.category.quality", "Quality"),
    SHADERS("aetherium.category.shaders", "Shaders"),
    UTILITIES("aetherium.category.utilities", "Utilities"),
    ADVANCED("aetherium.category.advanced", "Advanced"),
    ANDROID("aetherium.category.android", "Android");

    public final String translationKey;
    public final String fallback;

    OptionCategory(String key, String fallback) { this.translationKey = key; this.fallback = fallback; }
}
