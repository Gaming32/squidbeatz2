package io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm;

import java.util.Arrays;

import static io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath.SAMPLES_PER_FRAME;

public class GcAdpcmCoefficients {
    public static short[] calculateCoefficients(short[] source) {
        final int frameCount = (source.length - 1 + SAMPLES_PER_FRAME) / SAMPLES_PER_FRAME;

        final short[] pcmHistBuffer = new short[28];

        final short[] coefs = new short[16];

        final double[] vec1 = new double[3];
        final double[] vec2 = new double[3];
        final double[] buffer = new double[3];

        final double[][] mtx = new double[3][3];

        final int[] vecIdxs = new int[3];

        final double[][] records = new double[frameCount * 2][3];

        int recordCount = 0;

        final double[][] vecBest = new double[8][3];

        for (int sample = 0, remaining = source.length; sample < source.length; sample += 14, remaining -= 14) {
            Arrays.fill(pcmHistBuffer, 14, 28, (short)0);
            System.arraycopy(source, sample, pcmHistBuffer, 14, Math.min(14, remaining));

            innerProductMerge(vec1, pcmHistBuffer);
            if (Math.abs(vec1[0]) > 10.0) {
                outerProductMerge(mtx, pcmHistBuffer);
                if (!analyzeRanges(mtx, vecIdxs, buffer)) {
                    bidirectionalFilter(mtx, vecIdxs, vec1);
                    if (!quadraticMerge(vec1)) {
                        finishRecord(vec1, records, recordCount);
                        recordCount++;
                    }
                }
            }
            System.arraycopy(pcmHistBuffer, 14, pcmHistBuffer, 0, 14);
        }

        vec1[0] = 1.0;
        vec1[1] = 0.0;
        vec1[2] = 0.0;

        for (int z = 0; z < recordCount; z++) {
            matrixFilter(records, z, vecBest[0], mtx);
            for (int y = 1; y <= 2; y++) {
                vec1[y] += vecBest[0][y];
            }
        }
        for (int y = 1; y <= 2; y++) {
            vec1[y] /= recordCount;
        }

        mergeFinishRecord(vec1, vecBest[0]);

        int exp = 1;
        for (int w = 0; w < 3;) {
            vec2[0] = 0.0;
            vec2[1] = -1.0;
            vec2[2] = 0.0;
            for (int i = 0; i < exp; i++) {
                for (int y = 0; y <= 2; y++) {
                    vecBest[exp + i][y] = (0.01 * vec2[y]) + vecBest[i][y];
                }
            }
            ++w;
            exp = 1 << w;
            filterRecords(vecBest, exp, records, recordCount);
        }

        for (int z = 0; z < 8; z++) {
            double d;
            d = -vecBest[z][1] * 2048.0;
            if (d > 0.0) {
                coefs[z * 2] = d > Short.MAX_VALUE ? Short.MAX_VALUE : (short)Math.round(d);
            } else {
                coefs[z * 2] = d < Short.MIN_VALUE ? Short.MIN_VALUE : (short)Math.round(d);
            }
            d = -vecBest[z][2] * 2048.0;
            if (d > 0.0) {
                coefs[z * 2 + 1] = d > Short.MAX_VALUE ? Short.MAX_VALUE : (short)Math.round(d);
            } else {
                coefs[z * 2 + 1] = d < Short.MIN_VALUE ? Short.MIN_VALUE : (short)Math.round(d);
            }
        }
        return coefs;
    }

    private static void innerProductMerge(double[] vecOut, short[] pcmBuf) {
        for (int i = 0; i <= 2; i++) {
            vecOut[i] = 0f;
            for (int x = 0; x < 14; x++) {
                vecOut[i] -= pcmBuf[14 + x - i] * pcmBuf[14 + x];
            }
        }
    }

