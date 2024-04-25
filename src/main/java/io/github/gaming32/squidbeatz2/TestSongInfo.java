package io.github.gaming32.squidbeatz2;

import io.github.gaming32.squidbeatz2.game.assets.SongInfo;

import java.io.IOException;
import java.nio.file.Path;

public class TestSongInfo {
    public static void main(String[] args) throws IOException {
        System.out.println(SongInfo.loadFromCompressedArchive(Path.of(
            "C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs\\Etc\\MiniGame.szs"
        )));
    }
}
