package io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Arrays;

import static io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath.*;
import static io.github.gaming32.squidbeatz2.vgaudio.utilities.Helpers.*;

public class GcAdpcmEncoder {
    public static byte[] encode(short[] pcm, short[] coefs) {
        return encode(pcm, coefs, null);
    }

    public static byte[] encode(short[] pcm, short[] coefs, GcAdpcmParameters config) {
        if (config == null) {
            config = new GcAdpcmParameters();
        }
        final int sampleCount = config.sampleCount == -1 ? pcm.length : config.sampleCount;
        final byte[] adpcm = new byte[sampleCountToByteCount(sampleCount)];

        final short[] pcmBuffer = new short[2 + SAMPLES_PER_FRAME];
        final byte[] adpcmBuffer = new byte[BYTES_PER_FRAME];

        pcmBuffer[0] = config.history2;
        pcmBuffer[1] = config.history1;

        final int frameCount = (sampleCount - 1 + SAMPLES_PER_FRAME) / SAMPLES_PER_FRAME;
        final AdpcmEncodeBuffers buffers = new AdpcmEncodeBuffers();

        for (int frame = 0; frame < frameCount; frame++) {
            final int samplesToCopy = Math.min(sampleCount - frame * SAMPLES_PER_FRAME, SAMPLES_PER_FRAME);
            System.arraycopy(pcm, frame * SAMPLES_PER_FRAME, pcmBuffer, 2, samplesToCopy);
            Arrays.fill(pcmBuffer, 2 + samplesToCopy, SAMPLES_PER_FRAME + 2, (short)0);

            dspEncodeFrame(pcmBuffer, SAMPLES_PER_FRAME, adpcmBuffer, coefs, buffers);

            System.arraycopy(adpcmBuffer, 0, adpcm, frame * BYTES_PER_FRAME, sampleCountToByteCount(samplesToCopy));

            pcmBuffer[0] = pcmBuffer[14];
            pcmBuffer[1] = pcmBuffer[15];
        }

        return adpcm;
    }

    public static void dspEncodeFrame(short[] pcmInOut, int sampleCount, byte[] adpcmOut, short[] coefsIn) {
        dspEncodeFrame(pcmInOut, sampleCount, adpcmOut, coefsIn, null);
    }

    public static void dspEncodeFrame(short[] pcmInOut, int sampleCount, byte[] adpcmOut, short[] coefsIn, AdpcmEncodeBuffers b) {
        if (b == null) {
            b = new AdpcmEncodeBuffers();
        }

        for (int i = 0; i < 8; i++) {
            b.coefs[i][0] = coefsIn[i * 2];
            b.coefs[i][1] = coefsIn[i * 2 + 1];
        }

        final MutableInt outScale = new MutableInt();
        final MutableDouble outTotalDistance = new MutableDouble();
        for (int i = 0; i < 8; i++) {
            outScale.setValue(b.scale[i]);
            outTotalDistance.setValue(b.totalDistance[i]);
            dspEncodeCoef(pcmInOut, sampleCount, b.coefs[i], b.pcmOut[i], b.adpcmOut[i], outScale, outTotalDistance);
            b.scale[i] = outScale.intValue();
            b.totalDistance[i] = outTotalDistance.doubleValue();
        }

        int bestCoef = 0;

        double min = Double.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            if (b.totalDistance[i] < min) {
                min = b.totalDistance[i];
                bestCoef = i;
            }
        }

        for (int s = 0; s < sampleCount; s++) {
            pcmInOut[s + 2] = (short)b.pcmOut[bestCoef][s + 2];
        }

        adpcmOut[0] = combineNibbles(bestCoef, b.scale[bestCoef]);

        for (int s = sampleCount; s < 14; s++) {
            b.adpcmOut[bestCoef][s] = 0;
        }

        for (int i = 0; i < 7; i++) {
            adpcmOut[i + 1] = combineNibbles(b.adpcmOut[bestCoef][i * 2], b.adpcmOut[bestCoef][i * 2 + 1]);
        }
    }

    private static void dspEncodeCoef(
        short[] pcmIn,
        int sampleCount,
        short[] coefs,
        int[] pcmOut,
        int[] adpcmOut,
        MutableInt scalePower,
        MutableDouble totalDistance
    ) {
        int maxOverflow;
        int maxDistance = 0;

        pcmOut[0] = pcmIn[0];
        pcmOut[1] = pcmIn[1];

        for (int s = 0; s < sampleCount; s++) {
            final int inputSample = pcmIn[s + 2];
            final int predictedSample = (pcmIn[s] * coefs[1] + pcmIn[s + 1] * coefs[0]) / 2048;
            int distance = inputSample - predictedSample;
            distance = clamp16(distance);
            if (Math.abs(distance) > Math.abs(maxDistance)) {
                maxDistance = distance;
            }
        }

        scalePower.setValue(0);
        while (scalePower.intValue() <= 12 && (maxDistance > 7 || maxDistance < -8)) {
            maxDistance /= 2;
            scalePower.increment();
        }
        scalePower.setValue(scalePower.intValue() <= 1 ? -1 : scalePower.intValue() - 2);

        do {
            scalePower.increment();
            final int scale = (1 << scalePower.intValue()) * 2048;
            totalDistance.setValue(0);
            maxOverflow = 0;

            for (int s = 0; s < sampleCount; s++) {
                final int inputSample = pcmIn[s + 2] * 2048;
                final int predictedSample = pcmOut[s] * coefs[1] + pcmOut[s + 1] * coefs[0];
                final int distance = inputSample - predictedSample;
                final int unclampedAdpcmSample = distance > 0
                    ? (int)((double)((float)distance / scale) + 0.4999999f)
                    : (int)((double)((float)distance / scale) - 0.4999999f);

                final int adpcmSample = clamp4(unclampedAdpcmSample);
                if (adpcmSample != unclampedAdpcmSample) {
                    final int overflow = Math.abs(unclampedAdpcmSample - adpcmSample);
                    if (overflow > maxOverflow) {
                        maxOverflow = overflow;
                    }
                }

                adpcmOut[s] = adpcmSample;

                final int decodedDistance = adpcmSample * scale;
                final int correctedSample = predictedSample + decodedDistance;
                final int scaledSample = (correctedSample + 1024) >> 11;
                pcmOut[s + 2] = clamp16(scaledSample);
                final double actualDistance = pcmIn[s + 2] - pcmOut[s + 2];
                totalDistance.add(actualDistance * actualDistance);
            }

            for (int x = maxOverflow + 8; x > 256; x >>= 1) {
                if (scalePower.incrementAndGet() >= 12) {
                    scalePower.setValue(11);
                }
            }
        } while (scalePower.intValue() < 12 && maxOverflow > 1);
    }

    public static class AdpcmEncodeBuffers {
        public final short[][] coefs = new short[8][2];
        public final int[][] pcmOut = new int[8][16];
        public final int[][] adpcmOut = new int[8][14];
        public final int[] scale = new int[8];
        public final double[] totalDistance = new double[8];
    }
}
