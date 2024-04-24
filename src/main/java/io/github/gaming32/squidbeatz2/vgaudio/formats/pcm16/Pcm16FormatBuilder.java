package io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16;

import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioFormatBaseBuilder;

public class Pcm16FormatBuilder extends AudioFormatBaseBuilder<Pcm16Format, Pcm16FormatBuilder> {
    public short[][] channels;

    public Pcm16FormatBuilder(short[][] channels, int sampleRate) {
        if (channels == null || channels.length < 1) {
            throw new IllegalArgumentException("Channels parameter cannot be empty or null");
        }

        this.channels = channels;
        sampleCount = channels[0].length;
        this.sampleRate = sampleRate;

        for (final short[] channel : channels) {
            if (channel == null) {
                throw new IllegalArgumentException("All provided channels must be non-null");
            }

            if (channel.length != sampleCount) {
                throw new IllegalArgumentException("All channels must have the same sample count");
            }
        }
    }

    @Override
    public Pcm16Format build() {
        return new Pcm16Format(this);
    }

    @Override
    public int getChannelCount() {
        return channels.length;
    }
}
