package io.github.gaming32.squidbeatz2.font;

import io.github.gaming32.squidbeatz2.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

// BFTTF files are literally just xor encrypted TTF files
public class BFTTFInputStream extends FilterInputStream {
    private final int key;
    private int offset;

    public BFTTFInputStream(InputStream delegate) throws IOException {
        super(delegate);
        key = switch (Util.readInt(delegate, false)) {
            case 0x1A879BD9 -> 0xA6018502;
            case 0x1E1AF836 -> 0x49621806;
            case 0xC1DE68F3 -> 0x8CF2DCD9;
            default -> throw new IOException("Not a BFTTF/BFOTF file");
        };
        delegate.skipNBytes(4);
    }

    private int decrypt(int x) {
        return x ^ (key >>> (offset = (offset - 8) & 31));
    }

    @Override
    public int read() throws IOException {
        final int value = in.read();
        return value == -1 ? value : decrypt(value);
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        final int read = in.read(b, off, len);
        if (read > 0) {
            for (int i = off, end = off + read; i < end; i++) {
                b[i] = (byte)decrypt(b[i] & 0xff);
            }
        }
        return read;
    }
}
