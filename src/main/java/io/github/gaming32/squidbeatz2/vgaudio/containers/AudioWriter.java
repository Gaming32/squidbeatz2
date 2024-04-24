package io.github.gaming32.squidbeatz2.vgaudio.containers;

import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class AudioWriter {
    protected AudioData audioStream;

    public byte[] getFile(IAudioFormat audio) {
        return getByteArray(new AudioData(audio));
    }

    public void writeToStream(IAudioFormat audio, OutputStream stream) throws IOException {
        writeStream(new AudioData(audio), stream);
    }

    public byte[] getFile(AudioData audio) {
        return getByteArray(audio);
    }

    public void writeToStream(AudioData audio, OutputStream stream) throws IOException {
        writeStream(audio, stream);
    }

    protected abstract int getFileSize();

    protected abstract void setupWriter(AudioData audio);

    protected abstract void writeStream(OutputStream os) throws IOException;

    private byte[] getByteArray(AudioData audio) {
        setupWriter(audio);

        final int fileSize = getFileSize();
        final ByteArrayOutputStream stream = fileSize != -1
            ? new ByteArrayOutputStream(fileSize)
            : new ByteArrayOutputStream();

        try {
            writeStream(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return stream.toByteArray();
    }

    private void writeStream(AudioData audio, OutputStream stream) throws IOException {
        setupWriter(audio);
        writeStream(stream);
    }
}
