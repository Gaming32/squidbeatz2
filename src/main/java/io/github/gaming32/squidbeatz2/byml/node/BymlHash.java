package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

public final class BymlHash extends BymlCollection {
    private final NavigableMap<String, BymlNode> delegate;

    public BymlHash(Map<String, BymlNode> value) {
        this.delegate = new TreeMap<>(value);
    }

    public BymlHash() {
        this.delegate = new TreeMap<>();
    }

    @Override
    public int getType() {
        return BymlTypes.HASH;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlHash h && h.delegate.equals(delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public NavigableMap<String, BymlNode> asMap() {
        return delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    public BymlNode get(String key) {
        return delegate.get(key);
    }

    public BymlNode put(String key, BymlNode value) {
        return delegate.put(key, value);
    }

    public NavigableSet<String> keySet() {
        return delegate.navigableKeySet();
    }

    public Set<Map.Entry<String, BymlNode>> entrySet() {
        return delegate.entrySet();
    }
}
