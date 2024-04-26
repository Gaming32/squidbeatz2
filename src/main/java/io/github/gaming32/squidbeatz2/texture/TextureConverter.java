package io.github.gaming32.squidbeatz2.texture;

import io.github.gaming32.squidbeatz2.bntx.SurfaceFormat;
import io.github.gaming32.squidbeatz2.bntx.Texture;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.awt.image.BufferedImage;
import java.util.List;

public class TextureConverter {
    public static final Int2ObjectMap<FormatInfo> FORMATS = createFormats();

    public static BufferedImage toBufferedImage(Texture texture) {
        return toBufferedImage(texture, 0, 0, 0);
    }

    public static BufferedImage toBufferedImage(Texture texture, int targetArray, int targetMip, int targetDepth) {
        final int width = Math.max(1, texture.width >> targetMip);
        final int height = Math.max(1, texture.height >> targetMip);
        final byte[] data = getImageData(texture, targetArray, targetMip, targetDepth);
        if (texture.format == SurfaceFormat.R8_G8_B8_A8_UNORM) {
            return getBufferedImage(convertBgraToRgba(data), width, height);
        }

        switch (texture.format) {
            case SurfaceFormat.BC5_SNORM -> throw new UnsupportedOperationException("BC5_SNORM");
            case SurfaceFormat.ETC1_UNORM -> throw new UnsupportedOperationException("ETC1_UNORM");
            case SurfaceFormat.R5_G5_B5_A1_UNORM -> throw new UnsupportedOperationException("R5G5B5A1_UNORM");
        }

        BufferedImage result = decodeTex(data, width, height, texture.format);
        if (result != null && (result.getWidth() != width || result.getHeight() != height)) {
            result = result.getSubimage(0, 0, width, height);
        }
        return result;
    }

    private static BufferedImage decodeTex(byte[] data, int width, int height, int format) {
        return switch (format) {
            case SurfaceFormat.R8_G8_B8_A8_UNORM, SurfaceFormat.R8_G8_B8_A8_SRGB ->
                getBufferedImage(convertBgraToRgba(data), width, height);
            case SurfaceFormat.BC1_UNORM -> DDSCompressor.decompressBc1(data, width, height, false);
            case SurfaceFormat.BC1_SRGB -> DDSCompressor.decompressBc1(data, width, height, true);
            case SurfaceFormat.BC3_SRGB -> DDSCompressor.decompressBc3(data, width, height, false);
            case SurfaceFormat.BC3_UNORM -> DDSCompressor.decompressBc3(data, width, height, true);
            case SurfaceFormat.BC4_UNORM -> throw new UnsupportedOperationException("BC4_UNORM");
            case SurfaceFormat.BC4_SNORM -> throw new UnsupportedOperationException("BC4_SNORM");
            case SurfaceFormat.BC5_UNORM -> throw new UnsupportedOperationException("BC5_UNORM");
            case SurfaceFormat.BC7_UNORM -> throw new UnsupportedOperationException("BC7_UNORM");
            case SurfaceFormat.BC7_SRGB -> throw new UnsupportedOperationException("BC7_UNORM_SRGB");
            default -> null;
        };
    }

