package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmCoefficients;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmEncoder;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioFormatBase;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16FormatBuilder;
import io.github.gaming32.squidbeatz2.vgaudio.utilities.Helpers;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class GcAdpcmFormat extends AudioFormatBase<GcAdpcmFormat, GcAdpcmFormatBuilder> {
    public final GcAdpcmChannel[] channels;

    public final int alignmentMultiple;

    public GcAdpcmFormat() {
        channels = new GcAdpcmChannel[0];
        alignmentMultiple = 0;
    }

    public GcAdpcmFormat(GcAdpcmChannel[] channels, int sampleRate) {
        this(new GcAdpcmFormatBuilder(Arrays.asList(channels), sampleRate));
    }

    public GcAdpcmFormat(GcAdpcmFormatBuilder b) {
        super(b);
        channels = b.channels;
        alignmentMultiple = b.alignmentMultiple;

        IntStream.range(0, channels.length).parallel().forEach(i -> {
            channels[i] = channels[i]
                .getCloneBuilder()
                .withLoop(isLooping(), unalignedLoopStart, unalignedLoopEnd)
                .withLoopAlignment(b.alignmentMultiple)
                .build();
        });
    }

    private int getAlignmentSamples() {
        return Helpers.getNextMultiple(unalignedLoopStart, alignmentMultiple) - unalignedLoopStart;
    }

    @Override
    public int getLoopStart() {
        return unalignedLoopStart + getAlignmentSamples();
    }

    @Override
    public int getLoopEnd() {
        return unalignedLoopEnd + getAlignmentSamples();
    }

    @Override
    public int getSampleCount() {
        return getAlignmentSamples() == 0 ? unalignedSampleCount : getLoopEnd();
    }

    @Override
    public Pcm16Format toPcm16() {
        final short[][] pcmChannels = new short[channels.length][];
        IntStream.range(0, channels.length).parallel().forEach(i ->
            pcmChannels[i] = channels[i].getPcmAudio()
        );

        return new Pcm16FormatBuilder(pcmChannels, sampleRate)
            .withLoop(looping, getLoopStart(), getLoopEnd())
            .withTracks(tracks)
            .build();
    }

    @Override
    public IAudioFormat encodeFromPcm16(Pcm16Format pcm16) {
        final GcAdpcmChannel[] channels = new GcAdpcmChannel[pcm16.channelCount];

        IntStream.range(0, pcm16.channelCount).parallel().forEach(i ->
            channels[i] = encodeChannel(pcm16.channels[i], pcm16.getSampleCount())
        );

        return new GcAdpcmFormatBuilder(Arrays.asList(channels), pcm16.sampleRate)
            .withLoop(pcm16.looping, pcm16.getLoopStart(), pcm16.getLoopEnd())
            .withTracks(pcm16.tracks)
            .build();
    }

    @Override
    protected GcAdpcmFormat addInternal(GcAdpcmFormat adpcm) {
        final GcAdpcmFormatBuilder copy = getCloneBuilder();
        copy.channels = ArrayUtils.addAll(channels, adpcm.channels);
        return copy.build();
    }

    @Override
    protected GcAdpcmFormat getChannelsInternal(int[] channelRange) {
        final List<GcAdpcmChannel> channels = new ArrayList<>(channelRange.length);

        for (int i : channelRange) {
            if (i < 0 || i >= this.channels.length) {
                throw new IllegalArgumentException("Channel " + i + " does not exist.");
            }
            channels.add(this.channels[i]);
        }

        GcAdpcmFormatBuilder copy = getCloneBuilder();
        copy.channels = channels.toArray(GcAdpcmChannel[]::new);
        copy = copy.withTracks(tracks);
        return copy.build();
    }

    public byte[] buildSeekTable(int entryCount, boolean bigEndian) {
        final short[][] tables = new short[channels.length][];

        IntStream.range(0, tables.length).parallel().forEach(i ->
            tables[i] = channels[i].getSeekTable()
        );

        short[] table = Util.interleave(tables, 2);

        table = Arrays.copyOf(table, entryCount * 2 * channels.length);
        return Util.toByteArray(table, bigEndian);
    }

    public static GcAdpcmFormatBuilder getBuilder(GcAdpcmChannel[] channels, int sampleRate) {
        return new GcAdpcmFormatBuilder(Arrays.asList(channels), sampleRate);
    }

    @Override
    public GcAdpcmFormatBuilder getCloneBuilder() {
        GcAdpcmFormatBuilder builder = new GcAdpcmFormatBuilder(Arrays.asList(channels), sampleRate);
        builder = getCloneBuilderBase(builder);
        builder = builder.withTracks(tracks);
        builder.alignmentMultiple = alignmentMultiple;
        return builder;
    }

    public GcAdpcmFormat withAlignment(int loopStartAlignment) {
        return getCloneBuilder()
            .withAlignment(loopStartAlignment)
            .build();
    }

    private static GcAdpcmChannel encodeChannel(short[] pcm, int sampleCount) {
        final short[] coefs = GcAdpcmCoefficients.calculateCoefficients(pcm);
        final byte[] adpcm = GcAdpcmEncoder.encode(pcm, coefs);

        return new GcAdpcmChannel(adpcm, coefs, sampleCount);
    }
}
