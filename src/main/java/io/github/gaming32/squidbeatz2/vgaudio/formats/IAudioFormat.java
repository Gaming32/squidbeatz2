package io.github.gaming32.squidbeatz2.vgaudio.formats;

import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import org.apache.commons.lang3.mutable.MutableObject;

public interface IAudioFormat {
    int getSampleCount();

    int getSampleRate();

    int getChannelCount();

    int getLoopStart();

    int getLoopEnd();

    boolean isLooping();

    IAudioFormat withLoop(boolean loop, int loopStart, int loopEnd);

    IAudioFormat withLoop(boolean loop);

    Pcm16Format toPcm16();

    IAudioFormat encodeFromPcm16(Pcm16Format pcm16);

    IAudioFormat getChannels(int... channelRange);

    boolean tryAdd(IAudioFormat format, MutableObject<IAudioFormat> result);
}
