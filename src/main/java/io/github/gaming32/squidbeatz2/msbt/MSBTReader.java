package io.github.gaming32.squidbeatz2.msbt;

import io.github.gaming32.squidbeatz2.util.Util;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class MSBTReader {
    public static Map<String, String> readMsbt(InputStream is) throws IOException {
        final String magic = Util.readString(is, 8);
        if (!magic.equals("MsgStdBn")) {
            throw new IOException("Invalid MSBT magic: " + magic);
        }
        final int bom = Util.readShort(is, true);
        final boolean bigEndian = switch (bom) {
            case 0xFEFF -> true;
            case 0xFFFE -> false;
            default -> throw new IOException("Invalid SARC BOM: 0x" + Integer.toHexString(bom));
        };
        is.skipNBytes(4);
        final int sectionCount = Util.readShort(is, bigEndian);
        is.skipNBytes(2);
        final int fileSize = Util.readInt(is, bigEndian);
        is.skipNBytes(10);

        LabelsSection labelsSection = null;
        TextsSection textsSection = null;

        int totalRead = 32;
        for (int i = 0; i < sectionCount; i++) {
            final String sectionKey = Util.readString(is, 4);
            final int sectionSize = Util.readInt(is, bigEndian);
            is.skipNBytes(8);
            @SuppressWarnings("deprecation") // The builder is a massive object that really doesn't need initializing
            final BoundedInputStream boundedInput = new BoundedInputStream(is, sectionSize);
            switch (sectionKey) {
                case "LBL1" -> labelsSection = new LabelsSection(boundedInput, bigEndian);
                case "TXT2" -> textsSection = new TextsSection(boundedInput, bigEndian);
                default -> {}
            }
            is.skipNBytes(boundedInput.getRemaining());
            totalRead += 16 + sectionSize;
            if ((sectionSize & 15) != 0) {
                final int padding = 16 - (sectionSize & 15);
                is.skipNBytes(padding);
                totalRead += padding;
            }
        }

        if (labelsSection == null) {
            throw new IOException("Missing LBL1 in MSBT file");
        }
        if (textsSection == null) {
            throw new IOException("Missing TXT2 in MSBT file");
        }

        is.skipNBytes(fileSize - totalRead);
        final String[] strings = textsSection.strings;
        return labelsSection.stringRefs
            .object2IntEntrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(
                Object2IntMap.Entry::getKey,
                e -> strings[e.getIntValue()]
            ));
    }

    private static class LabelsSection {
        final Object2IntMap<String> stringRefs;

        LabelsSection(InputStream is, boolean bigEndian) throws IOException {
            final int groupCount = Util.readInt(is, bigEndian);
            record GroupInfo(int count, int offset) {
            }
            final int offsetShift = 4 + groupCount * 8;
            final GroupInfo[] groups = new GroupInfo[groupCount];
            int stringsEstimate = 0;
            for (int i = 0; i < groupCount; i++) {
                groups[i] = new GroupInfo(Util.readInt(is, bigEndian), Util.readInt(is, bigEndian));
                stringsEstimate += groups[i].count;
            }

            stringRefs = new Object2IntOpenHashMap<>(stringsEstimate);
            final byte[] stringData = is.readAllBytes();
            for (final GroupInfo group : groups) {
                int offset = group.offset - offsetShift;
                for (int i = 0; i < group.count; i++) {
                    final int length = stringData[offset++] & 0xff;
                    final String key = new String(stringData, offset, length, StandardCharsets.ISO_8859_1);
                    offset += length;
                    int index = stringData[offset++] |
                                ((stringData[offset++] & 0xff) << 8) |
                                ((stringData[offset++] & 0xff) << 16) |
                                ((stringData[offset++] & 0xff) << 24);
                    if (bigEndian) {
                        index = Integer.reverseBytes(index);
                    }
                    stringRefs.put(key, index);
                }
            }
        }
    }

    private static class TextsSection {
        String[] strings;

        TextsSection(InputStream is, boolean bigEndian) throws IOException {
            final int count = Util.readInt(is, bigEndian);
            final int offsetShift = 4 + count * 4;
            final int[] offsets = new int[count];
            for (int i = 0; i < count; i++) {
                offsets[i] = Util.readInt(is, bigEndian);
            }

            strings = new String[count];
            final byte[] stringData = is.readAllBytes();
            final Charset charset = bigEndian ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
            for (int i = 0; i < count; i++) {
                final int start = offsets[i] - offsetShift;
                final int end = i + 1 < count ? offsets[i + 1] - offsetShift : stringData.length;
                final String value = strings[i] = new String(stringData, start, end - start, charset);
                // Strip null terminator if it's present
                if (value.charAt(value.length() - 1) == '\0') {
                    strings[i] = value.substring(0, value.length() - 1);
                }
            }
        }
    }
}
