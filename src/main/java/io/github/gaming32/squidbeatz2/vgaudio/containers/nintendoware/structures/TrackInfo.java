package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioTrack;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class TrackInfo {
    public final List<AudioTrack> tracks = new ArrayList<>();
    public int type;

    public <S extends InputStream & Seekable> TrackInfo readBfstm(S is, boolean bigEndian) throws IOException {
        final int baseOffset = (int)is.tell();

        final ReferenceTable table = new ReferenceTable(is, bigEndian, baseOffset);

        for (final Reference trackInfo : table.references) {
            is.seek(trackInfo.getAbsoluteOffset());

            final AudioTrack track = new AudioTrack();
            track.volume = is.read();
            track.panning = is.read();
            track.surroundPanning = is.read();
            track.flags = is.read();

            final Reference trackRef = new Reference(is, bigEndian, trackInfo.getAbsoluteOffset());
            is.seek(trackRef.getAbsoluteOffset());

            track.channelCount = Util.readInt(is, bigEndian);
            track.channelLeft = is.read();
            track.channelRight = is.read();
            tracks.add(track);
        }
        return this;
    }

    public <S extends InputStream & Seekable> TrackInfo readBrstm(S is, boolean bigEndian, Reference reference) throws IOException {
        final int trackCount = is.read();
        type = is.read();
        is.seekBy(2);

        final List<Reference> references = new ArrayList<>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            references.add(new Reference(is, bigEndian, reference.baseOffset()));
        }

        for (final Reference trackInfo : references) {
            is.seek(trackInfo.getAbsoluteOffset());

            final AudioTrack track = new AudioTrack();
            if (trackInfo.getDataType() == BrstmTrackType.STANDARD) {
                track.volume = is.read();
                track.panning = is.read();
                is.seekBy(6);
            }

            track.channelCount = is.read();
            track.channelLeft = is.read();
            track.channelRight = is.read();

            tracks.add(track);
        }
        return this;
    }
}
