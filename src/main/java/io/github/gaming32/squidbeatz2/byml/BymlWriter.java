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
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntRBTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BymlWriter {
    private static final short MAGIC = ('B' << 8) | 'Y';

    private ByteBuffer buffer = ByteBuffer.allocate(4);
    private final BymlCollection root;
    private final int version;
    private final Object2IntSortedMap<String> hashKeyTable = new Object2IntRBTreeMap<>();
    private final Object2IntSortedMap<String> stringTable = new Object2IntRBTreeMap<>();
    private final Reference2IntMap<BymlCollection> hashCodeCache = new Reference2IntOpenHashMap<>();

    private BymlWriter(BymlCollection root, ByteOrder order, int version) {
        this.root = root;
        this.version = version;
        this.buffer.order(order);

        makeStringTables(root, hashKeyTable, stringTable);
        tagStringTable(hashKeyTable);
        tagStringTable(stringTable);
    }

    private static void makeStringTables(
        BymlNode data,
        Object2IntSortedMap<String> hashKeyTable,
        Object2IntSortedMap<String> stringTable
    ) {
        switch (data) {
            case BymlString s -> stringTable.put(s.toString(), 0);
            case BymlArray array -> {
                for (final BymlNode value : array) {
                    makeStringTables(value, hashKeyTable, stringTable);
                }
            }
            case BymlHash hash -> {
                for (final var entry : hash.entrySet()) {
                    hashKeyTable.put(entry.getKey(), 0);
                    makeStringTables(entry.getValue(), hashKeyTable, stringTable);
                }
            }
            default -> {
                // Nothing to do, has no strings
            }
        }
    }

    private static void tagStringTable(Object2IntSortedMap<String> table) {
        int i = 0;
        for (final var entry : table.object2IntEntrySet()) {
            entry.setValue(i++);
        }
    }

    public static void write(BymlCollection data, SeekableByteChannel output) throws IOException {
        write(data, output, ByteOrder.LITTLE_ENDIAN, 2);
    }

    public static void write(BymlCollection data, SeekableByteChannel output, ByteOrder order) throws IOException {
        write(data, output, order, 2);
    }

    public static void write(BymlCollection data, SeekableByteChannel output, int version) throws IOException {
        write(data, output, ByteOrder.LITTLE_ENDIAN, version);
    }

    public static void write(BymlCollection data, SeekableByteChannel output, ByteOrder order, int version) throws IOException {
        if (version < 1 || version > 7) {
            throw new IllegalArgumentException("Invalid version: " + version + " (expected 1-7)");
        }
        if (version == 1 && order == ByteOrder.BIG_ENDIAN) {
            throw new IllegalArgumentException("Invalid version: " + version + "-wiiu (expected 1-3)");
        }
        new BymlWriter(data, order, version).write(output);
    }

    private void write(SeekableByteChannel output) throws IOException {
        write(output, buffer.putShort(MAGIC).putShort((short)version));

        final PlaceholderOffsetWriter hashKeyTableOffsetWriter;
        if (!hashKeyTable.isEmpty()) {
            hashKeyTableOffsetWriter = writePlaceholderOffset(output);
        } else {
            write(output, buffer.putInt(0));
            hashKeyTableOffsetWriter = null;
        }

        final PlaceholderOffsetWriter stringTableOffsetWriter;
        if (!stringTable.isEmpty()) {
            stringTableOffsetWriter = writePlaceholderOffset(output);
        } else {
            write(output, buffer.putInt(0));
            stringTableOffsetWriter = null;
        }

        final PlaceholderOffsetWriter rootOffsetWriter = writePlaceholderOffset(output);

        if (hashKeyTableOffsetWriter != null) {
            hashKeyTableOffsetWriter.writeCurrentOffset(0);
            writeStringTable(output, hashKeyTable);
            align(output);
        }

        if (stringTableOffsetWriter != null) {
            stringTableOffsetWriter.writeCurrentOffset(0);
            writeStringTable(output, stringTable);
            align(output);
        }

        final Object2IntMap<BymlNode> nodeToOffset = new Object2IntOpenCustomHashMap<>(new Hash.Strategy<>() {
            @Override
            public int hashCode(BymlNode o) {
                return BymlWriter.this.hashCode(o);
            }

            @Override
            public boolean equals(BymlNode a, BymlNode b) {
                return Objects.equals(a, b);
            }
        });
        nodeToOffset.defaultReturnValue(-1);

        rootOffsetWriter.writeCurrentOffset(0);
        writeNonValueNode(output, root, nodeToOffset);
        align(output);
    }

    private int hashCode(BymlNode node) {
        if (!(node instanceof BymlCollection collection)) {
            return node.hashCode();
        }
        int h = hashCodeCache.getInt(collection);
        if (h == 0) {
            switch (collection) {
                case BymlArray array -> {
                    for (final BymlNode item : array) {
                        h = 31 * h + hashCode(item);
                    }
                }
                case BymlHash hash -> {
                    for (final var entry : hash.entrySet()) {
                        h = 31 * h + (entry.getKey().hashCode() ^ hashCode(entry.getValue()));
                    }
                }
            }
            if (h == 0) {
                h = 1;
            }
            hashCodeCache.put(collection, h);
        }
        return h;
    }

    private void writeNonValueNode(SeekableByteChannel output, BymlNode node, Object2IntMap<BymlNode> nodeToOffset) throws IOException {
        final List<Map.Entry<BymlNode, PlaceholderOffsetWriter>> nonValueNodes = new ArrayList<>();
        switch (node) {
            case BymlArray array -> {
                final ByteBuffer buffer = buffer(4 + array.size());
                write24BitSkip8(buffer, BymlTypes.ARRAY, array.size());
                for (final BymlNode item : array) {
                    buffer.put((byte)item.getType());
                }
                write(output, buffer);
                align(output);
                for (final BymlNode item : array) {
                    if (isValueType(item.getType())) {
                        writeValueNode(output, item);
                    } else {
                        nonValueNodes.add(Map.entry(item, writePlaceholderOffset(output)));
                    }
                }
            }
            case BymlHash hash -> {
                write(output, write24BitSkip8(buffer, BymlTypes.HASH, hash.size()));
                for (final var entry : hash.entrySet()) {
                    write(output, write24Bit8Bit(buffer, hashKeyTable.getInt(entry.getKey()), entry.getValue().getType()));
                    if (isValueType(entry.getValue().getType())) {
                        writeValueNode(output, entry.getValue());
                    } else {
                        nonValueNodes.add(Map.entry(entry.getValue(), writePlaceholderOffset(output)));
                    }
                }
            }
            case BymlBinary binary -> {
                final ByteString value = binary.toByteString();
                final ByteBuffer buffer = buffer(4 + value.size()).putInt(value.size());
                value.copyTo(buffer);
                write(output, buffer);
            }
            case BymlUInt64 u -> write(output, buffer(8).putLong(u.longValue()));
            case BymlInt64 i -> write(output, buffer(8).putLong(i.longValue()));
            case BymlDouble d -> write(output, buffer(8).putDouble(d.doubleValue()));
            default -> throw new IllegalArgumentException("Is a value node: " + node);
        }
        for (final var entry : nonValueNodes) {
            final int originalOffset = nodeToOffset.getInt(entry.getKey());
            if (originalOffset != -1) {
                entry.getValue().writeOffset(originalOffset, 0);
            } else {
                entry.getValue().writeCurrentOffset(0);
                nodeToOffset.put(entry.getKey(), (int)output.position());
                writeNonValueNode(output, entry.getKey(), nodeToOffset);
            }
        }
    }

    private void writeValueNode(SeekableByteChannel output, BymlNode node) throws IOException {
        switch (node) {
            case BymlString s -> write(output, buffer.putInt(stringTable.getInt(s.toString())));
            case BymlBool b -> write(output, buffer.putInt(b.booleanValue() ? 1 : 0));
            case BymlInt i -> write(output, buffer.putInt(i.intValue()));
            case BymlUInt u -> write(output, buffer.putInt(u.intValue()));
            case BymlFloat f -> write(output, buffer.putFloat(f.floatValue()));
            case BymlNull ignored -> write(output, buffer.putInt(0));
            default -> throw new IllegalArgumentException("Not a value node: " + node);
        }
    }

    private void writeStringTable(SeekableByteChannel output, Object2IntSortedMap<String> table) throws IOException {
        final int base = (int)output.position();
        write(output, write24BitSkip8(buffer, BymlTypes.STRING_TABLE, table.size()));
        final PlaceholderOffsetWriter[] offsetWriters = new PlaceholderOffsetWriter[table.size()];
        for (int i = 0; i < offsetWriters.length; i++) {
            offsetWriters[i] = writePlaceholderOffset(output);
        }
        final PlaceholderOffsetWriter lastOffsetWriter = writePlaceholderOffset(output);

        int i = 0;
        for (final String key : table.keySet()) {
            offsetWriters[i++].writeCurrentOffset(base);
            final ByteBuffer stringBuf = StandardCharsets.UTF_8.encode(key);
            while (stringBuf.hasRemaining()) {
                output.write(stringBuf);
            }
            write(output, buffer.put((byte)0));
        }
        lastOffsetWriter.writeCurrentOffset(base);
    }

    private PlaceholderOffsetWriter writePlaceholderOffset(SeekableByteChannel output) throws IOException {
        final PlaceholderOffsetWriter writer = new PlaceholderOffsetWriter(output);
        writer.writePlaceholder();
        return writer;
    }

    private ByteBuffer buffer(int cap) {
        assert cap > 4;
        ByteBuffer buffer = this.buffer;
        if (buffer.capacity() < cap) {
            return this.buffer = ByteBuffer.allocate(cap).order(buffer.order());
        }
        return buffer;
    }

    private void align(SeekableByteChannel output) throws IOException {
        // Some channels don't support seeking past the end properly (looking at you, ZipFileSystem), so we can just
        // write zeros from the current buffer
        final int startPos = (int)output.position();
        final int endPos = BymlReader.align(startPos);
        if (endPos > startPos) {
            // Put an int 0 to zero out the important part of the buffer
            write(output, buffer.putInt(0).position(endPos - startPos));
        }
    }

    private static ByteBuffer write24BitSkip8(ByteBuffer buffer, int eight, int twentyFour) {
        final int value;
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            value = (eight << 24) | twentyFour;
        } else {
            value = eight | (twentyFour << 8);
        }
        return buffer.putInt(value);
    }

    private static ByteBuffer write24Bit8Bit(ByteBuffer buffer, int twentyFour, int eight) {
        final int value;
        if (buffer.order() == ByteOrder.BIG_ENDIAN) {
            value = (twentyFour << 8) | eight;
        } else {
            value = (eight << 24) | twentyFour;
        }
        return buffer.putInt(value);
    }

    private static void write(WritableByteChannel output, ByteBuffer buffer) throws IOException {
        buffer.flip();
        while (buffer.hasRemaining()) {
            output.write(buffer);
        }
        buffer.clear();
    }

    private static boolean isValueType(int type) {
        return type == BymlTypes.STRING || type == BymlTypes.NULL || (type >= BymlTypes.BOOL && type <= BymlTypes.UINT);
    }

    private class PlaceholderOffsetWriter {
        private final SeekableByteChannel output;
        private final int offset;

        PlaceholderOffsetWriter(SeekableByteChannel output) throws IOException {
            this.output = output;
            this.offset = (int)output.position();
        }

        void writePlaceholder() throws IOException {
            write(output, buffer.putInt(-1));
        }

        void writeOffset(int offset, int base) throws IOException {
            final int currentOffset = (int)output.position();
            output.position(this.offset);
            write(output, buffer.putInt(offset - base));
            output.position(currentOffset);
        }

        void writeCurrentOffset(int base) throws IOException {
            writeOffset((int)output.position(), base);
        }
    }
}
