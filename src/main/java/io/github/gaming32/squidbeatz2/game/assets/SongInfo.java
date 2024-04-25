package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.byml.BymlReader;
import io.github.gaming32.squidbeatz2.byml.node.BymlArray;
import io.github.gaming32.squidbeatz2.byml.node.BymlCollection;
import io.github.gaming32.squidbeatz2.byml.node.BymlHash;
import io.github.gaming32.squidbeatz2.byml.node.BymlNode;
import io.github.gaming32.squidbeatz2.byml.node.BymlNumber;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record SongInfo(
    Dance dance,
    long delay,
    NoteData normalNotes,
    NoteData extremeNotes,
    String songId
) {
    public static List<SongInfo> loadFromCompressedArchive(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return loadFromCompressedArchive(is);
        }
    }

    public static List<SongInfo> loadFromCompressedArchive(InputStream is) throws IOException {
        return loadFromArchive(new Yaz0InputStream(is));
    }

    public static List<SongInfo> loadFromArchive(InputStream is) throws IOException {
        return loadFromArchive(SARCFile.read(is));
    }

    public static List<SongInfo> loadFromArchive(SARCFile sarcFile) throws IOException {
        return loadList(
            (BymlArray)loadBymlFile(sarcFile, "IkaRadioList.byml"),
            (BymlHash)loadBymlFile(sarcFile, "IkaRadioNoteData.byml")
        );
    }

    private static BymlCollection loadBymlFile(SARCFile sarcFile, String filename) throws IOException {
        final InputStream is = sarcFile.getInputStream(filename);
        if (is == null) {
            throw new IllegalArgumentException("Couldn't find " + filename);
        }
        return BymlReader.read(is.readAllBytes());
    }

    public static List<SongInfo> loadList(BymlArray songData, BymlHash noteData) {
        return songData.asList()
            .stream()
            .map(n -> load((BymlHash)n, noteData))
            .toList();
    }

    public static SongInfo load(BymlHash songData, BymlHash allNoteData) {
        final Dance dance = switch (songData.get("Dance").toString()) {
            case "Boy" -> Dance.BOY;
            case "Jerry" -> Dance.JERRY;
            case "Girl" -> Dance.GIRL;
            default -> throw new IllegalArgumentException("Unknown dance: " + songData.get("Dance"));
        };
        final BymlNumber delay = (BymlNumber)songData.get("Delay");
        final NoteData normalNotes = NoteData.load((BymlHash)allNoteData.get(songData.get("NoteNormal").toString()));
        final NoteData extremeNotes = NoteData.load((BymlHash)allNoteData.get(songData.get("NoteExtreme").toString()));
        final String songId = songData.get("Strm").toString();
        return new SongInfo(dance, delay != null ? delay.longValue() : 0L, normalNotes, extremeNotes, songId);
    }

    public record NoteData(
        long endNote,
        long[] downLeft,
        long[] downRight,
        long[] upLeft,
        long[] upRight
    ) {
        public static NoteData load(BymlHash data) {
            return new NoteData(
                ((BymlNumber)data.get("EndNote")).longValue(),
                loadLongArray(data.get("NotesDownL")),
                loadLongArray(data.get("NotesDownR")),
                loadLongArray(data.get("NotesUpL")),
                loadLongArray(data.get("NotesUpR"))
            );
        }

        private static long[] loadLongArray(BymlNode node) {
            final BymlArray array = (BymlArray)node;
            final long[] result = new long[array.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = ((BymlNumber)array.get(i)).longValue();
            }
            return result;
        }
    }
}
