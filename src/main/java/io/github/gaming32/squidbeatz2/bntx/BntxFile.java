package io.github.gaming32.squidbeatz2.bntx;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import org.apache.commons.io.function.IOSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BntxFile {
    private static final String SIGNATURE = "BNTX";

    public String name;
    public int versionMajor;
    public int versionMajor2;
    public int versionMinor;
    public int versionMinor2;
    public boolean bigEndian;
    public int alignment;
    public int targetAddressSize;
    public int flag;
    public int blockOffset;
    public RelocationTable relocationTable;
    public String target;
    public List<Texture> textures;
    // No textureDict
    public StringTable stringTable;

    public String getVersionFull() {
        return versionMajor + "." + versionMajor2 + '.' + versionMinor + '.' + versionMinor2;
    }

    public int getDataAlignment() {
        return 1 << alignment;
    }

    public <S extends InputStream & Seekable> BntxFile load(S is) throws IOException {
        stringTable = new StringTable();

        if (!Util.readString(is, SIGNATURE.length()).equals(SIGNATURE)) {
            throw new IOException("Invalid " + SIGNATURE + " magic");
        }
        is.seekBy(4);
        final int version = Util.readInt(is, false);
        setVersionInfo(version);
        this.bigEndian = Util.readShort(is, true) == 0xFEFF;
        alignment = is.read();
        targetAddressSize = is.read();
        final int offsetToFileName = Util.readInt(is, false);
        flag = Util.readShort(is, false);
        blockOffset = Util.readShort(is, false);
        is.seekBy(8); // relocationTableOffset, sizFile
        target = Util.readString(is, 4);
        final int textureCount = Util.readInt(is, false);
        final long textureArrayOffset = Util.readLong(is, false);

        textures = loadCustom(is, () -> {
            final List<Texture> texList = new ArrayList<>(textureCount);
            for (int i = 0; i < textureCount; i++) {
                texList.add(loadCustom(is, false, () -> new Texture().load(is, false)));
            }
            return texList;
        }, textureArrayOffset);
        is.seekBy(16); // textureData, textureDict
        name = loadString(is, false, offsetToFileName - 2);

        is.seek(textureArrayOffset + textureCount * 8L);

        stringTable.load(is, false);

        return this;
    }

    private void setVersionInfo(int version) {
        versionMajor = version >>> 24;
        versionMajor2 = version >> 16 & 0xff;
        versionMinor = version >> 8 & 0xff;
        versionMinor2 = version & 0xff;
    }

    static <S extends InputStream & Seekable> String loadString(S is, boolean bigEndian, long offset) throws IOException {
        if (offset == 0) {
            offset = Util.readLong(is, bigEndian);
        }
        final long oldOffset = is.tell();
        is.seek(offset);
        try {
            return Util.readString(is, Util.readShort(is, bigEndian));
        } finally {
            is.seek(oldOffset);
        }
    }

    static <S extends InputStream & Seekable, T> T loadCustom(S is, boolean bigEndian, IOSupplier<T> callback) throws IOException {
        return loadCustom(is, callback, Util.readLong(is, bigEndian));
    }

    static <S extends InputStream & Seekable, T> T loadCustom(S is, IOSupplier<T> callback, long offset) throws IOException {
        final long oldOffset = is.tell();
        is.seek(offset);
        try {
            return callback.get();
        } finally {
            is.seek(oldOffset);
        }
    }
}
