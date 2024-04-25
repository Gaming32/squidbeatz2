package io.github.gaming32.squidbeatz2.bntx;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;

import java.io.IOException;
import java.io.InputStream;

public class RelocationTable {
    private static final String SIGNATURE = "_RLT";

    public int position;

    public <S extends InputStream & Seekable> void load(S is, boolean bigEndian) throws IOException {
        position = (int)is.tell();

        if (!Util.readString(is, SIGNATURE.length()).equals(SIGNATURE)) {
            throw new IOException("Invalid " + SIGNATURE + " magic");
        }
        final int pos = Util.readInt(is, bigEndian);
        final int sectionCount = Util.readInt(is, bigEndian);
        is.seekBy(4);
    }
}
