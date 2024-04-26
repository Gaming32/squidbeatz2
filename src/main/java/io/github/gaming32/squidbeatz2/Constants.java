package io.github.gaming32.squidbeatz2;

import java.util.Locale;

public class Constants {
    public static final String TITLE = "Squid Beatz 2";
    public static final float VOLUME = 0.3f;

    public static final String STREAM_PATH = "Sound/Resource/Stream";
    public static final String ENGLISH_TRANSLATIONS_PATH = "Message/CommonMsg_USen.release.szs";
    public static final String MUSIC_NAMES_PATH = "MusicName.msbt";
    public static final String SONG_INFO_PATH = "Etc/MiniGame.szs";
    public static final String FONTS_PATH = "Font/ScalableFont.szs";
    public static final String FONT_PATH = "BlitzMain.bfotf";

    public static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
}
