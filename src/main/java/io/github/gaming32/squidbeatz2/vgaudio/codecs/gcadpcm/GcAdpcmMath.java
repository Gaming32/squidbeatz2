package io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm;

public class GcAdpcmMath {
    public static final int BYTES_PER_FRAME = 8;
    public static final int SAMPLES_PER_FRAME = 14;
    public static final int NIBBLES_PER_FRAME = 16;

    public static int nibbleCountToSampleCount(int nibbleCount) {
        final int frames = nibbleCount / NIBBLES_PER_FRAME;
        final int extraNibbles = nibbleCount % NIBBLES_PER_FRAME;
        final int extraSamples = extraNibbles < 2 ? 0 : extraNibbles - 2;

        return SAMPLES_PER_FRAME * frames + extraSamples;
    }

    public static int sampleCountToNibbleCount(int sampleCount) {
        final int frames = sampleCount / SAMPLES_PER_FRAME;
        final int extraSamples = sampleCount % SAMPLES_PER_FRAME;
        final int extraNibbles = extraSamples == 0 ? 0 : extraSamples + 2;

        return NIBBLES_PER_FRAME * frames + extraNibbles;
    }

    public static int sampleCountToByteCount(int sampleCount) {
        return (sampleCountToNibbleCount(sampleCount) + 1) / 2;
    }

    public static int byteCountToSampleCount(int byteCount) {
        return nibbleCountToSampleCount(byteCount * 2);
    }
}
