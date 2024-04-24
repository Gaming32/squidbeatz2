package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.Common;

import java.io.IOException;
import java.io.InputStream;

public class PrefetchData {
    public int startSample;
    public int size;
    public int sampleCount;
    public Reference audioData;

    public static  <S extends InputStream & Seekable> PrefetchData readPrefetchData(S is, boolean bigEndian, StreamInfo info) throws IOException {
        final int baseOffset = (int)is.tell();
        final PrefetchData pdat = new PrefetchData();

        pdat.startSample = Util.readInt(is, bigEndian);
        pdat.size = Util.readInt(is, bigEndian);
        pdat.sampleCount = Common.bytesToSamples(pdat.size / info.channelCount, info.codec);
        is.seekBy(4);
        pdat.audioData = new Reference(is, bigEndian, baseOffset);
        return pdat;
    }
}
