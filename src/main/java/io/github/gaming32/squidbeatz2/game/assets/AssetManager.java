package io.github.gaming32.squidbeatz2.game.assets;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import io.github.gaming32.squidbeatz2.Constants;
import io.github.gaming32.squidbeatz2.bntx.BntxFile;
import io.github.gaming32.squidbeatz2.bntx.Texture;
import io.github.gaming32.squidbeatz2.font.BFTTF;
import io.github.gaming32.squidbeatz2.game.GameTheme;
import io.github.gaming32.squidbeatz2.lightbfres.BfresExternalFilesLoader;
import io.github.gaming32.squidbeatz2.msbt.MSBTReader;
import io.github.gaming32.squidbeatz2.texture.TextureConverter;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableByteArrayInputStream;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.BCFstmReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.wave.WaveWriter;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import org.apache.commons.io.function.Uncheck;
import org.jetbrains.annotations.Nullable;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

public class AssetManager {
    private static List<SongInfo> songs;
    private static Table<TranslationCategory, String, String> translations;
    private static Map<String, SongAudio> songAudio;
    private static Font gameFont;
    private static Map<GameTheme, ThemeAssets> themeAssets;
    private static Map<Dance, List<BufferedImage>> dances;
    private static ResourceBundle resourceBundle;

    /**
     * @return The nanoseconds taken loading assets
     */
    public static long loadAssets(FileGetter<?> fileGetter, FloatConsumer progressConsumer) {
        final ProgressManager progressManager = new ProgressManager(progressConsumer, 7);
        final long start = System.nanoTime();

        final var songsFuture = CompletableFuture.supplyAsync(() -> {
            try (InputStream is = new BufferedInputStream(fileGetter.apply(Constants.SONG_INFO_PATH))) {
                return SongInfo.loadFromCompressedArchive(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).thenApply(progressManager::taskDone);

        final var translationsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                final SARCFile allTranslations;
                try (InputStream is = new Yaz0InputStream(new BufferedInputStream(fileGetter.apply(Constants.ENGLISH_TRANSLATIONS_PATH)))) {
                    allTranslations = SARCFile.read(is);
                }
                final ImmutableTable.Builder<TranslationCategory, String, String> result = ImmutableTable.builder();
                for (final TranslationCategory category : TranslationCategory.values()) {
                    try (InputStream is = allTranslations.getInputStream(category.filename)) {
                        for (final var entry : MSBTReader.readMsbt(is).entrySet()) {
                            result.put(category, entry.getKey(), entry.getValue());
                        }
                    }
                }
                return result.build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).thenApply(progressManager::taskDone);

        final var resourceBundleFuture = CompletableFuture.supplyAsync(() -> ResourceBundle.getBundle("SquidBeatz2"));

        final var themeAssetsFuture = loadThemeAssets(fileGetter, progressManager)
            .thenApply(progressManager::taskDone);

        final var dancesFuture = loadDances(fileGetter, progressManager)
            .thenApply(progressManager::taskDone);

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
        }).thenApply(progressManager::taskDone);

        final var songAudioFuture = songsFuture
            .thenCompose(songs -> loadSongAudio(fileGetter, songs, progressManager))
            .thenApply(progressManager::taskDone);

        songs = songsFuture.join();
        translations = translationsFuture.join();
        songAudio = songAudioFuture.join();
        gameFont = gameFontFuture.join();
        themeAssets = themeAssetsFuture.join();
        dances = dancesFuture.join();
        resourceBundle = resourceBundleFuture.join();

        progressManager.complete();
        return System.nanoTime() - start;
    }

