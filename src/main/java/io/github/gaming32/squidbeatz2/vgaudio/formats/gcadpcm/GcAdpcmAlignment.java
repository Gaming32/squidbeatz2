package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmDecoder;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmEncoder;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmParameters;
import io.github.gaming32.squidbeatz2.vgaudio.utilities.Helpers;

import static io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath.*;

public class GcAdpcmAlignment {
    public final byte[] adpcmAligned;
    public final short[] pcmAligned;

    public final int alignmentMultiple;
    public final int loopStart;
    public final int loopStartAligned;
    public final int loopEnd;
    public final int sampleCountAligned;
    public final boolean alignmentNeeded;

    public GcAdpcmAlignment(int multiple, int loopStart, int loopEnd, byte[] adpcm, short[] coefs) {
        alignmentMultiple = multiple;
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
        alignmentNeeded = !Helpers.loopPointsAreAligned(loopStart, multiple);

        if (!alignmentNeeded) {
            adpcmAligned = null;
            pcmAligned = null;
            loopStartAligned = 0;
            sampleCountAligned = 0;
            return;
        }

        final int loopLength = loopEnd - loopStart;
        loopStartAligned = Helpers.getNextMultiple(loopStart, multiple);
        sampleCountAligned = loopEnd + (loopStartAligned - loopStart);

        adpcmAligned = new byte[sampleCountToByteCount(sampleCountAligned)];
        pcmAligned = new short[sampleCountAligned];

        final int framesToKeep = loopEnd / SAMPLES_PER_FRAME;
        final int bytesToKeep = framesToKeep * BYTES_PER_FRAME;
        final int samplesToKeep = framesToKeep * SAMPLES_PER_FRAME;
        final int samplesToEncode = sampleCountAligned - samplesToKeep;

        final GcAdpcmParameters param = new GcAdpcmParameters();
        param.sampleCount = loopEnd;
        final short[] oldPcm = GcAdpcmDecoder.decode(adpcm, coefs, param);
        System.arraycopy(oldPcm, 0, pcmAligned, 0, loopEnd);
        final short[] newPcm = new short[samplesToEncode];

        System.arraycopy(oldPcm, samplesToKeep, newPcm, 0, loopEnd - samplesToKeep);

        for (int currentSample = loopEnd - samplesToKeep; currentSample < samplesToEncode; currentSample += loopLength) {
            System.arraycopy(pcmAligned, loopStart, newPcm, currentSample, Math.min(loopLength, samplesToEncode - currentSample));
        }

        param.sampleCount = samplesToEncode;
        param.history1 = samplesToKeep < 1 ? 0 : oldPcm[samplesToKeep - 1];
        param.history2 = samplesToKeep < 2 ? 0 : oldPcm[samplesToKeep - 2];

        final byte[] newAdpcm = GcAdpcmEncoder.encode(newPcm, coefs, param);
        System.arraycopy(adpcm, 0, adpcmAligned, 0, bytesToKeep);
        System.arraycopy(newAdpcm, 0, adpcmAligned, bytesToKeep, newAdpcm.length);

        final short[] decodedPcm = GcAdpcmDecoder.decode(newAdpcm, coefs, param);
        System.arraycopy(decodedPcm, 0, pcmAligned, samplesToKeep, samplesToEncode);
    }
}
