package io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm;

import static io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath.*;
import static io.github.gaming32.squidbeatz2.vgaudio.utilities.Helpers.*;

public class GcAdpcmDecoder {
    public static short[] decode(byte[] adpcm, short[] coefficients, GcAdpcmParameters config) {
        if (config == null) {
            config = new GcAdpcmParameters();
            config.sampleCount = byteCountToSampleCount(adpcm.length);
        }
        final short[] pcm = new short[config.sampleCount];

        if (config.sampleCount == 0) {
            return pcm;
        }

        final int frameCount = (config.sampleCount - 1 + SAMPLES_PER_FRAME) / SAMPLES_PER_FRAME;
        int currentSample = 0;
        int outIndex = 0;
        int inIndex = 0;
        short hist1 = config.history1;
        short hist2 = config.history2;

        for (int i = 0; i < frameCount; i++) {
            final byte predictorScale = adpcm[inIndex++];
            final int scale = (1 << getLowNibble(predictorScale)) * 2048;
            final int predictor = getHighNibble(predictorScale);
            final short coef1 = coefficients[predictor * 2];
            final short coef2 = coefficients[predictor * 2 + 1];

            final int samplesToRead = Math.min(SAMPLES_PER_FRAME, config.sampleCount - currentSample);

            for (int s = 0; s < samplesToRead; s++) {
                final int adpcmSample = s % 2 == 0 ? getHighNibbleSigned(adpcm[inIndex]) : getLowNibbleSigned(adpcm[inIndex++]);
                final int distance = scale * adpcmSample;
                final int predictedSample = coef1 * hist1 + coef2 * hist2;
                final int correctedSample = predictedSample + distance;
                final int scaledSample = (correctedSample + 1024) >> 11;
                final short clampedSample = clamp16(scaledSample);

                hist2 = hist1;
                hist1 = clampedSample;

                pcm[outIndex++] = clampedSample;
                currentSample++;
            }
        }
        return pcm;
    }

    public static byte getPredictorScale(byte[] adpcm, int sample) {
        return adpcm[sample / SAMPLES_PER_FRAME * BYTES_PER_FRAME];
    }
}
