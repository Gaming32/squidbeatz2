package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

public final class BymlString extends BymlNode {
    public static final BymlString EMPTY = new BymlString("");

    private final String value;

    private BymlString(String value) {
        this.value = value;
    }

    public static BymlString valueOf(String value) {
        return value.isEmpty() ? EMPTY : new BymlString(value);
    }

    @Override
    public int getType() {
        return BymlTypes.STRING;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlString s && s.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
