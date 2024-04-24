package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.PrefetchData;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.StreamInfo;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmChannel;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmChannelBuilder;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmChannelInfo;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmFormat;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmFormatBuilder;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16FormatBuilder;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.List;

public class Common {
    private static final NwVersion INCLUDE_UNALIGNED_LOOP_BFSTM = new NwVersion(0, 4);
    private static final NwVersion INCLUDE_CHECKSUM_BFSTM = new NwVersion(0, 5);

    private static final NwVersion INCLUDE_REGION_BCSTM = new NwVersion(2, 1);
    private static final NwVersion INCLUDE_UNALIGNED_LOOP_BCSTM = new NwVersion(2, 3);

    private static final NwVersion INCLUDE_UNALIGNED_LOOP_BFWAV = new NwVersion(0, 1, 2);
    private static final NwVersion INCLUDE_UNALIGNED_LOOP_BCWAV = new NwVersion(2, 1, 1);

    public static boolean includeRegionInfo(NwVersion version) {
        return (version.major >= 2 && version.packedVersion >= INCLUDE_REGION_BCSTM.packedVersion) ||
               version.major == 0;
    }

    public static boolean includeUnalignedLoop(NwVersion version) {
        return (version.major == 0 && version.packedVersion >= INCLUDE_UNALIGNED_LOOP_BFSTM.packedVersion) ||
               (version.major >= 2 && version.packedVersion >= INCLUDE_UNALIGNED_LOOP_BCSTM.packedVersion);
    }

    public static boolean includeChecksum(NwVersion version) {
        return version.major == 0 && version.packedVersion >= INCLUDE_CHECKSUM_BFSTM.packedVersion;
    }

    public static boolean includeUnalignedLoopWave(NwVersion version) {
        return (version.major == 0 && version.packedVersion >= INCLUDE_UNALIGNED_LOOP_BFWAV.packedVersion) ||
               (version.major >= 2 && version.packedVersion >= INCLUDE_UNALIGNED_LOOP_BCWAV.packedVersion);
    }

    public static int samplesToBytes(int sampleCount, int codec) {
        return switch (codec) {
            case NwCodec.GC_ADPCM -> GcAdpcmMath.sampleCountToByteCount(sampleCount);
            case NwCodec.PCM_16_BIT -> sampleCount * 2;
            case NwCodec.PCM_8_BIT -> sampleCount;
            default -> 0;
        };
    }

    public static int bytesToSamples(int byteCount, int codec) {
        return switch (codec) {
            case NwCodec.GC_ADPCM -> GcAdpcmMath.byteCountToSampleCount(byteCount);
            case NwCodec.PCM_16_BIT -> byteCount / 2;
            case NwCodec.PCM_8_BIT -> byteCount;
            default -> 0;
        };
    }

    public static IAudioFormat toAudioStream(BxstmStructure structure) {
        if (structure.prefetchData != null) {
            final PrefetchData pdat = structure.prefetchData.getFirst();
            structure.streamInfo.looping = false;
            structure.streamInfo.sampleCount = pdat.sampleCount;
        }
        return switch (structure.streamInfo.codec) {
            case NwCodec.GC_ADPCM -> toAdpcmStream(structure);
            case NwCodec.PCM_16_BIT -> toPcm16Stream(structure);
            case NwCodec.PCM_8_BIT -> throw new NotImplementedException("PCM_8_BIT");
            default -> null;
        };
    }

    private static GcAdpcmFormat toAdpcmStream(BxstmStructure structure) {
        final StreamInfo streamInfo = structure.streamInfo;
        final List<GcAdpcmChannelInfo> channelInfo = structure.channelInfo.channels;
        final GcAdpcmChannel[] channels = new GcAdpcmChannel[streamInfo.channelCount];

        for (int c = 0; c < channels.length; c++) {
            final GcAdpcmChannelBuilder channelBuilder = new GcAdpcmChannelBuilder(
                structure.audioData[c], channelInfo.get(c).coefs, streamInfo.sampleCount
            );
            channelBuilder.gain = channelInfo.get(c).gain;
            channelBuilder.startContext = channelInfo.get(c).start;

            channelBuilder.withLoop(streamInfo.looping, streamInfo.loopStart, streamInfo.sampleCount)
                .withLoopContext(
                    streamInfo.loopStart,
                    channelInfo.get(c).loop.predScale(),
                    channelInfo.get(c).loop.hist1(),
                    channelInfo.get(c).loop.hist2()
                );

            if (structure.seekTable != null) {
                channelBuilder.withSeekTable(structure.seekTable[c], streamInfo.samplesPerSeekTableEntry);
            }

            channels[c] = channelBuilder.build();
        }

        return new GcAdpcmFormatBuilder(Arrays.asList(channels), streamInfo.sampleRate)
            .withTracks(structure.trackInfo != null ? structure.trackInfo.tracks : null)
            .withLoop(streamInfo.looping, streamInfo.loopStart, streamInfo.sampleCount)
            .build();
    }

    private static Pcm16Format toPcm16Stream(BxstmStructure structure) {
        final StreamInfo info = structure.streamInfo;
        final short[][] channels = Arrays.stream(structure.audioData)
            .map(x -> Util.toShortArray(x, structure.bigEndian))
            .toArray(short[][]::new);
        return new Pcm16FormatBuilder(channels, info.sampleRate)
            .withTracks(structure.trackInfo != null ? structure.trackInfo.tracks : null)
            .withLoop(info.looping, info.loopStart, info.sampleCount)
            .build();
    }
}
