package io.github.gaming32.squidbeatz2.game;

public enum GameTheme {
    DEFAULT("Default", "", false),
    PEARL("Pearl", "Hime", true),
    MARINA("Marina", "Iida", true);

    public final String name;
    public final String resourceSuffix;
    public final boolean fullscreenManual;

    GameTheme(String name, String resourceName, boolean fullscreenManual) {
        this.name = name;
        this.resourceSuffix = resourceName;
        this.fullscreenManual = fullscreenManual;
    }
}
