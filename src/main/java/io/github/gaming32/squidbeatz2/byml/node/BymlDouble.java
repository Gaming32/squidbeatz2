package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlDouble extends BymlNumber {
    public static final BymlDouble ZERO = new BymlDouble(0.0);

    private final double value;

    private BymlDouble(double value) {
        this.value = value;
    }

    public static BymlDouble valueOf(double value) {
        return value == 0.0 ? ZERO : new BymlDouble(value);
    }

    @Override
    public int getType() {
        return BymlTypes.DOUBLE;
    }

    @Override
    public String toString() {
        return Double.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlDouble d && d.value == value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(value);
    }

    @Override
    public Double toNumber() {
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
        return (float)value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
}
