package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public record SongAudio(boolean loop, int loopStart, int loopEnd, short[] monoSamples, byte[] compressedWave) {
    public SongAudio(Pcm16Format pcm16, byte[] compressedWave) {
        this(pcm16.isLooping(), pcm16.getLoopStart(), pcm16.getLoopEnd(), toMono(pcm16.channels), compressedWave);
    }

    public InputStream getInputStream() {
        return new InflaterInputStream(new ByteArrayInputStream(compressedWave));
    }

    public AudioInputStream getAudioInputStream() throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(new BufferedInputStream(getInputStream()));
    }

    private static short[] toMono(short[][] input) {
        final int channels = input.length;
        if (channels == 1) {
            return input[0];
        }
        final int samples = input[0].length;
        final short[] output = new short[samples];
        for (int i = 0; i < samples; i++) {
            int sum = input[0][i];
            for (int j = 1; j < channels; j++) {
                sum += input[j][i];
            }
            output[i] = (short)(sum / channels);
        }
        return output;
    }
}
