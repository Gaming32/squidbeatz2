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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        final Path yuzuDir = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")
            ? Path.of(System.getenv("APPDATA"), "yuzu")
            : Path.of(System.getProperty("user.home"), ".local/share/yuzu");
        if (!Files.exists(yuzuDir)) {
            System.err.println("Yuzu directory (" + yuzuDir + ") not found");
            System.exit(1);
        }
        final Path romfsPath = yuzuDir.resolve("dump/0100F8F0000A2000/romfs");
        if (!Files.exists(romfsPath)) {
            System.err.println("Splatoon 2 dump not found. Did you forget to dump Splatoon 2?");
            System.exit(1);
        }
        final Path octoExpansionPath = yuzuDir.resolve("dump/0100F8F0000A3065/romfs");
        final boolean octoExpansionDumped = Files.exists(octoExpansionPath);

        final Map<String, String> songs = getSongIds(romfsPath);
        final Int2ObjectMap<String> songNumberIds = new Int2ObjectOpenHashMap<>(songs.size());
        songs.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(entry -> {
                final int id = songNumberIds.size() + 1;
                songNumberIds.put(id, entry.getKey());
                if (octoExpansionDumped || Files.exists(romfsPath.resolve(entry.getValue()))) {
                    System.out.printf("%2s -- %s\n", id, entry.getValue());
                }
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
        }

        final Path relSong = Path.of(Constants.STREAM_PATH, songId + ".bfstm");
        Path songFile = romfsPath.resolve(relSong);
        if (!Files.exists(songFile)) {
            songFile = octoExpansionPath.resolve(relSong);
        }
        final AudioData data;
        try (SeekableChannelInputStream is = new SeekableChannelInputStream(Files.newByteChannel(songFile))) {
            data = new BCFstmReader().read(is);
        }
        final byte[] songAudio = new WaveWriter().getFile(data);

        final AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(songAudio));
        final Clip clip = AudioSystem.getClip();
        clip.open(ais);
        ((FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(20f * (float)Math.log10(0.2));
        clip.start();
        do {
            Thread.sleep(1000);
        } while (clip.isRunning());
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
