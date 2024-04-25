package io.github.gaming32.squidbeatz2.util.seekable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class SeekableFileInputStream extends InputStream implements Seekable {
    private final RandomAccessFile raf;

    public SeekableFileInputStream(RandomAccessFile raf) {
        this.raf = raf;
    }

    public SeekableFileInputStream(File file) throws FileNotFoundException {
        this(new RandomAccessFile(file, "r"));
    }

    public SeekableFileInputStream(String file) throws FileNotFoundException {
        this(new RandomAccessFile(file, "r"));
    }

    @Override
    public int read() throws IOException {
        return raf.read();
    }

    @Override
    public int read(byte @NotNull [] b) throws IOException {
        return raf.read(b);
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        return raf.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        if (n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE) {
            return raf.skipBytes((int)n);
        }
        return super.skip(n);
    }

    @Override
    public long tell() throws IOException {
        return raf.getFilePointer();
    }

    @Override
    public void seek(long absolute) throws IOException {
        raf.seek(absolute);
    }
}
