package io.github.gaming32.squidbeatz2.lightbfres;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BfresExternalFilesLoader {
    public static <S extends InputStream & Seekable> Map<String, byte[]> loadExternalFiles(S is) throws IOException {
        if (!Util.readString(is, 4).equals("FRES")) {
            throw new IOException("Illegal FRES magic");
        }
        is.skipNBytes(4);

        final int version = Util.readInt(is, false);
        final int versionMajor = version >>> 24;
        final int versionMajor2 = (version >> 16) & 0xff;
        final int versionMinor = (version >> 8) & 0xff;
        final int versionMinor2 = version & 0xff;

//        final boolean bigEndian = Util.readShort(is, true) == 0xFEFF;
//        final int alignment = is.read();
//        final int targetAddressSize = is.read();
//        final int offsetToFileName = Util.readInt(is, false);
//        final int flag = Util.readShort(is, false);
//        final int blockOffset = Util.readShort(is, false);
//        final int relocationTableOffset = Util.readInt(is, false);
//        final int sizFile = Util.readInt(is, false);
        is.skipNBytes(2 + 1 + 1 + 4 + 2 + 2 + 4 + 4);
        final String name = loadString(is);
//        final long modelOffset = Util.readLong(is, false);
//        final long modelDictOffset = Util.readLong(is, false);
        is.skipNBytes(8 + 8);
        if (versionMajor2 >= 9) {
            is.skipNBytes(32);
        }
        skipDictValues(is, 5); // Animation-related fields
        is.skipNBytes(16); // Memory pool, buffer info

        if (versionMajor2 >= 10) {
            throw new IOException("Versions with flags unsupported");
        }

        return loadMap(is);
    }

    private static <S extends InputStream & Seekable> String loadString(S is) throws IOException {
        final long offset = Util.readLong(is, false);
        if (offset == 0) {
            return null;
        }

        final long oldAddr = is.tell();
        is.seek(offset);
        try {
            return readString(is);
        } finally {
            is.seek(oldAddr);
        }
    }

    private static String readString(InputStream is) throws IOException {
        final int size = Util.readShort(is, false);
        return Util.readString(is, size);
    }

    private static void skipDictValues(InputStream is, int fields) throws IOException {
        is.skipNBytes(16L * fields); // 8 bytes for values address, 8 bytes for keys address
    }

    private static <S extends InputStream & Seekable> Map<String, byte[]> loadMap(S is) throws IOException {
        final ByteBuffer bytes = ByteBuffer.wrap(is.readNBytes(16))
            .order(ByteOrder.LITTLE_ENDIAN);
        final long valuesOffset = bytes.getLong();
        final long keysOffset = bytes.getLong();
        if (keysOffset == 0 || valuesOffset == 0) {
            return Map.of();
        }

        final long oldOffset = is.tell();
        is.seek(keysOffset);
        try {
            final List<String> keys = loadKeys(is);
            final List<byte[]> values = loadValues(is, keys.size(), valuesOffset);
            final Map<String, byte[]> result = LinkedHashMap.newLinkedHashMap(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                result.put(keys.get(i), values.get(i));
            }
            return result;
        } finally {
            is.seek(oldOffset);
        }
    }

    private static <S extends InputStream & Seekable> List<String> loadKeys(S is) throws IOException {
        is.skipNBytes(4);
        final int count = Util.readInt(is, false);
        is.skipNBytes(16); // Root node
        if (count == 0) {
            return List.of();
        }
        final List<String> keys = new ArrayList<>(count);
        final ByteBuffer data = ByteBuffer.wrap(is.readNBytes(16 * count))
            .order(ByteOrder.LITTLE_ENDIAN);
        final long oldOffset = is.tell();
        for (int i = 0; i < count; i++) {
            data.position(data.position() + 8);
            is.seek(data.getLong());
            keys.add(readString(is));
        }
        is.seek(oldOffset);
        return keys;
    }

    private static <S extends InputStream & Seekable> List<byte[]> loadValues(S is, int count, long offset) throws IOException {
        if (count == 0 || offset == 0) {
            return List.of();
        }
        final List<byte[]> values = new ArrayList<>(count);
        final long oldOffset = is.tell();
        is.seek(offset);
        final ByteBuffer data = ByteBuffer.wrap(is.readNBytes(16 * count))
            .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) {
            final long dataOffset = data.getLong();
            final long dataSize = data.getLong();
            is.seek(dataOffset);
            values.add(is.readNBytes((int)dataSize));
        }
        is.seek(oldOffset);
        return values;
    }
}
