package io.github.gaming32.squidbeatz2.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class BFTTF {
    public static Font createFont(Path path) throws IOException, FontFormatException {
        try (InputStream is = Files.newInputStream(path)) {
            return createFont(is);
        }
    }

    public static Font createFont(InputStream is) throws IOException, FontFormatException {
        return Font.createFont(Font.TRUETYPE_FONT, new BFTTFInputStream(is));
    }
}
