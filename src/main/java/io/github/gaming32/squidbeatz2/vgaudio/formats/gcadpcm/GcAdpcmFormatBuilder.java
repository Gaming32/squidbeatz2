package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioFormatBaseBuilder;

import java.util.Collection;

public class GcAdpcmFormatBuilder extends AudioFormatBaseBuilder<GcAdpcmFormat, GcAdpcmFormatBuilder> {
    public GcAdpcmChannel[] channels;
    public int alignmentMultiple;

    public GcAdpcmFormatBuilder(Collection<GcAdpcmChannel> channels, int sampleRate) {
        if (channels == null || channels.isEmpty()) {
            throw new IllegalArgumentException("Channels parameter cannot be empty or null");
        }

        this.channels = channels.toArray(GcAdpcmChannel[]::new);
        sampleCount = this.channels[0].unalignedSampleCount;
        this.sampleRate = sampleRate;

        for (final GcAdpcmChannel channel : this.channels) {
            if (channel == null) {
                throw new IllegalArgumentException("All provided channels must be non-null");
            }

            if (channel.unalignedSampleCount != sampleCount) {
                throw new IllegalArgumentException("All channels must have the same sample count");
            }
        }
    }

    @Override
    public int getChannelCount() {
        return channels.length;
    }

    @Override
    public GcAdpcmFormat build() {
        return new GcAdpcmFormat(this);
    }

    public GcAdpcmFormatBuilder withAlignment(int loopAlignmentMultiple) {
        alignmentMultiple = loopAlignmentMultiple;
        return this;
    }
}
