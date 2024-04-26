package io.github.gaming32.squidbeatz2.bars;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;

public record Amta(String name, float volume, float loudness) {
    public static Amta read(InputStream is) throws IOException {
        if (!Util.readString(is, 4).equals("AMTA")) {
            throw new IOException("Invalid AMTA magic");
        }
        final int bom = Util.readShort(is, true);
        final boolean bigEndian = switch (bom) {
            case 0xFEFF -> true;
            case 0xFFFE -> false;
            default -> throw new IOException("Invalid AMTA BOM: 0x" + Integer.toHexString(bom));
        };
        final int version = Util.readShort(is, bigEndian);
        is.skipNBytes(4); // Size
        return switch (version) {
            case 0x0400 -> readV4(is, bigEndian);
            default -> throw new IOException("Unsupported AMTA version: 0x" + Integer.toHexString(version));
        };
    }

    private static Amta readV4(InputStream is, boolean bigEndian) throws IOException {
        final int dataOffset = Util.readInt(is, bigEndian);
        is.skipNBytes(8);
        final int nameOffset = Util.readInt(is, bigEndian);
        is.skipNBytes(dataOffset - 28); // 28 bytes have been read already
        is.skipNBytes(
            4 + // Magic
            4 + // Section size
            4 + // Name offset
            4 + // Unknown
            1 + // Type
            1 + // Channel count
            1 + // Used stream count
            1   // Flags
        );
        final float volume = Float.intBitsToFloat(Util.readInt(is, bigEndian));
        is.skipNBytes(
            4 + // Sample rate
            4 + // Loop start
            4   // Loop end
        );
        final float loudness = Float.intBitsToFloat(Util.readInt(is, bigEndian));
        is.skipNBytes(8 * 7); // 7 8-byte track fields
        is.skipNBytes(4);
        is.skipNBytes(nameOffset + 8 - dataOffset - 100); // 100 bytes since the dataOffset seek
        final String name = readNullTerminatedString(is);
        return new Amta(name, volume, loudness);
    }

    private static String readNullTerminatedString(InputStream is) throws IOException {
        final StringBuilder result = new StringBuilder();
        while (true) {
            final int c = is.read();
            if (c <= 0) break;
            result.appendCodePoint(c);
        }
        return result.toString();
    }
}
