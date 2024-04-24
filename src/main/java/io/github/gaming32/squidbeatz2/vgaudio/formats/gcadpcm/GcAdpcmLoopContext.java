package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmDecoder;

public class GcAdpcmLoopContext extends GcAdpcmContext {
    public final int loopStart;
    public final boolean isSelfCalculated;

    public GcAdpcmLoopContext(short predScale, short hist1, short hist2, int loopStart, boolean isSelfCalculated) {
        super(predScale, hist1, hist2);
        this.loopStart = loopStart;
        this.isSelfCalculated = isSelfCalculated;
    }

    public GcAdpcmLoopContext(byte[] adpcm, short[] pcm, int loopStart) {
        super(getPredScale(adpcm, loopStart), getHist1(pcm, loopStart), getHist2(pcm, loopStart));
        this.loopStart = loopStart;
        isSelfCalculated = true;
    }

    public static byte getPredScale(byte[] adpcm, int sampleNum) {
        return GcAdpcmDecoder.getPredictorScale(adpcm, sampleNum);
    }

    public static short getHist1(short[] pcm, int sampleNum) {
        return sampleNum < 1 ? 0 : pcm != null ? pcm[sampleNum - 1] : 0;
    }

    public static short getHist2(short[] pcm, int sampleNum) {
        return sampleNum < 2 ? 0 : pcm != null ? pcm[sampleNum - 2] : 0;
    }
}
