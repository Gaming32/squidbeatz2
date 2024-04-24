package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmChannelInfo;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ChannelInfo {
    public final List<GcAdpcmChannelInfo> channels = new ArrayList<>();
    public final List<Integer> waveAudioOffsets = new ArrayList<>();

    public <S extends InputStream & Seekable> ChannelInfo readBfstm(S is, boolean bigEndian) throws IOException {
        final int baseOffset = (int)is.tell();

        final ReferenceTable table = new ReferenceTable(is, bigEndian, baseOffset);

        for (final Reference channelInfo : table.references) {
            is.seek(channelInfo.getAbsoluteOffset());
            if (channelInfo.isType(ReferenceType.WAVE_CHANNEL_INFO)) {
                final Reference audioData = new Reference(is, bigEndian);
                waveAudioOffsets.add(audioData.offset());
            }

            final Reference adpcmInfo = new Reference(is, bigEndian, channelInfo.getAbsoluteOffset());

            if (adpcmInfo.isType(ReferenceType.GC_ADPCM_INFO)) {
                is.seek(adpcmInfo.getAbsoluteOffset());

                final GcAdpcmChannelInfo channel = new GcAdpcmChannelInfo();
                channel.coefs = new short[16];
                for (int i = 0; i < 16; i++) {
                    channel.coefs[i] = (short)Util.readShort(is, bigEndian);
                }
                channel.start = new GcAdpcmContext(is, bigEndian);
                channel.loop = new GcAdpcmContext(is, bigEndian);
                channels.add(channel);
            }
        }

        return this;
    }

    public <S extends InputStream & Seekable> ChannelInfo readBrstm(S is, boolean bigEndian, Reference reference) throws IOException {
        final int channelCount = is.read();
        is.seekBy(3);

        final List<Reference> references = new ArrayList<>(channelCount);
        for (int i = 0; i < channelCount; i++) {
            references.add(new Reference(is, bigEndian, reference.baseOffset()));
        }

        for (final Reference channelInfo : references) {
            is.seek(channelInfo.getAbsoluteOffset());
            final Reference adpcmInfo = new Reference(is, bigEndian, reference.baseOffset());

            if (adpcmInfo.offset() > 0) {
                is.seek(adpcmInfo.getAbsoluteOffset());

                final GcAdpcmChannelInfo channel = new GcAdpcmChannelInfo();
                channel.coefs = new short[16];
                for (int i = 0; i < 16; i++) {
                    channel.coefs[i] = (short)Util.readShort(is, bigEndian);
                }
                channel.gain = (short)Util.readShort(is, bigEndian);
                channel.start = new GcAdpcmContext(is, bigEndian);
                channel.loop = new GcAdpcmContext(is, bigEndian);
                channels.add(channel);
            }
        }

        return this;
    }
}
