package io.github.gaming32.squidbeatz2.vgaudio.containers.wave;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.vgaudio.containers.AudioWriter;
import io.github.gaming32.squidbeatz2.vgaudio.formats.AudioData;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import io.github.gaming32.squidbeatz2.vgaudio.utilities.riff.MediaSubtypes;
import io.github.gaming32.squidbeatz2.vgaudio.utilities.riff.WaveFormatTags;
import org.apache.commons.io.output.CountingOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class WaveWriter extends AudioWriter {
    private Pcm16Format pcm16;
    private IAudioFormat audioFormat;

    private int getChannelCount() {
        return audioFormat.getChannelCount();
    }

    private int getSampleCount() {
        return audioFormat.getSampleCount();
    }

    private int getSampleRate() {
        return audioFormat.getSampleRate();
    }

    private boolean isLooping() {
        return audioFormat.isLooping();
    }

    private int getLoopStart() {
        return audioFormat.getLoopStart();
    }

    private int getLoopEnd() {
        return audioFormat.getLoopEnd();
    }

    @Override
    protected int getFileSize() {
        return 8 + getRiffChunkSize();
    }

    private int getRiffChunkSize() {
        return 4 + 8 + getFmtChunkSize() + 8 + getDataChunkSize() + (isLooping() ? 8 + getSmplChunkSize() : 0);
    }

    private int getFmtChunkSize() {
        return getChannelCount() > 2 ? 40 : 16;
    }

    private int getDataChunkSize() {
        return getChannelCount() * getSampleCount() * getBytesPerSample();
    }

    private int getSmplChunkSize() {
        return 0x3c;
    }

    private int getBitDepth() {
        return 16;
    }

    private int getBytesPerSample() {
        return (getBitDepth() + 7) / 8;
    }

    private int getBytesPerSecond() {
        return getSampleRate() * getBytesPerSample() * getChannelCount();
    }

    private int getBlockAlign() {
        return getBytesPerSample() * getChannelCount();
    }

    @Override
    protected void setupWriter(AudioData audio) {
        pcm16 = audio.getFormat(Pcm16Format.class);
        audioFormat = pcm16;
    }

    @Override
    protected void writeStream(OutputStream os) throws IOException {
        final CountingOutputStream cos = new CountingOutputStream(os);
        writeRiffHeader(cos);
        writeFmtChunk(cos);
        if (isLooping()) {
            writeSmplChunk(cos);
        }
        writeDataChunk(cos);
    }

    private void writeRiffHeader(OutputStream os) throws IOException {
        Util.writeString(os, "RIFF");
        Util.writeInt(os, getRiffChunkSize(), false);
        Util.writeString(os, "WAVE");
    }

    private void writeFmtChunk(CountingOutputStream os) throws IOException {
        align(os);
        Util.writeString(os, "fmt ");
        Util.writeInt(os, getFmtChunkSize(), false);
        Util.writeShort(os, getChannelCount() > 2 ? WaveFormatTags.WAVE_FORMAT_EXTENSIBLE : WaveFormatTags.WAVE_FORMAT_PCM, false);
        Util.writeShort(os, getChannelCount(), false);
        Util.writeInt(os, getSampleRate(), false);
        Util.writeInt(os, getBytesPerSecond(), false);
        Util.writeShort(os, getBlockAlign(), false);
        Util.writeShort(os, getBitDepth(), false);

        if (getChannelCount() > 2) {
            Util.writeShort(os, 22, false);
            Util.writeShort(os, getBitDepth(), false);
            Util.writeInt(os, getChannelMask(getChannelCount()), false);
            os.write(MediaSubtypes.MEDIA_SUBTYPE_PCM);
        }
    }

    private void writeSmplChunk(CountingOutputStream os) throws IOException {
        final byte[] empty = new byte[28];
        align(os);
        Util.writeString(os, "smpl");
        Util.writeInt(os, getSmplChunkSize(), false);
        os.write(empty, 0, 7 * 4);
        Util.writeInt(os, 1, false);
        os.write(empty, 0, 3 * 4);
        Util.writeInt(os, getLoopStart(), false);
        Util.writeInt(os, getLoopEnd(), false);
        os.write(empty, 0, 2 * 4);
    }

    private void writeDataChunk(CountingOutputStream os) throws IOException {
        align(os);
        Util.writeString(os, "data");
        Util.writeInt(os, getDataChunkSize(), false);

        final byte[] audioData = Util.shortToInterleavedByte(pcm16.channels);
        os.write(audioData);
    }

    private static int getChannelMask(int channelCount) {
        return switch (channelCount) {
            case 4 -> 0x0033;
            case 5 -> 0x0133;
            case 6 -> 0x0633;
            case 7 -> 0x01f3;
            case 8 -> 0x06f3;
            default -> (1 << channelCount) - 1;
        };
    }

    private static void align(CountingOutputStream os) throws IOException {
        if ((os.getByteCount() & 1) != 0) {
            os.write(0);
        }
    }
}
