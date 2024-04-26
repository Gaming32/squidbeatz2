package io.github.gaming32.squidbeatz2.bars;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class BarsReader {
    public static <S extends InputStream & Seekable> Map<String, Amta> readAmtaEntries(S is) throws IOException {
        if (!Util.readString(is, 4).equals("BARS")) {
            throw new IOException("Invalid BARS magic");
        }
        is.seekBy(4); // File size
        final int bom = Util.readShort(is, true);
        final boolean bigEndian = switch (bom) {
            case 0xFEFF -> true;
            case 0xFFFE -> false;
            default -> throw new IOException("Invalid BARS BOM: 0x" + Integer.toHexString(bom));
        };
        is.seekBy(2); // Version
        final int fileCount = Util.readInt(is, bigEndian);
        is.seekBy(4L * fileCount); // CRC entries
        final Map<String, Amta> result = LinkedHashMap.newLinkedHashMap(fileCount);
        for (int i = 0; i < fileCount; i++) {
            final int amtaOffset = Util.readInt(is, bigEndian);
            final long currentOffset = is.tell();
            is.seek(amtaOffset);
            final Amta amta = Amta.read(is);
            is.seek(currentOffset + 4);
            result.put(amta.name(), amta);
        }
        return result;
    }
}
