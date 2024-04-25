package io.github.gaming32.squidbeatz2.texture;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DDSCompressor {
    public static BufferedImage decompressBc3(byte[] data, int width, int height, boolean isSrgb) {
        final int w = (width + 3) / 4;
        final int h = (height + 3) / 4;

        final byte[] output = new byte[w * h * 64];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int iOffs = (y * w + x) * 16;

                final byte[] tile = bCnDecodeTile(data, iOffs + 8, false);

                final byte[] alpha = new byte[8];

                alpha[0] = data[iOffs];
                alpha[1] = data[iOffs + 1];

                calculateBc3Alpha(alpha);

                final int alphaLow = get32(data, iOffs + 2);
                final int alphaHigh = get16(data, iOffs + 6);

                final long alphaCh = alphaLow | (long)alphaHigh << 32;

                int tOffset = 0;

                for (int ty = 0; ty < 4; ty++) {
                    for (int tx = 0; tx < 4; tx++) {
                        final int oOffset = (x * 4 + tx + (y * 4 + ty) * w * 4) * 4;

                        final byte alphaPx = alpha[(int)((alphaCh >>> (ty * 12 + tx * 3)) & 7)];

                        output[oOffset] = tile[tOffset];
                        output[oOffset + 1] = tile[tOffset + 1];
                        output[oOffset + 2] = tile[tOffset + 2];
                        output[oOffset + 3] = alphaPx;

                        tOffset += 4;
                    }
                }
            }
        }

        return TextureConverter.getBufferedImage(output, w * 4, h * 4);
    }

    private static byte[] bCnDecodeTile(byte[] input, int offset, boolean isBc1) {
        final Color[] clut = new Color[4];

        final int c0 = get16(input, offset);
        final int c1 = get16(input, offset + 2);

        clut[0] = decodeRgb565(c0);
        clut[1] = decodeRgb565(c1);
        clut[2] = calculateClut2(clut[0], clut[1], c0, c1, isBc1);
        clut[3] = calculateClut2(clut[0], clut[1], c0, c1, isBc1);

        final int indices = get32(input, offset + 4);

        int idxShift = 0;

        final byte[] output = new byte[4 * 4 * 4];

        int oOffset = 0;

        for (int ty = 0; ty < 4; ty++) {
            for (int tx = 0; tx < 4; tx++) {
                final int idx = (indices >> idxShift) & 3;

                idxShift += 2;

                final Color pixel = clut[idx];

                output[oOffset] = (byte)pixel.getBlue();
                output[oOffset + 1] = (byte)pixel.getGreen();
                output[oOffset + 2] = (byte)pixel.getRed();
                output[oOffset + 3] = (byte)pixel.getAlpha();

                oOffset += 4;
            }
        }
        return output;
    }

    public static int get16(byte[] data, int address) {
        return (data[address] & 0xff) | (data[address + 1] & 0xff) << 8;
    }

    public static int get32(byte[] data, int address) {
        return (data[address] & 0xff) |
               (data[address + 1] & 0xff) << 8 |
               (data[address + 2] & 0xff) << 16 |
               (data[address + 3] & 0xff) << 24;
    }

    private static Color decodeRgb565(int value) {
        final int b = (value & 0x1f) << 3;
        final int g = ((value >> 5) & 0x3f) << 2;
        final int r = ((value >> 11) & 0x1f) << 3;

        return new Color(
            r | (r >> 5),
            g | (g >> 6),
            b | (b >> 5)
        );
    }

    private static Color calculateClut2(Color c0Color, Color c1Color, int c0, int c1, boolean isBc1) {
        if (c0 > c1) {
            return new Color(
                (2 * c0Color.getRed() + c1Color.getRed()) / 3,
                (2 * c0Color.getGreen() + c1Color.getGreen()) / 3,
                (2 * c0Color.getBlue() + c1Color.getBlue()) / 3
            );
        } else {
            return new Color(
                (c0Color.getRed() + c1Color.getRed()) / 2,
                (c0Color.getGreen() + c1Color.getGreen()) / 2,
                (c0Color.getBlue() + c1Color.getBlue()) / 2
            );
        }
    }

    private static void calculateBc3Alpha(byte[] alpha) {
        final int a0 = alpha[0] & 0xff;
        final int a1 = alpha[1] & 0xff;
        if (Byte.compareUnsigned(alpha[0], alpha[1]) > 0) {
            for (int i = 2; i < 8; i++) {
                alpha[i] = (byte)(a0 + ((a1 - a0) * (i - 1)) / 7);
            }
        } else {
            for (int i = 2; i < 6; i++) {
                alpha[i] = (byte)(a0 + ((a1 - a0) * (i - 1)) / 5);
            }
            alpha[6] = 0;
            alpha[7] = (byte)255;
        }
    }
}
