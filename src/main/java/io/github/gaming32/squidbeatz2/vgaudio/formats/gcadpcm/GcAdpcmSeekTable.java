package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

public record GcAdpcmSeekTable(int samplesPerEntry, short[] seekTable, boolean isSelfCalculated) {
    public GcAdpcmSeekTable(short[] seekTable, int samplesPerEntry, boolean isSelfCalculated) {
        this(samplesPerEntry, seekTable, isSelfCalculated);
    }

    public GcAdpcmSeekTable(short[] pcm, int samplesPerEntry) {
        this(createSeekTable(pcm, samplesPerEntry), samplesPerEntry, true);
    }

    private static short[] createSeekTable(short[] pcm, int samplesPerEntry) {
        final int entryCount = (pcm.length - 1 + samplesPerEntry) / samplesPerEntry;
        final short[] seekTable = new short[entryCount * 2];

        for (int i = 1; i < entryCount; i++) {
            seekTable[i * 2] = pcm[i * samplesPerEntry - 1];
            seekTable[i * 2 + 1] = pcm[i * samplesPerEntry - 2];
        }

        return seekTable;
    }
}
