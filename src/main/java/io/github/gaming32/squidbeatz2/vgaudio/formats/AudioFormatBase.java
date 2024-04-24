package io.github.gaming32.squidbeatz2.vgaudio.formats;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;

public abstract class AudioFormatBase<F extends AudioFormatBase<F, B>, B extends AudioFormatBaseBuilder<F, B>> implements IAudioFormat {
    private final List<AudioTrack> _tracks;
    public final int sampleRate;
    public final int channelCount;
    public final int unalignedSampleCount;
    public final int unalignedLoopStart;
    public final int unalignedLoopEnd;
    public final boolean looping;
    public final List<AudioTrack> tracks;

    protected AudioFormatBase() {
        _tracks = null;
        sampleRate = 0;
        channelCount = 0;
        unalignedSampleCount = 0;
        unalignedLoopStart = 0;
        unalignedLoopEnd = 0;
        looping = false;
        tracks = null;
    }

    protected AudioFormatBase(B builder) {
        unalignedSampleCount = builder.sampleCount;
        sampleRate = builder.sampleRate;
        channelCount = builder.getChannelCount();
        looping = builder.looping;
        unalignedLoopStart = builder.loopStart;
        unalignedLoopEnd = builder.loopEnd;
        _tracks = builder.tracks;
        tracks = _tracks != null && !_tracks.isEmpty() ? _tracks : Lists.newArrayList(AudioTrack.getDefaultTrackList(channelCount));
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public int getSampleCount() {
        return unalignedSampleCount;
    }

    @Override
    public int getLoopStart() {
        return unalignedLoopStart;
    }

    @Override
    public int getLoopEnd() {
        return unalignedLoopEnd;
    }

    @Override
    public boolean isLooping() {
        return looping;
    }

    @Override
    public IAudioFormat getChannels(int... channelRange) {
        if (channelRange == null) {
            throw new NullPointerException("channelRange");
        }

        return getChannelsInternal(channelRange);
    }

    protected abstract F getChannelsInternal(int[] channelRange);

    @Override
    public F withLoop(boolean loop) {
        return getCloneBuilder().withLoop(loop).build();
    }

    @Override
    public F withLoop(boolean loop, int loopStart, int loopEnd) {
        return getCloneBuilder().withLoop(loop, loopStart, loopEnd).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean tryAdd(IAudioFormat format, MutableObject<IAudioFormat> result) {
        result.setValue(null);
        try {
            // This cast is checked in C# because of reified types, but I don't feel like reifying this one
            // The add method should throw an exception in the case of an incompatible type, due to the bridge method
            // in the subclass performing a checked cast.
            result.setValue(add((F)format));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public F add(F format) {
        if (format.unalignedSampleCount != unalignedSampleCount) {
            throw new IllegalArgumentException("Only audio streams of the same length can be added to each other.");
        }

        return addInternal(format);
    }

    protected abstract F addInternal(F format);

    public abstract B getCloneBuilder();

    protected B getCloneBuilderBase(B builder) {
        builder.sampleCount = unalignedSampleCount;
        builder.sampleRate = sampleRate;
        builder.looping = looping;
        builder.loopStart = unalignedLoopStart;
        builder.loopEnd = unalignedLoopEnd;
        builder.tracks = _tracks;
        return builder;
    }
}
