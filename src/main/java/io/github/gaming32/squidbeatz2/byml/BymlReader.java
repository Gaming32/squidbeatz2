package io.github.gaming32.squidbeatz2.byml;

import com.google.protobuf.ByteString;
import io.github.gaming32.squidbeatz2.byml.node.BymlArray;
import io.github.gaming32.squidbeatz2.byml.node.BymlBinary;
import io.github.gaming32.squidbeatz2.byml.node.BymlBool;
import io.github.gaming32.squidbeatz2.byml.node.BymlCollection;
import io.github.gaming32.squidbeatz2.byml.node.BymlDouble;
import io.github.gaming32.squidbeatz2.byml.node.BymlFloat;
import io.github.gaming32.squidbeatz2.byml.node.BymlHash;
import io.github.gaming32.squidbeatz2.byml.node.BymlInt;
import io.github.gaming32.squidbeatz2.byml.node.BymlInt64;
import io.github.gaming32.squidbeatz2.byml.node.BymlNode;
import io.github.gaming32.squidbeatz2.byml.node.BymlNull;
import io.github.gaming32.squidbeatz2.byml.node.BymlString;
import io.github.gaming32.squidbeatz2.byml.node.BymlUInt;
import io.github.gaming32.squidbeatz2.byml.node.BymlUInt64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public final class BymlReader {
    private final ByteBuffer buffer;
    private final int version;
    private final List<String> hashKeyTable;
    private final List<BymlString> stringTable;

    private BymlReader(byte[] data) {
        buffer = ByteBuffer.wrap(data);

        final boolean isValid = switch (buffer.get()) {
            case 'B' -> {
                buffer.order(ByteOrder.BIG_ENDIAN);
                yield buffer.get() == 'Y';
            }
            case 'Y' -> {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                yield buffer.get() == 'B';
            }
            default -> false;
        };
        if (!isValid) {
            throw new BymlFormatException(
                "Invalid magic: " +
                    new String(data, 0, 2, StandardCharsets.ISO_8859_1) +
                    " (expected 'BY' or 'YB')"
            );
        }

        version = buffer.getShort();
        hashKeyTable = parseStringTable(buffer, Function.identity());
        stringTable = parseStringTable(buffer, BymlString::valueOf);
    }

    private static <T> List<T> parseStringTable(ByteBuffer buffer, Function<String, T> converter) {
        final int offset = buffer.getInt();
        if (offset == 0) {
            return null;
        }
        final int beforeRead = buffer.position();
        buffer.position(offset);
        if (buffer.get() != (byte)BymlTypes.STRING_TABLE) {
            throw new BymlFormatException(
                "Invalid note type: 0x" +
                    Integer.toHexString(buffer.get(buffer.position() - 1) & 0xff) +
                    " (expected 0xC2)"
            );
        }
        final int length = read24Bit(buffer);
        final List<T> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            final int stringOffset = offset + buffer.getInt();
            result.add(converter.apply(readString(buffer, stringOffset)));
        }
        buffer.position(beforeRead);
        return result;
    }

    private static String readString(ByteBuffer buffer, int offset) {
        final int beforeRead = buffer.position();
        try {
            buffer.position(offset);
            while (buffer.get() != 0) {
                // Skip to end of string
            }
            // We always own the ByteBuffer while reading, so we know that there's always a backing array with no offset
            return new String(buffer.array(), offset, buffer.position() - offset - 1, StandardCharsets.UTF_8);
        } finally {
            buffer.position(beforeRead);
        }
    }

    private static int read24Bit(ByteBuffer buffer) {
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            return ((buffer.get() & 0xff) << 16) | (buffer.getShort() & 0xffff);
        } else {
            return (buffer.get() & 0xff) | ((buffer.getShort() & 0xffff) << 8);
        }
    }

    private static int read24BitSkip8(ByteBuffer buffer) {
        final int result = buffer.getInt();
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            return result & 0xffffff;
        } else {
            return result >>> 8;
        }
    }

    public static BymlCollection read(byte[] data) throws BymlFormatException {
        if (data.length < 2) {
            throw new BymlFormatException("Byml data is too small!");
        }
        return new BymlReader(data).readRoot();
    }

    private BymlCollection readRoot() {
        final int nodeType = buffer.get(buffer.getInt(buffer.position())) & 0xff;
        if (!isContainerType(nodeType)) {
            throw new BymlFormatException("Invalid root node: expected array or hash, got type 0x" + Integer.toHexString(nodeType));
        }
        return (BymlCollection)readNode(nodeType);
    }

    private BymlNode readNode(int nodeType) {
        return switch (nodeType) {
            case BymlTypes.STRING -> readString();
            case BymlTypes.BINARY -> readWithOffset(BymlReader::readBinary);
            case BymlTypes.ARRAY -> readWithOffset(BymlReader::readArray);
            case BymlTypes.HASH -> readWithOffset(BymlReader::readHash);
            case BymlTypes.BOOL -> readBool();
            case BymlTypes.INT -> readInt();
            case BymlTypes.FLOAT -> readFloat();
            case BymlTypes.UINT -> readUInt();
            case BymlTypes.INT64 -> readWithOffset(BymlReader::readInt64);
            case BymlTypes.UINT64 -> readWithOffset(BymlReader::readUInt64);
            case BymlTypes.DOUBLE -> readWithOffset(BymlReader::readDouble);
            case BymlTypes.NULL -> BymlNull.INSTANCE;
            default -> throw new BymlFormatException("Unknown node type: 0x" + Integer.toHexString(nodeType));
        };
    }

    private BymlNode readWithOffset(int nodeType, int offset) {
        final int oldPosition = buffer.position();
        buffer.position(offset);
        try {
            return readNode(nodeType);
        } finally {
            buffer.position(oldPosition);
        }
    }

    private <T extends BymlNode> T readWithOffset(Function<BymlReader, T> reader) {
        final int offset = buffer.getInt();
        final int oldPosition = buffer.position();
        buffer.position(offset);
        try {
            return reader.apply(this);
        } finally {
            buffer.position(oldPosition);
        }
    }

    private BymlString readString() {
        if (stringTable == null) {
            throw new BymlFormatException("Cannot read string node with missing stringTable");
        }
        return stringTable.get(buffer.getInt());
    }

    private BymlBinary readBinary() {
        final int size = buffer.getInt();
        return BymlBinary.valueOf(ByteString.copyFrom(buffer, size));
    }

    private BymlArray readArray() {
        final int size = read24BitSkip8(buffer);
        final List<BymlNode> result = new ArrayList<>(size);
        final int valuesOffset = buffer.position() + align(size);
        for (int i = 0; i < size; i++) {
            final int type = buffer.get() & 0xff;
            result.add(readWithOffset(type, valuesOffset + 4 * i));
        }
        return new BymlArray(result);
    }

    private BymlHash readHash() {
        final int size = read24BitSkip8(buffer);
        if (size > 0 && hashKeyTable == null) {
            throw new BymlFormatException("Cannot read hash node with missing hashKeyTable");
        }
        final Map<String, BymlNode> result = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            final String key = hashKeyTable.get(read24Bit(buffer));
            final int nodeType = buffer.get() & 0xff;
            result.put(key, readNode(nodeType));
        }
        return new BymlHash(result);
    }

    private BymlBool readBool() {
        return BymlBool.valueOf(buffer.getInt() != 0);
    }

    private BymlInt readInt() {
        return BymlInt.valueOf(buffer.getInt());
    }

    private BymlFloat readFloat() {
        return BymlFloat.valueOf(buffer.getFloat());
    }

    private BymlUInt readUInt() {
        return BymlUInt.valueOf(buffer.getInt());
    }

    private BymlInt64 readInt64() {
        return BymlInt64.valueOf(buffer.getLong());
    }

    private BymlUInt64 readUInt64() {
        return BymlUInt64.valueOf(buffer.getLong());
    }

    private BymlDouble readDouble() {
        return BymlDouble.valueOf(buffer.getDouble());
    }

    private static boolean isContainerType(int type) {
        return type == BymlTypes.ARRAY || type == BymlTypes.HASH;
    }

    static int align(int value) {
        return value + ((4 - (value & 3)) & 3);
    }
}
