package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;

public final class BymlArray extends BymlCollection implements Iterable<BymlNode> {
    private final List<BymlNode> delegate;

    public BymlArray(Collection<BymlNode> value) {
        this.delegate = new ArrayList<>(value);
    }

    public BymlArray() {
        this.delegate = new ArrayList<>();
    }

    @Override
    public int getType() {
        return BymlTypes.ARRAY;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlArray a && a.delegate.equals(delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public List<BymlNode> asList() {
        return delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    public BymlNode get(int index) {
        return delegate.get(index);
    }

    public BymlNode set(int index, BymlNode element) {
        return delegate.set(index, element);
    }

    public boolean add(BymlNode element) {
        return delegate.add(element);
    }

    @NotNull
    @Override
    public Iterator<BymlNode> iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator<BymlNode> spliterator() {
        return delegate.spliterator(); // Retain delegate's characteristics
    }
}
