package io.github.gaming32.squidbeatz2.test;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.font.BFTTF;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestFont {
    public static void main(String[] args) throws IOException, FontFormatException {
        final Font font = loadFont();
        System.out.println(font);
        final BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = image.createGraphics();
        g.setFont(font.deriveFont(48f));
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString("Hello, world!", 0, 200);
        g.dispose();
        System.out.println(image);
    }

    public static Font loadFont() throws IOException, FontFormatException {
        try (InputStream is = new Yaz0InputStream(Files.newInputStream(Path.of(
            "C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs\\Font\\ScalableFont.szs"
        )))) {
            final SARCFile sarc = SARCFile.read(is);
            try (InputStream fontIn = sarc.getInputStream(Constants.FONT_PATH)) {
                return BFTTF.createFont(fontIn);
            }
        }
    }
}
