package io.github.gaming32.squidbeatz2.test;

import io.github.gaming32.squidbeatz2.bars.Amta;
import io.github.gaming32.squidbeatz2.bars.BarsReader;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableFileInputStream;

import java.io.IOException;
import java.util.Map;

public class TestMetadata {
    public static void main(String[] args) throws IOException {
        final Map<String, Amta> metadata;
        try (var is = new SeekableFileInputStream(
            "C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs\\Sound\\Resource\\BgmMiniGame.bars"
        )) {
            metadata = BarsReader.readAmtaEntries(is);
        }
        System.out.println(metadata);
    }
}
