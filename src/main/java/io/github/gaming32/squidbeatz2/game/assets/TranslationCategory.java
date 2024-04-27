package io.github.gaming32.squidbeatz2.game.assets;

public enum TranslationCategory {
    SONG("MusicName.msbt"),
    LABEL("MiniGameLabel.msbt");

    public final String filename;

    TranslationCategory(String filename) {
        this.filename = filename;
    }
}
