package io.github.gaming32.squidbeatz2.vgaudio.formats;

import com.google.common.collect.Lists;

import java.util.List;

public abstract class AudioFormatBaseBuilder<F extends AudioFormatBase<F, B>, B extends AudioFormatBaseBuilder<F, B>> {
    protected boolean looping;
    protected int loopStart;
    protected int loopEnd;
    protected int sampleCount;
    protected int sampleRate;
    protected List<AudioTrack> tracks;

    public abstract int getChannelCount();

    public abstract F build();

    public B withLoop(boolean loop, int loopStart, int loopEnd) {
        if (!loop) {
            return withLoop(false);
        }

        if (loopStart < 0 || loopStart > sampleCount || loopEnd < 0 || loopEnd > sampleCount) {
            throw new IllegalArgumentException("Loop points must be less than the number of samples and non-negative");
        }

        if (loopEnd < loopStart) {
            throw new IllegalArgumentException("The loop end must be greater than the loop start");
        }

        looping = true;
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;

        return self();
    }

    public B withLoop(boolean loop) {
        looping = loop;
        loopStart = 0;
        loopEnd = loop ? sampleCount : 0;
        return self();
    }

    public B withTracks(Iterable<AudioTrack> tracks) {
        this.tracks = tracks != null ? Lists.newArrayList(tracks) : null;
        return self();
    }

    @SuppressWarnings("unchecked")
    private B self() {
        return (B)this;
    }
}
