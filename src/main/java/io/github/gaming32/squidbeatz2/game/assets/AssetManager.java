package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.font.BFTTF;
import io.github.gaming32.squidbeatz2.msbt.MSBTReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.BCFstmReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.wave.WaveWriter;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.DeflaterOutputStream;

public class AssetManager {
    private static List<SongInfo> songs;
    private static Map<String, String> songNames;
    private static Font gameFont;
    private static Map<String, SongAudio> songAudio;

    /**
     * @return The nanoseconds taken loading assets
     */
    public static long loadAssets(FileGetter<?> fileGetter) {
        final long start = System.nanoTime();

        final var songsFuture = CompletableFuture.supplyAsync(() -> {
            try (InputStream is = new BufferedInputStream(fileGetter.apply(Constants.SONG_INFO_PATH))) {
                return SongInfo.loadFromCompressedArchive(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        final var songNamesFuture = CompletableFuture.supplyAsync(() -> {
            try {
                final SARCFile allTranslations;
                try (InputStream is = new Yaz0InputStream(new BufferedInputStream(fileGetter.apply(Constants.ENGLISH_TRANSLATIONS_PATH)))) {
                    allTranslations = SARCFile.read(is);
                }
                try (InputStream is = allTranslations.getInputStream(Constants.MUSIC_NAMES_PATH)) {
                    return MSBTReader.readMsbt(is);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        final var gameFontFuture = CompletableFuture.supplyAsync(() -> {
            try (InputStream is = new Yaz0InputStream(new BufferedInputStream(fileGetter.apply(Constants.FONTS_PATH)))) {
                final SARCFile sarc = SARCFile.read(is);
                try (InputStream fontIn = sarc.getInputStream(Constants.FONT_PATH)) {
                    return BFTTF.createFont(fontIn);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (FontFormatException e) {
                throw new IllegalStateException(e);
            }
        });

        final var songAudioFuture = songsFuture.thenCompose(songs -> getSongAudio(fileGetter, songs));

        songs = songsFuture.join();
        songNames = songNamesFuture.join();
        gameFont = gameFontFuture.join();
        songAudio = songAudioFuture.join();

        return System.nanoTime() - start;
    }

    private static CompletableFuture<Map<String, SongAudio>> getSongAudio(FileGetter<?> fileGetter, List<SongInfo> songs) {
        final List<CompletableFuture<Map.Entry<String, SongAudio>>> futures = new ArrayList<>(songs.size());
        for (final SongInfo song : songs) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    final AudioData data;
                    try (var is = fileGetter.apply(Constants.STREAM_PATH + '/' + song.songId() + ".bfstm")) {
                        data = new BCFstmReader().read(is);
                    } catch (NoSuchFileException e) {
                        return null;
                    }
                    final ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
                    try (DeflaterOutputStream dos = new DeflaterOutputStream(compressedData)) {
                        new WaveWriter().writeToStream(data, dos);
                    }
                    final Pcm16Format pcm = data.getFormat(Pcm16Format.class);
                    return Map.entry(
                        song.songId(),
                        new SongAudio(pcm.isLooping(), pcm.getLoopStart(), pcm.getLoopEnd(), compressedData.toByteArray())
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored ->
            futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public static List<SongInfo> getSongs() {
        return songs;
    }

    public static String getSongName(String songId) {
        return songNames.get(songId);
    }

    public static SongAudio getSongAudio(String songId) {
        return songAudio.get(songId);
    }

    public static Font getGameFont() {
        return gameFont;
    }
}
