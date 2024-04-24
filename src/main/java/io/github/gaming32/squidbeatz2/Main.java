package io.github.gaming32.squidbeatz2;

import io.github.gaming32.squidbeatz2.msbt.MSBTReader;
import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableChannelInputStream;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.BCFstmReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.wave.WaveWriter;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        final Path romfsPath = Path.of("C:\\Users\\josia\\AppData\\Roaming\\yuzu\\dump\\0100F8F0000A2000\\romfs");
        final Map<String, String> songs = getSongIds(romfsPath);
        final Int2ObjectMap<String> songNumberIds = new Int2ObjectOpenHashMap<>(songs.size());
        songs.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> {
                final int id = songNumberIds.size() + 1;
                System.out.printf("%2s -- %s\n", id, entry.getValue());
                songNumberIds.put(id, entry.getKey());
            });
        System.out.print("Enter a song ID: ");

        final BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in, Util.consoleCharset()));
        final String line = inReader.readLine();
        final int songNumber;
        try {
            songNumber = Integer.decode(line.strip());
        } catch (NumberFormatException e) {
            System.out.println("Invalid song ID: " + line);
            System.exit(1);
            return;
        }
        final String songId = songNumberIds.get(songNumber);
        if (songId == null) {
            System.out.println("Unknown song ID: " + songNumber);
            System.exit(1);
            return;
        }

        final Path songFile = romfsPath.resolve(Constants.STREAM_PATH).resolve(songId + ".bfstm");
        final AudioData data;
        try (SeekableChannelInputStream is = new SeekableChannelInputStream(Files.newByteChannel(songFile))) {
            data = new BCFstmReader().read(is);
        }
        try (OutputStream os = Files.newOutputStream(Path.of("song.wav"))) {
            new WaveWriter().writeToStream(data, os);
        }
    }

    public static Map<String, String> getSongIds(Path romfsPath) throws IOException {
        final SARCFile allTranslations;
        try (InputStream is = new Yaz0InputStream(
            Files.newInputStream(romfsPath.resolve(Constants.ENGLISH_TRANSLATIONS_PATH))
        )) {
            allTranslations = SARCFile.read(is);
        }
        try (InputStream is = allTranslations.getInputStream(Constants.MUSIC_NAMES_PATH)) {
            return MSBTReader.readMsbt(is);
        }
    }
}
