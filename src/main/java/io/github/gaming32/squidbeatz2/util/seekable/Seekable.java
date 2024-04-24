package io.github.gaming32.squidbeatz2.util.seekable;

import java.io.IOException;

public interface Seekable {
    long tell() throws IOException;

    void seek(long absolute) throws IOException;

    default void seekBy(long by) throws IOException {
        seek(tell() + by);
    }
}
