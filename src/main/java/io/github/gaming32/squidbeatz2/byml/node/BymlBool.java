package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlBool extends BymlNode {
    public static final BymlBool TRUE = new BymlBool(true);
    public static final BymlBool FALSE = new BymlBool(false);

    private final boolean value;

    private BymlBool(boolean value) {
        this.value = value;
    }

    public static BymlBool valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override
    public int getType() {
        return BymlTypes.BOOL;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    public boolean booleanValue() {
        return value;
    }
}
