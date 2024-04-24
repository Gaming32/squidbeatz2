package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlInt extends BymlNumber {
    public static final BymlInt ZERO = new BymlInt(0);

    private final int value;

    private BymlInt(int value) {
        this.value = value;
    }

    public static BymlInt valueOf(int value) {
        return value == 0 ? ZERO : new BymlInt(value);
    }

    @Override
    public int getType() {
        return BymlTypes.INT;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlInt i && i.value == value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public Integer toNumber() {
        return value;
    }

    @Override
    public int intValue() {
        return value;
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
