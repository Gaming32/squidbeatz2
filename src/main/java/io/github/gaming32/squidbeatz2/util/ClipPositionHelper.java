package io.github.gaming32.squidbeatz2.util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;

public class ClipPositionHelper {
    private final Clip clip;
    private boolean looping;
    private int loopStart, loopEnd;

    public ClipPositionHelper(Clip clip) {
        this.clip = clip;
        loopEnd = clip.getFrameLength() - 1;
    }

    public void loop(int loopStart, int loopEnd) {
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
        clip.setLoopPoints(loopStart, loopEnd);
        loop();
    }

    public void loop() {
        looping = true;
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public long getFramePosition() {
        long pos = clip.getFramePosition();
        if (!looping) {
            return pos;
        }
        while (pos >= loopEnd) {
            pos = pos - loopEnd + loopStart;
        }
        return pos;
    }

    public long getMicrosecondPosition() {
        return framesToMicros(clip.getFormat(), getFramePosition());
    }

    public static long framesToMicros(AudioFormat format, long frames) {
        return (long)(((double)frames) / format.getFrameRate() * 1000000.0d);
    }
}
