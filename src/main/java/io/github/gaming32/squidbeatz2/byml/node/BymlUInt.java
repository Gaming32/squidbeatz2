package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlUInt extends BymlNumber {
    public static final BymlUInt ZERO = new BymlUInt(0);

    private final int value;

    private BymlUInt(int value) {
        this.value = value;
    }

    public static BymlUInt valueOf(int value) {
        return value == 0 ? ZERO : new BymlUInt(value);
    }

    public static BymlUInt valueOf(long value) {
        if (value == 0L) {
            return ZERO;
        }
        if (value < 0L || value > (1L << 32) - 1) {
            throw new IllegalArgumentException("Long value out of range for UInt");
        }
        return new BymlUInt((int)value);
    }

    @Override
    public int getType() {
        return BymlTypes.UINT;
    }

    @Override
    public String toString() {
        return Integer.toUnsignedString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlUInt u && u.value == value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public Long toNumber() {
        return Integer.toUnsignedLong(value);
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return Integer.toUnsignedLong(value);
    }

    @Override
    public float floatValue() {
        return Integer.toUnsignedLong(value);
    }

    @Override
    public double doubleValue() {
        return Integer.toUnsignedLong(value);
    }
}
