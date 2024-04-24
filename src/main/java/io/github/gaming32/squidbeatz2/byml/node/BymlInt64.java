package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlInt64 extends BymlNumber {
    public static final BymlInt64 ZERO = new BymlInt64(0L);

    private final long value;

    private BymlInt64(long value) {
        this.value = value;
    }

    public static BymlInt64 valueOf(long value) {
        return value == 0L ? ZERO : new BymlInt64(value);
    }

    @Override
    public int getType() {
        return BymlTypes.INT64;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlInt64 i && i.value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public Long toNumber() {
        return value;
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
