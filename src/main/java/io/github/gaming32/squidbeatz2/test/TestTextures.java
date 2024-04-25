package io.github.gaming32.squidbeatz2.test;

import io.github.gaming32.squidbeatz2.bntx.BntxFile;
import io.github.gaming32.squidbeatz2.lightbfres.BfresExternalFilesLoader;
import io.github.gaming32.squidbeatz2.texture.TextureConverter;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableByteArrayInputStream;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;
import org.apache.commons.io.function.Uncheck;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TestTextures {
    public static void main(String[] args) throws IOException {
        final SARCFile sarc;
        try (InputStream is = new Yaz0InputStream(Files.newInputStream(
            Path.of("C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs\\Model\\MiniGame.Nin_NX_NVN.szs")
        ))) {
            sarc = SARCFile.read(is);
        }
        final var texture = BfresExternalFilesLoader.loadExternalFiles(
            new SeekableByteArrayInputStream(
                sarc.getInputStream("output.bfres")
                    .readAllBytes()
            )
        ).entrySet()
            .stream()
            .filter(e -> e.getKey().endsWith(".bntx"))
            .map(Map.Entry::getValue)
            .map(SeekableByteArrayInputStream::new)
            .map(i -> Uncheck.apply(new BntxFile()::load, i))
            .map(f -> f.textures)
            .flatMap(List::stream)
            .filter(t -> t.name.equals("Title"))
            .findFirst()
            .orElseThrow();
        System.out.println(texture.name);
        final BufferedImage image = TextureConverter.toBufferedImage(texture);
        System.out.println(image);
    }
}
