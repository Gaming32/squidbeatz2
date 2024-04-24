package io.github.gaming32.squidbeatz2.vgaudio.codecs;

public class CodecParameters {
    public int sampleCount = -1;

    public CodecParameters() {
    }

    protected CodecParameters(CodecParameters source) {
        if (source == null) return;
        sampleCount = source.sampleCount;
    }
}
