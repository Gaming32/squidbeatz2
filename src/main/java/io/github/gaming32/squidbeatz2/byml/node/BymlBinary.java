package io.github.gaming32.squidbeatz2.byml.node;

import com.google.protobuf.ByteString;
import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlBinary extends BymlNode {
    public static final BymlBinary EMPTY = new BymlBinary(ByteString.EMPTY);

    private final ByteString value;

    private BymlBinary(ByteString value) {
        this.value = value;
    }

    public static BymlBinary valueOf(ByteString value) {
        return value.isEmpty() ? EMPTY : new BymlBinary(value);
    }

    public static BymlBinary valueOf(byte[] value) {
        return value.length == 0 ? EMPTY : new BymlBinary(ByteString.copyFrom(value));
    }

    @Override
    public int getType() {
        return BymlTypes.BINARY;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlBinary b && b.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public ByteString toByteString() {
        return value;
    }
}
