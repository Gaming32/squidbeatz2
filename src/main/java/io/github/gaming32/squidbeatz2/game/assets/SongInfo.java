package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.byml.BymlReader;
import io.github.gaming32.squidbeatz2.byml.node.BymlArray;
import io.github.gaming32.squidbeatz2.byml.node.BymlCollection;
import io.github.gaming32.squidbeatz2.byml.node.BymlHash;
import io.github.gaming32.squidbeatz2.byml.node.BymlNode;
import io.github.gaming32.squidbeatz2.byml.node.BymlNumber;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16FormatBuilder;
import io.github.gaming32.szslib.sarc.SARCFile;
import io.github.gaming32.szslib.yaz0.Yaz0InputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record SongInfo(
    Dance dance,
    int delay,
    int forcedDispId,
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
        final BymlNumber forcedDispId = (BymlNumber)songData.get("DispId");
        final NoteData normalNotes = NoteData.load((BymlHash)allNoteData.get(songData.get("NoteNormal").toString()));
        final NoteData extremeNotes = NoteData.load((BymlHash)allNoteData.get(songData.get("NoteExtreme").toString()));
        final String songId = songData.get("Strm").toString();
        return new SongInfo(
            dance,
            delay != null ? delay.intValue() : 0,
            forcedDispId != null ? forcedDispId.intValue() : 0,
            normalNotes, extremeNotes, songId
        );
    }

    public int getDisplayNumber(int index) {
        return forcedDispId != 0 ? forcedDispId : index + 1;
    }

    public AudioData insertSilence(AudioData data) {
        if (delay == 0) {
            return data;
        }
        final Pcm16Format pcm = data.getFormat(Pcm16Format.class);
        final short[][] channels = new short[pcm.getChannelCount()][];
        final int newSamples = (int)((long)delay * pcm.sampleRate / 1000L);
        final int originalSamples = pcm.getSampleCount();
        final int totalSamples = newSamples + originalSamples;
        for (int i = 0; i < channels.length; i++) {
            final short[] newChannel = new short[totalSamples];
            System.arraycopy(pcm.channels[i], 0, newChannel, newSamples, originalSamples);
            channels[i] = newChannel;
        }
        final Pcm16FormatBuilder builder = pcm.getCloneBuilder();
        builder.channels = channels;
        builder.sampleCount = totalSamples;
        builder.withLoop(pcm.looping, pcm.getLoopStart() + newSamples, pcm.getLoopEnd() + newSamples);
        return new AudioData(builder.build());
    }

    public record NoteData(
        int endNote,
        int[] downLeft,
        int[] downRight,
        int[] upLeft,
        int[] upRight
    ) {
        public static NoteData load(BymlHash data) {
            return new NoteData(
                ((BymlNumber)data.get("EndNote")).intValue(),
                loadIntArray(data.get("NotesDownL")),
                loadIntArray(data.get("NotesDownR")),
                loadIntArray(data.get("NotesUpL")),
                loadIntArray(data.get("NotesUpR"))
            );
        }

        private static int[] loadIntArray(BymlNode node) {
            final BymlArray array = (BymlArray)node;
            final int[] result = new int[array.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = ((BymlNumber)array.get(i)).intValue();
            }
            return result;
        }
    }
}
