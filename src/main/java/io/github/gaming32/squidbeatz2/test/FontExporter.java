package io.github.gaming32.squidbeatz2.test;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.font.BFTTFInputStream;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FontExporter {
    public static void main(String[] args) throws IOException {
        try (InputStream is = new Yaz0InputStream(Files.newInputStream(Path.of(
            "C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs\\Font\\ScalableFont.szs"
        )))) {
            final SARCFile sarc = SARCFile.read(is);
            try (InputStream fontIn = new BFTTFInputStream(sarc.getInputStream(Constants.FONT_PATH))) {
                try (OutputStream os = new FileOutputStream("Splatoon2.otf")) {
                    fontIn.transferTo(os);
                }
            }
        }
    }
}
