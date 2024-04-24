package io.github.gaming32.squidbeatz2.util.seekable;

import java.io.ByteArrayInputStream;

public class SeekableByteArrayInputStream extends ByteArrayInputStream implements Seekable {
    private int start;

    public SeekableByteArrayInputStream(byte[] buf) {
        super(buf);
    }

    public SeekableByteArrayInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
        this.start = offset;
    }

    @Override
    public long tell() {
        return pos - start;
    }

    @Override
    public void seek(long absolute) {
        seek0((int)(absolute + start));
    }

    @Override
    public void seekBy(long by) {
        seek0((int)(pos + by));
    }

    private void seek0(int absolute) {
        pos = absolute < start ? start : Math.min(absolute, count);
    }
}
