package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.Common;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.NwVersion;

import java.io.IOException;
import java.io.InputStream;

public class StreamInfo {
    public int codec;
    public boolean looping;
    public int channelCount;
    public int regionCount;
    public int sampleRate;
    public int loopStart;
    public int sampleCount;

    public int interleaveCount;
    public int interleaveSize;
    public int samplesPerInterleave;
    public int lastBlockSizeWithoutPadding;
    public int lastBlockSamples;
    public int lastBlockSize;
    public int bytesPerSeekTableEntry;
    public int samplesPerSeekTableEntry;

    public int audioDataOffset;
    public Reference audioReference;
    public Reference regionReference;

    public int regionInfoSize;
    public int loopStartUnaligned;
    public int loopEndUnaligned;
    public int checksum;

    public StreamInfo readBfstm(InputStream is, boolean bigEndian, NwVersion version) throws IOException {
        codec = is.read();
        looping = is.read() != 0;
        channelCount = is.read();
        regionCount = is.read();
        sampleRate = Util.readInt(is, bigEndian);
        loopStart = Util.readInt(is, bigEndian);
        sampleCount = Util.readInt(is, bigEndian);
        interleaveCount = Util.readInt(is, bigEndian);
        interleaveSize = Util.readInt(is, bigEndian);
        samplesPerInterleave = Util.readInt(is, bigEndian);
        lastBlockSizeWithoutPadding = Util.readInt(is, bigEndian);
        lastBlockSamples = Util.readInt(is, bigEndian);
        lastBlockSize = Util.readInt(is, bigEndian);
        bytesPerSeekTableEntry = Util.readInt(is, bigEndian);
        samplesPerSeekTableEntry = Util.readInt(is, bigEndian);
        audioReference = new Reference(is, bigEndian);

        if (Common.includeRegionInfo(version)) {
            regionInfoSize = Util.readShort(is, bigEndian);
            is.skipNBytes(2);
            regionReference = new Reference(is, bigEndian);
        }

        if (Common.includeUnalignedLoop(version)) {
            loopStartUnaligned = Util.readInt(is, bigEndian);
            loopEndUnaligned = Util.readInt(is, bigEndian);
        }

        if (Common.includeChecksum(version)) {
            checksum = Util.readInt(is, bigEndian);
        }

        return this;
    }

    public static <S extends InputStream & Seekable> StreamInfo readBfwav(S is, boolean bigEndian, NwVersion version) throws IOException {
        final StreamInfo info = new StreamInfo();
        info.codec = is.read();
        info.looping = is.read() != 0;
        is.seekBy(2);
        info.sampleRate = Util.readInt(is, bigEndian);
        info.loopStart = Util.readInt(is, bigEndian);
        info.sampleRate = Util.readInt(is, bigEndian);

        if (Common.includeUnalignedLoopWave(version)) {
            info.loopStartUnaligned = Util.readInt(is, bigEndian);
        } else {
            is.seekBy(4);
        }

        info.channelCount = Util.readInt(is, bigEndian);
        is.seekBy(-4);
        return info;
    }

    public static StreamInfo readBrstm(InputStream is, boolean bigEndian) throws IOException {
        final StreamInfo info = new StreamInfo();
        info.codec = is.read();
        info.looping = is.read() != 0;
        info.channelCount = is.read();
        is.skipNBytes(1);

        info.sampleRate = Util.readShort(is, bigEndian);
        is.skipNBytes(2);

        info.loopStart = Util.readInt(is, bigEndian);
        info.sampleCount = Util.readInt(is, bigEndian);
        info.audioDataOffset = Util.readInt(is, bigEndian);
        info.interleaveCount = Util.readInt(is, bigEndian);
        info.interleaveSize = Util.readInt(is, bigEndian);
        info.samplesPerInterleave = Util.readInt(is, bigEndian);
        info.lastBlockSizeWithoutPadding = Util.readInt(is, bigEndian);
        info.lastBlockSamples = Util.readInt(is, bigEndian);
        info.lastBlockSize = Util.readInt(is, bigEndian);
        info.samplesPerSeekTableEntry = Util.readInt(is, bigEndian);

        return info;
    }
}
