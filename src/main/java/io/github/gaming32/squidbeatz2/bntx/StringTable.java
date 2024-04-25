package io.github.gaming32.squidbeatz2.bntx;

import io.github.gaming32.squidbeatz2.util.Util;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;

import java.io.IOException;
import java.io.InputStream;

public class StringTable {
    public final Long2ObjectSortedMap<String> strings = new Long2ObjectAVLTreeMap<>();

    public void load(InputStream is, boolean bigEndian) throws IOException {
        if (!Util.readString(is, 4).equals("_STR")) {
            throw new IOException("Invalid _STR magic");
        }
        is.skipNBytes(16); // blockOffset, blockSize, stringCount
    }
}
