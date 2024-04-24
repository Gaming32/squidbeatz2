package io.github.gaming32.squidbeatz2.byml;

import io.github.gaming32.squidbeatz2.byml.node.BymlNode;

public final class BymlTypes {
    public static final int STRING = 0xA0;
    public static final int BINARY = 0xA1;
    public static final int ARRAY = 0xC0;
    public static final int HASH = 0xC1;
    /**
     * @apiNote This will never be returned from {@link BymlNode#getType}.
     */
    public static final int STRING_TABLE = 0xC2;
    public static final int BOOL = 0xD0;
    public static final int INT = 0xD1;
    public static final int FLOAT = 0xD2;
    public static final int UINT = 0xD3;
    public static final int INT64 = 0xD4;
    public static final int UINT64 = 0xD5;
    public static final int DOUBLE = 0xD6;
    public static final int NULL = 0xff;

    private BymlTypes() {
    }
}