    private static void outerProductMerge(double[][] mtxOut, short[] pcmBuf) {
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 2; y++) {
                mtxOut[x][y] = 0.0;
                for (int z = 0; z < 14; z++) {
                    mtxOut[x][y] += pcmBuf[14 + z - x] * pcmBuf[14 + z - y];
                }
            }
        }
    }

    private static boolean analyzeRanges(double[][] mtx, int[] vecIdxsOut, double[] recips) {
        double val, tmp, min, max;

        for (int x = 1; x <= 2; x++) {
            val = Math.max(Math.abs(mtx[x][1]), Math.abs(mtx[x][2]));
            if (val < Double.MIN_VALUE) { // I don't think the devs of this lib realized how small C#'s double.Epsilon is
                return true;
            }

            recips[x] = 1.0 / val;
        }

        int maxIndex = 0;
        for (int i = 1; i <= 2; i++) {
            for (int x = 1; x < i; x++) {
                tmp = mtx[x][i];
                for (int y = 1; y < x; y++) {
                    tmp -= mtx[x][y] * mtx[y][i];
                }
                mtx[x][i] = tmp;
            }

            val = 0.0;
            for (int x = i; x <= 2; x++) {
                tmp = mtx[x][i];
                for (int y = 1; y < i; y++) {
                    tmp -= mtx[x][y] * mtx[y][i];
                }

                mtx[x][i] = tmp;
                tmp = Math.abs(tmp) * recips[x];
                if (tmp >= val) {
                    val = tmp;
                    maxIndex = x;
                }
            }

            if (maxIndex != i) {
                for (int y = 1; y <= 2; y++) {
                    tmp = mtx[maxIndex][y];
                    mtx[maxIndex][y] = mtx[i][y];
                    mtx[i][y] = tmp;
                }
                recips[maxIndex] = recips[i];
            }

            vecIdxsOut[i] = maxIndex;

            if (i != 2) {
                tmp = 1.0 / mtx[i][i];
                for (int x = i + 1; x <= 2; x++) {
                    mtx[x][i] *= tmp;
                }
            }
        }

        min = 1.0e10;
        max = 0.0;
        for (int i = 1; i <= 2; i++) {
            tmp = Math.abs(mtx[i][i]);
            if (tmp < min) {
                min = tmp;
            }
            if (tmp > max) {
                max = tmp;
            }
        }

        return min / max < 1.0e-10;
    }

    private static void bidirectionalFilter(double[][] mtx, int[] vecIdxs, double[] vecOut) {
        double tmp;

        for (int i = 1, x = 0; i <= 2; i++) {
            final int index = vecIdxs[i];
            tmp = vecOut[index];
            vecOut[index] = vecOut[i];
            if (x != 0) {
                for (int y = x; y <= i - 1; y++) {
                    tmp -= vecOut[y] * mtx[i][i];
                }
            } else if (tmp != 0.0) {
                x = i;
            }
            vecOut[i] = tmp;
        }

        for (int i = 2; i > 0; i--) {
            tmp = vecOut[i];
            for (int y = i + 1; y <= 2; y++) {
                tmp -= vecOut[y] * mtx[i][y];
            }
            vecOut[i] = tmp / mtx[i][i];
        }

        vecOut[0] = 1.0;
    }

    private static boolean quadraticMerge(double[] inOutVec) {
        final double v2 = inOutVec[2];
        final double tmp = 1.0 - (v2 * v2);

        if (tmp == 0.0) {
            return true;
        }

        final double v0 = (inOutVec[0] - (v2 * v2)) / tmp;
        final double v1 = (inOutVec[1] - (inOutVec[1] * v2)) / tmp;

        inOutVec[0] = v0;
        inOutVec[1] = v1;

        return Math.abs(v1) > 1.0;
    }

    private static void finishRecord(double[] inR, double[][] outR, int row) {
        for (int z = 1; z <= 2; z++) {
            if (inR[z] >= 1.0) {
                inR[z] = 0.9999999999;
            } else if (inR[z] <= -1.0) {
                inR[z] = -0.9999999999;
            }
        }
        outR[row][0] = 1.0;
        outR[row][1] = (inR[2] * inR[1]) + inR[1];
        outR[row][2] = inR[2];
    }

    private static void matrixFilter(double[][] src, int row, double[] dst, double[][] mtx) {
        mtx[2][0] = 1.0;
        for (int i = 1; i <= 2; i++) {
            mtx[2][i] = -src[row][i];
        }

        for (int i = 2; i > 0; i--) {
            final double val = 1.0 - (mtx[i][i] * mtx[i][i]);
            for (int y = 1; y <= i; y++) {
                mtx[i - 1][y] = ((mtx[i][i] * mtx[i][y]) + mtx[i][y]) / val;
            }
        }

        dst[0] = 1.0;
        for (int i = 1; i <= 2; i++) {
            dst[i] = 0.0;
            for (int y = 1; y <= i; y++) {
                dst[i] += mtx[i][y] * dst[i - y];
            }
        }
    }

    private static void mergeFinishRecord(double[] src, double[] dst) {
        final double[] tmp = new double[3];
        double val = src[0];

        dst[0] = 1.0;
        for (int i = 1; i <= 2; i++) {
            double v2 = 0.0;
            for (int y = 1; y < i; y++) {
                v2 += dst[y] * src[i - y];
            }

            if (val > 0.0) {
                dst[i] = -(v2 + src[i]) / val;
            } else {
                dst[i] = 0.0;
            }

            tmp[i] = dst[i];

            for (int y = 1; y < i; y++) {
                dst[y] += dst[i] * dst[i - y];
            }

            val *= 1.0 - (dst[i] * dst[i]);
        }

        finishRecord(tmp, dst);
    }

    private static void finishRecord(double[] inR, double[] outR) {
        for (int z = 1; z <= 2; z++) {
            if (inR[z] >= 1.0) {
                inR[z] = 0.9999999999;
            } else if (inR[z] <= -1.0) {
                inR[z] = -0.9999999999;
            }
        }
        outR[0] = 1.0;
        outR[1] = (inR[2] * inR[1]) + inR[1];
        outR[2] = inR[2];
    }

    private static void filterRecords(double[][] vecBest, int exp, double[][] records, int recordCount) {
        final double[][] bufferList = new double[8][3];

        final double[][] mtx = new double[3][3];

        final int[] buffer1 = new int[8];
        final double[] buffer2 = new double[3];

        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < exp; y++) {
                buffer1[y] = 0;
                for (int i = 0; i <= 2; i++) {
                    bufferList[y][i] = 0.0;
                }
            }
            for (int z = 0; z < recordCount; z++) {
                int index = 0;
                double value = 1.0e30;
                for (int i = 0; i < exp; i++) {
                    final double tempVal = contrastVectors(vecBest[i], records, z);
                    if (tempVal < value) {
                        value = tempVal;
                        index = i;
                    }
                }
                buffer1[index]++;
                matrixFilter(records, z, buffer2, mtx);
                for (int i = 0; i <= 2; i++) {
                    bufferList[index][i] += buffer2[i];
                }
            }

            for (int i = 0; i < exp; i++) {
                if (buffer1[i] > 0) {
                    for (int y = 0; y <= 2; y++) {
                        bufferList[i][y] /= buffer1[i];
                    }
                }
            }

            for (int i = 0; i < exp; i++) {
                mergeFinishRecord(bufferList[i], vecBest[i]);
            }
        }
    }

    private static double contrastVectors(double[] source1, double[][] source2, int row) {
        final double val = (source2[row][2] * source2[row][1] - source2[row][1]) / (1.0 - source2[row][2] * source2[row][2]);
        final double val1 = (source1[0] * source1[0]) + (source1[1] * source1[1]) + (source1[2] * source1[2]);
        final double val2 = (source1[0] * source1[1]) + (source1[1] * source1[2]);
        final double val3 = source1[0] * source1[2];
        return val1 + (2.0 * val * val2) + (2.0 * (-source2[row][1] * val - source2[row][2]) * val3);
    }
}
