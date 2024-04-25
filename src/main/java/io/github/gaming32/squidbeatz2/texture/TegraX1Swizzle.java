package io.github.gaming32.squidbeatz2.texture;

import com.sun.jna.Native;
import com.sun.jna.Structure;

public class TegraX1Swizzle {
    static {
        Native.register("tegra_swizzle");
    }

    public static byte[] deswizzle(
        int width, int height, int depth,
        int blkWidth, int blkHeight, int blkDepth,
        int roundPitch,
        int bpp,
        int tileMode,
        int blockHeightLog2,
        byte[] data
    ) {
        if (tileMode == 1) {
            return swizzlePitchLinear(
                width, height, depth, blkWidth, blkHeight, blkDepth, roundPitch, bpp, blockHeightLog2, data, true, 0
            );
        } else {
            return swizzleDeswizzleBlockLinear(
                width, height, depth, blkWidth, blkHeight, blkDepth, bpp, blockHeightLog2, data, true, 0, 512
            );
        }
    }

    private static byte[] swizzlePitchLinear(
        int width, int height, int depth,
        int blkWidth, int blkHeight, int blkDepth,
        int roundPitch,
        int bpp,
        int blockHeightLog2,
        byte[] data,
        boolean deswizzle,
        int size
    ) {
        width = divRoundUp(width, blkWidth);
        height = divRoundUp(height, blkHeight);
        depth = divRoundUp(depth, blkDepth);

        int pitch;
        int surfSize;

        pitch = width * bpp;

        if (roundPitch == 1) {
            pitch = roundUp(pitch, 32);
        }

        surfSize = pitch * height;

        final byte[] result = new byte[surfSize];

        for (int z = 0; z < depth; z++) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int pos;
                    final int pos_;

                    pos = y * pitch + x * bpp;

                    pos_ = (y * width + x) * bpp;

                    if (pos + bpp <= surfSize) {
                        if (!deswizzle) {
                            System.arraycopy(data, pos, result, pos_, bpp);
                        } else {
                            System.arraycopy(data, pos_, result, pos, bpp);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static byte[] swizzleDeswizzleBlockLinear(
        int width, int height, int depth,
        int blkWidth, int blkHeight, int blkDepth,
        int bpp,
        int blockHeightLog2,
        byte[] data,
        boolean deswizzle,
        int size,
        int alignment
    ) {
        final long blockHeightMip0 = 1L << Math.max(Math.min(blockHeightLog2, 5), 0);

        if (deswizzle) {
            width = divRoundUp(width, blkWidth);
            height = divRoundUp(height, blkHeight);
            depth = divRoundUp(depth, blkDepth);

            final byte[] output = new byte[width * height * depth * bpp];

            deswizzleBlockLinear(width, height, depth, bpp, data, (int)blockHeightMip0, output);

            return output;
        } else {
            final BlockDim blockDim = new BlockDim();
            blockDim.width = blkWidth;
            blockDim.height = blkHeight;
            blockDim.depth = blkDepth;

            final long surfaceSize = swizzledSurfaceSize(width, height, depth, blockDim, (int)blockHeightMip0, bpp, 1, 1);
            final byte[] output = new byte[(int)surfaceSize];

            width = divRoundUp(width, blkWidth);
            height = divRoundUp(height, blkHeight);
            depth = divRoundUp(depth, blkDepth);

            swizzleBlockLinear(width, height, depth, bpp, data, (int)blockHeightMip0, output);

            return output;
        }
    }

    public static int divRoundUp(int n, int d) {
        return (n + d - 1) / d;
    }

    public static int roundUp(int x, int y) {
        return ((x - 1) | (y - 1)) + 1;
    }

    public static int pow2RoundUp(int x) {
        x--;
        x |= x >>> 1;
        x |= x >>> 2;
        x |= x >>> 4;
        x |= x >>> 8;
        x |= x >>> 16;
        return x + 1;
    }

    private static void deswizzleBlockLinear(int width, int height, int depth, int bpp, byte[] data, int blockHeightMip0, byte[] output) {
        deswizzle_block_linear(width, height, depth, data, data.length, output, output.length, blockHeightMip0, bpp);
    }

    private static long swizzledSurfaceSize(
        int width, int height, int depth,
        BlockDim blockDim,
        int blockHeightMip0,
        int bytesPerPixel,
        int mipmapCount,
        int arrayCount
    ) {
        return swizzled_surface_size(width, height, depth, blockDim, blockHeightMip0, bytesPerPixel, mipmapCount, arrayCount);
    }

    private static void swizzleBlockLinear(int width, int height, int depth, int bpp, byte[] data, int blockHeightMip0, byte[] output) {
        swizzle_block_linear(width, height, depth, data, data.length, output, output.length, blockHeightMip0, bpp);
    }

    private static native void deswizzle_block_linear(
        long width, long height, long depth,
        byte[] source, long sourceLength,
        byte[] destination, long destinationLength,
        long blockHeight,
        long bytesPerPixel
    );

    private static native long swizzled_surface_size(
        long width, long height, long depth,
        BlockDim blockDim,
        long blockHeightMip0,
        long bytesPerPixel,
        long mipmapCount,
        long arrayCount
    );

    private static native void swizzle_block_linear(
        long width, long height, long depth,
        byte[] source, long sourceLength,
        byte[] destination, long destinationLength,
        long blockHeight,
        long bytesPerPixel
    );

    private static class BlockDim extends Structure {
        public long width;
        public long height;
        public long depth;
    }
}
