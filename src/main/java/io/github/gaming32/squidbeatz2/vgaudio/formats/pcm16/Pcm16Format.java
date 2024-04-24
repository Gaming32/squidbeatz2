package io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16;

import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioFormatBase;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

public class Pcm16Format extends AudioFormatBase<Pcm16Format, Pcm16FormatBuilder> {
    public final short[][] channels;

    public Pcm16Format() {
        channels = new short[0][];
    }

    public Pcm16Format(short[][] channels, int sampleRate) {
        this(new Pcm16FormatBuilder(channels, sampleRate));
    }

    public Pcm16Format(Pcm16FormatBuilder b) {
        super(b);
        channels = b.channels;
    }

    @Override
    public Pcm16Format toPcm16() {
        return getCloneBuilder().build();
    }

    @Override
    public IAudioFormat encodeFromPcm16(Pcm16Format pcm16) {
        return pcm16.getCloneBuilder().build();
    }

    @Override
    protected Pcm16Format addInternal(Pcm16Format pcm16) {
        final Pcm16FormatBuilder copy = getCloneBuilder();
        copy.channels = ArrayUtils.addAll(channels, pcm16.channels);
        return copy.build();
    }

    @Override
    protected Pcm16Format getChannelsInternal(int[] channelRange) {
        final List<short[]> channels = new ArrayList<>(channelRange.length);

        for (final int i : channelRange) {
            if (i < 0 || i >= this.channels.length) {
                throw new IllegalArgumentException("Channel " + i + " does not exist.");
            }
            channels.add(this.channels[i]);
        }

        final Pcm16FormatBuilder copy = getCloneBuilder();
        copy.channels = channels.toArray(short[][]::new);
        return copy.build();
    }

    public static Pcm16FormatBuilder getBuilder(short[][] channels, int sampleRate) {
        return new Pcm16FormatBuilder(channels, sampleRate);
    }

    @Override
    public Pcm16FormatBuilder getCloneBuilder() {
        return getCloneBuilderBase(new Pcm16FormatBuilder(channels, sampleRate));
    }
}
