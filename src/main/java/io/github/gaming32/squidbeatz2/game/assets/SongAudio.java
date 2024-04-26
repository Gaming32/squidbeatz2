package io.github.gaming32.squidbeatz2.game.assets;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;

public record SongAudio(boolean loop, int loopStart, int loopEnd, byte[] compressedAudio) {
    public InputStream getInputStream() {
        return new InflaterInputStream(new ByteArrayInputStream(compressedAudio));
    }

    public AudioInputStream getAudioInputStream() throws UnsupportedAudioFileException, IOException {
        return AudioSystem.getAudioInputStream(new BufferedInputStream(getInputStream()));
    }
}
