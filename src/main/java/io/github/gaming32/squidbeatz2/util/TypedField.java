package io.github.gaming32.squidbeatz2.util;

import org.apache.commons.lang3.exception.UncheckedReflectiveOperationException;

import java.lang.reflect.Field;

public class TypedField<T> {
    private final Field field;
    private final Class<T> type;

    public TypedField(Field field, Class<T> type) {
        this.field = field;
        this.type = type;
        if (!type.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException(field + " is not of " + type);
        }
    }

    public T getStatic() {
        return get(null);
    }

    public T get(Object owner) {
        try {
            return type.cast(field.get(owner));
        } catch (ReflectiveOperationException e) {
            throw new UncheckedReflectiveOperationException(e);
        }
    }

    public void setStatic(T value) {
        set(null, value);
    }

    public void set(Object owner, T value) {
        try {
            field.set(owner, value);
        } catch (ReflectiveOperationException e) {
            throw new UncheckedReflectiveOperationException(e);
        }
    }
}
