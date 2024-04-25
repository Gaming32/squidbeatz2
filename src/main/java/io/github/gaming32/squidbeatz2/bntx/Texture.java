package io.github.gaming32.squidbeatz2.bntx;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Texture {
    private static final String SIGNATURE = "BRTI";

    public ChannelType channelRed;
    public ChannelType channelGreen;
    public ChannelType channelBlue;
    public ChannelType channelAlpha;
    public int width;
    public int height;
    public int mipCount;
    public int format;
    public String name;
    public String path;
    public int depth;
    public TileMode tileMode;
    public int swizzle;
    public int alignment;
    public int pitch;
    public Dim dim;
    public SurfaceDim surfaceDim;
    public long[] mipOffsets;
    public List<List<byte[]>> textureData;
    public int textureLayout;
    public int textureLayout2;
    public int accessFlags;
    public int[] regs;
    public int arrayLength;
    public int flags;
    public int imageSize;
    public int sampleCount;
    // No userDataDict
    // No userData
    public int readTextureLayout;
    public int sparseBinding;
    public int sparseResidency;
    public int blockHeightLog2;

    public boolean isUseSRGB() {
        final int dataType = format & 0xff;
        return dataType == 0x06;
    }

    public void setUseSRGB(boolean value) {
        final int format = (this.format >> 8) & 0xff;
        if (value) {
            this.format = format << 8 | 0x06;
        } else {
            this.format = format << 8 | 0x01;
        }
    }

    public <S extends InputStream & Seekable> Texture load(S is, boolean bigEndian) throws IOException {
        if (!Util.readString(is, SIGNATURE.length()).equals(SIGNATURE)) {
            throw new IOException("Invalid " + SIGNATURE + " magic");
        }
        is.seekBy(12);
        flags = is.read();
        dim = Dim.values()[is.read()];
        tileMode = TileMode.values()[Util.readShort(is, bigEndian)];
        swizzle = Util.readShort(is, bigEndian);
        mipCount = Util.readShort(is, bigEndian);
        sampleCount = Util.readInt(is, bigEndian);
        format = Util.readInt(is, bigEndian);

        accessFlags = Util.readInt(is, bigEndian);
        width = Util.readInt(is, bigEndian);
        height = Util.readInt(is, bigEndian);
        depth = Util.readInt(is, bigEndian);
        arrayLength = Util.readInt(is, bigEndian);
        textureLayout = Util.readInt(is, bigEndian);
        textureLayout2 = Util.readInt(is, bigEndian);
        is.seekBy(20);
        imageSize = Util.readInt(is, bigEndian);

        if (imageSize == 0) {
            throw new IOException("Empty image size!");
        }

        alignment = Util.readInt(is, bigEndian);
        final int channelType = Util.readInt(is, bigEndian);
        surfaceDim = SurfaceDim.values()[Util.readInt(is, bigEndian)];
        name = BntxFile.loadString(is, bigEndian, 0);
        final ByteBuffer offsetsBuffer = ByteBuffer.wrap(is.readNBytes(48))
            .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        final long parentOffset = offsetsBuffer.getLong();
        final long ptrOffset = offsetsBuffer.getLong();
        final long userDataOffset = offsetsBuffer.getLong();
        final long texPtr = offsetsBuffer.getLong();
        final long texView = offsetsBuffer.getLong();
        final long descSlotDataOffset = offsetsBuffer.getLong();
        is.seekBy(8); // No userDataDict

        // No userData
        mipOffsets = BntxFile.loadCustom(is, () -> Util.readLongArray(is, bigEndian, mipCount), ptrOffset);

        channelRed = ChannelType.values()[channelType & 0xff];
        channelGreen = ChannelType.values()[channelType >> 8 & 0xff];
        channelBlue = ChannelType.values()[channelType >> 16 & 0xff];
        channelAlpha = ChannelType.values()[channelType >>> 24 & 0xff];
        textureData = new ArrayList<>(arrayLength);

        readTextureLayout = flags & 1;
        sparseBinding = flags >> 1;
        sparseResidency = flags >> 2;
        blockHeightLog2 = textureLayout & 7;

        int arrayOffset = 0;
        final long oldOffset = is.tell();
        for (int a = 0; a < arrayLength; a++) {
            final List<byte[]> mips = new ArrayList<>(mipCount);
            for (int i = 0; i < mipCount; i++) {
                final int size = (int)((mipOffsets[0] + imageSize - mipOffsets[i]) / arrayLength);
                is.seek(arrayOffset + mipOffsets[i]);
                mips.add(is.readNBytes(size));
                if (mips.get(i).length == 0) {
                    throw new IOException(
                        "Empty mip size!" +
                        " Texture " + name +
                        " image size " + imageSize +
                        " mips level " + i +
                        " size " + size +
                        " array length " + arrayLength
                    );
                }
            }
            textureData.add(mips);

            arrayOffset += mips.getFirst().length;
        }
        is.seek(oldOffset);

        int mip = 0;
        final long startMip = mipOffsets[0];
        for (final long offset : mipOffsets) {
            mipOffsets[mip++] = offset - startMip;
        }

        return this;
    }
}
