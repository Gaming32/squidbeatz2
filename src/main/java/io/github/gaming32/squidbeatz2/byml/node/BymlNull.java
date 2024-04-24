package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlNull extends BymlNode {
    public static final BymlNull INSTANCE = new BymlNull();

    private BymlNull() {
    }

    @Override
    public int getType() {
        return BymlTypes.NULL;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
