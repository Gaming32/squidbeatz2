package io.github.gaming32.squidbeatz2.byml.node;

public abstract sealed class BymlCollection extends BymlNode permits BymlArray, BymlHash {
    public abstract int size();
}
