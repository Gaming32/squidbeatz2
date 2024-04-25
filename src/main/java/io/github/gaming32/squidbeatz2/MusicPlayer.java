package io.github.gaming32.squidbeatz2;

import io.github.gaming32.squidbeatz2.game.assets.SongInfo;
import io.github.gaming32.squidbeatz2.msbt.MSBTReader;
import io.github.gaming32.squidbeatz2.util.ClipPositionHelper;
import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableChannelInputStream;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.BCFstmReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.wave.WaveWriter;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.SplashScreen;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MusicPlayer {
    public static void main(String[] args) throws Exception {
        try {
            SplashScreen.getSplashScreen().close();
        } catch (NoClassDefFoundError | NullPointerException | UnsupportedOperationException ignored) {
        }

        final String dataParentEnvName = Constants.IS_WINDOWS ? "APPDATA" : "XDG_DATA_HOME";
        final String dataParentEnv = System.getenv(dataParentEnvName);
        final Path dataParent = dataParentEnv != null
            ? Path.of(dataParentEnv)
            : Path.of(System.getProperty("user.home"), ".local", "share");
        Path suyuDir = dataParent.resolve("suyu");
        if (!Files.exists(suyuDir)) {
            final Path tryDir = dataParent.resolve("yuzu");
            if (!Files.exists(tryDir)) {
                System.err.println("Suyu directory (" + suyuDir + ") not found");
                System.exit(1);
            }
            suyuDir = tryDir;
        }

        final Path romfsPath = suyuDir.resolve("dump/0100F8F0000A2000/romfs");
        if (!Files.exists(romfsPath)) {
            System.err.println("Splatoon 2 dump not found. Did you forget to dump Splatoon 2?");
            System.exit(1);
        }
        final Path octoExpansionPath = suyuDir.resolve("dump/0100F8F0000A3065/romfs");
        final boolean octoExpansionDumped = Files.exists(octoExpansionPath);

        final List<SongInfo> songs = SongInfo.loadFromCompressedArchive(romfsPath.resolve(Constants.SONG_INFO_PATH));

        final Map<String, String> songNames = getSongIds(romfsPath);
        final Int2ObjectMap<String> songNumberIds = new Int2ObjectOpenHashMap<>(songNames.size());
        for (final SongInfo song : songs) {
            final int id = songNumberIds.size() + 1;
            songNumberIds.put(id, song.songId());
            if (octoExpansionDumped || Files.exists(Path.of(Constants.STREAM_PATH, song.songId() + ".bfstm"))) {
                System.out.printf("%2s -- %s\n", id, songNames.get(song.songId()));
            }
        }
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
        ((FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN)).setValue(20f * (float)Math.log10(0.3));

        final Pcm16Format pcm16 = data.getFormat(Pcm16Format.class);
        final ClipPositionHelper positionHelper = new ClipPositionHelper(clip);
        if (pcm16.isLooping()) {
            positionHelper.loop(pcm16.getLoopStart(), pcm16.getLoopEnd() - 1);
        }

        clip.start();
        final long length = clip.getMicrosecondLength();
        do {
            final long position = positionHelper.getMicrosecondPosition();
            final int progressBar = (int)Math.round((double)position / length * 65);
            System.out.print(new StringBuilder("\r[")
                .repeat('=', progressBar)
                .repeat(' ', 65 - progressBar)
                .append("] ")
                .append(formatTimeMicros(position))
                .append(" / ")
                .append(formatTimeMicros(length))
            );
            Thread.sleep(100);
        } while (clip.isRunning());
        System.out.println();
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

    public static String formatTimeMicros(long micros) {
        final long minutes = TimeUnit.MICROSECONDS.toMinutes(micros);
        long seconds = TimeUnit.MICROSECONDS.toSeconds(micros) - minutes * 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
