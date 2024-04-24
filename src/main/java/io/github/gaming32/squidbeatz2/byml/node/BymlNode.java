package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;
import org.intellij.lang.annotations.MagicConstant;

import java.io.Serializable;

public abstract sealed class BymlNode implements Serializable permits BymlBinary, BymlBool, BymlCollection, BymlNull, BymlNumber, BymlString {
    protected BymlNode() {
    }

    @MagicConstant(valuesFromClass = BymlTypes.class)
    public abstract int getType();

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();
}
