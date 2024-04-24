package io.github.gaming32.squidbeatz2.util.seekable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

public class SeekableChannelInputStream extends InputStream implements Seekable {
    private final SeekableByteChannel channel;
    private final ByteBuffer read1Byte = ByteBuffer.allocate(1);

    public SeekableChannelInputStream(SeekableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public int read() throws IOException {
        while (true) {
            read1Byte.clear();
            switch (channel.read(read1Byte)) {
                case -1 -> {
                    return -1;
                }
                case 1 -> {
                    read1Byte.flip();
                    return read1Byte.get() & 0xff;
                }
            }
        }
    }

    @Override
    public int read(byte @NotNull [] b, int off, int len) throws IOException {
        return channel.read(ByteBuffer.wrap(b, off, len));
    }

    @Override
    public long skip(long n) throws IOException {
        final long size = channel.size();
        final long position = channel.position();
        n = Math.min(n, size - position);
        channel.position(position + n);
        return n;
    }

    @Override
    public long tell() throws IOException {
        return channel.position();
    }

    @Override
    public void seek(long absolute) throws IOException {
        channel.position(absolute);
    }
}
