package io.github.gaming32.squidbeatz2.vgaudio.containers;

import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableByteArrayInputStream;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;

import java.io.IOException;
import java.io.InputStream;

public abstract class AudioReader<T> {
    protected abstract <S extends InputStream & Seekable> T readFile(S is) throws IOException;
    protected abstract IAudioFormat toAudioStream(T structure);

    public AudioData read(byte[] data) {
        try (SeekableByteArrayInputStream input = new SeekableByteArrayInputStream(data)) {
            return read(input);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public <S extends InputStream & Seekable> AudioData read(S is) throws IOException {
        return new AudioData(toAudioStream(readFile(is)));
    }
}
