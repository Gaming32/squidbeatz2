package io.github.gaming32.squidbeatz2.vgaudio.formats;

import java.util.stream.IntStream;

public class AudioTrack {
    public int volume = 0x7f;
    public int panning = 0x40;
    public int channelCount;
    public int channelLeft;
    public int channelRight;

    public int surroundPanning;
    public int flags;

    public AudioTrack() {
    }

    public AudioTrack(int channelCount, int channelLeft, int channelRight) {
        this.channelCount = channelCount;
        this.channelLeft = channelLeft;
        this.channelRight = channelRight;
    }

    public AudioTrack(int channelCount, int channelLeft, int channelRight, int volume, int panning) {
        this.channelCount = channelCount;
        this.channelLeft = channelLeft;
        this.channelRight = channelRight;
        this.volume = volume;
        this.panning = panning;
    }

    public static Iterable<AudioTrack> getDefaultTrackList(int channelCount) {
        return IntStream.range(0, (channelCount + 1) / 2).mapToObj(i -> {
            final int trackChannelCount = Math.min(channelCount - i * 2, 2);
            return new AudioTrack(trackChannelCount, i * 2, trackChannelCount == 2 ? i * 2 + 1 : 0);
        })::iterator;
    }
}
