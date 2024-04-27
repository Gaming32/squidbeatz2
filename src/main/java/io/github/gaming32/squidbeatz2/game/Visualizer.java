package io.github.gaming32.squidbeatz2.game;

import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import org.apache.commons.math3.complex.Complex;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.IntSupplier;

// Based on https://github.com/jiwoojang/AudioVisualizer
public class Visualizer {
    private static final int BUFFER_SIZE = 4096;
    private static final int RAW_BUCKET_COUNT = 200;
    private static final int OUTPUT_BUCKET_COUNT = 4;
    private static final float MAX_SAMPLE_INDEX = BUFFER_SIZE / 2f;
    private static final double RAW_BUCKET_MULTIPLIER = Math.pow(10, Math.log10(BUFFER_SIZE / 2f) / (double)RAW_BUCKET_COUNT);
    private static final int RAW_BUCKETS_PER_OUTPUT = RAW_BUCKET_COUNT / OUTPUT_BUCKET_COUNT;
    private static final float[] WINDOW_CACHE = new float[BUFFER_SIZE];

    static {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            WINDOW_CACHE[i] = (float)(0.54 - 0.46 * Math.cos(2 * Math.PI * i / BUFFER_SIZE));
        }
    }

    private final IntSupplier frameGetter;
    private short[] samples;
    private final FloatArrayList points = new FloatArrayList();

    private final Complex[] activeSamples = new Complex[BUFFER_SIZE];
    private final FloatArrayList outputBuckets = new FloatArrayList();

    public Visualizer(IntSupplier frameGetter, short[] samples) {
        this.frameGetter = frameGetter;
        this.samples = samples;
        Arrays.fill(activeSamples, Complex.ZERO);
    }

    public void setSamples(short[] samples) {
        this.samples = samples;
    }

    public FloatList getPoints() {
        return points;
    }

    public void update() {
        collectSamples();

        fft(activeSamples);

        outputBuckets.clear();
        outputBuckets.ensureCapacity(OUTPUT_BUCKET_COUNT);

        int bucketCount = 1;
        double outputBucketAverage = 0;

        for (double i = 1; i <= MAX_SAMPLE_INDEX; i *= RAW_BUCKET_MULTIPLIER) {
            outputBucketAverage += Math.log10(activeSamples[(int)i].abs());
            bucketCount++;

            if (bucketCount % RAW_BUCKETS_PER_OUTPUT == 0) {
                outputBuckets.add((float)(outputBucketAverage / RAW_BUCKETS_PER_OUTPUT));

                outputBucketAverage = 0;
            }
        }

        points.clear();
        for (float i = 1; i < MAX_SAMPLE_INDEX; i *= 1.05f) {
            points.add((float)Math.min((-20 * Math.log(activeSamples[(int)i].abs())), 0));
        }
    }

    private void collectSamples() {
        final int frame = frameGetter.getAsInt();
        if (frame + BUFFER_SIZE < samples.length) {
            for (int i = 0; i < BUFFER_SIZE; i++) {
                activeSamples[i] = Complex.valueOf(samples[frame + i] * WINDOW_CACHE[i]);
            }
        }
    }

    private static void fft(Complex[] data) {
        final int N = data.length;
        int k = N, n;
        final double thetaT = Math.PI / N;
        Complex phiT = Complex.valueOf(Math.cos(thetaT), -Math.sin(thetaT)), T;
        while (k > 1) {
            n = k;
            k >>= 1;
            phiT = phiT.multiply(phiT);
            T = Complex.ONE;
            for (int l = 0; l < k; l++) {
                for (int a = l; a < N; a += n) {
                    final int b = a + k;
                    final Complex t = data[a].subtract(data[b]);
                    data[a] = data[a].add(data[b]);
                    data[b] = t.multiply(T);
                }
                T = T.multiply(phiT);
            }
        }

        final int m = IntMath.log2(N, RoundingMode.DOWN);
        for (int a = 0; a < N; a++) {
            int b = a;
            b = (((b & 0xaaaaaaaa) >>> 1) | ((b & 0x55555555) << 1));
            b = (((b & 0xcccccccc) >>> 2) | ((b & 0x33333333) << 2));
            b = (((b & 0xf0f0f0f0) >>> 4) | ((b & 0x0f0f0f0f) << 4));
            b = (((b & 0xff00ff00) >>> 8) | ((b & 0x00ff00ff) << 8));
            b = ((b >>> 16) | (b << 16)) >>> (32 - m);
            if (b > a) {
                final Complex t = data[a];
                data[a] = data[b];
                data[b] = t;
            }
        }

        final Complex f = Complex.valueOf(1.0 / Math.sqrt(N));
        for (int i = 0; i < N; i++) {
            data[i] = data[i].multiply(f);
        }
    }
}