    private static CompletableFuture<Map<GameTheme, ThemeAssets>> loadThemeAssets(FileGetter<?> fileGetter, ProgressManager progressManager) {
        progressManager.addTasks(Dance.values().length);
        final List<CompletableFuture<Map.Entry<GameTheme, ThemeAssets>>> futures = new ArrayList<>(GameTheme.values().length);
        for (final GameTheme theme : GameTheme.values()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    final ThemeAssets assets = new ThemeAssets();
                    assets.load(loadBfres(fileGetter, "Model/MiniGame" + theme.resourceSuffix + ".Nin_NX_NVN.szs")::iterator);
                    return Map.entry(theme, assets);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).thenApply(progressManager::taskDone));
        }
        return collectMapFutures(futures);
    }

    private static CompletableFuture<Map<Dance, List<BufferedImage>>> loadDances(FileGetter<?> fileGetter, ProgressManager progressManager) {
        progressManager.addTasks(Dance.values().length);
        final List<CompletableFuture<Map.Entry<Dance, List<BufferedImage>>>> futures = new ArrayList<>(Dance.values().length);
        for (final Dance dance : Dance.values()) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return Map.entry(
                        dance,
                        loadBfres(fileGetter, "Model/MiniGameDance" + dance.danceId + ".Nin_NX_NVN.szs")
                            .sorted(Comparator.comparing(t -> t.name))
                            .map(TextureConverter::toBufferedImage)
                            .toList()
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).thenApply(progressManager::taskDone));
        }
        return collectMapFutures(futures);
    }

    private static CompletableFuture<Map<String, SongAudio>> loadSongAudio(
        FileGetter<?> fileGetter, List<SongInfo> songs, ProgressManager progressManager
    ) {
        progressManager.addTasks(songs.size());
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
                    return Map.entry(song.songId(), new SongAudio(pcm, compressedData.toByteArray()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).thenApply(progressManager::taskDone));
        }
        return collectMapFutures(futures);
    }

    private static <K, V> CompletableFuture<Map<K, V>> collectMapFutures(List<CompletableFuture<Map.Entry<K, V>>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenApply(ignored ->
            futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    private static Stream<Texture> loadBfres(FileGetter<?> fileGetter, String path) throws IOException {
        final byte[] bfres;
        try (InputStream is = new Yaz0InputStream(new BufferedInputStream(fileGetter.apply(path)))) {
            final SARCFile sarc = SARCFile.read(is);
            final String bfresFile = sarc.listFiles()
                .stream()
                .filter(x -> x.endsWith(".bfres"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No .bfres files found!"));
            //noinspection DataFlowIssue
            bfres = sarc.getInputStream(bfresFile).readAllBytes();
        }
        return BfresExternalFilesLoader.loadExternalFiles(new SeekableByteArrayInputStream(bfres))
            .entrySet()
            .stream()
            .filter(e -> e.getKey().endsWith(".bntx"))
            .map(Map.Entry::getValue)
            .map(SeekableByteArrayInputStream::new)
            .map(i -> Uncheck.apply(new BntxFile()::load, i))
            .map(f -> f.textures)
            .flatMap(List::stream);
    }

    public static List<SongInfo> getSongs() {
        return songs;
    }

    public static String getTranslation(TranslationCategory category, String id) {
        return translations.get(category, id);
    }

    @Nullable
    public static SongAudio getSongAudio(String songId) {
        return songAudio.get(songId);
    }

    public static Font getGameFont() {
        return gameFont;
    }

    public static ThemeAssets getThemeAssets(GameTheme theme) {
        return themeAssets.get(theme);
    }

    public static List<BufferedImage> getDance(Dance dance) {
        return dances.get(dance);
    }

    public static ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    private static final class ProgressManager {
        final FloatConsumer consumer;
        final AtomicInteger progress = new AtomicInteger();
        final AtomicInteger maxProgress;

        ProgressManager(FloatConsumer consumer, int baseTasks) {
            this.consumer = consumer;
            maxProgress = new AtomicInteger(baseTasks);
        }

        <T> T taskDone(T t) {
            accept(progress.incrementAndGet(), maxProgress.get());
            return t;
        }

        void addTasks(int count) {
            accept(progress.get(), maxProgress.addAndGet(count));
        }

        void complete() {
            consumer.accept(1f);
        }

        private void accept(int progress, int maxProgress) {
            consumer.accept((float)progress / maxProgress);
        }
    }
}
