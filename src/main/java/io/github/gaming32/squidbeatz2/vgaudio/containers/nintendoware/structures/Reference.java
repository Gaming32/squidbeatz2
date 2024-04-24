package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Reference {
    private final int type;
    private final int offset;
    private final int baseOffset;

    public Reference() {
        type = 0;
        offset = 0;
        baseOffset = 0;
    }

    public Reference(InputStream is, boolean bigEndian) throws IOException {
        this(is, bigEndian, 0);
    }

    public Reference(InputStream is, boolean bigEndian, int baseOffset) throws IOException {
        type = Util.readShort(is, bigEndian);
        is.skipNBytes(2);
        offset = Util.readInt(is, bigEndian);
        this.baseOffset = baseOffset;
    }

    public int getAbsoluteOffset() {
        return baseOffset + offset;
    }

    public int getReferenceType() {
        return type >> 8;
    }

    public int getDataType() {
        return type & 0xff;
    }

    public boolean isType(int type) {
        return this.type == type && offset > 0;
    }

    public int type() {
        return type;
    }

    public int offset() {
        return offset;
    }

    public int baseOffset() {
        return baseOffset;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Reference)obj;
        return this.type == that.type &&
               this.offset == that.offset &&
               this.baseOffset == that.baseOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, offset, baseOffset);
    }

    @Override
    public String toString() {
        return "Reference[" +
               "type=" + type + ", " +
               "offset=" + offset + ", " +
               "baseOffset=" + baseOffset + ']';
    }

}
