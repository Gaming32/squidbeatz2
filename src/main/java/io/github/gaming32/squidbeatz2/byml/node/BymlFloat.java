package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlFloat extends BymlNumber {
    public static final BymlFloat ZERO = new BymlFloat(0f);

    private final float value;

    private BymlFloat(float value) {
        this.value = value;
    }

    public static BymlFloat valueOf(float value) {
        return value == 0f ? ZERO : new BymlFloat(value);
    }

    @Override
    public int getType() {
        return BymlTypes.FLOAT;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlFloat f && f.value == value;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(value);
    }

    @Override
    public Float toNumber() {
        return value;
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return (long)value;
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