    static BufferedImage getBufferedImage(byte[] buffer, int width, int height) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int[] ints = new int[width * height];
        for (int i = 0, j = 0; i < ints.length; i++) {
            ints[i] = (buffer[j++] & 0xff) |
                      (buffer[j++] & 0xff) << 8 |
                      (buffer[j++] & 0xff) << 16 |
                      (buffer[j++] & 0xff) << 24;
        }
        image.setRGB(0, 0, width, height, ints, 0, width);
        return image;
    }

    public static byte[] convertBgraToRgba(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalStateException("Data block returned null. Make sure the parameters and image properties are correct!");
        }

        for (int i = 0; i < bytes.length; i += 4) {
            final byte temp = bytes[i];
            bytes[i] = bytes[i + 2];
            bytes[i + 2] = temp;
        }
        return bytes;
    }

    public static byte[] getImageData(Texture texture) {
        return getImageData(texture, 0, 0, 0);
    }

    public static byte[] getImageData(Texture texture, int targetArray, int targetMip, int targetDepth) {
        final FormatInfo formatInfo = FORMATS.get(texture.format);
        if (formatInfo == null) {
            throw new IllegalArgumentException("Unknown texture format 0x" + Integer.toHexString(texture.format));
        }

        final int blkWidth = formatInfo.blockWidth;
        final int blkHeight = formatInfo.blockHeight;
        final int blkDepth = formatInfo.blockDepth;

        final int linesPerBlockHeight = (1 << texture.blockHeightLog2) * 8;
        final int bpp = formatInfo.bytesPerPixel;

        final int numDepth = Math.max(texture.depth, 1);

        for (int depthLevel = 0; depthLevel < numDepth; depthLevel++) {
            for (int arrayLevel = 0; arrayLevel < texture.textureData.size(); arrayLevel++) {
                final List<byte[]> textureData = texture.textureData.get(arrayLevel);
                int blockHeightShift = 0;

                for (int mipLevel = 0; mipLevel < textureData.size(); mipLevel++) {
                    final int width = Math.max(1, texture.width >> mipLevel);
                    final int height = Math.max(1, texture.height >> mipLevel);
                    final int depth = Math.max(1, texture.depth >> mipLevel);

                    final int size = TegraX1Swizzle.divRoundUp(width, blkWidth) * TegraX1Swizzle.divRoundUp(height, blkHeight) * bpp;

                    if (TegraX1Swizzle.pow2RoundUp(TegraX1Swizzle.divRoundUp(height, blkWidth)) < linesPerBlockHeight) {
                        blockHeightShift++;
                    }

                    byte[] result = TegraX1Swizzle.deswizzle(
                        width, height, depth,
                        blkWidth, blkHeight, blkDepth,
                        1,
                        bpp,
                        texture.tileMode.ordinal(),
                        Math.max(0, texture.blockHeightLog2 - blockHeightShift),
                        textureData.get(mipLevel)
                    );
                    final byte[] result_ = new byte[size];
                    System.arraycopy(result, 0, result_, 0, size);

                    result = null;

                    if (arrayLevel == targetArray && mipLevel == targetMip && depthLevel == targetDepth) {
                        return result_;
                    }
                }
            }
        }

        return new byte[0];
    }

    private static Int2ObjectMap<FormatInfo> createFormats() {
        final var formats = new Int2ObjectOpenHashMap<FormatInfo>();
        formats.put(SurfaceFormat.R8_G8_B8_A8_SNORM, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R8_G8_B8_A8_UNORM, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R8_G8_B8_A8_SRGB, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.A1_B5_G5_R5_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.B5_G5_R5_A1_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R5_G5_B5_A1_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.B8_G8_R8_A8_UNORM, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.B8_G8_R8_A8_SRGB, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R10_G10_B10_A2_UNORM, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.A4_B4_G4_R4_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R4_G4_B4_A4_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R4_G4_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R8_G8_SNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R8_G8_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
//        formats.put(SurfaceFormat.R16_SINT, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
//        formats.put(SurfaceFormat.R16_SNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R16_UINT, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R16_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R8_UNORM, new FormatInfo(1, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R11_G11_B10_UINT, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.B5_G6_R5_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.R5_G6_B5_UNORM, new FormatInfo(2, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC1_UNORM, new FormatInfo(8, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC1_SRGB, new FormatInfo(8, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC2_UNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC2_SRGB, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC3_UNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC3_SRGB, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC4_UNORM, new FormatInfo(8, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC4_SNORM, new FormatInfo(8, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC5_UNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC5_SNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC6_FLOAT, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC6_UFLOAT, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC7_UNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.BC7_SRGB, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_4x4_UNORM, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_4x4_SRGB, new FormatInfo(16, 4, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_5x4_UNORM, new FormatInfo(16, 5, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_5x4_SRGB, new FormatInfo(16, 5, 4, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_5x5_UNORM, new FormatInfo(16, 5, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_5x5_SRGB, new FormatInfo(16, 5, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_6x5_UNORM, new FormatInfo(16, 6, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_6x5_SRGB, new FormatInfo(16, 6, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_6x6_UNORM, new FormatInfo(16, 6, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_6x6_SRGB, new FormatInfo(16, 6, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x5_UNORM, new FormatInfo(16, 8, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x5_SRGB, new FormatInfo(16, 8, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x6_UNORM, new FormatInfo(16, 8, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x6_SRGB, new FormatInfo(16, 8, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x8_UNORM, new FormatInfo(16, 8, 8, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_8x8_SRGB, new FormatInfo(16, 8, 8, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x5_UNORM, new FormatInfo(16, 10, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x5_SRGB, new FormatInfo(16, 10, 5, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x6_UNORM, new FormatInfo(16, 10, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x6_SRGB, new FormatInfo(16, 10, 6, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x8_UNORM, new FormatInfo(16, 10, 8, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x8_SRGB, new FormatInfo(16, 10, 8, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x10_UNORM, new FormatInfo(16, 10, 10, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_10x10_SRGB, new FormatInfo(16, 10, 10, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_12x10_UNORM, new FormatInfo(16, 12, 10, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_12x10_SRGB, new FormatInfo(16, 12, 10, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_12x12_UNORM, new FormatInfo(16, 12, 12, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ASTC_12x12_SRGB, new FormatInfo(16, 12, 12, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ETC1_UNORM, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.ETC1_SRGB, new FormatInfo(4, 1, 1, 1, TargetBuffer.COLOR));
        formats.put(SurfaceFormat.D32_FLOAT_S8X24_UINT, new FormatInfo(8, 1, 1, 1, TargetBuffer.DEPTH_STENCIL));
        formats.trim();
        return Int2ObjectMaps.unmodifiable(formats);
    }

    public record FormatInfo(
        int bytesPerPixel,
        int blockWidth,
        int blockHeight,
        int blockDepth,
        TargetBuffer targetBuffer
    ) {
    }

    public enum TargetBuffer {
        COLOR, DEPTH, STENCIL, DEPTH_STENCIL
    }
}
